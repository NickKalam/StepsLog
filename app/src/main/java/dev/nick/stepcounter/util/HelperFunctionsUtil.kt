package dev.nick.stepcounter.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat

fun Context.hasRequiredPermissions(): Boolean
{

    val hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    } else true // Granted on install for older OS

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    return hasActivityPermission && hasNotificationPermission
}

fun changeAppLanguage(languageCode: String) {
    val localeList = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(localeList)
}