package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FmStationEntity::class,
        FavoriteStationEntity::class,
        HardwarePresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OndaFmDatabase : RoomDatabase() {
    abstract fun fmDao(): FmDao

    companion object {
        @Volatile
        private var instance: OndaFmDatabase? = null

        fun getInstance(context: Context): OndaFmDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OndaFmDatabase::class.java,
                    "onda_fm_receiver.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
