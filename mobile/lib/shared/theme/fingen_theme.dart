import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// FinGen Design System - Flutter Theme
/// Baseado no design system web com cores teal/green e tokens consistentes.
/// Utiliza Material 3 e ThemeExtensions para tokens personalizados.
class FinGenTheme {
  // ═══ Brand Colors (Teal/Green) ═══
  static const Color brand = Color(0xFF0F766E);      // #0F766E - teal-700
  static const Color brandHover = Color(0xFF0D9488); // #0d9488 - teal-600
  static const Color brandSoft = Color(0x140F766E);  // rgba(15,118,110,0.08)
  
  // ═══ Light Theme Colors ═══
  static const Color lightBg = Color(0xFFF4F7F4);        // #F4F7F4
  static const Color lightSurface = Color(0xFFFFFFFF);   // #FFFFFF
  static const Color lightSurface2 = Color(0xFFEDF2ED);  // #EDF2ED
  static const Color lightSurfaceH = Color(0xFFE4EDE4);  // #E4EDE4
  static const Color lightBorder = Color(0x1A0F766E);    // rgba(15,118,110,0.10)
  static const Color lightBorder2 = Color(0x2E0F766E);   // rgba(15,118,110,0.18)
  static const Color lightText1 = Color(0xFF0F2721);     // #0F2721
  static const Color lightText2 = Color(0xFF2D4A3E);     // #2D4A3E
  static const Color lightText3 = Color(0xFF6B7F6E);     // #6B7F6E
  
  // ═══ Dark Theme Colors ═══
  static const Color darkBg = Color(0xFF0F172A);         // #0F172A - slate-900
  static const Color darkSurface = Color(0xFF1E293B);    // #1E293B - slate-800
  static const Color darkSurface2 = Color(0xFF334155);   // #334155 - slate-700
  static const Color darkSurfaceH = Color(0xFF475569);   // #475569 - slate-600
  static const Color darkBorder = Color(0x1AFFFFFF);     // rgba(255,255,255,0.10)
  static const Color darkBorder2 = Color(0x26FFFFFF);    // rgba(255,255,255,0.15)
  static const Color darkText1 = Color(0xFFF8FAFC);      // #F8FAFC
  static const Color darkText2 = Color(0xFF94A3B8);      // #94A3B8
  static const Color darkText3 = Color(0xFF64748B);      // #64748B
  
  // ═══ Semantic Colors ═══
  static const Color success = Color(0xFF16A34A);        // #16a34a
  static const Color successBg = Color(0xFFF0FDF4);      // #f0fdf4
  static const Color successBorder = Color(0xFFBBF7D0);  // #bbf7d0
  
  static const Color error = Color(0xFFDC2626);          // #dc2626
  static const Color errorBg = Color(0xFFFEF2F2);        // #fef2f2
  static const Color errorBorder = Color(0xFFFECACA);    // #fecaca
  
  static const Color warning = Color(0xFFD97706);        // #D97706
  static const Color warningBg = Color(0xFFFFFBEB);      // #fffbeb
  static const Color warningBorder = Color(0xFFFDE68A);  // #fde68a

  // ═══ Theme Extensions ═══
  
  /// Get light custom colors
  static final lightColors = FinGenColors(
    brandSoft: brandSoft,
    surface2: lightSurface2,
    surfaceH: lightSurfaceH,
    border: lightBorder,
    border2: lightBorder2,
    text1: lightText1,
    text2: lightText2,
    text3: lightText3,
    success: success,
    successBg: successBg,
    successBorder: successBorder,
    warning: warning,
    warningBg: warningBg,
    warningBorder: warningBorder,
    error: error,
    errorBg: errorBg,
    errorBorder: errorBorder,
  );

