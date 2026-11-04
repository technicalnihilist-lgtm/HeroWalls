package com.herowalls.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.Metal
import com.herowalls.app.ui.theme.TextPrimary

@Composable
fun HeroShield(modifier: Modifier = Modifier, size: Int = 28) {
    Canvas(modifier = modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.16f)
            lineTo(w, h * 0.55f)
            quadraticBezierTo(w * 0.92f, h * 0.9f, w * 0.5f, h)
            quadraticBezierTo(w * 0.08f, h * 0.9f, 0f, h * 0.55f)
            lineTo(0f, h * 0.16f)
            close()
        }
        drawPath(shield, color = Crimson)
        val bolt = Path().apply {
            moveTo(w * 0.42f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.20f)
            lineTo(w * 0.52f, h * 0.48f)
            lineTo(w * 0.72f, h * 0.48f)
            lineTo(w * 0.34f, h * 0.88f)
            lineTo(w * 0.45f, h * 0.55f)
            lineTo(w * 0.28f, h * 0.55f)
            close()
        }
        drawPath(bolt, color = Color.Black.copy(alpha = 0.92f))
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(w * 0.12f, h * 0.18f),
            end = Offset(w * 0.5f, h * 0.04f),
            strokeWidth = h * 0.03f
        )
    }
}

@Composable
fun HeroWallsWordmark(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HeroShield(size = if (compact) 24 else 30)
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Row {
                Text(
                    text = "HERO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "WALLS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Crimson
                )
            }
            if (!compact) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "PREMIUM SUPERHERO WALLPAPERS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, letterSpacing = 1.4.sp),
                    color = Metal.copy(alpha = 0.75f)
                )
            }
        }
    }
}
