package com.example.streaming_service_app.activities

import android.content.*
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.streaming_service_app.utils.AuthorizationData
import com.example.streaming_service_app.utils.AuthorizationReport
import com.example.streaming_service_app.R
import com.example.streaming_service_app.utils.StreamingService
import com.example.streaming_service_app.databinding.ActivityMainBinding
import com.example.streaming_service_app.fragments.LoginFragment
import com.example.streaming_service_app.fragments.SignUpFragment

class MainActivity() : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var mService: Messenger? = null
    private var mBound = false
    lateinit var pref: SharedPreferences
    private var currentFragment = 0
    private lateinit var loginFragment: LoginFragment
    private lateinit var signUpFragment: SignUpFragment

    private var mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = Messenger(service)
            mBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )

        pref = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        mainAct = this
        signUpFragment = SignUpFragment()
        loginFragment = LoginFragment()
        supportFragmentManager.inTransaction { add(R.id.fragment_container, loginFragment) }

    }

    private inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().func().commit()
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, StreamingService::class.java),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            unbindService(mConnection)
            mBound = false
        }
    }

    fun changeCurrentFragment() {
        currentFragment = if (currentFragment == 0) {
            supportFragmentManager.inTransaction {
                remove(loginFragment)
                add(R.id.fragment_container, signUpFragment)
            }
            1
        } else {
            supportFragmentManager.inTransaction {
                remove(signUpFragment)
                add(R.id.fragment_container, loginFragment)
            }
            0
        }
    }

    class ResponseHandler : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == StreamingService.SIGN_IN_QUERY || msg.what == StreamingService.SIGN_UP_QUERY) {
                handleSign(msg)
            }
        }

        private fun handleSign(msg: Message) {
            if (msg.data.getSerializable("data") != null) {
                val authorizationReport = msg.data.getSerializable("data") as AuthorizationReport
                if (authorizationReport.status == "success") {
                    val authorizationData = authorizationReport.data as AuthorizationData
                    val editor = mainAct.pref.edit()
                    editor.putString(APP_PREFERENCES_TOKEN, authorizationData.token)
                    editor.putString(APP_PREFERENCES_EMAIL, authorizationData.email)
                    editor.putString(APP_PREFERENCES_NICKNAME, authorizationData.nickname)
                    editor.apply()
                    mainAct.toNextActivity()
                } else {
                    Toast.makeText(
                        mainAct.applicationContext,
                        "Something went wrong",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            } else {
                Toast.makeText(mainAct, "Error occured", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun toNextActivity() {
        val intent = Intent(this, ManageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    companion object {
        private lateinit var mainAct: MainActivity
        const val APP_PREFERENCES = "mysettings"
        const val APP_PREFERENCES_TOKEN = "token"
        const val APP_PREFERENCES_NICKNAME = "nickname"
        const val APP_PREFERENCES_EMAIL = "email"
    }

}
