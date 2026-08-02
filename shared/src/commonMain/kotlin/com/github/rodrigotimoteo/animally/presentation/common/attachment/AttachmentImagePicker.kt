package com.github.rodrigotimoteo.animally.presentation.common.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/**
 * Lets the user pick image files through the platform picker and lists the
 * currently attached images with a remove action per entry.
 *
 * @param imageUris Comma-separated paths of the currently attached images.
 * @param onFilesPicked Called with the picked files mapped to [PickedFile].
 * @param onRemove Called with the path of an attached image to detach.
 * @param modifier Optional modifier.
 */
@Composable
fun AttachmentImagePicker(
    imageUris: String?,
    onFilesPicked: (List<PickedFile>) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher =
        rememberFilePickerLauncher(
            type = FileKitType.Image,
            mode = FileKitMode.Multiple(),
            onResult = { files: List<PlatformFile>? ->
                onFilesPicked(
                    files.orEmpty().map { file ->
                        PickedFile(name = file.name, readBytes = { file.readBytes() })
                    },
                )
            },
        )
    Column(modifier = modifier) {
        Text("Attached Images", style = MaterialTheme.typography.labelLarge)
        val uris =
            imageUris
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        if (uris.isEmpty()) {
            Text(
                "No images attached",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            uris.forEach { uri ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uri.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onRemove(uri) }) {
                        Text("Remove")
                    }
                }
            }
        }
        OutlinedButton(
            onClick = launcher::launch,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Attach Images")
        }
    }
}
