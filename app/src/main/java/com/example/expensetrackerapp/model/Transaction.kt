package com.example.expensetrackerapp.model

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis()
)