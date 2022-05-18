package com.example.streaming_service_app.fragments

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.streaming_service_app.activities.MainActivity
import com.example.streaming_service_app.R
import com.example.streaming_service_app.utils.StreamingService
import com.example.streaming_service_app.databinding.FragmentSignupBinding

class SignUpFragment: Fragment() {
    private lateinit var binding: FragmentSignupBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_signup, container, false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.registerButton.setOnClickListener { registerPressed() }
        binding.toLoginButton.setOnClickListener { (activity as MainActivity).changeCurrentFragment() }
    }

    private fun registerPressed() {
        val msg = Message.obtain(null,
            StreamingService.SIGN_UP_QUERY
        )
        val b = Bundle()
        b.putString("email", binding.emailTextSignUp.text.toString())
        b.putString("password", binding.passwordTextSignUp.text.toString())
        b.putString("nickname", binding.nicknameTextSignUp.text.toString())
        msg.data = b

        msg.replyTo = Messenger(MainActivity.ResponseHandler())
        try {
            (activity as MainActivity).mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


}