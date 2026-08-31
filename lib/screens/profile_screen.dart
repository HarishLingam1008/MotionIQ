import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/user_profile.dart';
import '../providers/auth_provider.dart';
import '../providers/fitness_provider.dart';
import '../utils/app_theme.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final fitnessState = ref.watch(fitnessProvider);
    final fitnessNotifier = ref.read(fitnessProvider.notifier);
    final authController = ref.read(authControllerProvider.notifier);
    final authUser = ref.watch(authStateProvider).value;

    final profile = fitnessState.profile;
    final bmi = fitnessNotifier.calculateBmi();
    final bmiCategory = fitnessNotifier.getBmiCategory(bmi);
    final bmr = fitnessNotifier.calculateBmr();
    final caloriesNeeded = fitnessNotifier.calculateDailyCaloriesNeeded();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Athlete Profile', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            // User Avatar Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  children: [
                    CircleAvatar(
                      radius: 40,
                      backgroundColor: AppTheme.primaryGreen.withOpacity(0.2),
                      backgroundImage: authUser?.photoURL != null
                          ? NetworkImage(authUser!.photoURL!)
                          : null,
                      child: authUser?.photoURL == null
                          ? Text(
                              profile.name.isNotEmpty ? profile.name[0].toUpperCase() : 'A',
                              style: const TextStyle(
                                fontSize: 32,
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryGreen,
                              ),
                            )
                          : null,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      authUser?.displayName ?? profile.name,
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                    ),
                    Text(
                      authUser?.email ?? 'Athlete Account',
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    const SizedBox(height: 16),
                    OutlinedButton.icon(
                      onPressed: () => _showEditProfileDialog(context, profile, fitnessNotifier),
                      icon: const Icon(Icons.edit_rounded, size: 18),
                      label: const Text('Edit Profile'),
                      style: OutlinedButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 20),

            // Health Metrics Grid
            Row(
              children: [
                Expanded(
                  child: _ProfileMetricCard(
                    title: 'Height',
                    value: fitnessNotifier.formatHeight(profile.heightCm),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _ProfileMetricCard(
                    title: 'Weight',
                    value: fitnessNotifier.formatWeight(profile.weightKg),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _ProfileMetricCard(
                    title: 'Age',
                    value: '${profile.age} yrs',
                  ),
                ),
              ],
            ),

            const SizedBox(height: 12),

            Row(
              children: [
                Expanded(
                  child: _ProfileMetricCard(
                    title: 'BMI',
                    value: bmi.toStringAsFixed(1),
                    subtitle: bmiCategory,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _ProfileMetricCard(
                    title: 'BMR',
                    value: '$bmr kcal',
                    subtitle: 'Basal Met Rate',
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _ProfileMetricCard(
                    title: 'TDEE',
                    value: '$caloriesNeeded kcal',
                    subtitle: 'Daily Target',
                  ),
                ),
              ],
            ),

            const SizedBox(height: 32),

            // Logout Button (Required feature)
            SizedBox(
              width: double.infinity,
              height: 52,
              child: OutlinedButton.icon(
                onPressed: () => authController.signOut(),
                style: OutlinedButton.styleFrom(
                  foregroundColor: Theme.of(context).colorScheme.error,
                  side: BorderSide(color: Theme.of(context).colorScheme.error),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                icon: const Icon(Icons.logout_rounded),
                label: const Text(
                  'Log Out of MotionIQ',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showEditProfileDialog(
      BuildContext context, UserProfile profile, FitnessNotifier notifier) {
    final nameCtrl = TextEditingController(text: profile.name);
    final heightCtrl = TextEditingController(text: profile.heightCm.toString());
    final weightCtrl = TextEditingController(text: profile.weightKg.toString());
    final ageCtrl = TextEditingController(text: profile.age.toString());

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text('Edit Profile'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                decoration: const InputDecoration(labelText: 'Name', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: heightCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Height (cm)', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: weightCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Weight (kg)', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: ageCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Age', border: OutlineInputBorder()),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () {
              notifier.updateProfile(
                profile.copyWith(
                  name: nameCtrl.text.trim(),
                  heightCm: double.tryParse(heightCtrl.text) ?? profile.heightCm,
                  weightKg: double.tryParse(weightCtrl.text) ?? profile.weightKg,
                  age: int.tryParse(ageCtrl.text) ?? profile.age,
                ),
              );
              Navigator.pop(context);
            },
            child: const Text('Save Changes'),
          ),
        ],
      ),
    );
  }
}

class _ProfileMetricCard extends StatelessWidget {
  final String title;
  final String value;
  final String? subtitle;

  const _ProfileMetricCard({
    required this.title,
    required this.value,
    this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
        child: Column(
          children: [
            Text(title, style: const TextStyle(fontSize: 11, color: Colors.grey)),
            const SizedBox(height: 4),
            Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 2),
              Text(
                subtitle!,
                style: TextStyle(
                  fontSize: 10,
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
