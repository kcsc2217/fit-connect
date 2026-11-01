-- 초기 종목 사전. 직접 고치지 말 것: scripts/generate_exercise_seed.py가 docs/data/*.csv로 생성한다.
-- 출처: free-exercise-db (https://github.com/yuhonas/free-exercise-db), source_id가 원본 ID.
-- 맨몸 종목은 exercise_equipment 행이 없다 (장비와 관계없이 항상 후보).
-- 로컬은 Flyway를 끄고 ddl-auto=create를 쓰므로, application-local.yml의 spring.sql.init으로 이 파일을 실행한다.

INSERT INTO exercise (code, name_ko, name_en, body_part, load_type, min_level, compound, default_rest_sec, active,
                      source_id, created_at, updated_at)
VALUES
    ('BACK_SQUAT', '바벨 백 스쿼트', 'Barbell Squat', 'LEGS', 'BARBELL', 'BEGINNER', TRUE, 150, TRUE, 'Barbell_Squat', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('FRONT_SQUAT', '프론트 스쿼트', 'Front Barbell Squat', 'LEGS', 'BARBELL', 'ADVANCED', TRUE, 150, TRUE, 'Front_Barbell_Squat', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('GOBLET_SQUAT', '고블릿 스쿼트', 'Goblet Squat', 'LEGS', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'Goblet_Squat', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('LEG_PRESS', '레그 프레스', 'Leg Press', 'LEGS', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Leg_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('HACK_SQUAT', '핵 스쿼트', 'Hack Squat', 'LEGS', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Hack_Squat', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('SMITH_SQUAT', '스미스 머신 스쿼트', 'Smith Machine Squat', 'LEGS', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Smith_Machine_Squat', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BARBELL_LUNGE', '바벨 런지', 'Barbell Lunge', 'LEGS', 'BARBELL', 'INTERMEDIATE', TRUE, 150, TRUE, 'Barbell_Lunge', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_LUNGE', '덤벨 런지', 'Dumbbell Lunges', 'LEGS', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'Dumbbell_Lunges', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('WALKING_LUNGE', '맨몸 워킹 런지', 'Bodyweight Walking Lunge', 'LEGS', 'BODYWEIGHT', 'BEGINNER', TRUE, 120, TRUE, 'Bodyweight_Walking_Lunge', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_REVERSE_LUNGE', '덤벨 리버스 런지', 'Dumbbell Rear Lunge', 'LEGS', 'DUMBBELL', 'INTERMEDIATE', TRUE, 120, TRUE, 'Dumbbell_Rear_Lunge', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_STEP_UP', '덤벨 스텝업', 'Dumbbell Step Ups', 'LEGS', 'DUMBBELL', 'INTERMEDIATE', TRUE, 120, TRUE, 'Dumbbell_Step_Ups', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('LEG_EXTENSION', '레그 익스텐션', 'Leg Extensions', 'LEGS', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Leg_Extensions', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('LYING_LEG_CURL', '라잉 레그 컬', 'Lying Leg Curls', 'LEGS', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Lying_Leg_Curls', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('SEATED_LEG_CURL', '시티드 레그 컬', 'Seated Leg Curl', 'LEGS', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Seated_Leg_Curl', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('ROMANIAN_DEADLIFT', '루마니안 데드리프트', 'Romanian Deadlift', 'LEGS', 'BARBELL', 'INTERMEDIATE', TRUE, 150, TRUE, 'Romanian_Deadlift', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_STIFF_LEG_DEADLIFT', '덤벨 스티프 레그 데드리프트', 'Stiff-Legged Dumbbell Deadlift', 'LEGS', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'Stiff-Legged_Dumbbell_Deadlift', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('HIP_THRUST', '바벨 힙 쓰러스트', 'Barbell Hip Thrust', 'LEGS', 'BARBELL', 'INTERMEDIATE', TRUE, 150, TRUE, 'Barbell_Hip_Thrust', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('GLUTE_BRIDGE', '글루트 브릿지', 'Butt Lift (Bridge)', 'LEGS', 'BODYWEIGHT', 'BEGINNER', FALSE, 75, TRUE, 'Butt_Lift_Bridge', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('STANDING_CALF_RAISE', '스탠딩 카프 레이즈', 'Standing Calf Raises', 'LEGS', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Standing_Calf_Raises', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('SEATED_CALF_RAISE', '시티드 카프 레이즈', 'Seated Calf Raise', 'LEGS', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Seated_Calf_Raise', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('KETTLEBELL_SWING', '원암 케틀벨 스윙', 'One-Arm Kettlebell Swings', 'LEGS', 'DUMBBELL', 'INTERMEDIATE', TRUE, 120, TRUE, 'One-Arm_Kettlebell_Swings', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BENCH_PRESS', '바벨 벤치 프레스', 'Barbell Bench Press - Medium Grip', 'CHEST', 'BARBELL', 'BEGINNER', TRUE, 150, TRUE, 'Barbell_Bench_Press_-_Medium_Grip', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_BENCH_PRESS', '덤벨 벤치 프레스', 'Dumbbell Bench Press', 'CHEST', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'Dumbbell_Bench_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('INCLINE_BENCH_PRESS', '인클라인 바벨 벤치 프레스', 'Barbell Incline Bench Press - Medium Grip', 'CHEST', 'BARBELL', 'BEGINNER', TRUE, 150, TRUE, 'Barbell_Incline_Bench_Press_-_Medium_Grip', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('INCLINE_DUMBBELL_PRESS', '인클라인 덤벨 프레스', 'Incline Dumbbell Press', 'CHEST', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'Incline_Dumbbell_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_FLY', '덤벨 플라이', 'Dumbbell Flyes', 'CHEST', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Dumbbell_Flyes', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PEC_DECK', '펙 덱 플라이', 'Butterfly', 'CHEST', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Butterfly', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('MACHINE_CHEST_PRESS', '머신 체스트 프레스', 'Machine Bench Press', 'CHEST', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Machine_Bench_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CABLE_CROSSOVER', '케이블 크로스오버', 'Cable Crossover', 'CHEST', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Cable_Crossover', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PUSH_UP', '푸시업', 'Pushups', 'CHEST', 'BODYWEIGHT', 'BEGINNER', TRUE, 120, TRUE, 'Pushups', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CHEST_DIP', '체스트 딥스', 'Dips - Chest Version', 'CHEST', 'BODYWEIGHT', 'INTERMEDIATE', TRUE, 120, TRUE, 'Dips_-_Chest_Version', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('LAT_PULLDOWN', '랫 풀다운', 'Wide-Grip Lat Pulldown', 'BACK', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Wide-Grip_Lat_Pulldown', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PULL_UP', '풀업', 'Pullups', 'BACK', 'BODYWEIGHT', 'BEGINNER', TRUE, 120, TRUE, 'Pullups', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CHIN_UP', '친업', 'Chin-Up', 'BACK', 'BODYWEIGHT', 'BEGINNER', TRUE, 120, TRUE, 'Chin-Up', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BARBELL_ROW', '바벨 벤트오버 로우', 'Bent Over Barbell Row', 'BACK', 'BARBELL', 'BEGINNER', TRUE, 150, TRUE, 'Bent_Over_Barbell_Row', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('ONE_ARM_DUMBBELL_ROW', '원암 덤벨 로우', 'One-Arm Dumbbell Row', 'BACK', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'One-Arm_Dumbbell_Row', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('SEATED_CABLE_ROW', '시티드 케이블 로우', 'Seated Cable Rows', 'BACK', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Seated_Cable_Rows', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('T_BAR_ROW', '티바 로우', 'Lying T-Bar Row', 'BACK', 'MACHINE', 'INTERMEDIATE', TRUE, 120, TRUE, 'Lying_T-Bar_Row', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DEADLIFT', '바벨 데드리프트', 'Barbell Deadlift', 'BACK', 'BARBELL', 'INTERMEDIATE', TRUE, 150, TRUE, 'Barbell_Deadlift', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BACK_EXTENSION', '백 익스텐션', 'Hyperextensions (Back Extensions)', 'BACK', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Hyperextensions_Back_Extensions', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BARBELL_SHRUG', '바벨 슈러그', 'Barbell Shrug', 'BACK', 'BARBELL', 'BEGINNER', FALSE, 75, TRUE, 'Barbell_Shrug', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('OVERHEAD_PRESS', '스탠딩 오버헤드 프레스', 'Standing Military Press', 'SHOULDER', 'BARBELL', 'BEGINNER', TRUE, 150, TRUE, 'Standing_Military_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('SEATED_DUMBBELL_PRESS', '시티드 덤벨 숄더 프레스', 'Seated Dumbbell Press', 'SHOULDER', 'DUMBBELL', 'BEGINNER', TRUE, 120, TRUE, 'Seated_Dumbbell_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('ARNOLD_PRESS', '아놀드 프레스', 'Arnold Dumbbell Press', 'SHOULDER', 'DUMBBELL', 'INTERMEDIATE', TRUE, 120, TRUE, 'Arnold_Dumbbell_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('MACHINE_SHOULDER_PRESS', '머신 숄더 프레스', 'Machine Shoulder (Military) Press', 'SHOULDER', 'MACHINE', 'BEGINNER', TRUE, 120, TRUE, 'Machine_Shoulder_Military_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('LATERAL_RAISE', '사이드 레터럴 레이즈', 'Side Lateral Raise', 'SHOULDER', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Side_Lateral_Raise', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CABLE_LATERAL_RAISE', '케이블 레터럴 레이즈', 'Cable Seated Lateral Raise', 'SHOULDER', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Cable_Seated_Lateral_Raise', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('FRONT_RAISE', '덤벨 프론트 레이즈', 'Front Dumbbell Raise', 'SHOULDER', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Front_Dumbbell_Raise', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('REVERSE_FLY', '리버스 플라이', 'Reverse Flyes', 'SHOULDER', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Reverse_Flyes', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('FACE_PULL', '페이스 풀', 'Face Pull', 'SHOULDER', 'MACHINE', 'INTERMEDIATE', TRUE, 120, TRUE, 'Face_Pull', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BARBELL_CURL', '바벨 컬', 'Barbell Curl', 'ARMS', 'BARBELL', 'BEGINNER', FALSE, 75, TRUE, 'Barbell_Curl', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('DUMBBELL_CURL', '덤벨 컬', 'Dumbbell Bicep Curl', 'ARMS', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Dumbbell_Bicep_Curl', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('HAMMER_CURL', '해머 컬', 'Hammer Curls', 'ARMS', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Hammer_Curls', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PREACHER_CURL', '프리처 컬', 'Preacher Curl', 'ARMS', 'BARBELL', 'BEGINNER', FALSE, 75, TRUE, 'Preacher_Curl', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('TRICEPS_PUSHDOWN', '트라이셉스 푸시다운', 'Triceps Pushdown', 'ARMS', 'MACHINE', 'BEGINNER', FALSE, 75, TRUE, 'Triceps_Pushdown', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('LYING_TRICEPS_EXTENSION', '라잉 트라이셉스 익스텐션', 'Lying Triceps Press', 'ARMS', 'BARBELL', 'INTERMEDIATE', FALSE, 75, TRUE, 'Lying_Triceps_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('OVERHEAD_TRICEPS_EXTENSION', '덤벨 오버헤드 트라이셉스 익스텐션', 'Standing Dumbbell Triceps Extension', 'ARMS', 'DUMBBELL', 'BEGINNER', FALSE, 75, TRUE, 'Standing_Dumbbell_Triceps_Extension', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CLOSE_GRIP_BENCH_PRESS', '클로즈 그립 벤치 프레스', 'Close-Grip Barbell Bench Press', 'ARMS', 'BARBELL', 'BEGINNER', TRUE, 150, TRUE, 'Close-Grip_Barbell_Bench_Press', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('TRICEPS_DIP', '트라이셉스 딥스', 'Dips - Triceps Version', 'ARMS', 'BODYWEIGHT', 'BEGINNER', TRUE, 120, TRUE, 'Dips_-_Triceps_Version', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('BENCH_DIP', '벤치 딥스', 'Bench Dips', 'ARMS', 'BODYWEIGHT', 'BEGINNER', TRUE, 120, TRUE, 'Bench_Dips', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PLANK', '플랭크', 'Plank', 'CORE', 'BODYWEIGHT', 'BEGINNER', FALSE, 60, TRUE, 'Plank', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CRUNCH', '크런치', 'Crunches', 'CORE', 'BODYWEIGHT', 'BEGINNER', FALSE, 60, TRUE, 'Crunches', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CABLE_CRUNCH', '케이블 크런치', 'Cable Crunch', 'CORE', 'MACHINE', 'BEGINNER', FALSE, 60, TRUE, 'Cable_Crunch', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('HANGING_LEG_RAISE', '행잉 레그 레이즈', 'Hanging Leg Raise', 'CORE', 'BODYWEIGHT', 'ADVANCED', FALSE, 60, TRUE, 'Hanging_Leg_Raise', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('RUSSIAN_TWIST', '러시안 트위스트', 'Russian Twist', 'CORE', 'BODYWEIGHT', 'INTERMEDIATE', TRUE, 60, TRUE, 'Russian_Twist', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

-- 필요 장비: code로 종목 id를 찾아 넣는다 (AUTO_INCREMENT 값에 의존하지 않음)
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BACK_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BACK_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'FRONT_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'FRONT_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'GOBLET_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LEG_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'HACK_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'SMITH_SQUAT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BARBELL_LUNGE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BARBELL_LUNGE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_LUNGE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_REVERSE_LUNGE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_STEP_UP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_STEP_UP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LEG_EXTENSION';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LYING_LEG_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'SEATED_LEG_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'ROMANIAN_DEADLIFT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_STIFF_LEG_DEADLIFT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'HIP_THRUST';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'HIP_THRUST';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'STANDING_CALF_RAISE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'SEATED_CALF_RAISE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'KETTLEBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'KETTLEBELL_SWING';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'INCLINE_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'INCLINE_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'INCLINE_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'INCLINE_DUMBBELL_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'INCLINE_DUMBBELL_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_FLY';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_FLY';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'PEC_DECK';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'MACHINE_CHEST_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CABLE_CROSSOVER';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DIP_BAR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CHEST_DIP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LAT_PULLDOWN';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'PULLUP_BAR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'PULL_UP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'PULLUP_BAR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CHIN_UP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BARBELL_ROW';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'ONE_ARM_DUMBBELL_ROW';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'ONE_ARM_DUMBBELL_ROW';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'SEATED_CABLE_ROW';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'T_BAR_ROW';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DEADLIFT';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BACK_EXTENSION';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BARBELL_SHRUG';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'OVERHEAD_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'OVERHEAD_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'SEATED_DUMBBELL_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'SEATED_DUMBBELL_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'ARNOLD_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'ARNOLD_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'MACHINE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'MACHINE_SHOULDER_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LATERAL_RAISE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CABLE_LATERAL_RAISE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'FRONT_RAISE';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'REVERSE_FLY';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'FACE_PULL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BARBELL_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'DUMBBELL_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'HAMMER_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'PREACHER_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'PREACHER_CURL';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'TRICEPS_PUSHDOWN';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LYING_TRICEPS_EXTENSION';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'LYING_TRICEPS_EXTENSION';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DUMBBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'OVERHEAD_TRICEPS_EXTENSION';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CLOSE_GRIP_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CLOSE_GRIP_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'RACK', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CLOSE_GRIP_BENCH_PRESS';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'DIP_BAR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'TRICEPS_DIP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'BENCH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BENCH_DIP';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'CABLE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'CABLE_CRUNCH';
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at) SELECT id, 'PULLUP_BAR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'HANGING_LEG_RAISE';
