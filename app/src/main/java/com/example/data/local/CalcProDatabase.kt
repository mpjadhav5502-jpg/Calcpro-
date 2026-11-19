package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntity::class, CurrencyRateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CalcProDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun currencyRateDao(): CurrencyRateDao

    companion object {
        @Volatile
        private var INSTANCE: CalcProDatabase? = null

        fun getInstance(context: Context): CalcProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalcProDatabase::class.java,
                    "calcpro_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
