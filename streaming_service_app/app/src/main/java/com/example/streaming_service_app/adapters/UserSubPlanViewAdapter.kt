package com.example.streaming_service_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.streaming_service_app.R
import com.example.streaming_service_app.utils.UserSubscriptionPlan
import kotlinx.android.synthetic.main.user_subscription_list_item.view.*

class UserSubPlanViewAdapter: RecyclerView.Adapter<UserSubPlanViewAdapter.ViewHolder>() {
    var data: List<UserSubscriptionPlan> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.planName.text = item.name
        holder.activeFrom.text = item.activeFrom?.take(10)
        holder.activeTo.text = item.activeTo?.take(10)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.user_subscription_list_item, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder constructor(view: View): RecyclerView.ViewHolder(view) {
        val planName: TextView = view.planName
        val activeFrom:TextView = view.activeFrom
        val activeTo: TextView = view.activeTo
    }
}