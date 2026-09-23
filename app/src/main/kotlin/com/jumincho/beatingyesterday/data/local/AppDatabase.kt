package com.jumincho.beatingyesterday.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** The app's SQLite database. Schemas are exported to `app/schemas` for future migrations. */
@Database(
    entities = [MealEntity::class, FocusSessionEntity::class, TodoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    abstract fun focusSessionDao(): FocusSessionDao

    abstract fun todoDao(): TodoDao

    companion object {
        private const val FILE_NAME = "beating-yesterday.db"

        /** Opens (or creates) the database file. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME).build()
    }
}
