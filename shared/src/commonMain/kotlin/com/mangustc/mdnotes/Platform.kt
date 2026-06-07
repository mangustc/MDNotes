package com.mangustc.mdnotes

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform