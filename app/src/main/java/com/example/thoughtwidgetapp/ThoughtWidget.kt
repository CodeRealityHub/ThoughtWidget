
package com.example.thoughtwidgetapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun fixedColor(color: Color) =
    ColorProvider(day = color, night = color)

private data class WidgetPalette(
    val background: Color,
    val card: Color,
    val imageSurface: Color,
    val accent: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val divider: Color,
    val isGradient: Boolean = false
)

private fun paletteFor(appearance: String) =
    when (appearance) {

        ThoughtStorage.APPEARANCE_DARK -> WidgetPalette(
            background = Color(0xFF171526),
            card = Color(0xFF29263D),
            imageSurface = Color(0xFF37324F),
            accent = Color(0xFFC5B5FF),
            primaryText = Color(0xFFF5F2FF),
            secondaryText = Color(0xFFC6C0D8),
            divider = Color(0xFF49435D)
        )

        ThoughtStorage.APPEARANCE_GRADIENT -> WidgetPalette(
            background = Color(0xFF49328F),
            card = Color(0xFF39276D),
            imageSurface = Color(0xFF514080),
            accent = Color(0xFFE4D8FF),
            primaryText = Color.White,
            secondaryText = Color(0xFFDED4F5),
            divider = Color(0xFF705BA5),
            isGradient = true
        )

        else -> WidgetPalette(
            background = Color(0xFFF5F3FF),
            card = Color.White,
            imageSurface = Color(0xFFF0ECFA),
            accent = Color(0xFF6547C7),
            primaryText = Color(0xFF242333),
            secondaryText = Color(0xFF626174),
            divider = Color(0xFFEAE6F3)
        )
    }

class ThoughtWidget : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {

        val data = withContext(Dispatchers.IO) {

            val appearance = ThoughtStorage.getAppearance(context)

            WidgetData(
                thought = ThoughtStorage.getThought(context),
                ocrText = ThoughtStorage.getOcrText(context),
                bitmap = decodeWidgetBitmap(
                    ThoughtStorage.getImagePath(context)
                ),
                appearance = appearance,
                gradientBitmap = if (
                    appearance == ThoughtStorage.APPEARANCE_GRADIENT
                ) {
                    createGradientBitmap()
                } else {
                    null
                }
            )
        }

        val palette = paletteFor(data.appearance)

        val openAppAction = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        provideContent {

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(fixedColor(palette.background))
                    .cornerRadius(22.dp)
            ) {

                // Gradient background
                if (data.gradientBitmap != null) {
                    Image(
                        provider = ImageProvider(data.gradientBitmap),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }

                // Scrollable widget content
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {

                    item {

                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(openAppAction),
                            verticalAlignment = Alignment.Top
                        ) {

                            // HEADER
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "🧠",
                                    style = TextStyle(fontSize = 21.sp)
                                )

                                Spacer(
                                    modifier = GlanceModifier.width(8.dp)
                                )

                                Column {

                                    Text(
                                        text = "MY THOUGHTS",
                                        style = TextStyle(
                                            color = fixedColor(palette.accent),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    Text(
                                        text = "YOUR PERSONAL SPACE",
                                        style = TextStyle(
                                            color = fixedColor(
                                                palette.secondaryText
                                            ),
                                            fontSize = 9.sp
                                        )
                                    )
                                }

                                Spacer(
                                    modifier = GlanceModifier.width(8.dp)
                                )

                                Text(
                                    text = "✦",
                                    style = TextStyle(
                                        color = fixedColor(palette.accent),
                                        fontSize = 22.sp
                                    )
                                )
                            }

                            Spacer(
                                modifier = GlanceModifier.height(14.dp)
                            )

                            // CONTENT CARD
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(fixedColor(palette.card))
                                    .cornerRadius(18.dp)
                                    .padding(13.dp),
                                verticalAlignment = Alignment.Top
                            ) {

                                // CARD HEADER
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = "✦",
                                        style = TextStyle(
                                            color = fixedColor(palette.accent),
                                            fontSize = 17.sp
                                        )
                                    )

                                    Spacer(
                                        modifier = GlanceModifier.width(6.dp)
                                    )

                                    Text(
                                        text = "DAILY REMINDER",
                                        style = TextStyle(
                                            color = fixedColor(palette.accent),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(
                                    modifier = GlanceModifier.height(12.dp)
                                )

                                // IMAGE
                                if (data.bitmap != null) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = "▧",
                                            style = TextStyle(
                                                color = fixedColor(palette.accent),
                                                fontSize = 16.sp
                                            )
                                        )

                                        Spacer(
                                            modifier = GlanceModifier.width(6.dp)
                                        )

                                        Text(
                                            text = "YOUR MOMENT",
                                            style = TextStyle(
                                                color = fixedColor(
                                                    palette.secondaryText
                                                ),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(
                                        modifier = GlanceModifier.height(8.dp)
                                    )

                                    Image(
                                        provider = ImageProvider(data.bitmap),
                                        contentDescription = "Uploaded thought image",
                                        contentScale = ContentScale.Fit,
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .background(
                                                fixedColor(palette.imageSurface)
                                            )
                                            .cornerRadius(12.dp)
                                    )

                                    Spacer(
                                        modifier = GlanceModifier.height(14.dp)
                                    )
                                }

                                // THOUGHT
                                Row(
                                    verticalAlignment = Alignment.Top
                                ) {

                                    Text(
                                        text = "❝",
                                        style = TextStyle(
                                            color = fixedColor(palette.accent),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    Spacer(
                                        modifier = GlanceModifier.width(7.dp)
                                    )

                                    Text(
                                        text = data.thought.ifBlank {
                                            "Your thoughts deserve a place."
                                        },
                                        style = TextStyle(
                                            color = fixedColor(
                                                palette.primaryText
                                            ),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                // OCR
                                if (data.ocrText.isNotBlank()) {

                                    Spacer(
                                        modifier = GlanceModifier.height(14.dp)
                                    )

                                    Spacer(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(
                                                fixedColor(palette.divider)
                                            )
                                    )

                                    Spacer(
                                        modifier = GlanceModifier.height(10.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = "▤",
                                            style = TextStyle(
                                                color = fixedColor(palette.accent),
                                                fontSize = 16.sp
                                            )
                                        )

                                        Spacer(
                                            modifier = GlanceModifier.width(6.dp)
                                        )

                                        Text(
                                            text = "TEXT FROM IMAGE",
                                            style = TextStyle(
                                                color = fixedColor(palette.accent),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(
                                        modifier = GlanceModifier.height(6.dp)
                                    )

                                    Text(
                                        text = data.ocrText,
                                        style = TextStyle(
                                            color = fixedColor(
                                                palette.secondaryText
                                            ),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Spacer(
                                    modifier = GlanceModifier.height(8.dp)
                                )

                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {

                                    Text(
                                        text = "✧",
                                        style = TextStyle(
                                            color = fixedColor(palette.accent),
                                            fontSize = 18.sp
                                        )
                                    )
                                }
                            }

                            Spacer(
                                modifier = GlanceModifier.height(12.dp)
                            )

                            // FOOTER
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "♡",
                                    style = TextStyle(
                                        color = fixedColor(palette.accent),
                                        fontSize = 17.sp
                                    )
                                )

                                Spacer(
                                    modifier = GlanceModifier.width(5.dp)
                                )

                                Text(
                                    text = "Your thoughts, your journey",
                                    style = TextStyle(
                                        color = fixedColor(
                                            palette.secondaryText
                                        ),
                                        fontSize = 10.sp
                                    )
                                )

                                Spacer(
                                    modifier = GlanceModifier.width(5.dp)
                                )

                                Text(
                                    text = "+",
                                    style = TextStyle(
                                        color = fixedColor(palette.accent),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private data class WidgetData(
        val thought: String,
        val ocrText: String,
        val bitmap: Bitmap?,
        val appearance: String,
        val gradientBitmap: Bitmap?
    )

    private fun createGradientBitmap(): Bitmap {

        val width = 400
        val height = 400

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(
                    android.graphics.Color.rgb(101, 71, 199),
                    android.graphics.Color.rgb(65, 83, 190),
                    android.graphics.Color.rgb(119, 75, 190)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        return bitmap
    }

    private fun decodeWidgetBitmap(path: String): Bitmap? {

        if (path.isBlank()) return null

        val file = File(path)

        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(path, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sampleSize = 1

        while (
            bounds.outWidth / sampleSize > 700 ||
            bounds.outHeight / sampleSize > 700
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return try {
            BitmapFactory.decodeFile(path, options)
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}

class ThoughtWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget =
        ThoughtWidget()
}
