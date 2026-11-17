package com.controllg.app

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Enlace de la última versión. La app no descarga ni instala nada por su cuenta (eso hace que
 * Google Play Protect la tome por dañina): solo abre el enlace en el navegador.
 */
const val DOWNLOAD_URL = "https://github.com/jrpenalozat-a11y/control-lg/releases/latest/download/ControlCasa.apk"

fun installedVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

/** Abre el enlace de descarga en el navegador; luego se abre el archivo y se instala encima. */
fun openDownloadPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
