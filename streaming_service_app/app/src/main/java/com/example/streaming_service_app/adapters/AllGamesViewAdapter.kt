package com.example.streaming_service_app.adapters

import android.app.AlertDialog
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.streaming_service_app.*
import com.example.streaming_service_app.activities.MainActivity
import com.example.streaming_service_app.activities.ManageActivity
import com.example.streaming_service_app.fragments.AllGamesFragment
import com.example.streaming_service_app.utils.Game
import com.example.streaming_service_app.utils.StreamingService
import com.example.streaming_service_app.utils.Utility
import kotlinx.android.synthetic.main.games_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AllGamesViewAdapter :
    RecyclerView.Adapter<AllGamesViewAdapter.ViewHolder>() {
    var data: List<Game> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var ownedGames: List<Game> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.gameAvatar.setImageBitmap(
            Utility().generateCircleBitmap(
                ManageActivity.manageAct,
                50.0f,
                item.title?.last().toString()
            )
        )
        holder.gameTitle.text = item.title
        holder.gamePrice.text = item.price.toString()
        if (!ownedGames.contains(item)) {
            holder.isBoughtImage.setImageResource(R.drawable.buy_game)
            holder.isBoughtImage.setOnClickListener {

                val builder = AlertDialog.Builder(it.context)
                builder.setMessage("Do you wish to buy ${item.title} for ${item.price}?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        val msg = Message.obtain(null, StreamingService.ADD_USER_GAME_QUERY)
                        msg.replyTo = Messenger(AllGamesFragment.ResponseHandler())
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val currentDate = sdf.format(Date())
                        val b = Bundle()
                        b.putString("title", item.title)
                        b.putString("purchase_date", currentDate)
                        b.putString(
                            "token",
                            ManageActivity.manageAct.pref.getString(
                                MainActivity.APP_PREFERENCES_TOKEN,
                                ""
                            )
                        )
                        msg.data = b
                        try {
                            ManageActivity.manageAct.mService?.send(msg)
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                        dialog.cancel()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }.show()
            }
        } else {
            holder.isBoughtImage.setImageResource(R.drawable.game_bought)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.games_list_item, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val gameAvatar: ImageView = view.gameAvatar
        val gameTitle: TextView = view.gameTitle
        val gamePrice: TextView = view.gamePrice
        val isBoughtImage: ImageView = view.isBoughtImage
    }
}