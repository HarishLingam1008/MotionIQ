class CompletedActivity {
  final String id;
  final String activityType; // Running, Walking, Cycling, HIIT, Gym
  final int durationMinutes;
  final int caloriesBurned;
  final double distanceKm;
  final String date;

  CompletedActivity({
    required this.id,
    required this.activityType,
    required this.durationMinutes,
    required this.caloriesBurned,
    required this.distanceKm,
    required this.date,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'activityType': activityType,
        'durationMinutes': durationMinutes,
        'caloriesBurned': caloriesBurned,
        'distanceKm': distanceKm,
        'date': date,
      };

  factory CompletedActivity.fromJson(Map<String, dynamic> json) =>
      CompletedActivity(
        id: json['id'] ?? '',
        activityType: json['activityType'] ?? 'Workout',
        durationMinutes: json['durationMinutes'] ?? 0,
        caloriesBurned: json['caloriesBurned'] ?? 0,
        distanceKm: (json['distanceKm'] as num?)?.toDouble() ?? 0.0,
        date: json['date'] ?? '',
      );
}
