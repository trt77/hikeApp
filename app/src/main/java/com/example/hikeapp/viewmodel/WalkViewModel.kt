package com.example.hikeapp.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import java.math.BigDecimal
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import java.math.RoundingMode
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hikeapp.DatabaseProvider
import com.example.hikeapp.Nutriments
import com.example.hikeapp.NutriscoreData
import com.example.hikeapp.Product
import com.example.hikeapp.ProductDetails
import com.example.hikeapp.RetrofitInstance
import com.example.hikeapp.SearchResponse
import com.example.hikeapp.UserDataEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path


enum class StopwatchState {
    RUNNING, STOPPED
}

class WalkViewModel(application: Application) : AndroidViewModel(application) {
    var stopwatchState = MutableLiveData<StopwatchState>()
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private var lastLocation: Location? = null
    private var totalDistance = 0f
    val stopwatchTime = MutableLiveData<Long>()
    val userData = MutableLiveData<UserDataEntity>()
    val calorieIntake = MutableLiveData<Double>()
    val db = DatabaseProvider.getDatabase(application)
    val distanceWalked = MutableLiveData<Double>()
    val searchResults = MutableLiveData<List<String>>()
    val searchResults2 = MutableLiveData<List<ProductDetails>>()
    val selectedProductDetails = MutableLiveData<ProductDetails?>()
    private val _isVibrationEnabled = MutableLiveData<Boolean>(false)
    val isVibrationEnabled: LiveData<Boolean> = _isVibrationEnabled
    private var distanceSinceLastVibration = 0f


    fun selectProduct(productName: String) {
        // Directly find the product details from the search results
        val productDetails = searchResults2.value?.find { it.product_name == productName }
        Log.d("WalkViewModel", "Attempting to update selectedProductDetails with: $productDetails")
        if (productDetails != null) {
            selectedProductDetails.value = productDetails
            Log.d("WalkViewModel", "Updated selectedProductDetails with: ${selectedProductDetails.value}")
        } else {
            Log.d("WalkViewModel", "No matching product details found for: $productName")
        }
    }

    fun insertUserData(userDataEntity: UserDataEntity) {
        viewModelScope.launch {

            db.userDataDao().insertUserData(userDataEntity)
            userData.postValue(userDataEntity) // Update LiveData immediately
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            userData.postValue(db.userDataDao().getUserData())
        }
    }

    init {
        loadUserData()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                Log.d("WalkViewModel", "Location update received: Lat ${location.latitude}, Lng ${location.longitude}")
                lastLocation?.let {
                    val distance = it.distanceTo(location)
                    totalDistance += distance
                    distanceSinceLastVibration += distance
                    Log.d("WalkViewModel", "New location update. Distance to last: $distance meters. Total distance: $totalDistance meters.")
                }
                lastLocation = location

                // Check if distanceSinceLastVibration has exceeded 100 meters
                if (distanceSinceLastVibration >= 100 && _isVibrationEnabled.value == true) {
                    vibrate(getApplication<Application>().applicationContext)
                    distanceSinceLastVibration = 0f // Reset the distance since the last vibration
                }
            }
            val distanceInKm = totalDistance / 1000
            distanceWalked.postValue(distanceInKm.toDouble()) // Convert meters to kilometers
            Log.d("WalkViewModel", "Total distance walked: $distanceInKm kilometers")
        }
    }


    // Stopwatch state
    var isRunning = MutableLiveData<Boolean>(false)
    var timeElapsed = MutableLiveData<Long>(0L)

    // Method to update time elapsed
    fun updateTimeElapsed(newTime: Long) {
        // Log.d("WalkViewModel", "Updating time elapsed: $newTime")
        timeElapsed.postValue(newTime)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (stopwatchState.value == StopwatchState.RUNNING) {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000// Update interval in milliseconds
                fastestInterval = 5000 // Fastest update interval
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                lastLocation = location
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            Log.d("WalkViewModel", "Starting location updates.")
        } else {
            Log.d("WalkViewModel", "Attempted to start location updates, but stopwatch is not running.")
        }
    }

    fun stopLocationUpdates() {
        if (stopwatchState.value != StopwatchState.RUNNING) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            lastLocation = null
            totalDistance = 0f
            distanceWalked.postValue(0.0)
            Log.d("WalkViewModel", "Stopping location updates.")
        } else {
            Log.d("WalkViewModel", "Attempted to stop location updates, but stopwatch is still running.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates() // Stop location updates when ViewModel is destroyed
    }

    private fun calculateCalories(userData: UserDataEntity, distance: Double, timeElapsed: Long): Double {
        val weightInKg = userData.weight
        val heightInCm = userData.height
        val age = userData.age
        val sex = userData.sex

        // Calculate BMR
        val bmr = if (sex.equals("male", ignoreCase = true)) {
            88.362 + (13.397 * weightInKg) + (4.799 * heightInCm) - (5.677 * age)
        } else {
            447.593 + (9.247 * weightInKg) + (3.098 * heightInCm) - (4.330 * age)
        }

        // Convert time from milliseconds to hours
        val timeHours = timeElapsed / 1000.0 / 3600.0

        // Estimate speed in km/h
        val speed = if (timeHours > 0) distance / timeHours else 0.0

        // Determine MET value based on estimated speed
        val met = when {
            speed < 3.2 -> 2.0
            speed < 4.8 -> 3.0
            else -> 4.0
        }

        // Calculate calories burned (BMR * MET * time in hours)
        val calories = (bmr / 24) * met * timeHours // Divide BMR by 24 to get hourly rate

        // Return calories rounded to 2 decimal places
        return BigDecimal(calories).setScale(2, RoundingMode.HALF_EVEN).toDouble()
    }

    fun calculateCalories(distance: Double) {
        userData.value?.let { user ->
            val caloriesBurned = calculateCalories(user, distance, timeElapsed.value ?: 0L)
            calorieIntake.postValue(caloriesBurned)
        }
    }

    fun searchFood(query: String) {
        Log.d("WalkViewModel", "Searching for: $query")
        RetrofitInstance.api.searchProducts(query).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                if (response.isSuccessful) {
                    Log.d("WalkViewModel", "Successful response")
                    searchResults.value = response.body()?.products?.map { it.product_name } ?: emptyList()
                    searchResults2.value = response.body()?.products ?: emptyList()
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                Log.d("WalkViewModel", "Error")
            }
        })
    }

    fun <T> debounce(
        waitMs: Long = 300L,
        coroutineScope: CoroutineScope,
        destinationFunction: (T) -> Unit
    ): (T) -> Unit {
        var debounceJob: Job? = null
        return { param: T ->
            debounceJob?.cancel()
            debounceJob = coroutineScope.launch {
                delay(waitMs)
                destinationFunction(param)
            }
        }
    }

    fun setVibrationEnabled(isEnabled: Boolean) {
        _isVibrationEnabled.value = isEnabled
    }

    fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }



}