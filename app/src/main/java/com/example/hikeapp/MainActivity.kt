package com.example.hikeapp

import android.os.Bundle
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import android.graphics.Bitmap
import android.location.Location
import android.os.Environment
import android.util.Log
import java.io.FileOutputStream
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.Button
import android.widget.Toast
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import com.example.hikeapp.ui.theme.HikeAppTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.hikeapp.viewmodel.StopwatchState
import com.example.hikeapp.viewmodel.WalkViewModel
import kotlinx.coroutines.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.view.WindowInsetsCompat


class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    // Declare the BroadcastReceiver
    private val stopwatchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val time = intent?.getLongExtra("time", 0) ?: 0
            val viewModel = ViewModelProvider(this@MainActivity)[WalkViewModel::class.java]
            viewModel.updateTimeElapsed(time)
            Log.d("MainActivity", "Broadcast received: Time elapsed: $time")
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                    )
                )
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                // All permissions are granted
            } else {
                // Handle the case where permissions are denied
            }
        }

        val viewModel = ViewModelProvider(this).get(WalkViewModel::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopwatchReceiver, IntentFilter("StopwatchUpdate"), RECEIVER_EXPORTED)
        } else {
            registerReceiver(stopwatchReceiver, IntentFilter("StopwatchUpdate"))
        }

        viewModel.stopwatchTime.observe(this) { time ->
            viewModel.updateTimeElapsed(time)
        }


        setContent {
            HikeAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
        checkAndRequestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister BroadcastReceiver
        unregisterReceiver(stopwatchReceiver)
    }


}





@Composable
fun MainScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        var selectedItem by remember { mutableIntStateOf(0) }


        data class NavigationItem(val label: String, val icon: ImageVector)

        val items = listOf(
            NavigationItem("Timer", Icons.Filled.PlayArrow),
            NavigationItem("Gallery", Icons.Filled.Place),
            NavigationItem("Food", Icons.Filled.Edit),
            NavigationItem("Me", Icons.Filled.Person)
        )

        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }


        when (selectedItem) {
            0 -> TimerScreen()
            1 -> GalleryScreen()
            2 -> FoodScreen()
            3 -> MeScreen()
        }

    }

}

@Composable
fun TimerScreen(viewModel: WalkViewModel = viewModel()) {
    val context = LocalContext.current
    var timerJob by remember { mutableStateOf<Job?>(null) }
    val distanceWalked by viewModel.distanceWalked.observeAsState(0.0)
    val isRunning by viewModel.isRunning.observeAsState(false)
    val timeElapsed by viewModel.timeElapsed.observeAsState(0L)
    Log.d("TimerScreen", "Recomposing with time: $timeElapsed")
    val coroutineScope = rememberCoroutineScope()
    val caloriesBurned by viewModel.calorieIntake.observeAsState("")
    val isVibrationEnabled by viewModel.isVibrationEnabled.observeAsState(false)

    // Whenever distanceWalked is updated, recalculate calories
    LaunchedEffect(distanceWalked) {
        viewModel.calculateCalories(distanceWalked)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = String.format("%02d:%02d:%02d:%02d",
                (timeElapsed / 3600000),
                (timeElapsed / 60000) % 60,
                (timeElapsed / 1000) % 60,
                (timeElapsed / 50) % 20),
            style = MaterialTheme.typography.displayMedium
        )

        Row {
            Button(
                onClick = {
                    if (!isRunning) {
                        viewModel.isRunning.value = true
                        viewModel.stopwatchState.value = StopwatchState.RUNNING
                        viewModel.startLocationUpdates()
                        val startIntent = Intent(context, StopwatchService::class.java).apply {
                            action = "START"
                        }
                        ContextCompat.startForegroundService(context, startIntent)

                        timerJob = coroutineScope.launch {
                            while (isActive) {
                                delay(50)
                                viewModel.updateTimeElapsed(timeElapsed + 50)
                            }
                        }
                    }
                },
                enabled = !isRunning,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Start")
            }

            Button(
                onClick = {
                    viewModel.isRunning.value = false
                    val stopIntent = Intent(context, StopwatchService::class.java).apply {
                        action = "STOP"
                    }
                    viewModel.updateTimeElapsed(0L)
                    context.startService(stopIntent)
                    viewModel.stopwatchState.value = StopwatchState.STOPPED
                    viewModel.stopLocationUpdates()
                    timerJob?.cancel()
                    viewModel.calorieIntake.postValue(0.00)
                },
                enabled = isRunning,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Stop")
            }
        }

        Text(
            text = "Distance walked: ${"%.2f".format(distanceWalked)} km",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Calories burned: $caloriesBurned Kcal",
            style = MaterialTheme.typography.bodyLarge
        )

        // Toggle for vibration feature
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vibrate every 100m")
            Switch(
                checked = isVibrationEnabled,
                onCheckedChange = { isEnabled ->
                    Log.d("SwitchDebug", "Switch clicked: $isEnabled") // Debug log
                    viewModel.setVibrationEnabled(isEnabled)
                }
            )
        }

    }
}


