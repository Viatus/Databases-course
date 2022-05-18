package com.example.streaming_service_app.adapters

import android.app.AlertDialog
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.streaming_service_app.*
import com.example.streaming_service_app.activities.ManageActivity
import com.example.streaming_service_app.fragments.SubscriptionsFragment
import com.example.streaming_service_app.utils.StreamingService
import com.example.streaming_service_app.utils.SubscriptionPlan
import kotlinx.android.synthetic.main.dialog_add_subscription.view.*
import kotlinx.android.synthetic.main.subscription_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SubscriptionPlansViewAdapter :
    RecyclerView.Adapter<SubscriptionPlansViewAdapter.ViewHolder>() {
    var data: List<SubscriptionPlan> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.planName.text = item.name
        holder.planPrice.text = item.price.toString()
        holder.itemView.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            val view = ManageActivity.manageAct.layoutInflater.inflate(
                R.layout.dialog_add_subscription,
                null
            )
            view.durationSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    view.durationNumber.text = progress.toString()
                }
            })
            builder.setMessage("Choose duration of ${item.name}")
                .setView(
                    view
                )
                .setPositiveButton("Purchase") { dialog, _ ->
                    val seekBar =
                        (dialog as AlertDialog).findViewById<SeekBar>(R.id.durationSeekBar)
                    if (seekBar.progress > 0) {
                        val msg = Message.obtain(null, StreamingService.ADD_USER_SUB_PLAN_QUERY)

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val activeFrom = sdf.format(Date())
                        val calendar = Calendar.getInstance()
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, seekBar.progress)
                        val activeTo = sdf.format(calendar.time)

                        val b = Bundle()
                        b.putString("sub_plan_name", item.name)
                        b.putString("active_from", activeFrom)
                        b.putString("active_to", activeTo)
                        b.putString("token", ManageActivity.manageAct.pref.getString("token", ""))
                        msg.data = b
                        msg.replyTo = Messenger(SubscriptionsFragment.ResponseHandler())
                        try {
                            ManageActivity.manageAct.mService?.send(msg)
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                    }
                    dialog.cancel()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.subscription_list_item, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val planPrice: TextView = view.planPrice
        val planName: TextView = view.planName
    }
}