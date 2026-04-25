package com.rudra.swiggymind

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.rudra.swiggymind.domain.repository.SettingsRepository
import com.rudra.swiggymind.ui.MainScreen
import com.rudra.swiggymind.ui.theme.SwiggyMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            detectLocation()
        } else {
            lifecycleScope.launch {
                settingsRepository.setCurrentCity(AppConstants.DEFAULT_CITY)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        detectLocation()

        setContent {
            SwiggyMindTheme {
                MainScreen()
            }
        }
    }

    private fun detectLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                lifecycleScope.launch {
                    val detectedCity = withContext(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            val rawCity = addresses?.firstOrNull()?.locality ?: AppConstants.DEFAULT_CITY
                            when {
                                rawCity.contains("Mumbai", ignoreCase = true) -> "Mumbai"
                                rawCity.contains("Bangalore", ignoreCase = true) ||
                                    rawCity.contains("Bengaluru", ignoreCase = true) -> "Bangalore"
                                rawCity.contains("Ahmedabad", ignoreCase = true) -> "Ahmedabad"
                                else -> AppConstants.DEFAULT_CITY
                            }
                        } catch (e: Exception) {
                            AppConstants.DEFAULT_CITY
                        }
                    }
                    settingsRepository.setCurrentCity(detectedCity)
                }
            }
        }
    }
}
