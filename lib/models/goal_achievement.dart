class GoalAchievement {
  final String id;
  final String title;
  final String description;
  final String iconName;
  final bool isUnlocked;
  final String? unlockedDate;

  GoalAchievement({
    required this.id,
    required this.title,
    required this.description,
    required this.iconName,
    required this.isUnlocked,
    this.unlockedDate,
  });

  GoalAchievement copyWith({
    bool? isUnlocked,
    String? unlockedDate,
  }) {
    return GoalAchievement(
      id: id,
      title: title,
      description: description,
      iconName: iconName,
      isUnlocked: isUnlocked ?? this.isUnlocked,
      unlockedDate: unlockedDate ?? this.unlockedDate,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'description': description,
        'iconName': iconName,
        'isUnlocked': isUnlocked,
        'unlockedDate': unlockedDate,
      };

  factory GoalAchievement.fromJson(Map<String, dynamic> json) =>
      GoalAchievement(
        id: json['id'] ?? '',
        title: json['title'] ?? '',
        description: json['description'] ?? '',
        iconName: json['iconName'] ?? 'star',
        isUnlocked: json['isUnlocked'] ?? false,
        unlockedDate: json['unlockedDate'],
      );
}
