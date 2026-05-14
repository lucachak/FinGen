import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'shared/theme/fingen_theme.dart';
import 'shared/router.dart';

import 'package:intl/date_symbol_data_local.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('pt_BR', null);
  runApp(
    const ProviderScope(
      child: FinGenApp(),
    ),
  );
}

class FinGenApp extends StatelessWidget {
  const FinGenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'FinGen',
      debugShowCheckedModeBanner: false,
      theme: FinGenTheme.lightTheme,
      darkTheme: FinGenTheme.darkTheme,
      themeMode: ThemeMode.system,
      routerConfig: goRouter,
    );
  }
}
