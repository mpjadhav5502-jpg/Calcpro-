package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CalculationCategory {
    STANDARD,
    SCIENTIFIC,
    PERCENTAGE,
    GST,
    DISCOUNT,
    UNIT,
    CURRENCY
}

@Entity(tableName = "calculation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val category: String = CalculationCategory.STANDARD.name,
    val timestamp: Long = System.currentTimeMillis()
)
