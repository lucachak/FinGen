import 'package:flutter/material.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';

enum ZenShadowSize { xs, sm, md, lg, xl }

class ZenCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final ZenShadowSize shadowSize;
  final Color? color;
  final double? borderRadius;
  final BorderSide? border;

  const ZenCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.shadowSize = ZenShadowSize.sm,
    this.color,
    this.borderRadius,
    this.border,
  });

  @override
  Widget build(BuildContext context) {
    final shadows = FinGenTheme.shadows(context);
    final colors = FinGenTheme.colors(context);

    List<BoxShadow> getShadow() {
      switch (shadowSize) {
        case ZenShadowSize.xs: return shadows.xs;
        case ZenShadowSize.sm: return shadows.sm;
        case ZenShadowSize.md: return shadows.md;
        case ZenShadowSize.lg: return shadows.lg;
        case ZenShadowSize.xl: return shadows.xl;
      }
    }

    return Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: color ?? Theme.of(context).cardTheme.color,
        borderRadius: BorderRadius.circular(borderRadius ?? 24),
        border: Border.fromBorderSide(
          border ?? BorderSide(color: colors.border, width: 1),
        ),
        boxShadow: getShadow(),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(borderRadius ?? 24),
        child: child,
      ),
    );
  }
}
