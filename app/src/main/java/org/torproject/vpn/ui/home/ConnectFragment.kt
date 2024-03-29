package org.torproject.vpn.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.torproject.onionmasq.logging.LogObservable
import org.torproject.vpn.MainActivity
import org.torproject.vpn.MainActivity.Companion.KEY_ACTION
import org.torproject.vpn.R
import org.torproject.vpn.databinding.FragmentConnectBinding
import org.torproject.vpn.ui.exitselection.ExitSelectionBottomSheetFragment
import org.torproject.vpn.ui.home.model.ACTION_APPS
import org.torproject.vpn.ui.home.model.ACTION_CONNECTION
import org.torproject.vpn.ui.home.model.ACTION_EXIT_NODE_SELECTION
import org.torproject.vpn.ui.home.model.ACTION_LOGS
import org.torproject.vpn.ui.home.model.ACTION_REQUEST_NOTIFICATION_PERMISSION
import org.torproject.vpn.ui.home.model.ConnectFragmentViewModel
import org.torproject.vpn.utils.PreferenceHelper
import org.torproject.vpn.utils.startVectorAnimationWithEndCallback
import org.torproject.vpn.vpn.ConnectionState
import org.torproject.vpn.vpn.VpnServiceCommand
import org.torproject.vpn.vpn.VpnStatusObservable
import java.util.concurrent.TimeUnit
import org.torproject.vpn.utils.getDpInPx
import org.torproject.vpn.utils.navigateSafe


class ConnectFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val TAG: String = ConnectFragment::class.java.simpleName
    }

    private var _binding: FragmentConnectBinding? = null
    private val binding get() = _binding!!
    private lateinit var connectFragmentViewModel: ConnectFragmentViewModel

    //this is required to store current state that UI is in so we can decide whether to animate to next state
    private lateinit var currentVpnState: ConnectionState

    private lateinit var preferenceHelper: PreferenceHelper

    private var vpnPermissionDialogStartTime = 0L

    private var initStateFabSpacing: Int = 0
    private var connectingStateFabSpacing: Int = 0
    private var connectedStateFabSpacing: Int = 0
    private var animationDuration: Long = 0

    private var startForResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnServiceCommand.startVpn(context)

            // Fixes button color after permission dialog, Quick fix, need refactoring.
            startVectorAnimationWithEndCallback(
                binding.tvConnectActionBtn.background, viewLifecycleOwner.lifecycle
            ) {
                binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_cancel_to_connect)
            }
        } else {
            //this indicates that the permission request failed almost instantly. One of the reason could be that other VPN has always-on flag started.
            if (System.currentTimeMillis() - vpnPermissionDialogStartTime < 200) {
                LogObservable.getInstance().addLog("VPN permission request failed instantly. Other VPN is likely on ALWAYS-ON mode!")
            }

            VpnStatusObservable.update(ConnectionState.CONNECTION_ERROR)
        }
        connectFragmentViewModel.onVpnPrepared()
    }

    private var startNotificationRequestForResult: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                requireContext(),
                "TODO: SHOW PROPER HINT HOW TO ALLOW AGAIN NOTIFICATION PERMISSION",
                Toast.LENGTH_LONG
            ).show()
        }
        connectFragmentViewModel.onNotificationPermissionResult()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        currentVpnState = ConnectionState.INIT
        preferenceHelper = PreferenceHelper(requireContext())
        preferenceHelper.registerListener(this)
        connectFragmentViewModel = ViewModelProvider(this)[ConnectFragmentViewModel::class.java]
        _binding = FragmentConnectBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = connectFragmentViewModel
        initStateFabSpacing = getDpInPx(requireContext(), 0f)
        connectingStateFabSpacing = getDpInPx(requireContext(), (9f)) //dp padding between connect - connecting
        connectedStateFabSpacing = getDpInPx(requireContext(), (25f)) //dp padding between connect - stop
        animationDuration = resources.getInteger(R.integer.default_transition_anim_duration).toLong()

        connectFragmentViewModel.prepareVpn.observe(
            viewLifecycleOwner,
            Observer<Intent?> { intent ->
                intent?.let {
                    vpnPermissionDialogStartTime = System.currentTimeMillis()
                    startForResult.launch(it)
                }
            })

        connectFragmentViewModel.updateConnectionLabel()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    connectFragmentViewModel.connectionState.collect { vpnState ->
                        setUIState(vpnState)
                    }
                }

                launch {
                    connectFragmentViewModel.action.collect { action ->
                        when (action) {
                            ACTION_LOGS -> {
                                findNavController().navigateSafe(R.id.action_navigation_connect_to_loggingFragment)
                            }
                            ACTION_REQUEST_NOTIFICATION_PERMISSION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    startNotificationRequestForResult.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            ACTION_EXIT_NODE_SELECTION -> {
                                if (isAdded) {
                                    ExitSelectionBottomSheetFragment().show(parentFragmentManager, "exitNodeSelector")
                                }
                            }
                            ACTION_APPS -> findNavController().navigateSafe(R.id.action_navigation_connect_to_appRoutingFragment)
                            ACTION_CONNECTION -> findNavController().navigateSafe(R.id.action_navigation_connect_to_connectionFragment)
                            else -> {
                                //other cases of navigation.
                            }
                        }
                    }
                }
            }
        }

        arguments?.let { bundle ->
            val action = bundle.getString(KEY_ACTION)
            if (MainActivity.ACTION_REQUEST_VPN_PERMISSON == action) {
                connectFragmentViewModel.prepareToStartVPN()
            }
        }


        binding.includeStats.chronometer.setOnChronometerTickListener { chronometer ->
            val elapsedTime = SystemClock.elapsedRealtime() - chronometer.base

            val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedTime))
            val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))

            val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            chronometer.text = timeFormatted
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        preferenceHelper.unregisterListener(this)
    }

    private fun setUIState(vpnState: ConnectionState) {
        Log.d(
            TAG,
            "setUIState: ${if (::currentVpnState.isInitialized) currentVpnState else "not initialized"} --> ${vpnState.name}"
        )
        if (::currentVpnState.isInitialized && currentVpnState == vpnState) {
            return
        }
        binding.gradientView.setState(vpnState)

        when (vpnState) {
            ConnectionState.INIT -> {

            }

            ConnectionState.CONNECTING -> showConnectingTransition()


            ConnectionState.CONNECTED -> {
                binding.includeStats.chronometer.base = VpnStatusObservable.getStartTimeBase()
                binding.includeStats.chronometer.start()
                showConnectedTransition()
            }

            ConnectionState.DISCONNECTED -> {
                binding.includeStats.chronometer.stop()
                showDisconnectedTransition()
            }

            ConnectionState.CONNECTION_ERROR -> {
                binding.includeStats.chronometer.stop()
                showErrorTransition()
            }

            ConnectionState.DISCONNECTING -> {
                // disable btn?
            }
        }
        currentVpnState = vpnState
    }

    /*
    * Scene transitions
    *
    * ************
     */

    private fun showConnectingTransition() {
        if (currentVpnState == ConnectionState.INIT || currentVpnState == ConnectionState.DISCONNECTED || currentVpnState == ConnectionState.CONNECTION_ERROR) {
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_connect_to_cancel)

            binding.clSelectionExitInner.animate().translationX(connectingStateFabSpacing.toFloat()).setDuration(animationDuration)
                .setInterpolator(AccelerateInterpolator()).start()

            //connect button vector anim
            startVectorAnimationWithEndCallback(
                binding.tvConnectActionBtn.background, viewLifecycleOwner.lifecycle
            ) {
                binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_cancel_to_connect)
            }

        } else {
            binding.clSelectionExitInner.translationX = connectingStateFabSpacing.toFloat()
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_cancel_to_connect)
        }
    }

    private fun connectingToIdleTransition() {

    }

    private fun showConnectedTransition() {
        if (currentVpnState == ConnectionState.CONNECTING) {
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_cancel_to_stop)

            binding.clSelectionExitInner.animate().translationX(connectedStateFabSpacing.toFloat()).setDuration(animationDuration)
                .setInterpolator(DecelerateInterpolator()).start()

            //cancel to stop transition
            startVectorAnimationWithEndCallback(
                binding.tvConnectActionBtn.background, viewLifecycleOwner.lifecycle
            ) {
                binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_stop_connect)
            }

        } else {
            binding.clSelectionExitInner.translationX = connectedStateFabSpacing.toFloat()
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_stop_connect)
        }

    }

    private fun showErrorTransition() {
        if (currentVpnState == ConnectionState.CONNECTING) {
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_cancel_to_connect)

            binding.clSelectionExitInner.animate().translationX(initStateFabSpacing.toFloat()).setDuration(animationDuration)
                .setInterpolator(DecelerateInterpolator()).start()

            //cancel to stop transition
            startVectorAnimationWithEndCallback(
                binding.tvConnectActionBtn.background, viewLifecycleOwner.lifecycle
            ) {
                binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_connect_to_cancel)
            }

        } else {
            binding.clSelectionExitInner.translationX = initStateFabSpacing.toFloat()
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_connect_to_cancel)
        }

    }

    private fun showDisconnectedTransition() {
        if (currentVpnState == ConnectionState.DISCONNECTING) {

            binding.clSelectionExitInner.animate().translationX(initStateFabSpacing.toFloat()).setDuration(animationDuration)
                .setInterpolator(DecelerateInterpolator()).start()

            startVectorAnimationWithEndCallback(
                binding.tvConnectActionBtn.background, viewLifecycleOwner.lifecycle
            ) {
                binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_connect_to_cancel)
            }

        } else {
            binding.clSelectionExitInner.translationX = initStateFabSpacing.toFloat()
            binding.tvConnectActionBtn.setBackgroundResource(R.drawable.av_connect_to_cancel)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key?.let {
            when (it) {
                PreferenceHelper.PROTECT_ALL_APPS -> connectFragmentViewModel.updateVPNSettings()
                PreferenceHelper.EXIT_NODE_COUNTRY -> connectFragmentViewModel.updateExitNodeButton()
                PreferenceHelper.AUTOMATIC_EXIT_NODE_SELECTION -> connectFragmentViewModel.updateExitNodeButton()
                PreferenceHelper.BRIDGE_TYPE -> connectFragmentViewModel.updateConnectionLabel()
            }
        }
    }

}

