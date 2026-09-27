package com.arintrack.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.compose.ui.graphics.Color

class ArinTrackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Reads live room metrics
        provideContent {
            GlanceTheme {
                ArinTrackWidgetContent(
                    reelsToday = 34,
                    dailyCap = 45,
                    screenTimeStr = "2h 45m"
                )
            }
        }
    }

    @Composable
    private fun ArinTrackWidgetContent(reelsToday: Int, dailyCap: Int, screenTimeStr: String) {
        val bgDark = Color(0xFF171412)
        val orangeAccent = Color(0xFFFF6B00)
        val textPrimary = Color(0xFFFAF8F5)
        val textSecondary = Color(0xFF9E9790)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgDark)
                .padding(16.dp)
        ) {
            Column(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "DAILY LIMIT",
                        style = TextStyle(color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$reelsToday",
                        style = TextStyle(color = orangeAccent, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = " / $dailyCap",
                        style = TextStyle(color = textSecondary, fontSize = 14.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = "Screen Time: $screenTimeStr",
                    style = TextStyle(color = textPrimary, fontSize = 12.sp)
                )
            }
        }
    }
}

class ArinTrackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArinTrackWidget()
}