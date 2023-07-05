package org.torproject.vpn.ui.appdetail.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.torproject.onionmasq.circuit.Circuit
import org.torproject.vpn.R
import org.torproject.vpn.databinding.ViewCircuitsPerAppBinding

class CircuitCardAdapter(val isBrowser: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {
    var items: List<Circuit> = ArrayList()
    var expandedItemPos = -1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
       return CircuitCardViewHolder(ViewCircuitsPerAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CircuitCardViewHolder).bind(items[position], position)
    }

    fun update(list: List<Circuit>) {
        val mutableList = list.toMutableList()
        if (items != mutableList) {
            if (expandedItemPos != -1 && (mutableList.size <= expandedItemPos || mutableList[expandedItemPos] != items[expandedItemPos])) {
                expandedItemPos = -1
            }
            items = mutableList
            notifyDataSetChanged()
        }
    }

    inner class CircuitCardViewHolder(val binding: ViewCircuitsPerAppBinding) : ViewHolder(binding.root) {

        fun bind(item: Circuit, position: Int) {
            binding.root.setOnClickListener {
                if (expandedItemPos == position) {
                    expandedItemPos = -1
                    notifyItemChanged(position)
                } else if (expandedItemPos == -1) {
                    expandedItemPos = position
                    notifyItemChanged(position)
                } else {
                    val previousExpandedItemPos = expandedItemPos
                    expandedItemPos = position
                    notifyItemChanged(previousExpandedItemPos)
                    notifyItemChanged(position)
                }
            }

            val context = binding.root.context
            if (expandedItemPos == position) {
                binding.expandedContainer.tvAppExit.text = context.getString(if (isBrowser) R.string.this_browser else R.string.this_app)
                binding.expandedContainer.tvEntryNode.text = "Hong Kong"
                binding.expandedContainer.tvRelayNode.text = "Spain"
                binding.expandedContainer.tvExitNode.text = "Poland"
                binding.expandedContainer.tvCircuitDescription.text  = context.getString(R.string.circuit_app_description, item.destinationDomain)
                binding.expandedContainer.ivCountryFlagEntryNode.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.flag_hk))
                binding.expandedContainer.ivCountryFlagRelayNode.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.flag_es))
                binding.expandedContainer.ivCountryFlagExitNode.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.flag_pl))
            } else {
                binding.collapsedContainer.tvAddress.text = item.destinationDomain
                binding.collapsedContainer.tvRoutingDescription.text = context.getString(R.string.routed_over_country, "Poland")
                binding.collapsedContainer.ivCountryFlag.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.flag_pl))
            }
            binding.collapsedContainer.root.visibility = if (expandedItemPos == position) View.GONE else View.VISIBLE
            binding.expandedContainer.root.visibility = if (expandedItemPos == position) View.VISIBLE else View.GONE
        }
    }
}