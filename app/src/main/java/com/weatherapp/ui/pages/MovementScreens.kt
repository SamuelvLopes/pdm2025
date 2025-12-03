package com.weatherapp.ui.pages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RunCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.weatherapp.LoginActivity
import com.weatherapp.data.LocationInfo
import com.weatherapp.data.MovementCache
import com.weatherapp.data.MovementPreferences
import com.weatherapp.data.MovementRepository
import com.weatherapp.data.TrainingSummary
import com.weatherapp.data.TrainingType
import com.weatherapp.data.UserProfile
import com.weatherapp.data.WorkoutSession
import com.weatherapp.notifications.TrainingNotification
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthGateScreen(
    onGoHome: () -> Unit,
    onRequireOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val user = FirebaseAuth.getInstance().currentUser
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (navigated) return@LaunchedEffect
        if (user == null) {
            activity?.startActivity(Intent(context, LoginActivity::class.java))
            activity?.finish()
            navigated = true
            return@LaunchedEffect
        }
        try {
            val profile = MovementRepository.fetchUserProfile(user.uid)
            if (profile == null || profile.ageGroup.isBlank() || profile.level.isBlank()) {
                onRequireOnboarding()
            } else {
                val cached = MovementPreferences.cacheFlow(context).first()
                val hasRemoteData = profile.lastWorkoutAt > 0L ||
                    profile.streakCount > 0 ||
                    profile.pointsTotal > 0
                if (hasRemoteData && profile.lastWorkoutAt >= cached.lastWorkoutAt) {
                    MovementPreferences.updateCache(
                        context,
                        profile.lastWorkoutAt,
                        profile.streakCount,
                        profile.pointsTotal
                    )
                }
                onGoHome()
            }
        } catch (ex: Exception) {
            onGoHome()
        } finally {
            navigated = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(HeroGradient))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Movimente-se",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Carregando...", color = Color.White)
        }
    }
}

