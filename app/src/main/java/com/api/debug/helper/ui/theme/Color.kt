package com.api.debug.helper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

internal val Purple80 = androidx.compose.ui.graphics.Color(0xFFD0BCFF)
internal val PurpleGrey80 = androidx.compose.ui.graphics.Color(0xFFCCC2DC)
internal val Pink80 = androidx.compose.ui.graphics.Color(0xFFEFB8C8)

internal val Purple40 = androidx.compose.ui.graphics.Color(0xFF6650a4)
internal val PurpleGrey40 = androidx.compose.ui.graphics.Color(0xFF625b71)
internal val Pink40 = androidx.compose.ui.graphics.Color(0xFF7D5260)