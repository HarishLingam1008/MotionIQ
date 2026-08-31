import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const Color primaryGreen = Color(0xFF00E676);
  static const Color primaryBlue = Color(0xFF00B0FF);
  static const Color accentOrange = Color(0xFFFF6D00);
  static const Color darkBackground = Color(0xFF121824);
  static const Color darkSurface = Color(0xFF1E2638);
  static const Color lightBackground = Color(0xFFF8FAFC);
  static const Color lightSurface = Color(0xFFFFFFFF);

  static ThemeData lightTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    colorScheme: ColorScheme.fromSeed(
      seedColor: primaryGreen,
      brightness: Brightness.light,
      primary: primaryGreen,
      secondary: primaryBlue,
      tertiary: accentOrange,
      background: lightBackground,
      surface: lightSurface,
    ),
    scaffoldBackgroundColor: lightBackground,
    cardTheme: CardTheme(
      elevation: 2,
      shape: RoundedCornerShape(20),
      color: lightSurface,
    ),
    textTheme: GoogleFonts.interTextTheme(ThemeData.light().textTheme),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
    ),
  );

  static ThemeData darkTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: ColorScheme.fromSeed(
      seedColor: primaryGreen,
      brightness: Brightness.dark,
      primary: primaryGreen,
      secondary: primaryBlue,
      tertiary: accentOrange,
      background: darkBackground,
      surface: darkSurface,
    ),
    scaffoldBackgroundColor: darkBackground,
    cardTheme: CardTheme(
      elevation: 4,
      shape: RoundedCornerShape(20),
      color: darkSurface,
    ),
    textTheme: GoogleFonts.interTextTheme(ThemeData.dark().textTheme),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
    ),
  );
}
