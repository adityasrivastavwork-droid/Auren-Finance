package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    // Accounts
    @Query("SELECT * FROM accounts")
    fun getAccountsFlow(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun getAccountsDirect(): List<Account>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getTransactionsDirect(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    /**
     * Bulk insert — much faster than calling [insertTransaction] in a loop.
     * Used by SMS / bank-feed importers so 500 rows ≠ 500 round-trips.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Bills
    @Query("SELECT * FROM bills")
    fun getBillsFlow(): Flow<List<BillSubscription>>

    @Query("SELECT * FROM bills")
    suspend fun getBillsDirect(): List<BillSubscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillSubscription)

    @Update
    suspend fun updateBill(bill: BillSubscription)

    @Delete
    suspend fun deleteBill(bill: BillSubscription)

    // Goals
    @Query("SELECT * FROM goals")
    fun getGoalsFlow(): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM goals")
    suspend fun getGoalsDirect(): List<FinancialGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoal)

    @Update
    suspend fun updateGoal(goal: FinancialGoal)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoal)

    // Debts
    @Query("SELECT * FROM debts")
    fun getDebtsFlow(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    suspend fun getDebtsDirect(): List<Debt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    // WeeklyReviews
    @Query("SELECT * FROM weekly_reviews ORDER BY timestamp DESC")
    fun getReviewsFlow(): Flow<List<WeeklyReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: WeeklyReview)

    // WishlistItems
    @Query("SELECT * FROM wishlist_items ORDER BY createdAt ASC")
    fun getWishlistFlow(): Flow<List<WishlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItem)

    @Update
    suspend fun updateWishlistItem(item: WishlistItem)

    @Delete
    suspend fun deleteWishlistItem(item: WishlistItem)
}
