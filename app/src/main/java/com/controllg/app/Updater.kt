package com.controllg.app

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASE = "https://github.com/jrpenalozat-a11y/control-lg/releases/latest/download/"

/** Número de versión instalado (sube con cada compilación de GitHub). */
fun installedVersion(context: Context): Long {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
}

fun installedVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

private fun open(url: String): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 30_000
        instanceFollowRedirects = true
    }

/** Versión publicada en el enlace de descarga, o null si no hay internet. */
suspend fun latestVersion(): Long? = withContext(Dispatchers.IO) {
    runCatching {
        open(RELEASE + "version.txt").inputStream.use { it.readBytes().toString(Charsets.UTF_8).trim().toLong() }
    }.getOrNull()
}

/** Descarga la última versión a la caché (misma carpeta que «Compartir»). */
suspend fun downloadUpdate(context: Context): File = withContext(Dispatchers.IO) {
    val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
    val file = File(dir, "actualizacion.apk")
    open(RELEASE + "ControlCasa.apk").inputStream.use { input ->
        file.outputStream().use { input.copyTo(it) }
    }
    file
}

/** Abre el instalador de Android con el APK descargado (se instala encima, sin perder ajustes). */
fun installUpdate(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

