package com.example.expensetrackerapp

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.expensetrackerapp.model.Transaction
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar

class StatisticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var transactionStorage: TransactionStorage
    private val transactions = mutableListOf<Transaction>()
    private var monthlyLimit: Double = 0.0
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvCurrentLimit = view.findViewById<TextView>(R.id.tv_current_limit)

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        monthlyLimit = sharedPreferences.getFloat("monthly_limit", 0f).toDouble()

        // Отображаем текущий лимит
        updateCurrentLimitDisplay(tvCurrentLimit)

        // Кнопка для установки лимита
        val btnSetLimit = view.findViewById<Button>(R.id.btn_set_limit)
        btnSetLimit.setOnClickListener {
            showSetLimitDialog(tvCurrentLimit)
        }


        // Инициализация хранилища транзакций
        transactionStorage = TransactionStorage(requireContext())
        transactions.addAll(transactionStorage.loadTransactions())

        // Настройка Spinner для выбора периода
        val spinnerPeriod = view.findViewById<Spinner>(R.id.spinner_period)
        val periods = listOf("1 Day", "1 Week", "1 Month")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter

        // Обработка выбора периода
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPeriod = periods[position]
                loadChartData(selectedPeriod)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Настройка круговой диаграммы
        pieChart = view.findViewById(R.id.pie_chart)
        setupPieChart()

    }

    private fun updateCurrentLimitDisplay(tvCurrentLimit: TextView) {
        if (monthlyLimit > 0) {
            tvCurrentLimit.text = "Current Limit: ${"%.2f".format(monthlyLimit)} ₽"
        } else {
            tvCurrentLimit.text = "Current Limit: Not Set"
        }
    }

    private fun showSetLimitDialog(tvCurrentLimit: TextView) {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(if (monthlyLimit > 0) monthlyLimit.toString() else "")
        builder.setTitle("Set Monthly Limit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val limit = input.text.toString().toDoubleOrNull()
                if (limit != null && limit > 0) {
                    monthlyLimit = limit
                    sharedPreferences.edit().putFloat("monthly_limit", limit.toFloat()).apply()
                    updateCurrentLimitDisplay(tvCurrentLimit) // Обновляем текст
                    Toast.makeText(requireContext(), "Monthly limit set to $limit ₽", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Invalid limit", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
        builder.show()
    }



    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setCenterText("Expenses by Category")
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000, Easing.EaseInOutQuad)
    }

    private fun loadChartData(period: String) {
        // Вычисляем даты для фильтрации по периоду
        val currentTime = System.currentTimeMillis()
        val startTime = when (period) {
            "1 Day" -> currentTime - 24 * 60 * 60 * 1000 // 1 день назад
            "1 Week" -> currentTime - 7 * 24 * 60 * 60 * 1000 // 1 неделя назад
            "1 Month" -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1) // Устанавливаем начало текущего месяца
                calendar.timeInMillis
            }
            else -> 0L
        }

        // Фильтруем транзакции по дате
        val filteredTransactions = transactions.filter { it.date >= startTime }

        // Считаем суммы расходов по категориям
        val categoryTotals = filteredTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = categoryTotals.map { (category, total) ->
            PieEntry(total.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate() // Обновляем диаграмму
    }
}