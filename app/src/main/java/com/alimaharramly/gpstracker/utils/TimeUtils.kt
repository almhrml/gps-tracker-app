package com.alimaharramly.gpstracker.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("SimpleDateFormat")
object TimeUtils {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss")
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

    fun getTime(timeInMillis: Long): String { // Перевод времени в формат
        val cv = java.util.Calendar.getInstance()
        timeFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC") // Установка часового пояса
        cv.timeInMillis = timeInMillis
        return timeFormatter.format(cv.time)
    }

    fun getDate(): String {
        val cv = java.util.Calendar.getInstance()
        return dateFormatter.format(cv.time)
    }
}