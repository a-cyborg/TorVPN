package com.example.torwitharti.ui.approuting

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.torwitharti.R
import com.example.torwitharti.databinding.FragmentAppRoutingBinding
import com.example.torwitharti.ui.approuting.data.AppListAdapter
import com.example.torwitharti.ui.approuting.data.TorAppsAdapter
import com.example.torwitharti.ui.approuting.model.AppRoutingViewModel
import com.example.torwitharti.utils.PreferenceHelper
import com.example.torwitharti.utils.PreferenceHelper.Companion.PROTECT_ALL_APPS

class AppRoutingFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = AppRoutingFragment()
    }

    private val TAG = AppRoutingFragment::class.java.simpleName
    private lateinit var viewModel: AppRoutingViewModel
    private lateinit var appListAdapter: AppListAdapter
    private var _binding: FragmentAppRoutingBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        preferenceHelper = PreferenceHelper(requireContext())
        viewModel = ViewModelProvider(this)[AppRoutingViewModel::class.java]
        _binding = FragmentAppRoutingBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        preferenceHelper.registerListener(this)

        // setup vertical list
        appListAdapter = AppListAdapter(viewModel.getAppList(),
            TorAppsAdapter(viewModel.getAppList()),
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false),
            preferenceHelper)
        appListAdapter.onItemModelChanged = viewModel::onItemModelChanged
        binding.rvAppList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvAppList.adapter = appListAdapter
        viewModel.getObservableAppList().observe(viewLifecycleOwner, appListAdapter::update)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.refresh_circuits -> {
                    Toast.makeText(requireContext(), "Oops. Refreshing circuits is not yet implemented!", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        appListAdapter.onItemModelChanged = null
        preferenceHelper.unregisterListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key?.equals(PROTECT_ALL_APPS) == true) {
            viewModel.onProtectedAppsPrefsChanged(preferenceHelper.protectAllApps)
        }
    }

}