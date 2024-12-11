package com.example.expensetrackerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.expensetrackerapp.model.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionDialog(
    private val onTransactionAdded: (Transaction) -> Unit,
    private val transactionToEdit: Transaction? = null
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_transaction, container, false)

        val etName = view.findViewById<EditText>(R.id.et_name)
        val etAmount = view.findViewById<EditText>(R.id.et_amount)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinner_category)
        val etDate = view.findViewById<EditText>(R.id.et_date)
        val btnAdd = view.findViewById<Button>(R.id.btn_add)

        // Настройка Spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        // Обработка выбора даты
        var selectedDate = transactionToEdit?.date ?: System.currentTimeMillis()
        etDate.setText(formatDate(selectedDate))
        etDate.setOnClickListener {
            showDatePicker(selectedDate) { newDate ->
                selectedDate = newDate
                etDate.setText(formatDate(selectedDate))
            }
        }

        // Если редактируем транзакцию, заполняем поля
        transactionToEdit?.let {
            etName.setText(it.name)
            etAmount.setText(it.amount.toString())
            spinnerCategory.setSelection(resources.getStringArray(R.array.categories).indexOf(it.category))
            btnAdd.text = "Update"
        }

        btnAdd.setOnClickListener {
            val name = etName.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = spinnerCategory.selectedItem.toString()

            if (name.isNotEmpty() && amount != null) {
                // Если транзакция редактируется, сохраняем дату
                val transaction = transactionToEdit?.copy(
                    name = name,
                    amount = amount,
                    category = category,
                    date = selectedDate
                ) ?: Transaction(
                    name = name,
                    amount = amount,
                    category = category,
                    date = selectedDate // Используем выбранную дату
                )

                onTransactionAdded(transaction)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showDatePicker(initialDate: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = initialDate

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                onDateSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun formatDate(dateInMillis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(dateInMillis))
    }
}