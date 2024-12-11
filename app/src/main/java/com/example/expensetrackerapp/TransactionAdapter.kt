package com.example.expensetrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onTransactionLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val date: TextView = itemView.findViewById(R.id.tv_date) // Новый элемент для даты
        val category: TextView = itemView.findViewById(R.id.tv_category)
        val amount: TextView = itemView.findViewById(R.id.tv_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.name.text = transaction.name
        holder.category.text = transaction.category
        holder.amount.text = "${transaction.amount} ₽"
        holder.date.text = formatDate(transaction.date) // Устанавливаем дату

        // Добавляем обработчик долгого нажатия
        holder.itemView.setOnLongClickListener {
            onTransactionLongClick(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size

    private fun formatDate(dateInMillis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(dateInMillis))
    }
}