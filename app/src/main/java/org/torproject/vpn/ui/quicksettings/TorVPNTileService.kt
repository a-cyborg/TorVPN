package org.torproject.vpn.ui.quicksettings


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.lifecycle.Observer
import org.torproject.vpn.MainActivity
import org.torproject.vpn.MainActivity.Companion.ACTION_REQUEST_VPN_PERMISSON
import org.torproject.vpn.R
import org.torproject.vpn.vpn.ConnectionState
import org.torproject.vpn.vpn.ConnectionState.CONNECTED
import org.torproject.vpn.vpn.ConnectionState.CONNECTING
import org.torproject.vpn.vpn.ConnectionState.CONNECTION_ERROR
import org.torproject.vpn.vpn.ConnectionState.DISCONNECTED
import org.torproject.vpn.vpn.ConnectionState.DISCONNECTING
import org.torproject.vpn.vpn.ConnectionState.INIT
import org.torproject.vpn.vpn.VpnServiceCommand
import org.torproject.vpn.vpn.VpnStatusObservable


class TorVPNTileService : TileService() {

    private var vpnStatusObserver: Observer<ConnectionState>? = null
    private val mainHandler: Handler by lazy {
        Handler(mainLooper)
    }

    @SuppressLint("Override")
    override fun onClick() {
        super.onClick()
        if (!isLocked) {
            onTileTap()
        } else {
            unlockAndRun { onTileTap() }
        }
    }

    private fun onTileTap() {
        when (VpnStatusObservable.statusLiveData.value) {
            CONNECTING, CONNECTED -> {
                VpnServiceCommand.stopVpn(this)
            }
            CONNECTION_ERROR -> {
               startMainActivity(ACTION_MAIN)
            }
            else -> {
                if (VpnServiceCommand.prepareVpn(this) == null) {
                    VpnServiceCommand.startVpn(this)
                    VpnStatusObservable.update(CONNECTING)
                } else {
                    startMainActivity(ACTION_REQUEST_VPN_PERMISSON)
                }
            }
        }
    }

    private fun startMainActivity(action: String) {
        // retry to prepare and start the VPN but with some UI to show possible errors
        val startIntent = Intent(this.applicationContext, MainActivity::class.java)
        startIntent.action = action
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.startActivityAndCollapse(startIntent)
        } else {
            val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(this, 0, startIntent, flags)
            this.startActivityAndCollapse(pendingIntent)
        }
    }

    override fun onTileAdded() {
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TILE_TOR_VPN", "On Create")
        vpnStatusObserver = Observer { connectionState: ConnectionState? ->
            onStatusChanged(connectionState)
        }
    }

    private fun onStatusChanged(connectionState: ConnectionState?) {
        val t = qsTile ?: return
        Log.d("TILE_TOR_VPN", "CONNECTION STATE CHANGED: ${connectionState?.name}")
        when (connectionState) {
            INIT,
            DISCONNECTED -> {
                t.state = Tile.STATE_INACTIVE
                t.label = getString(R.string.app_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    t.subtitle = getString(R.string.state_disconnected)
                }
            }
            CONNECTING -> {
                t.state =  Tile.STATE_ACTIVE
                t.label = getString(R.string.app_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    t.subtitle = getString(R.string.state_connecting)
                }
            }
            DISCONNECTING,
            CONNECTED -> {
                t.state = Tile.STATE_ACTIVE
                t.label = getString(R.string.app_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    t.subtitle = getString(R.string.state_connected)
                }
            }
            CONNECTION_ERROR -> {
                t.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    t.label = getString(R.string.app_name)
                    t.subtitle = getString(R.string.qs_error)
                } else {
                    t.label = getString(R.string.qs_show_error)
                }
            }
            else -> {
                t.state = Tile.STATE_UNAVAILABLE
                t.label = getString(R.string.app_name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    t.subtitle = ""
                }
            }
        }
        t.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground_qs)
        t.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d("TILE_TOR_VPN", "onStartListening")
        vpnStatusObserver?.let {
            mainHandler.post {
                Log.d("TILE_TOR_VPN", "onStartListening - observe forever")
                VpnStatusObservable.statusLiveData.observeForever(it)
            }
        }
        onStatusChanged(VpnStatusObservable.statusLiveData.value)
    }

    override fun onStopListening() {
        vpnStatusObserver?.let {
            Log.d("TILE_TOR_VPN", "onStopListening")
            mainHandler.post {
                Log.d("TILE_TOR_VPN", "onStopListening - remove observer")
                VpnStatusObservable.statusLiveData.removeObserver(it)
            }
        }
        super.onStopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TILE_TOR_VPN", "onDestroy")
        vpnStatusObserver = null
    }
}