  /// Get light custom shadows
  static final lightShadows = FinGenShadows(
    xs: const [BoxShadow(color: Color(0x0F0F766E), offset: Offset(0, 1), blurRadius: 3)],
    sm: const [
      BoxShadow(color: Color(0x120F766E), offset: Offset(0, 4), blurRadius: 14),
      BoxShadow(color: Color(0x0A0F766E), offset: Offset(0, 1), blurRadius: 3),
    ],
    md: const [
      BoxShadow(color: Color(0x170F766E), offset: Offset(0, 8), blurRadius: 24),
      BoxShadow(color: Color(0x0D0F766E), offset: Offset(0, 2), blurRadius: 6),
    ],
    lg: const [
      BoxShadow(color: Color(0x1A0F766E), offset: Offset(0, 20), blurRadius: 48),
      BoxShadow(color: Color(0x0F0F766E), offset: Offset(0, 4), blurRadius: 12),
    ],
    xl: const [
      BoxShadow(color: Color(0x1F0F766E), offset: Offset(0, 32), blurRadius: 72),
      BoxShadow(color: Color(0x120F766E), offset: Offset(0, 8), blurRadius: 20),
    ],
  );

  /// Get dark custom colors
  static final darkColors = FinGenColors(
    brandSoft: brandSoft.withOpacity(0.15),
    surface2: darkSurface2,
    surfaceH: darkSurfaceH,
    border: darkBorder,
    border2: darkBorder2,
    text1: darkText1,
    text2: darkText2,
    text3: darkText3,
    success: success,
    successBg: const Color(0xFF064E3B), 
    successBorder: const Color(0xFF065F46).withOpacity(0.3),
    warning: warning,
    warningBg: const Color(0xFF78350F), 
    warningBorder: const Color(0xFF92400E).withOpacity(0.3),
    error: error,
    errorBg: const Color(0xFF7F1D1D),   
    errorBorder: const Color(0xFF991B1B).withOpacity(0.3),
  );

  /// Get dark custom shadows
  static final darkShadows = FinGenShadows(
    xs: const [BoxShadow(color: Color(0x4D000000), offset: Offset(0, 1), blurRadius: 3)],
    sm: const [
      BoxShadow(color: Color(0x66000000), offset: Offset(0, 4), blurRadius: 14),
      BoxShadow(color: Color(0x33000000), offset: Offset(0, 1), blurRadius: 3),
    ],
    md: const [
      BoxShadow(color: Color(0x80000000), offset: Offset(0, 8), blurRadius: 24),
      BoxShadow(color: Color(0x4D000000), offset: Offset(0, 2), blurRadius: 6),
    ],
    lg: const [
      BoxShadow(color: Color(0x99000000), offset: Offset(0, 20), blurRadius: 48),
      BoxShadow(color: Color(0x66000000), offset: Offset(0, 4), blurRadius: 12),
    ],
    xl: const [
      BoxShadow(color: Color(0xB3000000), offset: Offset(0, 32), blurRadius: 72),
      BoxShadow(color: Color(0x80000000), offset: Offset(0, 8), blurRadius: 20),
    ],
  );

  // ═══ Light Theme ═══
  static ThemeData get lightTheme {
    final base = ThemeData.light(useMaterial3: true);
    return _buildTheme(base, lightColors, lightShadows, Brightness.light);
  }
  
  // ═══ Dark Theme ═══
  static ThemeData get darkTheme {
    final base = ThemeData.dark(useMaterial3: true);
    return _buildTheme(base, darkColors, darkShadows, Brightness.dark);
  }

