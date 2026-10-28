package com.example.ui.screens.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.StudyFile
import com.example.viewmodel.DumuViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: DumuViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var pdfTitle by remember { mutableStateOf("Lecture_Notes_${System.currentTimeMillis() % 10000}") }
    var courseTag by remember { mutableStateOf("AI 401 - Machine Learning") }
    var isConverting by remember { mutableStateOf(false) }
    var conversionSuccessPath by remember { mutableStateOf<String?>(null) }

    // Multi-Image Picker using zero-permission Android Photo Picker
    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.addAll(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Image to PDF Converter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("image_to_pdf_screen")
        ) {
            // Conversion Config
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        label = { Text("PDF Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = courseTag,
                        onValueChange = { courseTag = it },
                        label = { Text("Course Tag / Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                pickMultipleMedia.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("select_images_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Images (${selectedUris.size})")
                        }

                        if (selectedUris.isNotEmpty()) {
                            TextButton(onClick = { selectedUris.clear() }) {
                                Text("Clear All", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Conversion Status banner if succeeded
            conversionSuccessPath?.let { path ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PDF Generated Successfully!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Saved to Media Hub: ${File(path).name}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Image Thumbnails Grid with Reordering
            if (selectedUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Images Selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap 'Select Images' to pick whiteboard photos, textbook pages, or lecture slides.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Selected Pages (${selectedUris.size}) - Reorder using arrows:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(selectedUris) { index, uri ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Page ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Page Number Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "Page ${index + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { selectedUris.removeAt(index) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Reorder controls
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val item = selectedUris.removeAt(index)
                                                selectedUris.add(index - 1, item)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < selectedUris.size - 1) {
                                                val item = selectedUris.removeAt(index)
                                                selectedUris.add(index + 1, item)
                                            }
                                        },
                                        enabled = index < selectedUris.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Merge & Convert Button
            Button(
                onClick = {
                    if (selectedUris.isNotEmpty()) {
                        isConverting = true
                        scope.launch {
                            val savedPath = convertImagesToPdf(
                                context = context,
                                uris = selectedUris.toList(),
                                fileName = if (pdfTitle.endsWith(".pdf")) pdfTitle else "$pdfTitle.pdf",
                                course = courseTag
                            )
                            isConverting = false
                            if (savedPath != null) {
                                conversionSuccessPath = savedPath
                                val fileObj = File(savedPath)
                                viewModel.addStudyFile(
                                    StudyFile(
                                        name = fileObj.name,
                                        filePath = savedPath,
                                        sizeBytes = fileObj.length(),
                                        lastModified = fileObj.lastModified(),
                                        category = "PDF",
                                        courseTag = courseTag
                                    )
                                )
                                Toast.makeText(context, "PDF saved: ${fileObj.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to compile PDF document", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = selectedUris.isNotEmpty() && !isConverting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("convert_to_pdf_button")
            ) {
                if (isConverting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Compressing & Merging (${selectedUris.size} Pages)...")
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert to PDF (~75% Compressed)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Background image compression and PDF rendering
private suspend fun convertImagesToPdf(
    context: Context,
    uris: List<Uri>,
    fileName: String,
    course: String
): String? = withContext(Dispatchers.IO) {
    try {
        val pdfDocument = PdfDocument()
        val contentResolver = context.contentResolver

        uris.forEachIndexed { index, uri ->
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                // Compress ~75% quality
                val compressedBytes = ByteArrayOutputStream()
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, compressedBytes)
                val compressedBitmap = BitmapFactory.decodeByteArray(
                    compressedBytes.toByteArray(),
                    0,
                    compressedBytes.size()
                )

                // Standard A4 aspect ratio: 595 x 842 pt
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Scale image to fit within A4 margins
                val scale = minOf(555f / compressedBitmap.width, 802f / compressedBitmap.height)
                val scaledWidth = (compressedBitmap.width * scale).toInt()
                val scaledHeight = (compressedBitmap.height * scale).toInt()
                val left = (595 - scaledWidth) / 2f
                val top = (842 - scaledHeight) / 2f

                val scaledBitmap = Bitmap.createScaledBitmap(compressedBitmap, scaledWidth, scaledHeight, true)
                canvas.drawBitmap(scaledBitmap, left, top, null)
                pdfDocument.finishPage(page)
            }
        }

        // Save PDF to app documents directory
        val docsDir = context.getExternalFilesDir("documents") ?: context.filesDir
        if (!docsDir.exists()) docsDir.mkdirs()
        val outputFile = File(docsDir, fileName)

        val outputStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        outputFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