@Composable
fun OnboardingProfileScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    var ageGroup by remember { mutableStateOf("Idoso") }
    var level by remember { mutableStateOf("Iniciante") }
    var isSaving by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(TealGradient))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Seu perfil",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Escolha faixa e nivel para personalizar",
                color = Color.White,
                textAlign = TextAlign.Center
            )
            OptionGroup(
                title = "Faixa",
                options = listOf("Idoso", "Adolescente"),
                selected = ageGroup,
                onSelect = { ageGroup = it }
            )
            OptionGroup(
                title = "Nivel",
                options = listOf("Iniciante", "Medio", "Avancado"),
                selected = level,
                onSelect = { level = it }
            )
            Button(
                onClick = {
                    if (user == null || isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        val displayName = user.displayName ?: user.email ?: "Usuario"
                        val profile = UserProfile(
                            displayName = displayName,
                            ageGroup = ageGroup,
                            level = level,
                            pointsTotal = 0,
                            streakCount = 0,
                            lastWorkoutAt = 0L,
                            pairId = null
                        )
                        try {
                            MovementRepository.createUserProfile(user.uid, profile)
                            MovementPreferences.updateCache(context, 0L, 0, 0)
                            onComplete()
                        } catch (ex: Exception) {
                            Toast.makeText(
                                context,
                                "Falha ao salvar perfil.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color(0xFF08BFA5),
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (isSaving) "Salvando..." else "Continuar",
                    color = Color(0xFF08BFA5),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onStartTraining: (String) -> Unit,
    onShowInvite: () -> Unit,
    onScanInvite: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val user = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    val cachedState: MutableState<MovementCache> = remember { mutableStateOf(MovementCache(0L, 0, 0)) }
    var cacheLoaded by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var partnerName by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var audioPermissionGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO))
    }
    var locationPermissionGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var locationInfo by remember { mutableStateOf<LocationInfo?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var cityUsersCount by remember { mutableStateOf<Int?>(null) }
    var isCountingCityUsers by remember { mutableStateOf(false) }
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    LaunchedEffect(Unit) {
        MovementPreferences.cacheFlow(context).collectLatest {
            cachedState.value = it
            cacheLoaded = true
        }
    }

    LaunchedEffect(user?.uid) {
        if (user == null) return@LaunchedEffect
        try {
            val fetched = MovementRepository.fetchUserProfile(user.uid)
            profile = fetched
            locationInfo = MovementRepository.fetchUserLocation(user.uid)
        } catch (ex: Exception) {
            Toast.makeText(context, "Sem conexao com o Firestore.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(cacheLoaded, profile?.lastWorkoutAt, profile?.streakCount, profile?.pointsTotal) {
        val fetched = profile ?: return@LaunchedEffect
        if (!cacheLoaded) return@LaunchedEffect
        val cached = cachedState.value
        val hasRemoteData = fetched.lastWorkoutAt > 0L ||
            fetched.streakCount > 0 ||
            fetched.pointsTotal > 0
        if (hasRemoteData && fetched.lastWorkoutAt >= cached.lastWorkoutAt) {
            MovementPreferences.updateCache(
                context,
                fetched.lastWorkoutAt,
                fetched.streakCount,
                fetched.pointsTotal
            )
        }
    }

    LaunchedEffect(profile?.pairId) {
        val pairId = profile?.pairId ?: return@LaunchedEffect
        try {
            val pair = MovementRepository.fetchPair(pairId) ?: return@LaunchedEffect
            val otherUid = if (pair.userAUid == user?.uid) pair.userBUid else pair.userAUid
            partnerName = MovementRepository.fetchUserDisplayName(otherUid) ?: "Parceiro"
        } catch (_: Exception) {
            partnerName = null
        }
    }

    LaunchedEffect(locationInfo?.cityName) {
        val city = locationInfo?.cityName
        if (city.isNullOrBlank()) {
            cityUsersCount = null
            return@LaunchedEffect
        }
        isCountingCityUsers = true
        try {
            cityUsersCount = MovementRepository.countUsersInCity(city)
        } catch (_: Exception) {
            cityUsersCount = null
        } finally {
            isCountingCityUsers = false
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermissionGranted = granted
        if (granted) {
            startVoiceListening(speechRecognizer, context) { isListening = it }
        } else {
            Toast.makeText(
                context,
                "Permissao de microfone necessaria para iniciar por voz.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            isLocating = true
            fetchLocation(
                fusedLocationClient = fusedLocationClient,
                onSuccess = { info ->
                    val uid = user?.uid
                    locationInfo = info
                    scope.launch {
                        val cityName = resolveCityName(context, info.latitude, info.longitude)
                        val infoWithCity = info.copy(cityName = cityName)
                        locationInfo = infoWithCity
                        if (uid != null) {
                            try {
                                MovementRepository.updateUserLocation(uid, infoWithCity)
                            } catch (_: Exception) {
                            }
                        }
                    }
                },
                onError = {
                    Toast.makeText(context, "Falha ao obter GPS.", Toast.LENGTH_SHORT).show()
                },
                onComplete = { isLocating = false }
            )
        } else {
            Toast.makeText(context, "Permissao de localizacao negada.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(
                    context,
                    "Nao foi possivel iniciar a voz.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val allText = matches?.joinToString(" ")?.lowercase(Locale.getDefault()).orEmpty()
                when {
                    allText.contains("iniciar alongamento") -> onStartTraining(TrainingType.Stretch.id)
                    allText.contains("iniciar caminhada") -> onStartTraining(TrainingType.Walk.id)
                    allText.contains("iniciar treino") -> onStartTraining(TrainingType.Walk.id)
                    else -> Toast.makeText(
                        context,
                        "Comando nao reconhecido.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose { speechRecognizer?.destroy() }
    }

    val cached = cachedState.value
    val mergedProfile = profile?.let {
        if (cached.lastWorkoutAt > it.lastWorkoutAt) {
            it.copy(
                lastWorkoutAt = cached.lastWorkoutAt,
                streakCount = cached.streakCount,
                pointsTotal = cached.pointsTotal
            )
        } else {
            it
        }
    }
    val displayName = mergedProfile?.displayName ?: user?.displayName ?: "Usuario"
    val pointsTotal = mergedProfile?.pointsTotal ?: cached.pointsTotal
    val streak = mergedProfile?.streakCount ?: cached.streakCount
    val lastWorkoutAt = mergedProfile?.lastWorkoutAt ?: cached.lastWorkoutAt

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F7FB)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ola, $displayName",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pronto para se movimentar hoje?",
                        color = Color(0xFF6B7280)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Sequencia",
                        value = "$streak dias",
                        colors = OrangeGradient,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Pontos",
                        value = pointsTotal.toString(),
                        colors = BlueGradient,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                StatCard(
                    label = "Ultimo treino",
                    value = formatDateTime(lastWorkoutAt),
                    colors = LavenderGradient,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                GradientCard(
                    title = "Possiveis parceiros proximos",
                    subtitle = "",
                    colors = TealGradient
                ) {
                    val cityLabel = locationInfo?.cityName ?: "Cidade nao encontrada"
                    Text(text = "Cidade: $cityLabel", color = Color.White.copy(alpha = 0.95f))
                    val cityCountLabel = when {
                        locationInfo?.cityName.isNullOrBlank() -> "Usuarios na cidade: --"
                        isCountingCityUsers -> "Usuarios na cidade: contando..."
                        cityUsersCount != null -> "Usuarios na cidade: $cityUsersCount"
                        else -> "Usuarios na cidade: indisponivel"
                    }
                    Text(text = cityCountLabel, color = Color.White.copy(alpha = 0.9f))
                    Button(
                        onClick = {
                            if (isLocating) return@Button
                            if (!locationPermissionGranted) {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                return@Button
                            }
                            isLocating = true
                            fetchLocation(
                                fusedLocationClient = fusedLocationClient,
                                onSuccess = { info ->
                                    val uid = user?.uid
                                    locationInfo = info
                                    scope.launch {
                                        val cityName = resolveCityName(context, info.latitude, info.longitude)
                                        val infoWithCity = info.copy(cityName = cityName)
                                        locationInfo = infoWithCity
                                        if (uid != null) {
                                            try {
                                                MovementRepository.updateUserLocation(uid, infoWithCity)
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                                },
                                onError = {
                                    Toast.makeText(context, "Falha ao obter GPS.", Toast.LENGTH_SHORT).show()
                                },
                                onComplete = { isLocating = false }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = if (isLocating) "Buscando GPS..." else "Atualizar parceiro",
                            color = Color(0xFF07BEB8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item {
                GradientCard(
                    title = "Treino do dia",
                    subtitle = "Escolha seu modo (60s)",
                    colors = HeroGradient
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onStartTraining(TrainingType.Walk.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Filled.RunCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Caminhada", color = Color(0xFF0CB8FF))
                        }
                        Button(
                            onClick = { onStartTraining(TrainingType.Stretch.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alongamento", color = Color(0xFF0CB8FF))
                        }
                    }
                }
            }
            item {
                GradientCard(
                    title = "Desafio intergeracional",
                    subtitle = partnerName?.let { "Conectado com $it" } ?: "Ainda sem parceiro",
                    colors = PurpleGradient
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onShowInvite,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Group, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gerar QR", color = Color(0xFF8A46FF))
                        }
                        Button(
                            onClick = onScanInvite,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escanear", color = Color(0xFF8A46FF))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (!audioPermissionGranted) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            startVoiceListening(speechRecognizer, context) { isListening = it }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14C38E)),
                    enabled = !isListening
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isListening) "Ouvindo..." else "Iniciar por voz")
                }
            }
            item {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        activity?.startActivity(Intent(context, LoginActivity::class.java))
                        activity?.finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF2E2E2E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair", color = Color(0xFF2E2E2E))
                }
            }
        }
    }
}

@Composable
fun TrainingScreen(
    type: String,
    onFinish: (TrainingSummary) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handler = remember { Handler(Looper.getMainLooper()) }
    val user = FirebaseAuth.getInstance().currentUser
    val trainingType = remember(type) { TrainingType.fromId(type) }
    val isWalk = trainingType == TrainingType.Walk
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val stepCounterSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val stepDetectorSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }
    val activeStepSensor = if (isWalk) stepCounterSensor ?: stepDetectorSensor else null
    val usesStepCounter = isWalk && stepCounterSensor != null
    var activityPermissionGranted by remember {
        mutableStateOf(hasActivityRecognitionPermission(context))
    }
    var stepBaseline by remember { mutableStateOf<Float?>(null) }
    var steps by remember { mutableStateOf(0) }
    var remainingSeconds by remember { mutableStateOf(60) }
    var isFinished by remember { mutableStateOf(false) }
    var pendingNotificationPoints by remember { mutableStateOf<Int?>(null) }
    val cachedState: MutableState<MovementCache> = remember { mutableStateOf(MovementCache(0L, 0, 0)) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        MovementPreferences.cacheFlow(context).collectLatest {
            cachedState.value = it
        }
    }

    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        try {
            profile = MovementRepository.fetchUserProfile(uid)
        } catch (_: Exception) {
            profile = null
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        activityPermissionGranted = granted
        if (!granted) {
            Toast.makeText(
                context,
                "Permissao de atividade necessaria para passos.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val points = pendingNotificationPoints ?: return@rememberLauncherForActivityResult
        if (granted) {
            TrainingNotification.showTrainingComplete(context, points)
        }
        pendingNotificationPoints = null
    }

    LaunchedEffect(isWalk) {
        if (isWalk && needsActivityRecognitionPermission() && !activityPermissionGranted) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    fun notifyCompletion(points: Int) {
        if (needsPostNotificationsPermission() &&
            !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            pendingNotificationPoints = points
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TrainingNotification.showTrainingComplete(context, points)
        }
    }

    fun finishTraining() {
        if (isFinished) return
        isFinished = true
        vibrateShort(context)
        val uid = user?.uid
        val cached = cachedState.value
        val profileWorkoutAt = profile?.lastWorkoutAt ?: 0L
        val useCache = cached.lastWorkoutAt > profileWorkoutAt
        val lastWorkoutAt = if (useCache) cached.lastWorkoutAt else (profile?.lastWorkoutAt ?: cached.lastWorkoutAt)
        val lastStreak = if (useCache) cached.streakCount else (profile?.streakCount ?: cached.streakCount)
        val lastPoints = if (useCache) cached.pointsTotal else (profile?.pointsTotal ?: cached.pointsTotal)
        val pointsGained = if (trainingType == TrainingType.Walk) {
            val extra = min(steps / 20, 10)
            10 + extra
        } else {
            12
        }
        val now = System.currentTimeMillis()
        val newStreak = computeStreak(lastWorkoutAt, lastStreak, now)
        val newPointsTotal = lastPoints + pointsGained
        notifyCompletion(pointsGained)
        scope.launch {
            if (uid != null) {
                val session = WorkoutSession(
                    type = trainingType.id,
                    durationSec = 60,
                    steps = if (trainingType == TrainingType.Walk) steps else 0,
                    points = pointsGained,
                    createdAt = now,
                    pairId = profile?.pairId
                )
                try {
                    MovementRepository.addSession(uid, session)
                    MovementRepository.updateUserProfile(
                        uid,
                        mapOf(
                            "pointsTotal" to newPointsTotal,
                            "streakCount" to newStreak,
                            "lastWorkoutAt" to now
                        )
                    )
                    profile?.pairId?.let { MovementRepository.addPairSession(it, session) }
                } catch (ex: Exception) {
                    Toast.makeText(
                        context,
                        "Falha ao salvar no Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            MovementPreferences.updateCache(context, now, newStreak, newPointsTotal)
            onFinish(
                TrainingSummary(
                    type = trainingType.id,
                    steps = if (trainingType == TrainingType.Walk) steps else 0,
                    points = pointsGained,
                    streak = newStreak
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isFinished) {
            delay(1000)
            remainingSeconds -= 1
            if (trainingType == TrainingType.Stretch &&
                (remainingSeconds == 40 || remainingSeconds == 20)
            ) {
                vibrateShort(context)
            }
        }
        if (remainingSeconds == 0 && !isFinished) {
            finishTraining()
        }
    }

    DisposableEffect(activeStepSensor, activityPermissionGranted, isWalk) {
        if (!isWalk) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (usesStepCounter) {
                    val value = event.values.firstOrNull() ?: return
                    handler.post {
                        if (stepBaseline == null) {
                            stepBaseline = value
                            return@post
                        }
                        val delta = (value - (stepBaseline ?: value)).toInt()
                        steps = if (delta < 0) 0 else delta
                    }
                } else {
                    handler.post { steps += 1 }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (activeStepSensor != null && activityPermissionGranted) {
            sensorManager.registerListener(
                listener,
                activeStepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = trainingType.label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Tempo restante: ${remainingSeconds}s")
        if (trainingType == TrainingType.Walk) {
            Text(text = "Passos: $steps")
            if (activeStepSensor == null) {
                Text(text = "Sensor de passos indisponivel.")
            } else if (!activityPermissionGranted) {
                Text(text = "Permissao de atividade necessaria para contar passos.")
            }
        } else {
            Text(text = "Vibracao guia aos 20s e 40s.")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { finishTraining() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A3D))
        ) {
            Icon(Icons.Filled.StopCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Encerrar")
        }
    }
}

@Composable
fun ResultScreen(
    type: String,
    steps: Int,
    points: Int,
    streak: Int,
    onRestart: () -> Unit
) {
    val trainingType = remember(type) { TrainingType.fromId(type) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF2ED572),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "Treino concluido!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Tipo: ${trainingType.label}")
                Text(text = "Duracao: 60s")
                if (trainingType == TrainingType.Walk) {
                    Text(text = "Passos: $steps")
                }
                Text(text = "Pontos ganhos: $points")
                Text(text = "Sequencia atual: $streak")
            }
        }
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0CB8FF))
        ) {
            Text(text = "Voltar para Home")
        }
    }
}

@Composable
fun InviteQrScreen(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val inviteCode = remember(user?.uid) { user?.uid?.let { generateInviteCode(it) } }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(true) }

    LaunchedEffect(inviteCode) {
        if (inviteCode == null || user == null) return@LaunchedEffect
        isSaving = true
        try {
            qrBitmap = generateQrBitmap(inviteCode, 720)
            MovementRepository.createInvite(inviteCode, user.uid)
        } catch (_: Exception) {
            Toast.makeText(context, "Convite offline. Verifique o Firestore.", Toast.LENGTH_SHORT).show()
        } finally {
            isSaving = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Convite intergeracional",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Compartilhe o QR para conectar")
        if (isSaving) {
            CircularProgressIndicator()
        } else {
            qrBitmap?.let { bitmap ->
                ImageBox(bitmap = bitmap)
            }
            Text(text = inviteCode ?: "--", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14C38E))
        ) {
            Text(text = "Voltar")
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    onClose: () -> Unit,
    onPaired: () -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var cameraPermissionGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.CAMERA))
    }
    var isProcessing by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) {
            Toast.makeText(context, "Permissao de camera negada.", Toast.LENGTH_SHORT).show()
        }
    }

    if (!cameraPermissionGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F7FB))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Camera necessaria para escanear.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "Permitir camera")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClose) {
                Text(text = "Voltar")
            }
        }
        return
    }

    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var hasScanned by remember { mutableStateOf(false) }

    DisposableEffect(cameraPermissionGranted) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val scanner = BarcodeScanning.getClient()
        val handler = Handler(Looper.getMainLooper())
        analyzer.setAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null && !hasScanned && !isProcessing) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val code = barcodes.firstOrNull()?.rawValue?.trim()
                        if (!code.isNullOrBlank()) {
                            hasScanned = true
                            handler.post {
                                if (user == null) {
                                    Toast.makeText(context, "Usuario nao logado.", Toast.LENGTH_SHORT).show()
                                    return@post
                                }
                                if (isProcessing) return@post
                                isProcessing = true
                                scope.launch {
                                    val result = try {
                                        MovementRepository.redeemInvite(code, user.uid)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    if (result == null) {
                                        Toast.makeText(
                                            context,
                                            "Convite invalido ou usado.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        hasScanned = false
                                        isProcessing = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Parceiro conectado!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onPaired()
                                    }
                                }
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analyzer
        )
        onDispose {
            analyzer.clearAnalyzer()
            cameraProvider.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(text = "Aponte a camera para o QR")
        }
        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text(text = "Voltar")
        }
    }
}

@Composable
private fun OptionGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val selectedColor = if (option == selected) Color.White else Color.White.copy(alpha = 0.3f)
                val textColor = if (option == selected) Color(0xFF08BFA5) else Color.White
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(option) },
                    color = selectedColor
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = option, color = textColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientCard(
    title: String,
    subtitle: String,
    colors: List<Color>,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = Color.White.copy(alpha = 0.9f))
        content()
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .padding(16.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.9f))
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ImageBox(bitmap: Bitmap) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
    }
}

private fun startVoiceListening(
    recognizer: SpeechRecognizer?,
    context: Context,
    setListening: (Boolean) -> Unit
) {
    if (recognizer == null) {
        Toast.makeText(
            context,
            "Reconhecimento de voz indisponivel.",
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
    }
    setListening(true)
    try {
        recognizer.startListening(intent)
    } catch (_: SecurityException) {
        setListening(false)
        Toast.makeText(
            context,
            "Permissao de microfone necessaria para iniciar por voz.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun fetchLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onSuccess: (LocationInfo) -> Unit,
    onError: () -> Unit,
    onComplete: () -> Unit
) {
    val tokenSource = CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        tokenSource.token
    ).addOnSuccessListener { location ->
        if (location != null) {
            onSuccess(
                LocationInfo(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                    capturedAt = System.currentTimeMillis()
                )
            )
        } else {
            onError()
        }
    }.addOnFailureListener {
        onError()
    }.addOnCompleteListener {
        onComplete()
    }
}

private suspend fun resolveCityName(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(latitude, longitude, 1)
            val address = results?.firstOrNull()
            address?.locality ?: address?.subAdminArea ?: address?.adminArea
        } catch (_: Exception) {
            null
        }
    }
}

private fun generateInviteCode(uid: String): String {
    val suffix = (System.currentTimeMillis() / 1000 % 100000).toString().padStart(5, '0')
    return "${uid.take(6)}-$suffix"
}

private fun generateQrBitmap(text: String, size: Int): Bitmap {
    val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bmp
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun needsActivityRecognitionPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

private fun needsPostNotificationsPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

private fun hasActivityRecognitionPermission(context: Context): Boolean {
    return if (needsActivityRecognitionPermission()) {
        hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
    } else {
        true
    }
}

private fun vibrateShort(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(150)
    }
}

private fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Sem registros"
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private fun computeStreak(lastWorkoutAt: Long, currentStreak: Int, now: Long): Int {
    if (lastWorkoutAt <= 0L) return 1
    val diffDays = daysBetween(lastWorkoutAt, now)
    return when {
        diffDays == 0L -> currentStreak
        diffDays == 1L -> currentStreak + 1
        else -> 1
    }
}

private fun daysBetween(start: Long, end: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = start
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startDay = cal.timeInMillis
    cal.timeInMillis = end
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val endDay = cal.timeInMillis
    return ((endDay - startDay) / (24L * 60L * 60L * 1000L))
}
