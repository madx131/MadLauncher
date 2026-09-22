package com.madlauncher.data

data class FloatingConv(
    val conversationId: String,
    val minimized: Boolean = false,
    val x: Float,
    val y: Float,
    val w: Float = 300f,
    val h: Float = 280f,
)
