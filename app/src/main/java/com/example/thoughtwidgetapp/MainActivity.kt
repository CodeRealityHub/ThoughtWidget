
package com.example.thoughtwidgetapp

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private val AppBackground = Color(0xFFF5F3FF)
private val AccentPurple = Color(0xFF6547C7)
private val PrimaryText = Color(0xFF242333)
private val SecondaryText = Color(0xFF626174)
private val DividerColor = Color(0xFFEAE6F3)
private val SoftPurple = Color(0xFFECE7FF)
private val White = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val colorScheme = lightColorScheme(
                primary = AccentPurple,
                onPrimary = White,
                background = AppBackground,
                onBackground = PrimaryText,
                surface = White,
                onSurface = PrimaryText,
                secondary = SecondaryText
            )

            MaterialTheme(colorScheme = colorScheme) {
                ThoughtEditor()
            }
        }
    }

    @Composable
    private fun ThoughtEditor() {

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var thought by rememberSaveable {
            mutableStateOf(ThoughtStorage.getThought(context))
        }

        var extractedText by rememberSaveable {
            mutableStateOf(ThoughtStorage.getOcrText(context))
        }

        var imagePath by rememberSaveable {
            mutableStateOf(ThoughtStorage.getImagePath(context))
        }

        var appearance by rememberSaveable {
            mutableStateOf(ThoughtStorage.getAppearance(context))
        }

        var status by rememberSaveable {
            mutableStateOf("")
        }

        var loading by remember {
            mutableStateOf(false)
        }

        // IMAGE PICKER
        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                loading = true
                status = "Reading your image..."

                var newPath: String? = null

                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        copyImageToStorage(uri)
                    }

                    newPath = savedPath

                    val recognizer = TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                    )

                    try {
                        val inputImage = InputImage.fromFilePath(
                            context,
                            Uri.fromFile(File(savedPath))
                        )

                        val result = recognizer.process(inputImage).await()

                        val oldPath = imagePath

                        extractedText = result.text
                        imagePath = savedPath

                        ThoughtStorage.save(
                            context,
                            thought,
                            extractedText,
                            imagePath
                        )

                        if (oldPath.isNotBlank() && oldPath != savedPath) {
                            withContext(Dispatchers.IO) {
                                File(oldPath).delete()
                            }
                        }

                        ThoughtWidget().updateAll(context)

                        status = if (extractedText.isBlank()) {
                            "Image saved. No text detected."
                        } else {
                            "Image and text extracted successfully!"
                        }

                    } finally {
                        recognizer.close()
                    }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {

                    newPath?.let { path ->
                        if (path != imagePath) {
                            withContext(Dispatchers.IO) {
                                File(path).delete()
                            }
                        }
                    }

                    status = "Could not process image: ${e.message}"

                } finally {
                    loading = false
                }
            }
        }

        // IMAGE PREVIEW
        val previewBitmap by produceState<ImageBitmap?>(
            initialValue = null,
            key1 = imagePath
        ) {
            value = if (imagePath.isBlank()) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    decodeSampled(imagePath, maxDim = 1080)?.asImageBitmap()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // HEADER
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    SoftPurple,
                                    RoundedCornerShape(15.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦",
                                color = AccentPurple,
                                fontSize = 27.sp
                            )
                        }

                        Column {
                            Text(
                                text = "THOUGHT SPACE",
                                color = AccentPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            Text(
                                text = "My Thoughts",
                                color = PrimaryText,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "A little space to capture what matters to you.",
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                }

                // WIDGET APPEARANCE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = White
                    ),
                    border = BorderStroke(1.dp, DividerColor)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        SectionHeading(
                            symbol = "◐",
                            title = "Widget appearance",
                            subtitle = "Choose your home-screen style"
                        )

                        AppearanceOption(
                            title = "Light",
                            subtitle = "Clean lavender and white",
                            symbol = "☀",
                            selected = appearance ==
                                    ThoughtStorage.APPEARANCE_LIGHT,
                            onClick = {
                                updateAppearance(
                                    ThoughtStorage.APPEARANCE_LIGHT,
                                    context,
                                    scope
                                ) {
                                    appearance = it
                                }
                            }
                        )

                        AppearanceOption(
                            title = "Dark",
                            subtitle = "Comfortable dark surfaces",
                            symbol = "☾",
                            selected = appearance ==
                                    ThoughtStorage.APPEARANCE_DARK,
                            onClick = {
                                updateAppearance(
                                    ThoughtStorage.APPEARANCE_DARK,
                                    context,
                                    scope
                                ) {
                                    appearance = it
                                }
                            }
                        )

                        AppearanceOption(
                            title = "Gradient",
                            subtitle = "Purple and blue gradient",
                            symbol = "✦",
                            selected = appearance ==
                                    ThoughtStorage.APPEARANCE_GRADIENT,
                            onClick = {
                                updateAppearance(
                                    ThoughtStorage.APPEARANCE_GRADIENT,
                                    context,
                                    scope
                                ) {
                                    appearance = it
                                }
                            }
                        )

                        Text(
                            text = "Your widget updates automatically after selecting a style.",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                }

                // ONE THOUGHT EDITOR
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = White
                    ),
                    border = BorderStroke(1.dp, DividerColor)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        SectionHeading(
                            symbol = "✎",
                            title = "One thought at a time",
                            subtitle = "Capture one meaningful idea"
                        )

                        OutlinedTextField(
                            value = thought,
                            onValueChange = {
                                thought = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            placeholder = {
                                Text(
                                    "Write one thought you want to remember...",
                                    color = SecondaryText.copy(alpha = 0.7f)
                                )
                            },
                            minLines = 5,
                            maxLines = 12,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = DividerColor,
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                cursorColor = AccentPurple
                            )
                        )

                        Text(
                            text = "Focus on one idea. Save it to your home-screen widget.",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }

                // IMAGE UPLOAD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = White
                    ),
                    border = BorderStroke(1.dp, DividerColor)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        SectionHeading(
                            symbol = "▧",
                            title = "Add inspiration",
                            subtitle = "Upload an image and extract its text"
                        )

                        if (previewBitmap != null) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        AppBackground,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {

                                Image(
                                    bitmap = previewBitmap!!,
                                    contentDescription = "Uploaded thought image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Text(
                                text = "Image added successfully",
                                color = AccentPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                        } else {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        AppBackground,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    Text(
                                        text = "▧",
                                        color = AccentPurple,
                                        fontSize = 36.sp
                                    )

                                    Text(
                                        text = "No image selected",
                                        color = PrimaryText,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = "Choose an image from your gallery",
                                        color = SecondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                imagePicker.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !loading,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, AccentPurple),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentPurple
                            )
                        ) {

                            Text(
                                text = if (imagePath.isBlank()) {
                                    "＋  Upload image"
                                } else {
                                    "↻  Change image"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // OCR SECTION
                if (extractedText.isNotBlank()) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = White
                        ),
                        border = BorderStroke(1.dp, DividerColor)
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {

                            SectionHeading(
                                symbol = "≡",
                                title = "Extracted text",
                                subtitle = "Review, copy or edit the detected text"
                            )

                            OutlinedTextField(
                                value = extractedText,
                                onValueChange = {
                                    extractedText = it
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                minLines = 4,
                                maxLines = 12,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = DividerColor,
                                    cursorColor = AccentPurple
                                ),
                                label = {
                                    Text("Edit extracted text")
                                }
                            )

                            // COPY OCR TEXT
                            OutlinedButton(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(
                                            ClipboardManager::class.java
                                        )

                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "Extracted Thought",
                                            extractedText
                                        )
                                    )

                                    Toast.makeText(
                                        context,
                                        "OCR text copied! Paste it into your thought.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = extractedText.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AccentPurple),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AccentPurple
                                )
                            ) {
                                Text(
                                    text = "▣  Copy extracted text",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "Copy the text, then paste it into your thought above.",
                                color = SecondaryText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // LOADING
                if (loading) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = White
                        )
                    ) {

                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = AccentPurple,
                                strokeWidth = 2.dp
                            )

                            Text(
                                text = "Processing your image...",
                                color = SecondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // STATUS
                if (status.isNotBlank()) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SoftPurple
                        )
                    ) {

                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Text(
                                text = "✦",
                                color = AccentPurple,
                                fontSize = 18.sp
                            )

                            Text(
                                text = status,
                                color = PrimaryText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // SAVE THOUGHT
                Button(
                    onClick = {
                        scope.launch {

                            ThoughtStorage.save(
                                context,
                                thought,
                                extractedText,
                                imagePath
                            )

                            ThoughtWidget().updateAll(context)

                            status = "Thought saved successfully!"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple,
                        contentColor = White
                    )
                ) {

                    Text(
                        text = "Save Thought  ✓",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // CLEAR EVERYTHING
                OutlinedButton(
                    onClick = {

                        val pathToDelete = imagePath

                        thought = ""
                        extractedText = ""
                        imagePath = ""
                        status = ""

                        ThoughtStorage.save(
                            context,
                            "",
                            "",
                            ""
                        )

                        scope.launch {

                            if (pathToDelete.isNotBlank()) {
                                withContext(Dispatchers.IO) {
                                    File(pathToDelete).delete()
                                }
                            }

                            ThoughtWidget().updateAll(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DividerColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SecondaryText
                    )
                ) {

                    Text(
                        text = "Clear everything",
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Made for your thoughts  ✦",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = SecondaryText.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }

    // APPEARANCE UPDATE
    private fun updateAppearance(
        newAppearance: String,
        context: android.content.Context,
        scope: CoroutineScope,
        onUpdated: (String) -> Unit
    ) {
        onUpdated(newAppearance)

        ThoughtStorage.saveAppearance(
            context,
            newAppearance
        )

        scope.launch {
            ThoughtWidget().updateAll(context)
        }
    }

    // SECTION HEADING
    @Composable
    private fun SectionHeading(
        symbol: String,
        title: String,
        subtitle: String
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        SoftPurple,
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = symbol,
                    color = AccentPurple,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                Text(
                    text = title,
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }

    // APPEARANCE OPTION
    @Composable
    private fun AppearanceOption(
        title: String,
        subtitle: String,
        symbol: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (selected) SoftPurple else White,
            border = BorderStroke(
                width = 1.dp,
                color = if (selected) AccentPurple else DividerColor
            )
        ) {

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (selected) AccentPurple else AppBackground,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = symbol,
                        color = if (selected) White else AccentPurple,
                        fontSize = 20.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = subtitle,
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }

                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AccentPurple
                    )
                )
            }
        }
    }

    // IMAGE STORAGE
    private fun copyImageToStorage(uri: Uri): String {

        val destination = File(
            filesDir,
            "thought_image_${System.currentTimeMillis()}.jpg"
        )

        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open selected image")

        input.use { source ->
            FileOutputStream(destination).use { output ->
                source.copyTo(output)
            }
        }

        return destination.absolutePath
    }

    // IMAGE DECODING
    private fun decodeSampled(
        path: String,
        maxDim: Int
    ): Bitmap? {

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(path, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sample = 1

        while (
            bounds.outWidth / sample > maxDim ||
            bounds.outHeight / sample > maxDim
        ) {
            sample *= 2
        }

        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }
}
