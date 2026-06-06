package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val dao: FinanceDao) {
    val profile: Flow<UserProfile?> = dao.getProfileFlow()
    val accounts: Flow<List<Account>> = dao.getAccountsFlow()
    val transactions: Flow<List<Transaction>> = dao.getTransactionsFlow()
    val bills: Flow<List<BillSubscription>> = dao.getBillsFlow()
    val goals: Flow<List<FinancialGoal>> = dao.getGoalsFlow()
    val debts: Flow<List<Debt>> = dao.getDebtsFlow()
    val reviews: Flow<List<WeeklyReview>> = dao.getReviewsFlow()

    suspend fun getProfileDirect(): UserProfile? = dao.getProfileDirect()
    suspend fun getAccountsDirect(): List<Account> = dao.getAccountsDirect()
    suspend fun getTransactionsDirect(): List<Transaction> = dao.getTransactionsDirect()
    suspend fun getBillsDirect(): List<BillSubscription> = dao.getBillsDirect()
    suspend fun getGoalsDirect(): List<FinancialGoal> = dao.getGoalsDirect()
    suspend fun getDebtsDirect(): List<Debt> = dao.getDebtsDirect()

    suspend fun saveProfile(profile: UserProfile) = dao.upsertProfile(profile)

    suspend fun insertAccount(account: Account) = dao.insertAccount(account)
    suspend fun updateAccount(account: Account) = dao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
        // Adjust balance automatically
        val accounts = dao.getAccountsDirect()
        val acc = accounts.find { it.id == transaction.accountId }
        if (acc != null) {
            val balanceDiff = when (transaction.type) {
                "Income", "Refund" -> transaction.amount
                "Expense", "Debt", "Savings" -> -transaction.amount
                "Transfer" -> -transaction.amount // outgoing
                else -> 0.0
            }
            dao.updateAccount(acc.copy(balance = acc.balance + balanceDiff))

            // If it's a transfer, also add of target account in the future if applicable
            if (transaction.type == "Transfer" && transaction.targetAccountId != null) {
                val targetAcc = accounts.find { it.id == transaction.targetAccountId }
                if (targetAcc != null) {
                    dao.updateAccount(targetAcc.copy(balance = targetAcc.balance + transaction.amount))
                }
            }
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction)
        // Undo balance changes
        val accounts = dao.getAccountsDirect()
        val acc = accounts.find { it.id == transaction.accountId }
        if (acc != null) {
            val balanceDiff = when (transaction.type) {
                "Income", "Refund" -> -transaction.amount
                "Expense", "Debt", "Savings" -> transaction.amount
                "Transfer" -> transaction.amount
                else -> 0.0
            }
            dao.updateAccount(acc.copy(balance = acc.balance + balanceDiff))

            // Revert target account transfer
            if (transaction.type == "Transfer" && transaction.targetAccountId != null) {
                val targetAcc = accounts.find { it.id == transaction.targetAccountId }
                if (targetAcc != null) {
                    dao.updateAccount(targetAcc.copy(balance = targetAcc.balance - transaction.amount))
                }
            }
        }
    }

    suspend fun insertBill(bill: BillSubscription) = dao.insertBill(bill)
    suspend fun updateBill(bill: BillSubscription) = dao.updateBill(bill)
    suspend fun deleteBill(bill: BillSubscription) = dao.deleteBill(bill)

    suspend fun insertGoal(goal: FinancialGoal) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: FinancialGoal) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: FinancialGoal) = dao.deleteGoal(goal)

    suspend fun insertDebt(debt: Debt) = dao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = dao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = dao.deleteDebt(debt)

    suspend fun insertReview(review: WeeklyReview) = dao.insertReview(review)
}
