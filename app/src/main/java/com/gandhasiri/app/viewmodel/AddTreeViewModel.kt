package com.gandhasiri.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandhasiri.app.data.GandhaSiriDatabase
import com.gandhasiri.app.data.entities.Tree
import com.gandhasiri.app.data.repository.TreeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTreeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GandhaSiriDatabase.getDatabase(application)
    private val repository = TreeRepository(db.treeDao(), db.treeMeasurementDao())

    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath: StateFlow<String?> = _photoPath.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    fun setPhotoPath(path: String) { _photoPath.value = path }

    fun setLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
    }

    fun saveTree(girthCm: Double, ageYears: Int, notes: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val tree = Tree(
                    photoPath = _photoPath.value ?: "",
                    latitude = _latitude.value,
                    longitude = _longitude.value,
                    girthCm = girthCm,
                    ageYears = ageYears,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now
                )
                val id = repository.createTreeWithInitialMeasurement(tree) ?: return@launch
                withContext(Dispatchers.Main) { onSuccess(id) }
            } catch (e: Exception) {
                Log.e("GandhaSiri", "Failed to save tree", e)
            }
        }
    }
}
