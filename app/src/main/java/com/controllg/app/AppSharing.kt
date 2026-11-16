package com.controllg.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Comparte la propia app: copia el APK instalado a la caché y abre el menú «Compartir» de Android
 * (WhatsApp, Bluetooth, Quick Share, correo…). No necesita internet.
 */
fun shareApp(context: Context): Result<Unit> = runCatching {
    val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
    val apk = File(dir, "ControlLG.apk")
    File(context.applicationInfo.sourceDir).copyTo(apk, overwrite = true)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.android.package-archive"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            "Control LG: control remoto infrarrojo para teles LG. " +
                "Instala el APK adjunto (necesita un celular con emisor infrarrojo).",
        )
        clipData = ClipData.newRawUri("ControlLG.apk", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Compartir Control LG").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
