package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Repository-level tests for [FinanceRepository].
 *
 * Focus: bulk-insert path used by [com.example.ui.FinanceViewModel.importBankTransactions]
 * must correctly recompute account balances in ONE sweep (regression: v1 called
 * insertTransaction N times → N round-trips, visibly slow on real SMS imports).
 */
class FinanceRepositoryTest {

    @Test
    fun `insertTransactions applies income and expense deltas in one pass`() = runBlocking {
        val dao = FakeFinanceDao()
        dao.accounts.value = listOf(
            Account(id = 1, name = "Bank", type = "Savings", balance = 1000.0),
            Account(id = 2, name = "Wallet", type = "Cash", balance = 500.0)
        )
        val repo = FinanceRepository(dao)

        repo.insertTransactions(
            listOf(
                tx(accId = 1, amount = 200.0, type = "Income"),
                tx(accId = 1, amount = 50.0, type = "Expense"),
                tx(accId = 2, amount = 100.0, type = "Expense")
            )
        )

        val a1 = dao.accounts.value.first { it.id == 1L }
        val a2 = dao.accounts.value.first { it.id == 2L }
        assertEquals(1150.0, a1.balance, 0.0001) // 1000 + 200 − 50
        assertEquals(400.0, a2.balance, 0.0001)  // 500 − 100
        assertEquals(3, dao.transactions.value.size)
    }

    @Test
    fun `insertTransactions handles transfer between two accounts`() = runBlocking {
        val dao = FakeFinanceDao()
        dao.accounts.value = listOf(
            Account(id = 1, name = "Bank", type = "Savings", balance = 1000.0),
            Account(id = 2, name = "Wallet", type = "Cash", balance = 0.0)
        )
        val repo = FinanceRepository(dao)

        repo.insertTransactions(
            listOf(tx(accId = 1, amount = 300.0, type = "Transfer", targetId = 2))
        )

        val a1 = dao.accounts.value.first { it.id == 1L }
        val a2 = dao.accounts.value.first { it.id == 2L }
        assertEquals(700.0, a1.balance, 0.0001)
        assertEquals(300.0, a2.balance, 0.0001)
    }

    @Test
    fun `insertTransactions on empty list is a no-op`() = runBlocking {
        val dao = FakeFinanceDao()
        dao.accounts.value = listOf(Account(id = 1, name = "Bank", type = "Savings", balance = 1000.0))
        val repo = FinanceRepository(dao)

        repo.insertTransactions(emptyList())

        assertEquals(1000.0, dao.accounts.value.single().balance, 0.0001)
        assertTrue(dao.transactions.value.isEmpty())
    }

    private fun tx(
        accId: Long,
        amount: Double,
        type: String,
        targetId: Long? = null
    ) = Transaction(
        amount = amount,
        type = type,
        date = 0L,
        accountId = accId,
        targetAccountId = targetId,
        category = "x",
        merchant = "x"
    )
}

/** Hand-rolled fake DAO — full Room test would need Robolectric / instrumented harness. */
private class FakeFinanceDao : FinanceDao {
    val profile = MutableStateFlow<UserProfile?>(null)
    val accounts = MutableStateFlow<List<Account>>(emptyList())
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val bills = MutableStateFlow<List<BillSubscription>>(emptyList())
    val goals = MutableStateFlow<List<FinancialGoal>>(emptyList())
    val debts = MutableStateFlow<List<Debt>>(emptyList())
    val reviews = MutableStateFlow<List<WeeklyReview>>(emptyList())

    override fun getProfileFlow(): Flow<UserProfile?> = profile
    override suspend fun getProfileDirect(): UserProfile? = profile.value
    override suspend fun upsertProfile(profile: UserProfile) { this.profile.value = profile }

    override fun getAccountsFlow(): Flow<List<Account>> = accounts
    override suspend fun getAccountsDirect(): List<Account> = accounts.value
    override suspend fun insertAccount(account: Account) {
        accounts.value = accounts.value + account
    }
    override suspend fun updateAccount(account: Account) {
        accounts.value = accounts.value.map { if (it.id == account.id) account else it }
    }
    override suspend fun deleteAccount(account: Account) {
        accounts.value = accounts.value.filter { it.id != account.id }
    }

    override fun getTransactionsFlow(): Flow<List<Transaction>> = transactions
    override suspend fun getTransactionsDirect(): List<Transaction> = transactions.value
    override suspend fun insertTransaction(transaction: Transaction) {
        transactions.value = transactions.value + transaction
    }
    override suspend fun insertTransactions(transactions: List<Transaction>) {
        this.transactions.value = this.transactions.value + transactions
    }
    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.value = transactions.value.map { if (it.id == transaction.id) transaction else it }
    }
    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.value = transactions.value.filter { it.id != transaction.id }
    }

    override fun getBillsFlow(): Flow<List<BillSubscription>> = bills
    override suspend fun getBillsDirect(): List<BillSubscription> = bills.value
    override suspend fun insertBill(bill: BillSubscription) { bills.value = bills.value + bill }
    override suspend fun updateBill(bill: BillSubscription) {
        bills.value = bills.value.map { if (it.id == bill.id) bill else it }
    }
    override suspend fun deleteBill(bill: BillSubscription) {
        bills.value = bills.value.filter { it.id != bill.id }
    }

    override fun getGoalsFlow(): Flow<List<FinancialGoal>> = goals
    override suspend fun getGoalsDirect(): List<FinancialGoal> = goals.value
    override suspend fun insertGoal(goal: FinancialGoal) { goals.value = goals.value + goal }
    override suspend fun updateGoal(goal: FinancialGoal) {
        goals.value = goals.value.map { if (it.id == goal.id) goal else it }
    }
    override suspend fun deleteGoal(goal: FinancialGoal) {
        goals.value = goals.value.filter { it.id != goal.id }
    }

    override fun getDebtsFlow(): Flow<List<Debt>> = debts
    override suspend fun getDebtsDirect(): List<Debt> = debts.value
    override suspend fun insertDebt(debt: Debt) { debts.value = debts.value + debt }
    override suspend fun updateDebt(debt: Debt) {
        debts.value = debts.value.map { if (it.id == debt.id) debt else it }
    }
    override suspend fun deleteDebt(debt: Debt) {
        debts.value = debts.value.filter { it.id != debt.id }
    }

    override fun getReviewsFlow(): Flow<List<WeeklyReview>> = reviews
    override suspend fun insertReview(review: WeeklyReview) { reviews.value = reviews.value + review }
}
