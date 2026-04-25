package com.rudra.swiggymind

import android.content.Intent
import android.os.Build
import android.net.Uri

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    
    var context: android.content.Context? = null

    override fun openUrl(url: String) {
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context?.startActivity(Intent.createChooser(intent, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

private val androidPlatform = AndroidPlatform()

actual fun getPlatform(): Platform = androidPlatform

fun setPlatformContext(context: android.content.Context) {
    androidPlatform.context = context
}
