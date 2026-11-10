package org.tensorflow.lite.examples.shravan.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.utils.FeedbackCoordinator
import org.tensorflow.lite.examples.shravan.utils.HapticPattern
import org.tensorflow.lite.examples.shravan.utils.Priority

/**
 * Thiếu quyền camera / micro: nói lý do, nửa dưới màn hình là một nút lớn.
 * Nếu người dùng đã từ chối hẳn (hệ thống không hỏi lại nữa) thì nút mở cài đặt ứng dụng.
 */
@Composable
fun PermissionRequiredScreen(
    feedback: FeedbackCoordinator,
    askedBefore: Boolean,
    onRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val deniedForever = askedBefore && activity != null &&
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            .any { !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    val title = stringResource(R.string.permission_title)
    val body = stringResource(
        if (deniedForever) R.string.permission_denied_forever else R.string.permission_body
    )
    val buttonLabel = stringResource(
        if (deniedForever) R.string.permission_open_settings else R.string.permission_grant
    )

    LaunchedEffect(deniedForever) {
        feedback.say("$title. $body", Priority.ALERT, userInitiated = true)
        feedback.vibrate(HapticPattern.DOUBLE, force = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Text(body, color = Color.White, fontSize = 24.sp, lineHeight = 32.sp)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                feedback.vibrate(HapticPattern.SHORT, force = true)
                if (deniedForever) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    onRequest()
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)
        ) {
            Text(buttonLabel, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Máy thiếu giọng đọc tiếng Việt: hộp thoại chữ to, mở màn cài giọng đọc. */
@Composable
fun MissingVoiceDialog(onInstall: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.error_tts_missing_title), fontSize = 26.sp) },
        text = { Text(stringResource(R.string.error_tts_missing_body), fontSize = 22.sp, lineHeight = 30.sp) },
        confirmButton = {
            Button(onClick = onInstall, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.error_tts_install), fontSize = 22.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.action_later), fontSize = 22.sp)
            }
        }
    )
}
