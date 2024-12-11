package com.example.expensetrackerapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Transaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class TransactionListFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionStorage: TransactionStorage
    private val transactions = mutableListOf<Transaction>()
    private val filteredTransactions = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация хранилища транзакций
        transactionStorage = TransactionStorage(requireContext())

        // Загрузка транзакций
        transactions.addAll(transactionStorage.loadTransactions())
        filteredTransactions.addAll(transactions) // Изначально отображаем все транзакции

        // Настройка RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_transactions)
        transactionAdapter = TransactionAdapter(filteredTransactions) { transaction ->
            showContextMenu(transaction)
        }
        recyclerView.adapter = transactionAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Настройка Spinner
        val spinnerFilter = view.findViewById<Spinner>(R.id.spinner_filter)
        val categories = mutableListOf("All Categories")
        categories.addAll(transactions.map { it.category }.distinct())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        // Обработка выбора категории
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                filterTransactions(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                filterTransactions("All Categories")
            }
        }

        // Обработчик добавления новой транзакции
        val fabAddTransaction = view.findViewById<FloatingActionButton>(R.id.fab_add_transaction)
        fabAddTransaction.setOnClickListener {
            AddTransactionDialog(
                onTransactionAdded = { newTransaction ->
                    // Добавляем транзакцию в основной список
                    transactions.add(newTransaction)

                    // Сохраняем транзакции
                    transactionStorage.saveTransactions(transactions)

                    // Отправляем Broadcast для проверки лимита
                    val intent = Intent(requireContext(), LimitBroadcastReceiver::class.java)
                    requireContext().sendBroadcast(intent)

                    // Проверяем текущий фильтр и обновляем отображение
                    val spinnerFilter = view.findViewById<Spinner>(R.id.spinner_filter)
                    val selectedCategory = spinnerFilter.selectedItem as String
                    filterTransactions(selectedCategory)

                    // Добавляем новую категорию в список категорий, если её ещё нет
                    if (!categories.contains(newTransaction.category)) {
                        categories.add(newTransaction.category)
                        (spinnerFilter.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                    }

                    // Показываем Snackbar
                    Snackbar.make(requireView(), "Transaction added", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            transactions.removeAt(transactions.size - 1)
                            transactionAdapter.notifyItemRemoved(transactions.size)
                            transactionStorage.saveTransactions(transactions)
                            filterTransactions(selectedCategory)
                        }
                            .setAnchorView(it)
                            .show()

                }
            ).show(parentFragmentManager, "AddTransactionDialog")
        }
    }

    private fun filterTransactions(category: String) {
        if (category == "All Categories") {
            filteredTransactions.clear()
            filteredTransactions.addAll(transactions)
        } else {
            val filtered = transactions.filter { it.category == category }

            // Анимация удаления
            val itemsToRemove = filteredTransactions.filter { it !in filtered }
            itemsToRemove.forEach {
                val index = filteredTransactions.indexOf(it)
                filteredTransactions.removeAt(index)
                transactionAdapter.notifyItemRemoved(index)
            }

            // Добавляем новые элементы, если необходимо
            val itemsToAdd = filtered.filter { it !in filteredTransactions }
            itemsToAdd.forEach {
                filteredTransactions.add(it)
                transactionAdapter.notifyItemInserted(filteredTransactions.size - 1)
            }
        }

        transactionAdapter.notifyDataSetChanged()
    }

    private fun showContextMenu(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose an action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTransactionDialog(transaction) // Редактировать
                    1 -> deleteTransaction(transaction) // Удалить
                }
            }
            .show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        AddTransactionDialog(
            onTransactionAdded = { updatedTransaction ->
                // Находим индекс изменённой транзакции
                val index = transactions.indexOfFirst { it.id == transaction.id }
                if (index != -1) {
                    val oldTransaction = transactions[index]
                    transactions[index] = updatedTransaction

                    // Сохраняем транзакции
                    transactionStorage.saveTransactions(transactions)

                    // Проверяем текущий фильтр и обновляем отображение
                    val spinnerFilter = view?.findViewById<Spinner>(R.id.spinner_filter)
                    val selectedCategory = spinnerFilter?.selectedItem as String
                    filterTransactions(selectedCategory)

                    // Показываем Snackbar для подтверждения
                    Snackbar.make(requireView(), "Transaction updated", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            transactions[index] = oldTransaction
                            transactionStorage.saveTransactions(transactions)
                            filterTransactions(selectedCategory) // Переключаем фильтр обратно
                        }.setAnchorView(R.id.fab_add_transaction).show()
                }
            },
            transactionToEdit = transaction
        ).show(parentFragmentManager, "EditTransactionDialog")
    }

    private fun deleteTransaction(transaction: Transaction) {
        val index = transactions.indexOf(transaction)
        if (index != -1) {
            transactions.removeAt(index)

            // Сохраняем обновлённый список транзакций
            transactionStorage.saveTransactions(transactions)

            // Обновляем фильтрованный список
            val spinnerFilter = view?.findViewById<Spinner>(R.id.spinner_filter)
            val selectedCategory = spinnerFilter?.selectedItem as String
            filterTransactions(selectedCategory)

            // Показываем Snackbar для подтверждения
            Snackbar.make(requireView(), "Transaction deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    transactions.add(index, transaction)
                    transactionStorage.saveTransactions(transactions)
                    filterTransactions(selectedCategory) // Переключаем фильтр обратно
                }.setAnchorView(R.id.fab_add_transaction).show()
        }
    }
}