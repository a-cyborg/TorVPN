package org.torproject.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import org.torproject.vpn.vpn.TorVpnService.Companion.ACTION_START_VPN
import org.torproject.vpn.vpn.TorVpnService.Companion.ACTION_STOP_VPN

object VpnServiceCommand {

    fun prepareVpn(context: Context?): Intent? {
        try {
            return VpnService.prepare(context?.applicationContext) // stops the VPN connection created by another application.
        } catch (npe: NullPointerException) {
            VpnStatusObservable.update(ConnectionState.CONNECTION_ERROR)
        } catch (ise: IllegalStateException) {
            VpnStatusObservable.update(ConnectionState.CONNECTION_ERROR)
        }
        return null
    }

    fun startVpn(context: Context?) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        val intent = Intent(appContext, TorVpnService::class.java)
        intent.action = ACTION_START_VPN
        startServiceIntent(appContext, intent)
    }


    fun stopVpn(context: Context?) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        val intent = Intent(appContext, TorVpnService::class.java)
        intent.action = ACTION_STOP_VPN
        startServiceIntent(appContext, intent)
    }

    private fun startServiceIntent(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}