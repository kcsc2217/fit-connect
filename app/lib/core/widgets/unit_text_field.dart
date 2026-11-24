import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/theme/app_theme.dart';

/// 숫자 입력 + 단위 (프로토타입 `.inline-input`, 예: 175 cm).
/// 처음 값은 [initialValue]이고, 이후 값은 [onChanged]로 바깥(입력값 객체)이 가진다.
class UnitTextField extends StatefulWidget {
  const UnitTextField({
    required this.fieldKey,
    required this.initialValue,
    required this.unit,
    required this.onChanged,
    this.decimal = false,
    this.errorText,
    super.key,
  });

  /// 입력칸 자체의 키 (테스트가 찾는다)
  final Key fieldKey;
  final String initialValue;
  final String unit;
  final ValueChanged<String> onChanged;

  /// 소수점 입력 허용
  final bool decimal;

  /// 값을 넣었는데 범위 밖일 때 보여 줄 문구
  final String? errorText;

  @override
  State<UnitTextField> createState() => _UnitTextFieldState();
}

class _UnitTextFieldState extends State<UnitTextField> {
  late final _controller = TextEditingController(text: widget.initialValue);

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return TextField(
      key: widget.fieldKey,
      controller: _controller,
      onChanged: widget.onChanged,
      keyboardType: TextInputType.numberWithOptions(decimal: widget.decimal),
      inputFormatters: [
        // 소수점이 쉼표인 지역(독일 등)의 키패드는 ','를 낸다. 서버 형식인 '.'으로 바꾼다
        if (widget.decimal) _commaToDot,
        FilteringTextInputFormatter.allow(
          RegExp(widget.decimal ? r'[0-9.]' : r'[0-9]'),
        ),
        LengthLimitingTextInputFormatter(widget.decimal ? 5 : 3),
      ],
      style: text.headlineSmall,
      decoration: InputDecoration(
        suffixText: widget.unit,
        suffixStyle: text.bodyLarge?.copyWith(color: AppColors.textSecondary),
        errorText: widget.errorText,
        filled: true,
        fillColor: AppColors.surface,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: 16,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.tile),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.tile),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.tile),
          borderSide: const BorderSide(color: AppColors.lime),
        ),
      ),
    );
  }
}

final _commaToDot = TextInputFormatter.withFunction(
  (oldValue, newValue) =>
      newValue.copyWith(text: newValue.text.replaceAll(',', '.')),
);
