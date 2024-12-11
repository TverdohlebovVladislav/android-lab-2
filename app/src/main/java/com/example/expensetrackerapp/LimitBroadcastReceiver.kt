package com.example.expensetrackerapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.expensetrackerapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class LimitBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Инициализация TransactionStorage
        val transactionStorage = TransactionStorage(context)
        val transactions = transactionStorage.loadTransactions()

        Log.d("TransactionsDebug", "Loaded transactions: $transactions")

        // Получаем лимит из SharedPreferences
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val monthlyLimit = sharedPreferences.getFloat("monthly_limit", 0f).toDouble()

        if (monthlyLimit > 0) {
            // Считаем расходы за текущий месяц
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1) // Устанавливаем начало текущего месяца
            val startOfMonth = calendar.timeInMillis

            val totalExpenses = transactions.filter { it.date >= startOfMonth }
                .sumOf { it.amount }

            Log.d("TransactionsDebug", "Total expenses for current month: $totalExpenses")


            // Отправляем уведомление, если превышен лимит
            if (totalExpenses >= monthlyLimit * 0.8) {
                sendLimitNotification(context, totalExpenses, monthlyLimit)
            }
        }
    }

    private fun sendLimitNotification(context: Context, totalExpenses: Double, monthlyLimit: Double) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, "expense_notifications")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Monthly Limit Warning")
            .setContentText("You have spent ${"%.2f".format(totalExpenses)} ₽, which is 80% of your limit!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }
}