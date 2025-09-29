package com.example.fuel_tracker_app.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// Ekstenzije za rad sa datumima
object DateExt {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Konvertuj Timestamp u String
    fun Timestamp.toDateString(): String {
        return dateFormat.format(this.toDate())
    }

    // Dobij početak tekućeg meseca
    fun getStartOfCurrentMonth(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    // Dobij kraj tekućeg meseca
    fun getEndOfCurrentMonth(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return Timestamp(calendar.time)
    }

    // Dobij početak tekuće godine
    fun getStartOfCurrentYear(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    // Dobij kraj tekuće godine
    fun getEndOfCurrentYear(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return Timestamp(calendar.time)
    }
}