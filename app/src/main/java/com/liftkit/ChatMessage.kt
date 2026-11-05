package com.liftkit

data class ChatMessage(
    val sender: String, // "You" or "Grok"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)