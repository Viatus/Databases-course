package com.example.streaming_service_app.fragments

import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.streaming_service_app.*
import com.example.streaming_service_app.activities.MainActivity
import com.example.streaming_service_app.activities.ManageActivity
import com.example.streaming_service_app.adapters.SubscriptionPlansViewAdapter
import com.example.streaming_service_app.adapters.UserSubPlanViewAdapter
import com.example.streaming_service_app.databinding.FragmentSubscriptionsBinding
import com.example.streaming_service_app.utils.*

class SubscriptionsFragment : Fragment() {
    private lateinit var binding: FragmentSubscriptionsBinding

    private var isUsersSubs = false

    fun isUsersSubs() = isUsersSubs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_subscriptions, container, false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fragment = this
        val layoutManager = LinearLayoutManager(activity)
        binding.subscriptionList.layoutManager = layoutManager
        val adapter = if (isUsersSubs) {
            UserSubPlanViewAdapter()
        } else {
            SubscriptionPlansViewAdapter()
        }
        binding.subscriptionList.adapter = adapter
        updateItems()
    }

    fun updateItems() {
        val spinner = activity?.findViewById<Spinner>(R.id.gameGenreSpinner)
        spinner?.visibility = View.GONE
        val msg: Message?
        if (!isUsersSubs) {
            msg = Message.obtain(
                null,
                StreamingService.GET_ALL_SUB_PLANS_QUERY
            )
        } else {
            msg = Message.obtain(
                null,
                StreamingService.GET_USER_SUB_PLANS_QUERY
            )
            val b = Bundle()
            b.putString(
                "token", ManageActivity.manageAct.pref.getString(
                    MainActivity.APP_PREFERENCES_TOKEN, ""
                )
            )
            msg.data = b
        }

        msg.replyTo = Messenger(ResponseHandler())
        try {
            (activity as ManageActivity).mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun changeAdapter() {
        if (isUsersSubs) {
            binding.subscriptionList.adapter = SubscriptionPlansViewAdapter()
        } else {
            binding.subscriptionList.adapter = UserSubPlanViewAdapter()
        }
        isUsersSubs = !isUsersSubs
        updateItems()
    }

    fun updateSubPlans(plans: List<SubscriptionPlan>) {
        val adapter = binding.subscriptionList.adapter as SubscriptionPlansViewAdapter
        adapter.data = plans
    }

    fun updateUserSubPlans(plans: List<UserSubscriptionPlan>) {
        val adapter = binding.subscriptionList.adapter as UserSubPlanViewAdapter
        adapter.data = plans
    }

    class ResponseHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                StreamingService.GET_ALL_SUB_PLANS_QUERY -> getAllSubPlans(msg)
                StreamingService.GET_USER_SUB_PLANS_QUERY -> getUserSubPlans(msg)
                StreamingService.ADD_USER_SUB_PLAN_QUERY -> addUserSubPlan(msg)
            }
        }

        private fun getAllSubPlans(msg: Message) {
            val report = msg.data.getSerializable("data") as SubPlanReport
            val subList = report.data ?: ArrayList()

            if (subList.isNotEmpty()) {
                fragment.updateSubPlans(subList)
            }
        }

        private fun getUserSubPlans(msg: Message) {
            val subList: List<UserSubscriptionPlan>
            if (msg.data.getSerializable("data") != null) {
                val report = msg.data.getSerializable("data") as UserSubPlanReport
                subList = report.data ?: ArrayList()
            } else {
                subList = ArrayList()
            }
            if (subList.isNotEmpty()) {
                fragment.updateUserSubPlans(subList)
            }
        }

        private fun addUserSubPlan(msg: Message) {
            if (msg.data.getSerializable("data") != null) {
                val report = msg.data.getSerializable("data") as AddReport
                if (report.status == "success") {
                    Toast.makeText(fragment.activity, "Plan purchased", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        fragment.activity,
                        "Error occured while trying to purchase plan",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } else {
                Toast.makeText(
                    fragment.activity,
                    "Error occured while trying to purchase plan",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    companion object {
        private lateinit var fragment: SubscriptionsFragment
    }
}