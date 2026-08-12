package com.rudra.swiggymind

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
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

        // Handle OAuth callback if app was launched from redirect
        handleOAuthCallback(intent)

        detectLocation()

        setContent {
            SwiggyMindTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle OAuth callback when app is already running
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "swiggymind" && uri.host == "auth") {
                val code = uri.getQueryParameter("code")
                val state = uri.getQueryParameter("state")
                val error = uri.getQueryParameter("error")

                if (error != null) {
                    // Handle OAuth error
                    android.util.Log.e("OAuth", "OAuth error: $error")
                    return
                }

                if (code != null) {
                    // Exchange authorization code for access token
                    lifecycleScope.launch {
                        exchangeCodeForToken(code)
                    }
                }
            }
        }
    }

    private suspend fun exchangeCodeForToken(code: String) {
        // TODO: Implement token exchange when Swiggy provides OAuth client credentials
        // For now, just log the code for testing
        android.util.Log.d("OAuth", "Received authorization code: $code")
        // In production: POST to Swiggy's token endpoint with code + redirect_uri + client_id + client_secret
        // Store resulting access_token in SettingsRepository
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
