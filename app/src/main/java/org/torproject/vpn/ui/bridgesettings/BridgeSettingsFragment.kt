package org.torproject.vpn.ui.bridgesettings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import org.torproject.vpn.R
import org.torproject.vpn.databinding.FragmentBridgesettingsBinding
import org.torproject.vpn.ui.base.view.BaseDialogFragment
import org.torproject.vpn.ui.bridgesettings.model.BridgeSettingsFragmentViewModel

class BridgeSettingsFragment: Fragment(R.layout.fragment_bridgesettings), ClickHandler {

    private lateinit var viewModel: BridgeSettingsFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[BridgeSettingsFragmentViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentBridgesettingsBinding.bind(view)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onManualBridgeSelectionClicked(v: View) {
        val dialog = BridgeDialogFragment.create()
        dialog.show(parentFragmentManager, "BRIDGE_DIALOG")
    }

}