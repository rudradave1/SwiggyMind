package com.rudra.swiggymind

interface Platform {
    val name: String
    fun openUrl(url: String)
    fun shareText(text: String)
}

expect fun getPlatform(): Platform