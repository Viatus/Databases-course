package com.example.streaming_service_app.fragments

import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.streaming_service_app.*
import com.example.streaming_service_app.activities.MainActivity
import com.example.streaming_service_app.activities.ManageActivity
import com.example.streaming_service_app.adapters.AllGamesViewAdapter
import com.example.streaming_service_app.adapters.UserGamesViewAdapter
import com.example.streaming_service_app.databinding.FragmentAllgamesBinding
import com.example.streaming_service_app.utils.*
import kotlin.collections.ArrayList

class AllGamesFragment : Fragment() {
    private lateinit var binding: FragmentAllgamesBinding

    lateinit var genres: List<String>

    private var isAdapterAllGames = true

    fun isAdapterAllGames() = isAdapterAllGames

    var isOrderASC = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_allgames, container, false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fragment = this
        val layoutManager = LinearLayoutManager(activity)
        binding.gamesList.layoutManager = layoutManager
        val adapter = if (isAdapterAllGames) {
            AllGamesViewAdapter()
        } else {
            UserGamesViewAdapter()
        }
        binding.gamesList.adapter = adapter
        val msg = Message.obtain(
            null,
            StreamingService.GET_GENRES_QUERY
        )
        msg.replyTo = Messenger(ResponseHandler())
        try {
            ManageActivity.manageAct.mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun changeAdapter() {
        if (isAdapterAllGames) {
            binding.gamesList.adapter = UserGamesViewAdapter()
        } else {
            binding.gamesList.adapter = AllGamesViewAdapter()
        }
        isAdapterAllGames = !isAdapterAllGames
        updateItems()
    }

    fun updateItems() {
        var msg: Message
        val spinner = activity?.findViewById<Spinner>(R.id.gameGenreSpinner)
        spinner?.visibility = View.VISIBLE
        var isAll = true
        if (spinner != null) {
            if (spinner.selectedItem == null) {
                msg = Message.obtain(
                    null,
                    StreamingService.GET_GENRES_QUERY
                )
                msg.replyTo = Messenger(ResponseHandler())
                try {
                    ManageActivity.manageAct.mService?.send(msg)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
                return
            }
            if (spinner.selectedItem.toString() != "ALL") {
                val msgForGames =
                    Message.obtain(
                        null,
                        StreamingService.GET_GAMES_OF_GENRE_QUERY
                    )
                msgForGames.replyTo = Messenger(ResponseHandler())
                val b = Bundle()
                b.putString("genre", spinner.selectedItem.toString())
                msgForGames.data = b
                try {
                    ManageActivity.manageAct.mService?.send(msgForGames)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
                isAll = false
            }
        }
        if (isAll) {
            msg = Message.obtain(
                null,
                StreamingService.GET_ALL_GAMES_QUERY
            )
            msg.replyTo = Messenger(ResponseHandler())
            val b = Bundle()
            b.putString("order", if (isOrderASC) "ASC" else "DESC")
            msg.data = b
            try {
                ManageActivity.manageAct.mService?.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        msg = Message.obtain(
            null,
            StreamingService.GET_USER_GAMES_QUERY
        )
        msg.replyTo = Messenger(ResponseHandler())
        val b = Bundle()
        b.putString(
            "token",
            ManageActivity.manageAct.pref.getString(MainActivity.APP_PREFERENCES_TOKEN, "")
        )
        b.putString("order", if (isOrderASC) "ASC" else "DESC")
        msg.data = b
        try {
            ManageActivity.manageAct.mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private var listsAdded = 0

    fun updateGamesLists(games: List<Game>, listNumber: Int) {
        if (isAdapterAllGames) {
            val adapter = binding.gamesList.adapter as AllGamesViewAdapter
            if (listNumber == 1) {
                adapter.data = games
            } else {
                adapter.ownedGames = games
            }
        } else {
            val adapter = binding.gamesList.adapter as UserGamesViewAdapter
            if (listsAdded == 0) {
                adapter.data = games
                listsAdded++
            } else {
                val adapterGameList = adapter.data
                val merged = mutableListOf<Game>()
                for (game in adapterGameList) {
                    if (games.contains(game)) {
                        merged.add(game)
                    }
                }
                adapter.data = merged
                listsAdded = 0
            }
        }
    }

    class ResponseHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                StreamingService.GET_ALL_GAMES_QUERY, StreamingService.GET_USER_GAMES_QUERY, StreamingService.GET_GAMES_OF_GENRE_QUERY -> gameSelectionResponse(
                    msg
                )
                StreamingService.ADD_USER_GAME_QUERY -> addUserGameResponse(msg)
                StreamingService.GET_GENRES_QUERY -> getGenresResponse(msg)
                StreamingService.ADD_USER_SESSION_QUERY -> addUserSessionResponse(msg)
            }
        }

        private fun gameSelectionResponse(msg: Message) {
            val gameList = if (msg.data.getSerializable("data") != null) {
                val gameReport = msg.data.getSerializable("data") as GameReport
                gameReport.data ?: ArrayList()
            } else {
                ArrayList()
            }
            if (msg.what == StreamingService.GET_ALL_GAMES_QUERY || msg.what == StreamingService.GET_GAMES_OF_GENRE_QUERY) {
                fragment.updateGamesLists(gameList, 1)
            } else {
                if (msg.what == StreamingService.GET_USER_GAMES_QUERY) {
                    fragment.updateGamesLists(gameList, 2)
                }
            }

        }

        private fun addUserGameResponse(msg: Message) {
            val report = msg.data.getSerializable("data") as AddReport
            if (report.status == "success") {
                fragment.updateItems()
                Toast.makeText(fragment.activity, "Game purchased", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        private fun getGenresResponse(msg: Message) {
            val report = msg.data.getSerializable("data") as GetGenresReport
            val genreList = report.data

            if (report.status == "success" && genreList != null) {
                val genres = mutableListOf("ALL")
                for (genre in genreList) {
                    genres.add(genre.name.toString())
                }
                val spinner =
                    ManageActivity.manageAct.findViewById<Spinner>(
                        R.id.gameGenreSpinner
                    )
                spinner.adapter = ArrayAdapter<String>(
                    ManageActivity.manageAct.applicationContext,
                    R.layout.game_genre_spinner_item,
                    genres
                )
                fragment.genres = genres
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        return
                    }

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        fragment.updateItems()
                    }
                }
            }
        }

        private fun addUserSessionResponse(msg: Message) {
            val report = msg.data.getSerializable("data") as AddMachineUsageReport

            if (report.status == "success") {
                Toast.makeText(
                    fragment.activity,
                    "Session finished",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private lateinit var fragment: AllGamesFragment
    }

}