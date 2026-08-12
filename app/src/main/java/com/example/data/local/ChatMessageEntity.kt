package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER" or "ISRAEL"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null
)
