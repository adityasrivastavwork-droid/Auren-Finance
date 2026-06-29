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
        WeeklyReview::class,
        WishlistItem::class
    ],
    version = 6,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN shakeToAddEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN widgetOrder TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN monthlyDiscretionaryBudget REAL NOT NULL DEFAULT 0.0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wishlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `estimatedPrice` REAL NOT NULL,
                        `targetMonths` INTEGER NOT NULL,
                        `dailyAllocation` REAL NOT NULL DEFAULT 0.0,
                        `isPurchased` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `priority` INTEGER NOT NULL DEFAULT 1,
                        `savedAmount` REAL NOT NULL DEFAULT 0.0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auren_money_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
