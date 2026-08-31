import 'dart:math';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/user_profile.dart';
import '../models/daily_activity.dart';
import '../models/meal_log.dart';
import '../models/completed_activity.dart';
import '../models/goal_achievement.dart';
import '../services/fitness_repository.dart';

final fitnessRepositoryProvider =
    Provider<FitnessRepository>((ref) => FitnessRepository());

class FitnessState {
  final UserProfile profile;
  final DailyActivity todayActivity;
  final List<MealLog> meals;
  final List<CompletedActivity> workouts;
  final List<GoalAchievement> achievements;
  final bool isDarkMode;
  final bool notificationsEnabled;
  final bool isTracking;

  FitnessState({
    required this.profile,
    required this.todayActivity,
    required this.meals,
    required this.workouts,
    required this.achievements,
    required this.isDarkMode,
    required this.notificationsEnabled,
    required this.isTracking,
  });

  FitnessState copyWith({
    UserProfile? profile,
    DailyActivity? todayActivity,
    List<MealLog>? meals,
    List<CompletedActivity>? workouts,
    List<GoalAchievement>? achievements,
    bool? isDarkMode,
    bool? notificationsEnabled,
    bool? isTracking,
  }) {
    return FitnessState(
      profile: profile ?? this.profile,
      todayActivity: todayActivity ?? this.todayActivity,
      meals: meals ?? this.meals,
      workouts: workouts ?? this.workouts,
      achievements: achievements ?? this.achievements,
      isDarkMode: isDarkMode ?? this.isDarkMode,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      isTracking: isTracking ?? this.isTracking,
    );
  }
}

class FitnessNotifier extends StateNotifier<FitnessState> {
  final FitnessRepository _repo;

  FitnessNotifier(this._repo)
      : super(FitnessState(
          profile: UserProfile.defaultProfile(),
          todayActivity: DailyActivity(
            date: '',
            steps: 0,
            distanceKm: 0,
            caloriesBurned: 0,
            activeMinutes: 0,
            waterIntakeMl: 0,
          ),
          meals: [],
          workouts: [],
          achievements: [],
          isDarkMode: true,
          notificationsEnabled: true,
          isTracking: true,
        )) {
    loadInitialData();
  }

  Future<void> loadInitialData() async {
    final profile = await _repo.getUserProfile();
    final todayAct = await _repo.getTodayActivity();
    final meals = await _repo.getTodayMeals();
    final workouts = await _repo.getWorkouts();
    final achievements = await _repo.getAchievements();
    final darkMode = await _repo.isDarkMode();
    final notifs = await _repo.getNotificationsEnabled();

    state = state.copyWith(
      profile: profile,
      todayActivity: todayAct,
      meals: meals,
      workouts: workouts,
      achievements: achievements,
      isDarkMode: darkMode,
      notificationsEnabled: notifs,
    );
  }

  void addSteps(int count) {
    final newSteps = state.todayActivity.steps + count;
    final newDist = newSteps * 0.00075;
    final newCal = (newSteps * 0.04).round();
    final newActMin = (newSteps / 110).round();

    final updated = state.todayActivity.copyWith(
      steps: newSteps,
      distanceKm: newDist,
      caloriesBurned: newCal,
      activeMinutes: newActMin,
    );

    state = state.copyWith(todayActivity: updated);
    _repo.saveDailyActivity(updated);
  }

  void addWater(int amountMl) {
    final newWater = state.todayActivity.waterIntakeMl + amountMl;
    final updated = state.todayActivity.copyWith(waterIntakeMl: newWater);
    state = state.copyWith(todayActivity: updated);
    _repo.saveDailyActivity(updated);
  }

  void addMeal(MealLog meal) {
    final updatedMeals = [...state.meals, meal];
    state = state.copyWith(meals: updatedMeals);
    _repo.saveTodayMeals(updatedMeals);
  }

  void deleteMeal(String id) {
    final updatedMeals = state.meals.where((m) => m.id != id).toList();
    state = state.copyWith(meals: updatedMeals);
    _repo.saveTodayMeals(updatedMeals);
  }

  void addWorkout(CompletedActivity workout) {
    final updatedWorkouts = [workout, ...state.workouts];
    state = state.copyWith(workouts: updatedWorkouts);
    _repo.saveWorkouts(updatedWorkouts);

    // Also update calories & distance
    final newCal = state.todayActivity.caloriesBurned + workout.caloriesBurned;
    final newDist = state.todayActivity.distanceKm + workout.distanceKm;
    final newAct = state.todayActivity.activeMinutes + workout.durationMinutes;

    final updatedDaily = state.todayActivity.copyWith(
      caloriesBurned: newCal,
      distanceKm: newDist,
      activeMinutes: newAct,
    );
    state = state.copyWith(todayActivity: updatedDaily);
    _repo.saveDailyActivity(updatedDaily);
  }

  void updateProfile(UserProfile profile) {
    state = state.copyWith(profile: profile);
    _repo.saveUserProfile(profile);
  }

  void toggleDarkMode(bool value) {
    state = state.copyWith(isDarkMode: value);
    _repo.setDarkMode(value);
  }

  void toggleUnitSystem(bool isImperial) {
    final updated = state.profile.copyWith(isImperial: isImperial);
    updateProfile(updated);
  }

  void toggleNotifications(bool value) {
    state = state.copyWith(notificationsEnabled: value);
    _repo.setNotificationsEnabled(value);
  }

  void toggleTracking() {
    state = state.copyWith(isTracking: !state.isTracking);
  }

  Future<void> resetAllData() async {
    await _repo.clearAllData();
    await loadInitialData();
  }

  // Helpers
  double calculateBmi() {
    if (state.profile.heightCm <= 0) return 22.0;
    final heightM = state.profile.heightCm / 100.0;
    return state.profile.weightKg / (heightM * heightM);
  }

  String getBmiCategory(double bmi) {
    if (bmi < 18.5) return 'Underweight';
    if (bmi < 25.0) return 'Normal Weight';
    if (bmi < 30.0) return 'Overweight';
    return 'Obese';
  }

  int calculateBmr() {
    final p = state.profile;
    if (p.gender.toLowerCase() == 'female') {
      return (10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age - 161).round();
    }
    return (10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + 5).round();
  }

  int calculateDailyCaloriesNeeded() {
    return (calculateBmr() * 1.375).round();
  }

  String formatDistance(double km) {
    if (state.profile.isImperial) {
      final miles = km * 0.621371;
      return '${miles.toStringAsFixed(2)} mi';
    }
    return '${km.toStringAsFixed(2)} km';
  }

  String formatWeight(double kg) {
    if (state.profile.isImperial) {
      final lbs = kg * 2.20462;
      return '${lbs.toStringAsFixed(1)} lbs';
    }
    return '${kg.toStringAsFixed(1)} kg';
  }

  String formatHeight(double cm) {
    if (state.profile.isImperial) {
      final totalInches = cm / 2.54;
      final feet = totalInches ~/ 12;
      final inches = (totalInches % 12).round();
      return "$feet'$inches\"";
    }
    return '${cm.round()} cm';
  }
}

final fitnessProvider =
    StateNotifierProvider<FitnessNotifier, FitnessState>((ref) {
  final repo = ref.watch(fitnessRepositoryProvider);
  return FitnessNotifier(repo);
});
