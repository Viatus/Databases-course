package com.example.streaming_service_app.fragments

import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.streaming_service_app.*
import com.example.streaming_service_app.activities.ManageActivity
import com.example.streaming_service_app.adapters.SessionsViewAdapter
import com.example.streaming_service_app.databinding.FragmentSessionsBinding
import com.example.streaming_service_app.utils.GetMachineUsageReport
import com.example.streaming_service_app.utils.MachineUsage
import com.example.streaming_service_app.utils.StreamingService

class SessionsFragment : Fragment() {
    private lateinit var binding: FragmentSessionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sessions, container, false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fragment = this
        val layoutManager = LinearLayoutManager(activity)
        binding.sessionsList.layoutManager = layoutManager
        binding.sessionsList.adapter = SessionsViewAdapter()
        updateSessions()
    }

    private fun updateSessionsList(sessions: List<MachineUsage>) {
        if (sessions.isNotEmpty()) {
            val adapter = binding.sessionsList.adapter as SessionsViewAdapter
            adapter.data = sessions
        }
    }

    fun updateSessions() {
        val spinner = activity?.findViewById<Spinner>(R.id.gameGenreSpinner)
        spinner?.visibility = View.GONE
        val msg = Message.obtain(
            null,
            StreamingService.GET_USER_SESSIONS_QUERY
        )

        val b = Bundle()
        b.putString("token", ManageActivity.manageAct.pref.getString("token", ""))
        msg.data = b
        msg.replyTo = Messenger(ResponseHandler())
        try {
            ManageActivity.manageAct.mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    class ResponseHandler : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == StreamingService.GET_USER_SESSIONS_QUERY) {
                val sessionsList: List<MachineUsage>
                if (msg.data.getSerializable("data") != null) {
                    val report = msg.data.getSerializable("data") as GetMachineUsageReport
                    sessionsList = report.data ?: ArrayList()
                } else {
                    sessionsList = ArrayList()
                }
                fragment.updateSessionsList(sessionsList)
            }
        }
    }

    companion object {
        private lateinit var fragment: SessionsFragment
    }
}