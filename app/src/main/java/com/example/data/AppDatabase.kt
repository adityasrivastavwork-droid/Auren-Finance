package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfile::class,
        Account::class,
        Transaction::class,
        BillSubscription::class,
        FinancialGoal::class,
        Debt::class,
        WeeklyReview::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 → v2 was the schema bump that introduced `autoSaveEnabled`,
         * `autoSavePercentage`, `autoSaveGoalId` on [UserProfile] and `usageConfirmed`
         * on [BillSubscription]. The original repo used
         * `.fallbackToDestructiveMigration()` which would wipe real users' finance data
         * on every upgrade — see AGENT.md §1 (Security/data safety).
         *
         * This non-destructive migration ALTERs the columns and keeps data.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // user_profile additions
                db.execSQL("ALTER TABLE user_profile ADD COLUMN autoSaveEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN autoSavePercentage REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN autoSaveGoalId INTEGER")

                // bills additions
                db.execSQL("ALTER TABLE bills ADD COLUMN usageConfirmed INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * v2 → v3 introduces multi-step onboarding state:
         *   - `onboardingStep` — cursor for the in-flight wizard. **Default -1** so
         *     existing onboarded users are NOT thrown back into the wizard on app
         *     update. We also explicitly UPDATE existing rows where `isOnboarded = 1`
         *     to -1 (idempotent, matches the default but defensive).
         *   - `hiddenWidgets` — CSV of widget keys to hide on the home dashboard.
         *     Single column instead of N booleans per AGENT.md §2 reuse rule; adding
         *     a future widget requires no migration.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN onboardingStep INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN hiddenWidgets TEXT NOT NULL DEFAULT ''")
                // Belt-and-braces: any pre-existing onboarded user is explicitly marked completed.
                db.execSQL("UPDATE user_profile SET onboardingStep = -1 WHERE isOnboarded = 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auren_money_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Never silently wipe finance data on future upgrades.
                    // If we add a new schema version we MUST write the migration.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
