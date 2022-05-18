package com.example.streaming_service_app.activities

import android.app.AlertDialog
import android.content.*
import android.os.*
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.streaming_service_app.R
import com.example.streaming_service_app.utils.StreamingService
import com.example.streaming_service_app.utils.Utility
import com.example.streaming_service_app.databinding.ActivityManageBinding
import com.example.streaming_service_app.fragments.AllGamesFragment
import com.example.streaming_service_app.fragments.SessionsFragment
import com.example.streaming_service_app.fragments.SubscriptionsFragment
import com.example.streaming_service_app.fragments.WelcomeFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_manage.*

class ManageActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityManageBinding
    var mService: Messenger? = null
    private var mBound = false
    lateinit var pref: SharedPreferences
    private lateinit var allGamesFragment: AllGamesFragment
    private lateinit var welcomeFragment: WelcomeFragment
    private lateinit var currentFragment: Fragment
    private lateinit var subscriptionsFragment: SubscriptionsFragment
    private lateinit var sessionsFragment: SessionsFragment

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
        setContentView(R.layout.activity_manage)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_manage
        )
        manageAct = this
        pref = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)

        val nickname = pref.getString(MainActivity.APP_PREFERENCES_NICKNAME, "User") ?: "User"
        val navHeaderView = navigation.inflateHeaderView(R.layout.header_layout)
        val userAvatarView = navHeaderView.findViewById<ImageView>(R.id.userAvatar)
        userAvatarView.setImageBitmap(
            Utility().generateCircleBitmap(
                this,
                70.0f,
                nickname.first().toString()
            )
        )
        userAvatarView.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)

            builder.setMessage("Update your account. If you don't want to fill one of the fields, just leave it empty")
                .setView(layoutInflater.inflate(R.layout.dialog_update_user, null))
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton("Update") { dialog, _ ->
                    val nicknameField =
                        (dialog as AlertDialog).findViewById<TextView>(R.id.nicknameTextUpdate)
                    val passwordField =
                        dialog.findViewById<TextView>(R.id.passwordTextUpdate)
                    if (nicknameField.text.toString() != "" || passwordField.text.toString() != "") {
                        val msg = Message.obtain(null, StreamingService.UPDATE_USER_QUERY)
                        msg.replyTo = Messenger(ResponseHandler())

                        val b = Bundle()
                        b.putString(
                            "token",
                            pref.getString(
                                MainActivity.APP_PREFERENCES_TOKEN,
                                ""
                            )
                        )
                        if (nicknameField.text.toString() != "") {
                            b.putString("nickname", nicknameField.text.toString())
                        } else {
                            b.putString("password", passwordField.text.toString())
                        }

                        msg.data = b
                        try {
                            mService?.send(msg)
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                    }
                    dialog.cancel()
                }.show()
        }
        val nicknameFieldView = navHeaderView.findViewById<TextView>(R.id.nicknameField)
        nicknameFieldView.text = nickname
        val emailFieldView = navHeaderView.findViewById<TextView>(R.id.emailField)
        emailFieldView.text =
            pref.getString(MainActivity.APP_PREFERENCES_EMAIL, "box@mail.ru") ?: "box@mail.ru"
        setNavigationViewListener()
        Toast.makeText(applicationContext, "Logged in", Toast.LENGTH_LONG)
            .show()
        welcomeFragment = WelcomeFragment()
        allGamesFragment = AllGamesFragment()
        subscriptionsFragment = SubscriptionsFragment()
        sessionsFragment = SessionsFragment()
        supportFragmentManager.inTransaction { add(R.id.contentFrame, welcomeFragment) }
        currentFragment = welcomeFragment
        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.games_toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            unbindService(mConnection)
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, StreamingService::class.java),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
        val msg = Message.obtain(null, 0)
        msg.replyTo = Messenger(MainActivity.ResponseHandler())
        try {
            mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun setNavigationViewListener() {
        navigation.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.log_out -> logout()
            R.id.games -> {
                if (currentFragment != allGamesFragment) {
                    supportFragmentManager.inTransaction {
                        add(R.id.contentFrame, allGamesFragment)
                        remove(currentFragment)
                    }
                    currentFragment = allGamesFragment
                    allGamesFragment.updateItems()
                }
            }
            R.id.subscriptions -> {
                if (currentFragment != subscriptionsFragment) {
                    supportFragmentManager.inTransaction {
                        add(R.id.contentFrame, subscriptionsFragment)
                        remove(currentFragment)
                    }
                    currentFragment = subscriptionsFragment
                }
            }
            R.id.my_sessions -> {
                if (currentFragment != sessionsFragment) {
                    supportFragmentManager.inTransaction {
                        add(R.id.contentFrame, sessionsFragment)
                        remove(currentFragment)
                    }
                    currentFragment = sessionsFragment
                }
            }
        }
        return true
    }

    private inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().func().commit()
    }


    fun logout() {
        val editor = pref.edit()
        editor.remove(MainActivity.APP_PREFERENCES_EMAIL)
        editor.remove(MainActivity.APP_PREFERENCES_NICKNAME)
        editor.remove(MainActivity.APP_PREFERENCES_TOKEN)
        editor.apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    class ResponseHandler : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == StreamingService.UPDATE_USER_QUERY) {
                if (msg.data.getSerializable("data") != null) {
                    manageAct.logout()
                } else {
                    Toast.makeText(
                        manageAct,
                        "error occurred",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.games_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                when (currentFragment) {
                    allGamesFragment -> allGamesFragment.updateItems()
                    subscriptionsFragment -> subscriptionsFragment.updateItems()
                    sessionsFragment -> sessionsFragment.updateSessions()
                }
                return true
            }
            R.id.playBuy -> {
                when (currentFragment) {
                    allGamesFragment -> {
                        allGamesFragment.changeAdapter()
                        if (!allGamesFragment.isAdapterAllGames()) {
                            item.setIcon(R.drawable.shopping_cart_black_18dp)
                        } else {
                            item.setIcon(R.drawable.play_arrow_circle)
                        }
                    }
                    subscriptionsFragment -> {
                        subscriptionsFragment.changeAdapter()
                        if (subscriptionsFragment.isUsersSubs()) {
                            item.setIcon(R.drawable.shopping_cart_black_18dp)
                        } else {
                            item.setIcon(R.drawable.play_arrow_circle)
                        }
                    }
                }
                return true
            }
            R.id.sort -> {
                if (currentFragment == allGamesFragment) {
                    allGamesFragment.isOrderASC = !allGamesFragment.isOrderASC
                    allGamesFragment.updateItems()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        lateinit var manageAct: ManageActivity
    }
}
