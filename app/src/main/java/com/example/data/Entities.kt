package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val currency: String = "₹",
    val country: String = "India",
    val primaryObjective: String = "Control spending",
    val appMode: String = "Strict Mode",
    val salaryAmount: Double = 60000.0,
    val salaryDate: Int = 1,
    val incomeFrequency: String = "Fixed monthly salary",
    val safetyBuffer: Double = 2000.0,
    val currentEmergencyFund: Double = 0.0,
    val isOnboarded: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val autoSavePercentage: Double = 0.0,
    val autoSaveGoalId: Long? = null,
    val onboardingStep: Int = -1,
    val hiddenWidgets: String = "",
    val shakeToAddEnabled: Boolean = false,
    val widgetOrder: String = "",
    /** Monthly discretionary budget — what the user wants to spend beyond basics (EMI, rent etc).
     *  This drives safeToSpendToday rather than account balance. 0 = use auto-calc. */
    val monthlyDiscretionaryBudget: Double = 0.0
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val balance: Double,
    val creditLimit: Double = 0.0,
    val institution: String = ""
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val date: Long,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val category: String,
    val merchant: String,
    val note: String = "",
    val isRecurring: Boolean = false,
    val businessType: String = "Personal"
)

@Entity(tableName = "bills")
data class BillSubscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDate: Int,
    val frequency: String = "Monthly",
    val category: String,
    val isSubscription: Boolean = false,
    val accountId: Long = 0,
    val lastPaidTimestamp: Long = 0L,
    val usageConfirmed: Boolean = true
)

@Entity(tableName = "goals")
data class FinancialGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val priority: String = "Important",
    val monthlyContribution: Double = 0.0,
    val category: String = "Savings"
)

/** A product/purchase the user is planning to save toward within a set timeframe.
 *  Its daily cost is deducted from the discretionary daily spend limit. */
@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val estimatedPrice: Double,
    /** Target months from now to purchase */
    val targetMonths: Int,
    /** Derived: estimatedPrice / (targetMonths * 30) — daily amount to set aside */
    val dailyAllocation: Double = 0.0,
    val isPurchased: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val outstandingAmount: Double,
    val interestRate: Double,
    val minimumPayment: Double,
    val emiAmount: Double,
    val dueDate: Int,
    val remainingTenure: Int,
    val lender: String = "",
    val payoffStrategy: String = "Snowball"
)

@Entity(tableName = "weekly_reviews")
data class WeeklyReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val positiveAchievement: String = "",
    val overspendingReason: String = "",
    val categoryExceeded: String = "",
    val adjustedBudgetAmount: Double = 0.0,
    val leakToMonitor: String = "",
    val focusAction: String = ""
)


@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "Savings", "Credit Card", "Cash", "Wallet", "Investment"
    val balance: Double,
    val creditLimit: Double = 0.0,
    val institution: String = ""
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "Income", "Expense", "Transfer", "Savings", "Debt"
    val date: Long, // timestamp
    val accountId: Long,
    val targetAccountId: Long? = null, // for Transfer
    val category: String,
    val merchant: String,
    val note: String = "",
    val isRecurring: Boolean = false,
    val businessType: String = "Personal" // "Personal", "Business", "Shared"
)

@Entity(tableName = "bills")
data class BillSubscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDate: Int, // Day of month
    val frequency: String = "Monthly",
    val category: String,
    val isSubscription: Boolean = false,
    val accountId: Long = 0,
    val lastPaidTimestamp: Long = 0L,
    val usageConfirmed: Boolean = true
)

@Entity(tableName = "goals")
data class FinancialGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long, // timestamp
    val priority: String = "Important", // "Critical", "Important", "Lifestyle", "Aspirational"
    val monthlyContribution: Double = 0.0,
    val category: String = "Savings"
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "Personal Loan", "Credit Card", "EMIs", "Friends/Family"
    val outstandingAmount: Double,
    val interestRate: Double, // annual percentage e.g. 12.5%
    val minimumPayment: Double,
    val emiAmount: Double,
    val dueDate: Int, // Day of month
    val remainingTenure: Int, // months
    val lender: String = "",
    val payoffStrategy: String = "Snowball" // "Snowball", "Avalanche"
)

@Entity(tableName = "weekly_reviews")
data class WeeklyReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val positiveAchievement: String = "",
    val overspendingReason: String = "",
    val categoryExceeded: String = "",
    val adjustedBudgetAmount: Double = 0.0,
    val leakToMonitor: String = "",
    val focusAction: String = ""
)
