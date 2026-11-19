package com.example.data.repository

import com.example.data.local.CalculationCategory
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    fun searchHistory(query: String): Flow<List<HistoryEntity>> {
        return if (query.isBlank()) {
            historyDao.getAllHistory()
        } else {
            historyDao.searchHistory(query.trim())
        }
    }

    suspend fun saveCalculation(
        expression: String,
        result: String,
        category: CalculationCategory
    ) {
        if (expression.isBlank() || result.isBlank()) return
        historyDao.insert(
            HistoryEntity(
                expression = expression.trim(),
                result = result.trim(),
                category = category.name,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteRecord(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearAll() {
        historyDao.deleteAll()
    }
}
