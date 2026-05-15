package com.gandhasiri.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandhasiri.app.data.GandhaSiriDatabase
import com.gandhasiri.app.data.entities.Tree
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TreesListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GandhaSiriDatabase.getDatabase(application)
    private val treeDao = db.treeDao()

    private val treeMeasurementDao = db.treeMeasurementDao()
    private val alertLogDao = db.alertLogDao()

    val trees: StateFlow<List<Tree>> = treeDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteAllData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            treeDao.deleteAll()
            treeMeasurementDao.deleteAll()
            alertLogDao.deleteAll()
        }
    }
}
