#!/usr/bin/env python3
"""초기 종목 CSV(docs/data/)로 Flyway 시드 마이그레이션 V3__seed_exercises.sql을 만든다.

종목을 바꿀 때는 CSV를 고치고 이 스크립트를 다시 실행한다.
    python3 scripts/generate_exercise_seed.py
경로를 바꿔 실행할 수도 있다 (검사 동작 확인용):
    python3 scripts/generate_exercise_seed.py <exercises.csv> <exercise_equipment.csv> <출력.sql>

운영에 V3가 적용된 뒤에는 V3를 다시 만들지 말고, 바뀐 부분만 새 버전(V4 등)으로 추가한다.
"""
import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXERCISES = ROOT / "docs/data/exercises.csv"
EQUIPMENT = ROOT / "docs/data/exercise_equipment.csv"
OUTPUT = ROOT / "src/main/resources/db/migration/V3__seed_exercises.sql"

# DDL 컬럼 길이와 같아야 한다 (V2__create_workout_tables.sql)
MAX_LENGTH = {"code": 50, "name_ko": 50, "name_en": 100, "source_id": 100}
REQUIRED = ["code", "name_ko", "name_en", "body_part", "load_type", "min_level", "compound", "default_rest_sec"]
CODE_PATTERN = re.compile(r"^[A-Z0-9_]+$")
REST_PATTERN = re.compile(r"^[0-9]+$")  # str.isdigit()은 '²' 같은 문자도 숫자로 본다
SMALLINT_MAX = 32767

# 애플리케이션 enum과 같아야 한다 (BodyPart, LoadType, Experience, Equipment)
BODY_PARTS = {"CHEST", "BACK", "SHOULDER", "LEGS", "ARMS", "CORE"}
LOAD_TYPES = {"BARBELL", "DUMBBELL", "MACHINE", "BODYWEIGHT"}
LEVELS = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}
EQUIPMENTS = {"BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BENCH", "RACK", "DIP_BAR", "PULLUP_BAR", "KETTLEBELL", "BODYWEIGHT"}


def sql_str(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def fail(message: str) -> None:
    sys.exit(f"오류: {message}")


def main() -> None:
    exercises_path, equipment_path, output_path = (
        [Path(p) for p in sys.argv[1:4]] if len(sys.argv) == 4 else [EXERCISES, EQUIPMENT, OUTPUT]
    )
    with exercises_path.open(encoding="utf-8") as f:
        exercises = list(csv.DictReader(f))
    with equipment_path.open(encoding="utf-8") as f:
        equipment = list(csv.DictReader(f))

    codes = set()
    for line, row in enumerate(exercises, start=2):
        for column in REQUIRED:
            if not (row.get(column) or "").strip():
                fail(f"exercises.csv {line}행: {column}이 비어 있음")
        code = row["code"]
        if not CODE_PATTERN.match(code):
            fail(f"{code}: 코드는 대문자·숫자·밑줄만 쓸 수 있음")
        for column, limit in MAX_LENGTH.items():
            if len(row.get(column) or "") > limit:
                fail(f"{code}: {column}이 {limit}자를 넘음")
        if code in codes:
            fail(f"종목 코드 중복: {code}")
        codes.add(code)
        if row["body_part"] not in BODY_PARTS:
            fail(f"{code}: 알 수 없는 body_part {row['body_part']}")
        if row["load_type"] not in LOAD_TYPES:
            fail(f"{code}: 알 수 없는 load_type {row['load_type']}")
        if row["min_level"] not in LEVELS:
            fail(f"{code}: 알 수 없는 min_level {row['min_level']}")
        if row["compound"] not in ("true", "false"):
            fail(f"{code}: compound는 true/false {row['compound']}")
        rest = row["default_rest_sec"]
        if not REST_PATTERN.match(rest) or not 1 <= int(rest) <= SMALLINT_MAX:
            fail(f"{code}: default_rest_sec는 1~{SMALLINT_MAX} 사이 숫자여야 함 ({rest})")

    pairs = set()
    for row in equipment:
        if row["code"] not in codes:
            fail(f"장비 CSV의 종목 코드가 종목 CSV에 없음: {row['code']}")
        if row["equipment"] not in EQUIPMENTS:
            fail(f"{row['code']}: 알 수 없는 equipment {row['equipment']}")
        pair = (row["code"], row["equipment"])
        if pair in pairs:
            fail(f"장비 중복: {pair}")
        pairs.add(pair)

    # 장비 행이 없으면 맨몸 종목으로 취급돼 누구에게나 추천된다. 기구 종목의 장비 누락을 여기서 막는다
    equipped = {code for code, _ in pairs}
    for row in exercises:
        if row["load_type"] != "BODYWEIGHT" and row["code"] not in equipped:
            fail(f"{row['code']}: load_type이 {row['load_type']}인데 필요 장비가 없음 (맨몸 종목으로 추천됨)")

    lines = [
        "-- 초기 종목 사전. 직접 고치지 말 것: scripts/generate_exercise_seed.py가 docs/data/*.csv로 생성한다.",
        "-- 출처: free-exercise-db (https://github.com/yuhonas/free-exercise-db), source_id가 원본 ID.",
        "-- 맨몸 종목은 exercise_equipment 행이 없다 (장비와 관계없이 항상 후보).",
        "-- 로컬은 Flyway를 끄고 ddl-auto=create를 쓰므로, application-local.yml의 spring.sql.init으로 이 파일을 실행한다.",
        "",
        "INSERT INTO exercise (code, name_ko, name_en, body_part, load_type, min_level, compound, default_rest_sec, active,",
        "                      source_id, created_at, updated_at)",
        "VALUES",
    ]
    values = []
    for row in exercises:
        values.append(
            "    ("
            + ", ".join(
                [
                    sql_str(row["code"]),
                    sql_str(row["name_ko"]),
                    sql_str(row["name_en"]),
                    sql_str(row["body_part"]),
                    sql_str(row["load_type"]),
                    sql_str(row["min_level"]),
                    row["compound"].upper(),
                    row["default_rest_sec"],
                    "TRUE",
                    sql_str(row["source_id"]) if row["source_id"] else "NULL",
                    "UTC_TIMESTAMP(6)",
                    "UTC_TIMESTAMP(6)",
                ]
            )
            + ")"
        )
    lines.append(",\n".join(values) + ";")
    lines.append("")
    lines.append("-- 필요 장비: code로 종목 id를 찾아 넣는다 (AUTO_INCREMENT 값에 의존하지 않음)")
    for row in equipment:
        lines.append(
            "INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) "
            f"SELECT id, {sql_str(row['equipment'])}, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = {sql_str(row['code'])};"
        )

    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{output_path} 생성: 종목 {len(exercises)}개, 필요 장비 {len(equipment)}행")


if __name__ == "__main__":
    main()
