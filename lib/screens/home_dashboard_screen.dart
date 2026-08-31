import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/fitness_provider.dart';
import '../utils/app_theme.dart';
import '../widgets/step_ring.dart';
import '../widgets/stat_card.dart';

class HomeDashboardScreen extends ConsumerWidget {
  const HomeDashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final fitnessState = ref.watch(fitnessProvider);
    final fitnessNotifier = ref.read(fitnessProvider.notifier);

    final act = fitnessState.todayActivity;
    final profile = fitnessState.profile;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Hello, ${profile.name}',
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
            ),
            const Text(
              'Ready for today\'s goal?',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: Icon(
              fitnessState.isTracking
                  ? Icons.pause_circle_filled_rounded
                  : Icons.play_circle_fill_rounded,
              color: fitnessState.isTracking
                  ? AppTheme.primaryGreen
                  : AppTheme.accentOrange,
              size: 32,
            ),
            onPressed: () => fitnessNotifier.toggleTracking(),
            tooltip: fitnessState.isTracking ? 'Pause Tracking' : 'Resume Tracking',
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Center Step Ring
            Center(
              child: StepRing(
                currentSteps: act.steps,
                goalSteps: profile.stepGoal,
                onTap: () => fitnessNotifier.addSteps(100),
              ),
            ),

            const SizedBox(height: 16),

            // Quick Step Adder Button
            Center(
              child: ElevatedButton.icon(
                onPressed: () => fitnessNotifier.addSteps(250),
                icon: const Icon(Icons.add, size: 18),
                label: const Text('Add 250 Steps'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryGreen,
                  foregroundColor: Colors.black,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(20),
                  ),
                ),
              ),
            ),

            const SizedBox(height: 24),

            Text(
              'Today\'s Overview',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),

            const SizedBox(height: 12),

            // Stat Cards Grid
            Row(
              children: [
                Expanded(
                  child: StatCard(
                    title: 'Distance',
                    value: fitnessNotifier.formatDistance(act.distanceKm),
                    subtitle: 'Walked today',
                    icon: Icons.straighten_rounded,
                    iconColor: AppTheme.primaryBlue,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: StatCard(
                    title: 'Calories',
                    value: '${act.caloriesBurned} kcal',
                    subtitle: 'Active burn',
                    icon: Icons.local_fire_department_rounded,
                    iconColor: AppTheme.accentOrange,
                  ),
                ),
              ],
            ),

            const SizedBox(height: 12),

            Row(
              children: [
                Expanded(
                  child: StatCard(
                    title: 'Active Time',
                    value: '${act.activeMinutes} min',
                    subtitle: 'Moving time',
                    icon: Icons.timer_rounded,
                    iconColor: Colors.purple,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: StatCard(
                    title: 'Water Intake',
                    value: '${act.waterIntakeMl} ml',
                    subtitle: 'Goal: ${profile.waterGoalMl} ml',
                    icon: Icons.water_drop_rounded,
                    iconColor: Colors.cyan,
                  ),
                ),
              ],
            ),

            const SizedBox(height: 24),

            // Recent Workouts Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Recent Workouts',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                ),
                Text(
                  '${fitnessState.workouts.length} total',
                  style: const TextStyle(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),

            const SizedBox(height: 12),

            if (fitnessState.workouts.isEmpty)
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(20.0),
                  child: Center(
                    child: Text('No workouts logged yet today.'),
                  ),
                ),
              )
            else
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: fitnessState.workouts.length,
                itemBuilder: (context, index) {
                  final w = fitnessState.workouts[index];
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: AppTheme.primaryGreen.withOpacity(0.15),
                        child: const Icon(
                          Icons.fitness_center_rounded,
                          color: AppTheme.primaryGreen,
                        ),
                      ),
                      title: Text(
                        w.activityType,
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                      subtitle: Text(
                        '${w.durationMinutes} min • ${w.caloriesBurned} kcal ${w.distanceKm > 0 ? "• ${fitnessNotifier.formatDistance(w.distanceKm)}" : ""}',
                        style: const TextStyle(fontSize: 12),
                      ),
                      trailing: Text(
                        w.date,
                        style: const TextStyle(fontSize: 11, color: Colors.grey),
                      ),
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
}
