class UserProfile {
  final String name;
  final double heightCm;
  final double weightKg;
  final int age;
  final String gender;
  final int stepGoal;
  final int waterGoalMl;
  final bool isImperial;

  UserProfile({
    required this.name,
    required this.heightCm,
    required this.weightKg,
    required this.age,
    required this.gender,
    required this.stepGoal,
    required this.waterGoalMl,
    required this.isImperial,
  });

  UserProfile copyWith({
    String? name,
    double? heightCm,
    double? weightKg,
    int? age,
    String? gender,
    int? stepGoal,
    int? waterGoalMl,
    bool? isImperial,
  }) {
    return UserProfile(
      name: name ?? this.name,
      heightCm: heightCm ?? this.heightCm,
      weightKg: weightKg ?? this.weightKg,
      age: age ?? this.age,
      gender: gender ?? this.gender,
      stepGoal: stepGoal ?? this.stepGoal,
      waterGoalMl: waterGoalMl ?? this.waterGoalMl,
      isImperial: isImperial ?? this.isImperial,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'heightCm': heightCm,
      'weightKg': weightKg,
      'age': age,
      'gender': gender,
      'stepGoal': stepGoal,
      'waterGoalMl': waterGoalMl,
      'isImperial': isImperial,
    };
  }

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      name: json['name'] ?? 'Athlete',
      heightCm: (json['heightCm'] as num?)?.toDouble() ?? 175.0,
      weightKg: (json['weightKg'] as num?)?.toDouble() ?? 70.0,
      age: json['age'] ?? 28,
      gender: json['gender'] ?? 'Male',
      stepGoal: json['stepGoal'] ?? 10000,
      waterGoalMl: json['waterGoalMl'] ?? 2500,
      isImperial: json['isImperial'] ?? false,
    );
  }

  static UserProfile defaultProfile() {
    return UserProfile(
      name: 'Runner',
      heightCm: 175.0,
      weightKg: 70.0,
      age: 28,
      gender: 'Male',
      stepGoal: 10000,
      waterGoalMl: 2500,
      isImperial: false,
    );
  }
}
