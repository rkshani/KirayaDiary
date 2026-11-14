package com.kiraya.diary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Base = Typography()

val KirayaTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall,
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium,
    labelSmall = Base.labelSmall,
)

val KirayaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
