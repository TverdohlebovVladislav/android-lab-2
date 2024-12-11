package com.example.expensetrackerapp

import android.content.Context
import android.content.SharedPreferences
import com.example.expensetrackerapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionStorage(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Сохранение транзакций
    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", json).apply()
    }

    // Загрузка транзакций
    fun loadTransactions(): MutableList<Transaction> {
        val json = sharedPreferences.getString("transactions", null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}