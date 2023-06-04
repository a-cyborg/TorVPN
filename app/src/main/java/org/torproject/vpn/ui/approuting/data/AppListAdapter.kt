package org.torproject.vpn.ui.approuting.data

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.torproject.vpn.databinding.AppRoutingTableHeaderBinding
import org.torproject.vpn.databinding.AppSwitchItemViewBinding
import org.torproject.vpn.databinding.AppTitleViewBinding
import org.torproject.vpn.databinding.HorizontalRecyclerViewItemBinding
import org.torproject.vpn.ui.approuting.model.AppItemModel
import org.torproject.vpn.ui.glide.ApplicationInfoModel
import org.torproject.vpn.utils.PreferenceHelper

class AppListAdapter(list: List<AppItemModel>,
                     var torAppsAdapter: TorAppsAdapter,
                     var preferenceHelper: PreferenceHelper
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val SECTION_HEADER_VIEW = 0
        const val CELL = 1
        const val HORIZONTAL_RECYCLER_VIEW = 2
        const val TABLE_HEADER_VIEW = 3

        val TAG = AppListAdapter::class.java.simpleName
    }

    var items: MutableList<AppItemModel> = list.toMutableList()
    var onItemModelChanged: ((pos: Int, item: AppItemModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewHolder: RecyclerView.ViewHolder = when (viewType) {
            CELL -> AppListItemViewHolder(AppSwitchItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            HORIZONTAL_RECYCLER_VIEW -> HorizontalRecyclerViewItemViewHolder(HorizontalRecyclerViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TABLE_HEADER_VIEW -> TableHeaderViewHolder(AppRoutingTableHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> AppListTitleViewHolder(AppTitleViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        return viewHolder
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            when(items[position].viewType) {
                CELL -> {
                    val protectAllApps = payloads.first() as Boolean
                    (holder as AppListItemViewHolder).setEnabled(!protectAllApps)
                }
                else -> {}
            }
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(items[position].viewType) {
            SECTION_HEADER_VIEW -> (holder as AppListTitleViewHolder).bind(items[position])
            CELL -> (holder as AppListItemViewHolder).bind(items[position], position)
            TABLE_HEADER_VIEW -> (holder as TableHeaderViewHolder).bind(preferenceHelper)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    fun update(list: List<AppItemModel>) {
        var mutableList = list.toMutableList()
        mutableList.add(0, AppItemModel(TABLE_HEADER_VIEW))
        // The following comparison depends on AppItemModels equals() implementation
        // Hence, dataSetChanged remains false if protectAllApps of AppItemModel changed
        // b/c protectAllApps is excluded from AppItemModel's equals() method
        val dataSetChanged = mutableList != items
        if (!dataSetChanged) {
            // check separately if protectAllApps changed and update cells without redrawing them
            for (i in 0..mutableList.size-1) {
                if (items[i].protectAllApps != mutableList[i].protectAllApps) {
                    notifyItemChanged(i, mutableList[i].protectAllApps)
                }
            }
        }
        items = ArrayList(mutableList.map { it.copy() })
        if (dataSetChanged) {
            notifyDataSetChanged()
        }
    }

    internal class AppListTitleViewHolder(val binding: AppTitleViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(appItem: AppItemModel) {
            binding.tvTitle.text = appItem.text
        }
    }

    inner class AppListItemViewHolder(val binding: AppSwitchItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val TAG = "AppListItemViewHolder";
        private var pos = -1

        fun bind(appItem: AppItemModel, pos: Int) {
            this.pos = pos
            appItem.appId?.also {
                Log.d("--->", "load icn for ${appItem.appId}")
                Glide.with(binding.root.context)
                    .load(ApplicationInfoModel(appItem.appId))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .let { requestBuilder ->
                        binding.ivAppImage.drawable?.let {
                            requestBuilder.placeholder(it)
                        } ?: run {
                            requestBuilder
                        }
                    }
                    .into(binding.ivAppImage)
            } ?: run {
                binding.ivAppImage.setImageDrawable(null)
            }
            binding.tvTitle.text = appItem.text
            binding.smItemSwitch.isChecked = appItem.isRoutingEnabled == true
            binding.tvTitle.setOnClickListener(View.OnClickListener {
                Log.d(TAG, "TODO: open detail screen for " + binding.tvTitle.text)
            })
            binding.smItemSwitch.setOnCheckedChangeListener { switchBtn, isChecked ->
                if (switchBtn.isPressed) {
                    val itemModel = items[pos]
                    itemModel.isRoutingEnabled = isChecked
                    // pos - 1: the first item is the header view, which is manually added only here in AppListAdapter
                    onItemModelChanged?.invoke(pos - 1, itemModel)
                }
            }
            binding.smItemSwitch.isEnabled = appItem.protectAllApps == false
        }

        fun setEnabled(enabled: Boolean) {
            Log.d(TAG, "set enabled $enabled called for ${binding.tvTitle.text}")
            binding.smItemSwitch.isEnabled = enabled
        }
    }

    internal class HorizontalRecyclerViewItemViewHolder(val binding: HorizontalRecyclerViewItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
        fun bind(appItem: AppItemModel, adapter: TorAppsAdapter, layoutManager: LinearLayoutManager) {
            Log.d("--", "bind horizontal layout")
            binding.rvTorApps.adapter = adapter
            appItem.appList?.also {
                Log.d("--", "updateAppList $it")
                adapter.update(it)
            }
        }
    }

    internal class TableHeaderViewHolder(val binding: AppRoutingTableHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(preferenceHelper: PreferenceHelper) {
            binding.smProtectAllApps.isChecked = preferenceHelper.protectAllApps
            binding.smProtectAllApps.setOnCheckedChangeListener { switchBtn, isChecked ->
                if (switchBtn.isPressed) {
                    preferenceHelper.protectAllApps = isChecked
                }
            }
        }
    }
}