@Composable
fun ImageListItem(uri: Uri, dateTime: String, locationString: String, context: Context, onDelete: () -> Unit, onShowLocation: () -> Unit, onImageClick: () -> Unit) {
    val thumbnailBitmap = loadThumbnail(uri, context)?.asImageBitmap()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val contentUri = uri.path
                    ?.let { File(it) }
                    ?.let {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            it
                        )
                    }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Handle the case where no app can handle the intent
                }
            }
            .padding(8.dp)
    ) {
        thumbnailBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Thumbnail",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.weight(1f))

        Column(modifier = Modifier.weight(1f)) {
            Text(dateTime)
            // You can add more text or components here if needed
        }

        IconButton(onClick = {
            Log.d("GalleryScreen", "Show Location clicked. Location: $locationString")
            parseLocation(locationString)?.let { (lat, lng) ->
                Log.d("GalleryScreen", "Parsed Location: Lat: $lat, Lng: $lng")
                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                } else {
                    Log.d("GalleryScreen", "No Intent available to handle action")
                }
            } ?: run {
                Log.d("GalleryScreen", "Location parsing failed")
            }
        }) {
            Icon(Icons.Filled.Map, contentDescription = "Show Location")
        }

        IconButton(onClick = { onDelete() }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Image")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: WalkViewModel = viewModel()) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var location: Location? by remember { mutableStateOf(null) }
    var imagesList by remember { mutableStateOf(listOf<Triple<Uri, String, String>>()) }
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    var showDialog by remember { mutableStateOf(false) }
    var imageUriToDelete by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        imagesList = loadImagesFromFolder(context)
    }

    // Deleting image
    fun deleteImage(uri: String) {
        coroutineScope.launch {
            db.imageDao().deleteImageByUri(uri)
            imagesList = db.imageDao().getAllImages().map { Triple(Uri.parse(it.uri), it.dateTime, it.location) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete this image?") },
            confirmButton = {
                Button(onClick = {
                    deleteImage(imageUriToDelete)
                    showDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Flashlight state
    var isFlashlightOn by remember { mutableStateOf(false) }

    // Toggle flashlight
    val toggleFlashlight = {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0] // Usually the back camera has the flashlight
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            // Handle exceptions like no camera available
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            captureLocation(context) { location ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val imageFile = createImageFile(context)
                        saveBitmapToFile(bitmap, imageFile)
                        Log.d(
                            "GalleryScreen",
                            "Image file created and saved: ${imageFile.absolutePath}"
                        )
                        val imageUri = Uri.fromFile(imageFile).toString()
                        val currentTime = dateFormat.format(Date())
                        val locationString = "${location?.latitude}, ${location?.longitude}"

                        // Save to database
                        val imageEntity = ImageEntity(imageUri, currentTime, locationString)
                        db.imageDao().insertImage(imageEntity)

                        // Update imagesList
                        imagesList = db.imageDao().getAllImages()
                            .map { Triple(Uri.parse(it.uri), it.dateTime, it.location) }

                    } catch (e: Exception) {
                        Log.e("GalleryScreen", "Error saving image", e)
                    }
                }
            }
        } else {
            Log.d("GalleryScreen", "Bitmap returned from camera is null")
        }
    }

    // Load images from database
    LaunchedEffect(Unit) {
        imagesList = db.imageDao().getAllImages().map { Triple(Uri.parse(it.uri), it.dateTime, it.location) }
    }

    Column {
        // Top Bar with Camera and Flashlight Icons
        TopAppBar(
            title = { Text("Gallery") },
            actions = {
                IconButton(onClick = {takePictureLauncher.launch(null)}) {

                    Icon(Icons.Filled.Camera, contentDescription = "Open Camera")
                }
                IconButton(onClick = { toggleFlashlight() }) {
                    Icon(Icons.Filled.FlashlightOn, contentDescription = "Toggle Flashlight")
                }
            }
        )

        // List of Images
        LazyColumn () {
            items(imagesList) { (uri, dateTime, locationString) ->
                ImageListItem(
                    uri = uri,
                    dateTime = dateTime,
                    locationString = locationString,
                    context = context,
                    onDelete = {
                        imageUriToDelete = uri.toString()
                        showDialog = true },
                    onShowLocation = {

                    },
                    onImageClick = {
                        // Logic to open the image
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}




@Composable
fun FoodScreen(viewModel: WalkViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.observeAsState(emptyList())
    //
    val selectedProductDetails by viewModel.selectedProductDetails.observeAsState()
    //
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val lazyColumnHeight = screenHeight / 3

    val debounceSearch = remember {
        viewModel.debounce<String>(1000L, CoroutineScope(Dispatchers.Main)) {
            viewModel.searchFood(it)
        }
    }

    // When an item is clicked in LazyColumn
    fun selectProduct(productName: String) {
        Log.d("WalkViewModel", "Product selected: $productName")
        viewModel.selectProduct(productName)
        query = productName // Update the text field with the selected product name
        showResults = false // Hide the search suggestions
        isEditing = false
    }



    // Handler for hiding the results
    fun hideResults() {
        showResults = false
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { hideResults() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Find product",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    isEditing = it.isNotEmpty()
                    debounceSearch(it)
                },
                label = { Text("Search Food") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isEditing = focusState.isFocused || query.isNotEmpty()
                    }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = lazyColumnHeight)
            ) {
                items(searchResults) { result ->
                    TextButton(
                        onClick = {
                            selectProduct(result)
                            query = result
                            hideResults()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(result)
                    }
                }
            }


            selectedProductDetails?.let { details ->
                Column {
                    Text(
                        "Nutrition values:",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(Modifier.padding(bottom = 4.dp)) {
                        Text(
                            "Carbohydrates per 100g:",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${details.nutriments.carbohydrates}g",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.padding(bottom = 4.dp)) {
                        Text(
                            "Energy:",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${details.nutriments.energy} J",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.padding(bottom = 4.dp)) {
                        Text(
                            "Sugars:",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${details.nutriments.sugars}g",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(viewModel: WalkViewModel = viewModel()) {
    val context = LocalContext.current
    var sex by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("female", "male")
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    val currentUserData by viewModel.userData.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Your Info",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))
/*
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = sex,
                onValueChange = { sex = it },
                readOnly = true,
                label = { Text("Sex") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedOptionText = selectionOption
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
  */
        TextField(
            value = sex,
            onValueChange = { sex = it },
            label = { Text("Sex") },
            placeholder = { Text("Enter your sex (male or female)")},
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )

            Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight") },
            placeholder = { Text("Enter your weight in kg") },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height") },
            placeholder = { Text("Enter your height in cm") },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            placeholder = { Text("Enter your age") },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = {
                viewModel.insertUserData(
                    UserDataEntity(
                        sex = sex,
                        weight = weight.toDoubleOrNull() ?: 0.0,
                        height = height.toDoubleOrNull() ?: 0.0,
                        age = age.toIntOrNull() ?: 0
                    )
                )
                Toast.makeText(context, "Successfully saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            Text("Save")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            currentUserData?.let {
                Text("Sex: ${it.sex}")
                Text("Weight: ${it.weight}")
                Text("Height: ${it.height}")
                Text("Age: ${it.age}")
            }
                ?: Text("Please fill out the information in order to count calories burned during hike.")
        }
    }
}




fun createImageFile(context: Context): File {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "HikeApp"
        )
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "JPEG_${timeStamp}.jpg")
    } catch (e: Exception) {
        Log.e("GalleryScreen", "Error creating image file", e)
        throw e // Rethrow the exception so you can handle it higher up if needed
    }
}


fun saveBitmapToFile(bitmap: Bitmap, file: File) {
    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    } catch (e: Exception) {
        Log.e("GalleryScreen", "Error saving bitmap to file", e)
        throw e // Rethrow the exception so you can handle it higher up if needed
    }
}

fun loadThumbnail(uri: Uri, context: Context): Bitmap? {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, 100, 100)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
        }.also { options ->
            // Must reopen the stream as it was consumed by the first decode call
            return context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }
    }
    return null
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun loadImagesFromFolder(context: Context): List<Triple<Uri, String, String>> {
    val sharedPreferences = context.getSharedPreferences("HikeAppPreferences", Context.MODE_PRIVATE)
    val imagesList = mutableListOf<Triple<Uri, String, String>>()
    val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "HikeApp")
    val files = storageDir.listFiles()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    files?.forEach { file ->
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val date = dateFormat.format(Date(file.lastModified()))
        val locationData = sharedPreferences.getString(uri.toString(), "Unknown Location") ?: "Unknown Location"

        imagesList.add(Triple(uri, date, locationData))
    }

    return imagesList
}

fun captureLocation(context: Context, onLocationCaptured: (Location?) -> Unit) {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Check for location permission
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                onLocationCaptured(location)
            }
            .addOnFailureListener {
                onLocationCaptured(null)
            }
    } else {
        onLocationCaptured(null)
    }
}

fun saveLocationData(context: Context, imageUri: Uri, location: Location?) {
    val sharedPreferences = context.getSharedPreferences("HikeAppPreferences", Context.MODE_PRIVATE)
    val locationString = if (location != null) "${location.latitude},${location.longitude}" else "Unknown"
    sharedPreferences.edit().putString(imageUri.toString(), locationString).apply()
}

fun parseLocation(locationString: String): Pair<Double, Double>? {
    return locationString.split(",").let {
        if (it.size == 2) {
            try {
                val latitude = it[0].toDouble()
                val longitude = it[1].toDouble()
                return Pair(latitude, longitude)
            } catch (e: NumberFormatException) {
                return null
            }
        } else null
    }
}

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey val uri: String,
    val dateTime: String,
    val location: String
)

@Entity(tableName = "user_data")
data class UserDataEntity(
    @PrimaryKey val id: Int = 1, // Fixed ID,
    val sex: String,
    val weight: Double,
    val height: Double,
    val age: Int
)

@Dao
interface ImageDao {
    @Insert
    suspend fun insertImage(imageEntity: ImageEntity)

    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("DELETE FROM images WHERE uri = :uri")
    suspend fun deleteImageByUri(uri: String)
}

@Dao
interface UserDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserData(userDataEntity: UserDataEntity)

    @Query("SELECT * FROM user_data LIMIT 1")
    suspend fun getUserData(): UserDataEntity?
}


@Database(entities = [ImageEntity::class, UserDataEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun userDataDao(): UserDataDao
}

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hike-app-database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
