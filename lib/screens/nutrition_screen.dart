import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/meal_log.dart';
import '../providers/fitness_provider.dart';
import '../utils/app_theme.dart';
import '../widgets/add_meal_dialog.dart';

class NutritionScreen extends ConsumerWidget {
  const NutritionScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final fitnessState = ref.watch(fitnessProvider);
    final fitnessNotifier = ref.read(fitnessProvider.notifier);

    final meals = fitnessState.meals;
    final totalCal = meals.fold<int>(0, (sum, m) => sum + m.calories);
    final totalProtein = meals.fold<double>(0, (sum, m) => sum + m.proteinG);
    final totalCarbs = meals.fold<double>(0, (sum, m) => sum + m.carbsG);
    final totalFat = meals.fold<double>(0, (sum, m) => sum + m.fatG);

    final targetCal = fitnessNotifier.calculateDailyCaloriesNeeded();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Nutrition & Water', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          showDialog(
            context: context,
            builder: (_) => AddMealDialog(
              onAdd: (name, cal, prot, carbs, fat) {
                fitnessNotifier.addMeal(
                  MealLog(
                    id: DateTime.now().millisecondsSinceEpoch.toString(),
                    name: name,
                    calories: cal,
                    proteinG: prot,
                    carbsG: carbs,
                    fatG: fat,
                    time: 'Just now',
                  ),
                );
              },
            ),
          );
        },
        backgroundColor: AppTheme.accentOrange,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.restaurant_menu_rounded),
        label: const Text('Log Meal', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Calorie Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Daily Calories',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        Text(
                          '$totalCal / $targetCal kcal',
                          style: const TextStyle(
                            color: AppTheme.accentOrange,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    LinearProgressIndicator(
                      value: (totalCal / targetCal).clamp(0.0, 1.0),
                      color: AppTheme.accentOrange,
                      backgroundColor: AppTheme.accentOrange.withOpacity(0.15),
                      minHeight: 10,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _MacroItem('Protein', '${totalProtein.round()}g', AppTheme.primaryGreen),
                        _MacroItem('Carbs', '${totalCarbs.round()}g', AppTheme.primaryBlue),
                        _MacroItem('Fat', '${totalFat.round()}g', AppTheme.accentOrange),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 20),

            // Water Tracker Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.cyan.withOpacity(0.15),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.water_drop_rounded, color: Colors.cyan, size: 32),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Water Hydration',
                            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                          ),
                          Text(
                            '${fitnessState.todayActivity.waterIntakeMl} / ${fitnessState.profile.waterGoalMl} ml',
                            style: const TextStyle(color: Colors.grey, fontSize: 13),
                          ),
                        ],
                      ),
                    ),
                    ElevatedButton(
                      onPressed: () => fitnessNotifier.addWater(250),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.cyan,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                      child: const Text('+ 250 ml'),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 24),

            // Meal Log List
            Text(
              'Today\'s Meals',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 12),

            if (meals.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32.0),
                  child: Text('No meals logged today yet.'),
                ),
              )
            else
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: meals.length,
                itemBuilder: (context, index) {
                  final m = meals[index];
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      leading: const CircleAvatar(
                        backgroundColor: AppTheme.accentOrange,
                        child: Icon(Icons.fastfood_rounded, color: Colors.white, size: 18),
                      ),
                      title: Text(m.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Text(
                        'P: ${m.proteinG.round()}g • C: ${m.carbsG.round()}g • F: ${m.fatG.round()}g',
                        style: const TextStyle(fontSize: 12),
                      ),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            '${m.calories} kcal',
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete_outline, size: 20, color: Colors.grey),
                            onPressed: () => fitnessNotifier.deleteMeal(m.id),
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

  Widget _MacroItem(String label, String value, Color color) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: color)),
        const SizedBox(height: 2),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}
