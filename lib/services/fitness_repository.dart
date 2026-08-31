import 'dart:convert';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user_profile.dart';
import '../models/daily_activity.dart';
import '../models/meal_log.dart';
import '../models/completed_activity.dart';
import '../models/goal_achievement.dart';

class FitnessRepository {
  static const String _keyProfile = 'motioniq_user_profile';
  static const String _keyDaily = 'motioniq_daily_activity_';
  static const String _keyMeals = 'motioniq_meals_';
  static const String _keyWorkouts = 'motioniq_completed_workouts';
  static const String _keyAchievements = 'motioniq_achievements';
  static const String _keyDarkMode = 'motioniq_dark_mode';
  static const String _keyNotifications = 'motioniq_notifications';

  String get todayDate => DateFormat('yyyy-MM-dd').format(DateTime.now());

  Future<UserProfile> getUserProfile() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_keyProfile);
    if (jsonString != null) {
      try {
        return UserProfile.fromJson(jsonDecode(jsonString));
      } catch (_) {}
    }
    return UserProfile.defaultProfile();
  }

  Future<void> saveUserProfile(UserProfile profile) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyProfile, jsonEncode(profile.toJson()));
  }

  Future<DailyActivity> getTodayActivity() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_keyDaily + todayDate);
    if (jsonString != null) {
      try {
        return DailyActivity.fromJson(jsonDecode(jsonString));
      } catch (_) {}
    }
    return DailyActivity(
      date: todayDate,
      steps: 4280,
      distanceKm: 3.2,
      caloriesBurned: 245,
      activeMinutes: 38,
      waterIntakeMl: 1250,
    );
  }

  Future<void> saveDailyActivity(DailyActivity activity) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyDaily + activity.date, jsonEncode(activity.toJson()));
  }

  Future<List<MealLog>> getTodayMeals() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_keyMeals + todayDate);
    if (jsonString != null) {
      try {
        final List list = jsonDecode(jsonString);
        return list.map((e) => MealLog.fromJson(e)).toList();
      } catch (_) {}
    }
    return [
      MealLog(
        id: '1',
        name: 'Oatmeal with Berries & Protein',
        calories: 380,
        proteinG: 24.0,
        carbsG: 52.0,
        fatG: 6.5,
        time: '08:30 AM',
      ),
      MealLog(
        id: '2',
        name: 'Grilled Chicken & Quinoa Bowl',
        calories: 550,
        proteinG: 45.0,
        carbsG: 48.0,
        fatG: 12.0,
        time: '01:15 PM',
      ),
    ];
  }

  Future<void> saveTodayMeals(List<MealLog> meals) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyMeals + todayDate, jsonEncode(meals.map((m) => m.toJson()).toList()));
  }

  Future<List<CompletedActivity>> getWorkouts() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_keyWorkouts);
    if (jsonString != null) {
      try {
        final List list = jsonDecode(jsonString);
        return list.map((e) => CompletedActivity.fromJson(e)).toList();
      } catch (_) {}
    }
    return [
      CompletedActivity(
        id: 'w1',
        activityType: 'Morning Jog',
        durationMinutes: 28,
        caloriesBurned: 210,
        distanceKm: 3.8,
        date: todayDate,
      ),
      CompletedActivity(
        id: 'w2',
        activityType: 'HIIT Workout',
        durationMinutes: 20,
        caloriesBurned: 180,
        distanceKm: 0.0,
        date: todayDate,
      ),
    ];
  }

  Future<void> saveWorkouts(List<CompletedActivity> workouts) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyWorkouts, jsonEncode(workouts.map((w) => w.toJson()).toList()));
  }

  Future<List<GoalAchievement>> getAchievements() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_keyAchievements);
    if (jsonString != null) {
      try {
        final List list = jsonDecode(jsonString);
        return list.map((e) => GoalAchievement.fromJson(e)).toList();
      } catch (_) {}
    }
    return [
      GoalAchievement(
        id: '1',
        title: 'First Step',
        description: 'Complete 1,000 steps in a day',
        iconName: 'directions_walk',
        isUnlocked: true,
        unlockedDate: '2026-07-20',
      ),
      GoalAchievement(
        id: '2',
        title: '10K Master',
        description: 'Reach 10,000 steps in a single day',
        iconName: 'emoji_events',
        isUnlocked: true,
        unlockedDate: '2026-07-24',
      ),
      GoalAchievement(
        id: '3',
        title: 'Hydration Hero',
        description: 'Drink 2,500ml of water in a day',
        iconName: 'water_drop',
        isUnlocked: false,
      ),
      GoalAchievement(
        id: '4',
        title: 'Calorie Crusher',
        description: 'Burn 500 active calories in one workout',
        iconName: 'local_fire_department',
        isUnlocked: false,
      ),
    ];
  }

  Future<void> saveAchievements(List<GoalAchievement> achievements) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyAchievements, jsonEncode(achievements.map((a) => a.toJson()).toList()));
  }

  Future<bool> isDarkMode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_keyDarkMode) ?? true;
  }

  Future<void> setDarkMode(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyDarkMode, value);
  }

  Future<bool> getNotificationsEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_keyNotifications) ?? true;
  }

  Future<void> setNotificationsEnabled(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyNotifications, value);
  }

  Future<void> clearAllData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
  }
}