  static ThemeData _buildTheme(ThemeData base, FinGenColors colors, FinGenShadows shadows, Brightness brightness) {
    final bool isDark = brightness == Brightness.dark;
    
    return base.copyWith(
      primaryColor: brand,
      scaffoldBackgroundColor: isDark ? darkBg : lightBg,
      extensions: [colors, shadows],
      colorScheme: ColorScheme.fromSeed(
        seedColor: brand,
        brightness: brightness,
        primary: brand,
        secondary: brand,
        surface: isDark ? darkSurface : lightSurface,
        onSurface: colors.text1,
        onPrimary: Colors.white,
        error: colors.error,
      ),
      textTheme: _buildTextTheme(base.textTheme, colors),
      cardTheme: CardTheme(
        color: isDark ? darkSurface : lightSurface,
        elevation: 0,
        margin: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
          side: BorderSide(color: colors.border, width: 1),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colors.surface2,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border, width: 1),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border, width: 1),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: brand, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.error, width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        hintStyle: GoogleFonts.dmSans(
          color: colors.text3.withOpacity(0.6),
          fontSize: 14.5,
        ),
        labelStyle: GoogleFonts.dmSans(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: colors.text3,
          letterSpacing: 0.84,
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: brand,
          foregroundColor: Colors.white,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: GoogleFonts.dmSans(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            letterSpacing: 0,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: colors.text2,
          side: BorderSide(color: colors.border, width: 1.5),
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: GoogleFonts.dmSans(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: GoogleFonts.notoSerifJp(
          fontSize: 22,
          fontWeight: FontWeight.w600,
          color: colors.text1,
          letterSpacing: -0.4,
        ),
        iconTheme: IconThemeData(color: colors.text2, size: 22),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: isDark ? darkSurface.withOpacity(0.8) : lightSurface.withOpacity(0.8),
        selectedItemColor: brand,
        unselectedItemColor: colors.text3,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
        selectedLabelStyle: GoogleFonts.dmSans(fontSize: 11, fontWeight: FontWeight.w600),
        unselectedLabelStyle: GoogleFonts.dmSans(fontSize: 11, fontWeight: FontWeight.w400),
      ),
      dividerTheme: DividerThemeData(
        color: colors.border,
        thickness: 1,
        space: 1,
      ),
    );
  }

  static TextTheme _buildTextTheme(TextTheme base, FinGenColors colors) {
    return base.copyWith(
      headlineLarge: GoogleFonts.notoSerifJp(
        fontSize: 32,
        fontWeight: FontWeight.w500,
        color: colors.text1,
        letterSpacing: -0.64,
        height: 1.2,
      ),
      headlineMedium: GoogleFonts.notoSerifJp(
        fontSize: 28,
        fontWeight: FontWeight.w500,
        color: colors.text1,
        letterSpacing: -0.56,
        height: 1.2,
      ),
      titleLarge: GoogleFonts.dmSans(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: colors.text1,
        letterSpacing: -0.4,
      ),
      titleMedium: GoogleFonts.dmSans(
        fontSize: 17,
        fontWeight: FontWeight.w600,
        color: colors.text1,
        letterSpacing: -0.34,
      ),
      bodyLarge: GoogleFonts.dmSans(
        fontSize: 16,
        fontWeight: FontWeight.w400,
        color: colors.text1,
        height: 1.6,
      ),
      bodyMedium: GoogleFonts.dmSans(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        color: colors.text2,
        height: 1.6,
      ),
      bodySmall: GoogleFonts.dmSans(
        fontSize: 13,
        fontWeight: FontWeight.w400,
        color: colors.text3,
        height: 1.5,
      ),
      labelLarge: GoogleFonts.dmSans(
        fontSize: 13,
        fontWeight: FontWeight.w500,
        color: colors.text1,
        letterSpacing: 0.1,
      ),
      labelMedium: GoogleFonts.dmSans(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        color: colors.text3,
        letterSpacing: 0.84,
        height: 1.3,
      ),
    );
  }
  
  // ═══ Legacy Helpers (Mantidos para compatibilidade) ═══
  
  static const Color primary = brand;
  static const Color background = lightBg;
  static const Color surface = lightSurface;
  static const Color textPrimary = lightText1;
  static const Color textSecondary = lightText2;

  static Color text1(BuildContext context) => colors(context).text1;
  static Color text2(BuildContext context) => colors(context).text2;
  static Color text3(BuildContext context) => colors(context).text3;
  static Color surfaceColor(BuildContext context) => Theme.of(context).colorScheme.surface;
  static Color surface2(BuildContext context) => colors(context).surface2;
  static Color border(BuildContext context) => colors(context).border;

  static FinGenColors colors(BuildContext context) => 
      Theme.of(context).extension<FinGenColors>() ?? lightColors;

  static FinGenShadows shadows(BuildContext context) => 
      Theme.of(context).extension<FinGenShadows>() ?? lightShadows;
}

/// Extensão de Cores para o Design System FinGen
class FinGenColors extends ThemeExtension<FinGenColors> {
  final Color brandSoft;
  final Color surface2;
  final Color surfaceH;
  final Color border;
  final Color border2;
  final Color text1;
  final Color text2;
  final Color text3;
  final Color success;
  final Color successBg;
  final Color successBorder;
  final Color warning;
  final Color warningBg;
  final Color warningBorder;
  final Color error;
  final Color errorBg;
  final Color errorBorder;

