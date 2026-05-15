package com.gandhasiri.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gandhasiri.app.data.GandhaSiriDatabase
import com.gandhasiri.app.data.entities.Tree
import com.gandhasiri.app.data.entities.TreeMeasurement
import com.gandhasiri.app.data.repository.TreeRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ProjectedGirthPoint(val year: Int, val predictedGirth: Double)

private val MODELS = listOf(
    "gemini-2.5-flash" to "v1beta",
    "gemini-1.5-flash" to "v1beta"
)

class TreeDetailViewModel(application: Application, private val treeId: String) : AndroidViewModel(application) {
    private val db = GandhaSiriDatabase.getDatabase(application)
    private val treeDao = db.treeDao()
    private val measurementDao = db.treeMeasurementDao()
    private val alertLogDao = db.alertLogDao()
    private val repository = TreeRepository(treeDao, measurementDao)

    val tree: StateFlow<Tree?> = treeDao.getTreeById(treeId)
        .catch { e -> Log.e("GandhaSiri", "tree flow error", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val measurements: StateFlow<List<TreeMeasurement>> = treeDao.getMeasurementsForTree(treeId)
        .catch { e -> Log.e("GandhaSiri", "measurements flow error", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isLoadingAI = MutableStateFlow(false)
    val isLoadingAI: StateFlow<Boolean> = _isLoadingAI

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError

    private val _projectedGrowth = MutableStateFlow<List<ProjectedGirthPoint>>(emptyList())
    val projectedGrowth: StateFlow<List<ProjectedGirthPoint>> = _projectedGrowth.asStateFlow()

    private val _isLoadingProjection = MutableStateFlow(false)
    val isLoadingProjection: StateFlow<Boolean> = _isLoadingProjection.asStateFlow()

    private val _projectionError = MutableStateFlow<String?>(null)
    val projectionError: StateFlow<String?> = _projectionError.asStateFlow()

    private val _panicLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val panicLocation: StateFlow<Pair<Double, Double>?> = _panicLocation

    init {
        measurements.filter { it.isNotEmpty() }
            .take(1)
            .onEach { list ->
                tree.value?.let { t ->
                    val plantingYear = Calendar.getInstance().apply { timeInMillis = t.createdAt }.get(Calendar.YEAR)
                    fetchGrowthProjection(t.girthCm, t.ageYears, plantingYear, list)
                }
            }
            .launchIn(viewModelScope)
    }

    fun fetchGrowthProjection(
        currentGirthCm: Double,
        ageYears: Int,
        plantingYear: Int,
        measurementHistory: List<TreeMeasurement>,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingProjection.value = true
            _projectionError.value = null

            if (!forceRefresh) {
                val cached = tree.value?.projectedGrowthJson
                if (!cached.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<List<ProjectedGirthPoint>>() {}.type
                        _projectedGrowth.value = Gson().fromJson(cached, type)
                        _isLoadingProjection.value = false
                        return@launch
                    } catch (e: Exception) {
                        Log.e("GandhaSiri", "Failed to parse cached projection", e)
                    }
                }
            }

            val historyStr = measurementHistory.joinToString(", ") { m ->
                val year = Calendar.getInstance().apply { timeInMillis = m.measuredAt }.get(Calendar.YEAR)
                "$year:${m.girthCm}"
            }
            val remainingYears = 20 - ageYears
            val prompt = """
                You are a sandalwood growth expert. A sandalwood tree in Karnataka is currently $ageYears years old with a current girth of $currentGirthCm cm. The tree was planted in $plantingYear. Here are its recorded girth measurements over the years: [$historyStr]. Based on this growth history, predict the girth of this tree for each of the next $remainingYears years until it reaches 60 cm or until year 20 from planting, whichever comes first. Return your answer as a JSON array only, no explanation, no extra text, just the raw JSON array in this exact format: [{"year": 2025, "predictedGirth": 32.5}, {"year": 2026, "predictedGirth": 35.1}] and so on for each future year.
            """.trimIndent()

            val apiKey = com.gandhasiri.app.BuildConfig.GEMINI_API_KEY
            var success = false

            for ((modelName, apiVersion) in MODELS) {
                if (success) break
                var attempt = 0
                while (attempt < 3 && !success) {
                    try {
                        attempt++
                        Log.d("GandhaSiri", "Growth projection: $modelName attempt $attempt")
                        val model = GenerativeModel(modelName, apiKey, requestOptions = RequestOptions(apiVersion = apiVersion))
                        val rawResponse = model.generateContent(prompt).text ?: ""
                        val cleanJson = rawResponse.trim()
                            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        val type = object : TypeToken<List<ProjectedGirthPoint>>() {}.type
                        _projectedGrowth.value = Gson().fromJson(cleanJson, type)
                        repository.saveProjectedGrowth(treeId, cleanJson)
                        success = true
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        Log.e("GandhaSiri", "Projection $modelName attempt $attempt failed: $msg")
                        if (attempt < 3 && (msg.contains("503") || msg.contains("UNAVAILABLE"))) {
                            delay(2000)
                        } else break
                    }
                }
            }

            if (!success) _projectionError.value = "AI services are busy. Please try again later."
            _isLoadingProjection.value = false
        }
    }

    fun getAIEstimate(age: Int, girth: Double) {
        val currentTree = tree.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingAI.value = true
            _aiError.value = null

            val apiKey = com.gandhasiri.app.BuildConfig.GEMINI_API_KEY
            val prompt = "A sandalwood tree in Karnataka is $age years old and has a " +
                "current girth of $girth cm. Estimate how many more years are needed before " +
                "the heartwood is ready for legal harvest. Also give 2 practical tips to " +
                "improve heartwood formation. Keep the answer under 80 words."

            var lastError = "AI services are currently busy. Please try again later."
            var quotaHit = false

            for ((modelName, apiVersion) in MODELS) {
                if (quotaHit) break
                try {
                    Log.d("GandhaSiri", "AI estimate: trying $modelName")
                    val model = GenerativeModel(modelName, apiKey, requestOptions = RequestOptions(apiVersion = apiVersion))
                    val aiText = model.generateContent(prompt).text
                    if (aiText != null) {
                        treeDao.update(currentTree.copy(aiEstimate = aiText, aiTimestamp = System.currentTimeMillis()))
                        _aiError.value = null
                        _isLoadingAI.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: "unknown"
                    Log.e("GandhaSiri", "AI estimate $modelName failed: $msg")
                    lastError = when {
                        msg.contains("404") || msg.contains("not found", ignoreCase = true) ->
                            "AI model unavailable. Please try again later."
                        msg.contains("quota", ignoreCase = true) || msg.contains("429") -> {
                            quotaHit = true
                            "Quota exceeded. Please check settings."
                        }
                        else -> "AI Error: $msg"
                    }
                }
            }

            _aiError.value = lastError
            _isLoadingAI.value = false
        }
    }

    fun saveNewMeasurement(newGirth: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                repository.addMeasurement(treeId, newGirth)
            } catch (e: Exception) {
                Log.e("GandhaSiri", "saveNewMeasurement failed", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateAge(newAge: Int) {
        val t = tree.value ?: return
        viewModelScope.launch(Dispatchers.IO) { treeDao.update(t.copy(ageYears = newAge)) }
    }

    fun updatePhoto(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try { repository.updateTreePhoto(treeId, uri) }
            catch (e: Exception) { Log.e("GandhaSiri", "updatePhoto failed", e) }
        }
    }

    fun deleteTree(onDeleted: () -> Unit) {
        val t = tree.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            treeDao.delete(t)
            withContext(Dispatchers.Main) { onDeleted() }
        }
    }

    fun insertAlert(note: String, type: String = "Suspicious Activity") {
        viewModelScope.launch(Dispatchers.IO) {
            alertLogDao.insert(
                com.gandhasiri.app.data.entities.AlertLog(
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    note = note
                )
            )
        }
    }

    fun prefetchPanicLocation(context: android.content.Context) {
        val hasFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        viewModelScope.launch {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
                try {
                    if (lm.isProviderEnabled(provider)) {
                        lm.getLastKnownLocation(provider)?.let {
                            _panicLocation.value = it.latitude to it.longitude
                            return@launch
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w("GandhaSiri", "Location denied: $provider")
                }
            }

            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> return@launch
            }

            val loc = withTimeoutOrNull(15_000L) {
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine<android.location.Location?> { cont ->
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                @Suppress("OVERRIDE_DEPRECATION")
                                lm.getCurrentLocation(provider, null, context.mainExecutor) { cont.resume(it) }
                            } else {
                                @Suppress("DEPRECATION")
                                lm.requestSingleUpdate(provider, object : android.location.LocationListener {
                                    override fun onLocationChanged(l: android.location.Location) { cont.resume(l) }
                                    override fun onProviderDisabled(p: String) { cont.resume(null) }
                                    @Suppress("DEPRECATION")
                                    override fun onStatusChanged(p: String, s: Int, e: android.os.Bundle?) {}
                                }, android.os.Looper.getMainLooper())
                            }
                        } catch (e: SecurityException) {
                            Log.e("GandhaSiri", "Location SecurityException", e)
                            cont.resume(null)
                        }
                    }
                }
            }

            loc?.let { _panicLocation.value = it.latitude to it.longitude }
                ?: Log.w("GandhaSiri", "Could not obtain location")
        }
    }
}

class TreeDetailViewModelFactory(
    private val application: Application,
    private val treeId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreeDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TreeDetailViewModel(application, treeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
