import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/completed_activity.dart';
import '../providers/fitness_provider.dart';
import '../utils/app_theme.dart';
import '../widgets/add_workout_dialog.dart';

class ActivityScreen extends ConsumerWidget {
  const ActivityScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final fitnessState = ref.watch(fitnessProvider);
    final fitnessNotifier = ref.read(fitnessProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Activity & Workouts', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          showDialog(
            context: context,
            builder: (_) => AddWorkoutDialog(
              onAdd: (type, duration, calories, distance) {
                fitnessNotifier.addWorkout(
                  CompletedActivity(
                    id: DateTime.now().millisecondsSinceEpoch.toString(),
                    activityType: type,
                    durationMinutes: duration,
                    caloriesBurned: calories,
                    distanceKm: distance,
                    date: fitnessNotifier.todayDate,
                  ),
                );
              },
            ),
          );
        },
        backgroundColor: AppTheme.primaryGreen,
        foregroundColor: Colors.black,
        icon: const Icon(Icons.add),
        label: const Text('Log Workout', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Achievements Section
            Text(
              'Achievements & Badges',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              height: 110,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: fitnessState.achievements.length,
                itemBuilder: (context, index) {
                  final ach = fitnessState.achievements[index];
                  return Container(
                    width: 140,
                    margin: const EdgeInsets.only(right: 12),
                    child: Card(
                      color: ach.isUnlocked
                          ? Theme.of(context).cardColor
                          : Theme.of(context).cardColor.withOpacity(0.5),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                        side: BorderSide(
                          color: ach.isUnlocked
                              ? AppTheme.primaryGreen
                              : Colors.transparent,
                        ),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              ach.isUnlocked
                                  ? Icons.emoji_events_rounded
                                  : Icons.lock_outline_rounded,
                              color: ach.isUnlocked
                                  ? AppTheme.accentOrange
                                  : Colors.grey,
                              size: 28,
                            ),
                            const SizedBox(height: 6),
                            Text(
                              ach.title,
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                              ),
                              textAlign: TextAlign.center,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            Text(
                              ach.isUnlocked ? 'Unlocked' : 'Locked',
                              style: TextStyle(
                                fontSize: 10,
                                color: ach.isUnlocked
                                    ? AppTheme.primaryGreen
                                    : Colors.grey,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),

            const SizedBox(height: 24),

            // Workout History
            Text(
              'Workout History',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 12),

            if (fitnessState.workouts.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32.0),
                  child: Text('No workouts logged yet. Tap + Log Workout below!'),
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
                    margin: const EdgeInsets.only(bottom: 12),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Row(
                        children: [
                          CircleAvatar(
                            radius: 24,
                            backgroundColor: AppTheme.primaryBlue.withOpacity(0.15),
                            child: const Icon(
                              Icons.directions_run_rounded,
                              color: AppTheme.primaryBlue,
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  w.activityType,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  'Duration: ${w.durationMinutes} min • Calories: ${w.caloriesBurned} kcal',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Theme.of(context)
                                        .colorScheme
                                        .onSurfaceVariant,
                                  ),
                                ),
                                if (w.distanceKm > 0)
                                  Text(
                                    'Distance: ${fitnessNotifier.formatDistance(w.distanceKm)}',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: Theme.of(context)
                                          .colorScheme
                                          .onSurfaceVariant,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                        ],
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