  FinGenColors({
    required this.brandSoft,
    required this.surface2,
    required this.surfaceH,
    required this.border,
    required this.border2,
    required this.text1,
    required this.text2,
    required this.text3,
    required this.success,
    required this.successBg,
    required this.successBorder,
    required this.warning,
    required this.warningBg,
    required this.warningBorder,
    required this.error,
    required this.errorBg,
    required this.errorBorder,
  });

  @override
  ThemeExtension<FinGenColors> copyWith({
    Color? brandSoft,
    Color? surface2,
    Color? surfaceH,
    Color? border,
    Color? border2,
    Color? text1,
    Color? text2,
    Color? text3,
    Color? success,
    Color? successBg,
    Color? successBorder,
    Color? warning,
    Color? warningBg,
    Color? warningBorder,
    Color? error,
    Color? errorBg,
    Color? errorBorder,
  }) {
    return FinGenColors(
      brandSoft: brandSoft ?? this.brandSoft,
      surface2: surface2 ?? this.surface2,
      surfaceH: surfaceH ?? this.surfaceH,
      border: border ?? this.border,
      border2: border2 ?? this.border2,
      text1: text1 ?? this.text1,
      text2: text2 ?? this.text2,
      text3: text3 ?? this.text3,
      success: success ?? this.success,
      successBg: successBg ?? this.successBg,
      successBorder: successBorder ?? this.successBorder,
      warning: warning ?? this.warning,
      warningBg: warningBg ?? this.warningBg,
      warningBorder: warningBorder ?? this.warningBorder,
      error: error ?? this.error,
      errorBg: errorBg ?? this.errorBg,
      errorBorder: errorBorder ?? this.errorBorder,
    );
  }

  @override
  ThemeExtension<FinGenColors> lerp(ThemeExtension<FinGenColors>? other, double t) {
    if (other is! FinGenColors) return this;
    return FinGenColors(
      brandSoft: Color.lerp(brandSoft, other.brandSoft, t)!,
      surface2: Color.lerp(surface2, other.surface2, t)!,
      surfaceH: Color.lerp(surfaceH, other.surfaceH, t)!,
      border: Color.lerp(border, other.border, t)!,
      border2: Color.lerp(border2, other.border2, t)!,
      text1: Color.lerp(text1, other.text1, t)!,
      text2: Color.lerp(text2, other.text2, t)!,
      text3: Color.lerp(text3, other.text3, t)!,
      success: Color.lerp(success, other.success, t)!,
      successBg: Color.lerp(successBg, other.successBg, t)!,
      successBorder: Color.lerp(successBorder, other.successBorder, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      warningBg: Color.lerp(warningBg, other.warningBg, t)!,
      warningBorder: Color.lerp(warningBorder, other.warningBorder, t)!,
      error: Color.lerp(error, other.error, t)!,
      errorBg: Color.lerp(errorBg, other.errorBg, t)!,
      errorBorder: Color.lerp(errorBorder, other.errorBorder, t)!,
    );
  }
}

/// Extensão de Sombras Zen para o Design System FinGen
class FinGenShadows extends ThemeExtension<FinGenShadows> {
  final List<BoxShadow> xs;
  final List<BoxShadow> sm;
  final List<BoxShadow> md;
  final List<BoxShadow> lg;
  final List<BoxShadow> xl;

  FinGenShadows({
    required this.xs,
    required this.sm,
    required this.md,
    required this.lg,
    required this.xl,
  });

  @override
  ThemeExtension<FinGenShadows> copyWith({
    List<BoxShadow>? xs,
    List<BoxShadow>? sm,
    List<BoxShadow>? md,
    List<BoxShadow>? lg,
    List<BoxShadow>? xl,
  }) {
    return FinGenShadows(
      xs: xs ?? this.xs,
      sm: sm ?? this.sm,
      md: md ?? this.md,
      lg: lg ?? this.lg,
      xl: xl ?? this.xl,
    );
  }

  @override
  ThemeExtension<FinGenShadows> lerp(ThemeExtension<FinGenShadows>? other, double t) {
    if (other is! FinGenShadows) return this;
    return FinGenShadows(
      xs: BoxShadow.lerpList(xs, other.xs, t)!,
      sm: BoxShadow.lerpList(sm, other.sm, t)!,
      md: BoxShadow.lerpList(md, other.md, t)!,
      lg: BoxShadow.lerpList(lg, other.lg, t)!,
      xl: BoxShadow.lerpList(xl, other.xl, t)!,
    );
  }
}