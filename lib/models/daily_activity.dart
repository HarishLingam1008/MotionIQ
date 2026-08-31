class DailyActivity {
  final String date; // YYYY-MM-DD
  final int steps;
  final double distanceKm;
  final int caloriesBurned;
  final int activeMinutes;
  final int waterIntakeMl;

  DailyActivity({
    required this.date,
    required this.steps,
    required this.distanceKm,
    required this.caloriesBurned,
    required this.activeMinutes,
    required this.waterIntakeMl,
  });

  DailyActivity copyWith({
    String? date,
    int? steps,
    double? distanceKm,
    int? caloriesBurned,
    int? activeMinutes,
    int? waterIntakeMl,
  }) {
    return DailyActivity(
      date: date ?? this.date,
      steps: steps ?? this.steps,
      distanceKm: distanceKm ?? this.distanceKm,
      caloriesBurned: caloriesBurned ?? this.caloriesBurned,
      activeMinutes: activeMinutes ?? this.activeMinutes,
      waterIntakeMl: waterIntakeMl ?? this.waterIntakeMl,
    );
  }

  Map<String, dynamic> toJson() => {
        'date': date,
        'steps': steps,
        'distanceKm': distanceKm,
        'caloriesBurned': caloriesBurned,
        'activeMinutes': activeMinutes,
        'waterIntakeMl': waterIntakeMl,
      };

  factory DailyActivity.fromJson(Map<String, dynamic> json) => DailyActivity(
        date: json['date'] ?? '',
        steps: json['steps'] ?? 0,
        distanceKm: (json['distanceKm'] as num?)?.toDouble() ?? 0.0,
        caloriesBurned: json['caloriesBurned'] ?? 0,
        activeMinutes: json['activeMinutes'] ?? 0,
        waterIntakeMl: json['waterIntakeMl'] ?? 0,
      );
}
