package org.torproject.vpn.ui.appdetail.model

import android.app.Application
import android.text.format.Formatter
import android.widget.CompoundButton
import androidx.lifecycle.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.torproject.onionmasq.OnionMasq
import org.torproject.vpn.R
import org.torproject.vpn.utils.PreferenceHelper
import org.torproject.vpn.utils.toFlagEmoji
import org.torproject.vpn.utils.updateDataUsage
import org.torproject.vpn.vpn.DataUsage
import java.util.*

class AppDetailFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val appId = MutableLiveData("")
    val appName = MutableLiveData("")
    val isBrowser = MutableLiveData(false)


    val circuitDescription: StateFlow<String> = appName.asFlow().map { appName ->
        return@map application.getString(R.string.circuits_app_description, appName)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val circuitExitApp: StateFlow<String> = isBrowser.asFlow().map { isBrowser ->
        if (isBrowser) {
            return@map application.getString(R.string.this_browser)
        }
        return@map application.getString(R.string.this_app)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val circuitExitNode: StateFlow<String> = appId.asFlow().map { appId ->
        // TODO: request node data based on appId
        // this is a dummy implementation for now
        return@map "es".toFlagEmoji() + "\t" + Locale("", "es").displayCountry
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val circuitRelayNode: StateFlow<String> = appId.asFlow().map { appId ->
        // TODO: request node data based on appId
        // this is a dummy implementation for now
        return@map "de".toFlagEmoji() + "\t" + Locale("", "de").displayCountry
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )
    val circuitEntryNode: StateFlow<String> = appId.asFlow().map { appId ->
        // TODO: request node data based on appId
        // this is a dummy implementation for now
        return@map "pl".toFlagEmoji() + "\t" +Locale("", "pl").displayCountry
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    private val _dataUsage: MutableLiveData<DataUsage> = MutableLiveData(DataUsage())
    val dataUsage: LiveData<DataUsage> = _dataUsage

    val dataUsageDownstream: StateFlow<String> = dataUsage.asFlow().map { data ->
        return@map Formatter.formatFileSize(application, data.downstreamData)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Formatter.formatFileSize(application, 0)
    )

    val dataUsageUpstream: StateFlow<String> = dataUsage.asFlow().map { data ->
        return@map Formatter.formatFileSize(application, data.upstreamData)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Formatter.formatFileSize(application, 0)
    )

    private val preferenceHelper = PreferenceHelper(getApplication())

    val protectThisApp: Boolean get() {
        val apps = preferenceHelper.protectedApps?.toSet() ?: emptySet()
        return apps.contains(appId.value)
    }
    fun onProtectThisAppChanged(compoundButton: CompoundButton, isChecked: Boolean) {
        val protectedApps = preferenceHelper.protectedApps?.toMutableSet() ?: emptySet<String>().toMutableSet()
        if (isChecked) {
            protectedApps.add(appId.value)
        } else {
            protectedApps.remove(appId.value)
        }
        preferenceHelper.protectedApps = protectedApps
    }

    private val timer: Timer by lazy {
        Timer()
    }

    init {
        viewModelScope.launch {
            timer.schedule(object: TimerTask() {
                override fun run() {
                    // TODO: replace Onionmasq.getBytesReceived and ~.getBytesSent() with actual per-app data usage API once it was implemented in onionmasq
                    _dataUsage.postValue(updateDataUsage(dataUsage, OnionMasq.getBytesReceived(), OnionMasq.getBytesSent()))
                }
            }, 0, 1000)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }

}