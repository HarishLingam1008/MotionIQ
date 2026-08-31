import 'package:flutter/material.dart';
import 'package:percent_indicator/circular_percent_indicator.dart';
import '../utils/app_theme.dart';

class StepRing extends StatelessWidget {
  final int currentSteps;
  final int goalSteps;
  final VoidCallback? onTap;

  const StepRing({
    super.key,
    required this.currentSteps,
    required this.goalSteps,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final double percent = (currentSteps / goalSteps).clamp(0.0, 1.0);

    return GestureDetector(
      onTap: onTap,
      child: CircularPercentIndicator(
        radius: 110.0,
        lineWidth: 18.0,
        animation: true,
        percent: percent,
        center: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.directions_walk_rounded,
              size: 32,
              color: AppTheme.primaryGreen,
            ),
            const SizedBox(height: 4),
            Text(
              '$currentSteps',
              style: const TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.bold,
                letterSpacing: -1,
              ),
            ),
            Text(
              'Goal: $goalSteps',
              style: TextStyle(
                fontSize: 13,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        circularStrokeCap: CircularStrokeCap.round,
        linearGradient: const LinearGradient(
          colors: [AppTheme.primaryGreen, AppTheme.primaryBlue],
        ),
        backgroundColor: AppTheme.primaryGreen.withOpacity(0.15),
      ),
    );
  }
}
