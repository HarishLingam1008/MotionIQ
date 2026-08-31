import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/fitness_provider.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final fitnessState = ref.watch(fitnessProvider);
    final fitnessNotifier = ref.read(fitnessProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('App Settings', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: ListView(
        padding: const EdgeInsets.all(20.0),
        children: [
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  title: const Text('Dark Theme Mode'),
                  subtitle: const Text('Enable dark mode UI styling'),
                  secondary: const Icon(Icons.dark_mode_rounded),
                  value: fitnessState.isDarkMode,
                  onChanged: (val) => fitnessNotifier.toggleDarkMode(val),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  title: const Text('Imperial Units'),
                  subtitle: const Text('Use miles and lbs instead of km and kg'),
                  secondary: const Icon(Icons.square_foot_rounded),
                  value: fitnessState.profile.isImperial,
                  onChanged: (val) => fitnessNotifier.toggleUnitSystem(val),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  title: const Text('Goal Notifications'),
                  subtitle: const Text('Daily step goal completion reminders'),
                  secondary: const Icon(Icons.notifications_active_rounded),
                  value: fitnessState.notificationsEnabled,
                  onChanged: (val) => fitnessNotifier.toggleNotifications(val),
                ),
              ],
            ),
          ),

          const SizedBox(height: 24),

          Card(
            child: ListTile(
              leading: const Icon(Icons.restore_rounded, color: Colors.red),
              title: const Text('Reset Application Data', style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
              subtitle: const Text('Clear local cached step logs, meals, and settings'),
              onTap: () async {
                final confirm = await showDialog<bool>(
                  context: context,
                  builder: (_) => AlertDialog(
                    title: const Text('Reset All Local Data?'),
                    content: const Text('This action will reset your step logs, meals, and local settings back to defaults.'),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
                        onPressed: () => Navigator.pop(context, true),
                        child: const Text('Reset Data'),
                      ),
                    ],
                  ),
                );

                if (confirm == true) {
                  await fitnessNotifier.resetAllData();
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('App data reset successfully.')),
                    );
                  }
                }
              },
            ),
          ),
        ],
      ),
    );
  }
}
