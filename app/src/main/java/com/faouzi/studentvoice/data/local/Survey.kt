package com.faouzi.studentvoice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class Survey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherName: String = "",
    val className: String = "",
    val subject: String = "",
    val studentCount: Int,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateCompleted: Long = System.currentTimeMillis()
)
