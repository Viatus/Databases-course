package com.example.streaming_service_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.streaming_service_app.utils.MachineUsage
import com.example.streaming_service_app.activities.ManageActivity
import com.example.streaming_service_app.R
import com.example.streaming_service_app.utils.Utility
import kotlinx.android.synthetic.main.sessions_list_item.view.*
import java.lang.StringBuilder

class SessionsViewAdapter: RecyclerView.Adapter<SessionsViewAdapter.ViewHolder>() {
    var data: List<MachineUsage> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.gameTitle.text = item.gameTitle
        holder.gameAvatar.setImageBitmap(
            Utility().generateCircleBitmap(
                ManageActivity.manageAct,
                50.0f,
                item.gameTitle?.last().toString()
            )
        )

        holder.inUseFrom.text = StringBuilder(item.inUseFrom?.take(10)?:"").append(" ${item.inUseFrom?.subSequence(11,18)?:""}").toString()
        holder.inUseTo.text = StringBuilder(item.inUseTo?.take(10)?:"").append(" ${item.inUseTo?.subSequence(11,18)?:""}").toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.sessions_list_item, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder constructor(view: View): RecyclerView.ViewHolder(view) {
        val gameAvatar: ImageView = view.gameAvatar
        val gameTitle: TextView = view.gameTitle
        val inUseFrom: TextView = view.inUseFrom
        val inUseTo: TextView = view.inUseTo
    }
}