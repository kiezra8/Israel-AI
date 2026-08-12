package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeInMillis: Long,
    val isCompleted: Boolean = false,
    val category: String = "GENERAL", // ALARM, APPOINTMENT, EMAIL, REMINDER, WHATSAPP, SEARCH
    val createdAt: Long = System.currentTimeMillis()
)
