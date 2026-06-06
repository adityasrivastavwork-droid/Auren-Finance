package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val currency: String = "₹",
    val country: String = "India",
    val primaryObjective: String = "Control spending", // "Control spending", "Start saving", "Clear debt", etc.
    val appMode: String = "Strict Mode", // "Strict Mode", "Balanced Mode", "Relaxed Mode"
    val salaryAmount: Double = 60000.0,
    val salaryDate: Int = 1, // Day of month 1..31
    val incomeFrequency: String = "Fixed monthly salary",
    val safetyBuffer: Double = 2000.0,
    val currentEmergencyFund: Double = 0.0,
    val isOnboarded: Boolean = false
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
