package com.hardycheng.androidtoolshub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardycheng.androidtoolshub.databinding.ItemMarkBinding

class MarkAdapter: RecyclerView.Adapter<MarkAdapter.ViewHolder>() {

    val list: MutableList<StopWatch.Mark> = mutableListOf()

    fun reset(){
        list.clear()
        notifyDataSetChanged()
    }

    fun addMark(mark: StopWatch.Mark){
        list.add(mark)
        list.sortBy { it.id }
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemMarkBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMarkBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mark = list[position]
        holder.binding.id.text = String.format("mark #%02d",mark.id+1)
        holder.binding.interval.text = FormatUtil.toTimerString(mark.interval)
        holder.binding.total.text = FormatUtil.toTimerString(mark.total)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}