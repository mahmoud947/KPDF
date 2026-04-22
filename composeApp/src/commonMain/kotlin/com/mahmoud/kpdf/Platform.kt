package com.mahmoud.kpdf

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform