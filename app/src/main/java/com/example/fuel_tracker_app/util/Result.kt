package com.example.fuel_tracker_app.util

// Wrapper klasa za rezultate operacija koje mogu neuspjeti
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}