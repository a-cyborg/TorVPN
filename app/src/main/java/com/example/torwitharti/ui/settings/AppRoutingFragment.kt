package com.example.torwitharti.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.torwitharti.databinding.FragmentAppRoutingBinding

class AppRoutingFragment : Fragment() {

    companion object {
        fun newInstance() = AppRoutingFragment()
    }

    private val TAG = AppRoutingFragment.javaClass.simpleName
    private lateinit var viewModel: AppRoutingViewModel
    private lateinit var torAppsAdapter: TorAppsAdapter
    private var _binding: FragmentAppRoutingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[AppRoutingViewModel::class.java]
        _binding = FragmentAppRoutingBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        torAppsAdapter = TorAppsAdapter(viewModel.getAppList())
        binding.rvTorApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTorApps.adapter = torAppsAdapter
        viewModel.isLoading().observe(viewLifecycleOwner) { isLoading: Boolean ->
            Log.d(TAG, "isLoading: $isLoading")
        }
        viewModel.getObservableAppList().observe(viewLifecycleOwner, torAppsAdapter::update)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}