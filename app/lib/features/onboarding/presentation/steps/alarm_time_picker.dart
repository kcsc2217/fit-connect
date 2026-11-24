import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/theme/app_theme.dart';
import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';

/// 알림 시각 휠: 시(0~23)와 분(5분 간격). 서버는 5분 단위만 받는다 (명세)
class AlarmTimePicker extends StatefulWidget {
  const AlarmTimePicker({
    required this.value,
    required this.onChanged,
    super.key,
  });

  final AlarmTime value;
  final ValueChanged<AlarmTime> onChanged;

  @override
  State<AlarmTimePicker> createState() => _AlarmTimePickerState();
}

class _AlarmTimePickerState extends State<AlarmTimePicker> {
  static const _minuteStep = 5;
  static const _itemExtent = 44.0;

  late final _hours = FixedExtentScrollController(
    initialItem: widget.value.hour,
  );
  late final _minutes = FixedExtentScrollController(
    initialItem: widget.value.minute ~/ _minuteStep,
  );

  @override
  void dispose() {
    _hours.dispose();
    _minutes.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.headlineSmall;
    Widget wheel(
      FixedExtentScrollController controller,
      int count,
      String Function(int) label,
      void Function(int) onSelected,
    ) {
      return SizedBox(
        width: 72,
        child: ListWheelScrollView.useDelegate(
          controller: controller,
          itemExtent: _itemExtent,
          physics: const FixedExtentScrollPhysics(),
          diameterRatio: 1.4,
          onSelectedItemChanged: onSelected,
          childDelegate: ListWheelChildBuilderDelegate(
            childCount: count,
            builder: (context, i) =>
                Center(child: Text(label(i), style: style)),
          ),
        ),
      );
    }

    String two(int n) => n.toString().padLeft(2, '0');
    return Container(
      height: _itemExtent * 3,
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppRadius.tile),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          wheel(
            _hours,
            24,
            two,
            (h) => widget.onChanged(AlarmTime(h, widget.value.minute)),
          ),
          Text(':', style: style),
          wheel(
            _minutes,
            60 ~/ _minuteStep,
            (i) => two(i * _minuteStep),
            (i) =>
                widget.onChanged(AlarmTime(widget.value.hour, i * _minuteStep)),
          ),
        ],
      ),
    );
  }
}
