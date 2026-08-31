class MealLog {
  final String id;
  final String name;
  final int calories;
  final double proteinG;
  final double carbsG;
  final double fatG;
  final String time;

  MealLog({
    required this.id,
    required this.name,
    required this.calories,
    required this.proteinG,
    required this.carbsG,
    required this.fatG,
    required this.time,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'calories': calories,
        'proteinG': proteinG,
        'carbsG': carbsG,
        'fatG': fatG,
        'time': time,
      };

  factory MealLog.fromJson(Map<String, dynamic> json) => MealLog(
        id: json['id'] ?? '',
        name: json['name'] ?? '',
        calories: json['calories'] ?? 0,
        proteinG: (json['proteinG'] as num?)?.toDouble() ?? 0.0,
        carbsG: (json['carbsG'] as num?)?.toDouble() ?? 0.0,
        fatG: (json['fatG'] as num?)?.toDouble() ?? 0.0,
        time: json['time'] ?? '',
      );
}
