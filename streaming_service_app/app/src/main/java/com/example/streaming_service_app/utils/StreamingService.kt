package com.example.streaming_service_app.utils

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StreamingService : Service() {

    private val mRetrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.106:3000")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val mMessenger = Messenger(
        IncomingHandler(
            mRetrofit
        )
    )

    class IncomingHandler(private val mRetrofit: Retrofit) : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SIGN_IN_QUERY -> signIn(msg)
                SIGN_UP_QUERY -> signUp(msg)
                GET_ALL_GAMES_QUERY -> getAllGames(msg)
                GET_USER_GAMES_QUERY -> getUserGames(msg)
                ADD_USER_GAME_QUERY -> addUserGame(msg)
                GET_GENRES_QUERY -> getGenres(msg)
                GET_GAMES_OF_GENRE_QUERY -> getGamesOfGenre(msg)
                GET_ALL_SUB_PLANS_QUERY -> getAllSubPlans(msg)
                GET_USER_SUB_PLANS_QUERY -> getUserSubscriptionPlans(msg)
                ADD_USER_SUB_PLAN_QUERY -> addUserSubPlan(msg)
                GET_USER_SESSIONS_QUERY -> getAllUserSessions(msg)
                ADD_USER_SESSION_QUERY -> addUserSession(msg)
                UPDATE_USER_QUERY -> updateUser(msg)
                else -> signIn(msg)
            }
        }

        private fun signIn(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val email = msg.data.getString("email")
            val password = msg.data.getString("password")
            val replyTo = msg.replyTo
            if (email != null && password != null) {
                val call = serviceAPI.signIn(
                    SignInClient(
                        email,
                        password
                    )
                )
                call.enqueue(object : retrofit2.Callback<AuthorizationReport> {
                    override fun onFailure(call: Call<AuthorizationReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<AuthorizationReport>,
                        response: Response<AuthorizationReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            SIGN_IN_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun signUp(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val email = msg.data.getString("email")
            val password = msg.data.getString("password")
            val nickname = msg.data.getString("nickname")
            val replyTo = msg.replyTo
            if (email != null && password != null && nickname != null) {
                val call = serviceAPI.signUp(
                    SignUpClient(
                        email,
                        password,
                        nickname
                    )
                )
                call.enqueue(object : retrofit2.Callback<AuthorizationReport> {
                    override fun onFailure(call: Call<AuthorizationReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<AuthorizationReport>,
                        response: Response<AuthorizationReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            SIGN_UP_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun getAllGames(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo
            val sort = msg.data.getString("sort") ?: "title"
            val order = msg.data.getString("order")
            val call = serviceAPI.getGames(sort, order)
            call.enqueue(object : retrofit2.Callback<GameReport> {
                override fun onFailure(call: Call<GameReport>, t: Throwable) {
                    Log.i("StreamingService", "call failed")
                }

                override fun onResponse(
                    call: Call<GameReport>,
                    response: Response<GameReport>
                ) {
                    val msgWithClient = Message.obtain(
                        null,
                        GET_ALL_GAMES_QUERY
                    )
                    val b = Bundle()
                    if (response.body() != null) {
                        b.putSerializable("data", response.body())
                    } else {
                        b.putString("error", response.errorBody().toString())
                    }
                    msgWithClient.data = b
                    replyTo.send(msgWithClient)
                }
            })
        }

        private fun getUserGames(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo
            val token = msg.data.getString("token")

            val sort = msg.data.getString("sort") ?: "title"
            val order = msg.data.getString("order")

            if (token != null) {
                val call = serviceAPI.getUserGames(token, sort, order)

                call.enqueue(object : retrofit2.Callback<GameReport> {
                    override fun onFailure(call: Call<GameReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<GameReport>,
                        response: Response<GameReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            GET_USER_GAMES_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun addUserGame(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo
            val token = msg.data.getString("token")

            val title = msg.data.getString("title")
            val purchaseDate = msg.data.getString("purchase_date")

            if (token != null && title != null && purchaseDate != null) {
                val call = serviceAPI.addUserGame(
                    token,
                    NewGame(title, purchaseDate)
                )

                call.enqueue(object : retrofit2.Callback<AddReport> {
                    override fun onFailure(call: Call<AddReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<AddReport>,
                        response: Response<AddReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            ADD_USER_GAME_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun getGenres(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val call = serviceAPI.getGenres()

            call.enqueue(object : retrofit2.Callback<GetGenresReport> {
                override fun onFailure(call: Call<GetGenresReport>, t: Throwable) {
                    Log.i("StreamingService", "call failed")
                }

                override fun onResponse(
                    call: Call<GetGenresReport>,
                    response: Response<GetGenresReport>
                ) {
                    val msgWithClient = Message.obtain(
                        null,
                        GET_GENRES_QUERY
                    )
                    val b = Bundle()
                    if (response.body() != null) {
                        b.putSerializable("data", response.body())
                    } else {
                        b.putString("error", response.errorBody().toString())
                    }
                    msgWithClient.data = b
                    replyTo.send(msgWithClient)
                }
            })
        }

        private fun getGamesOfGenre(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val sort = msg.data.getString("sort") ?: "title"
            val order = msg.data.getString("order")

            val genre = msg.data.getString("genre")
            if (genre != null) {
                val call = serviceAPI.getGamesOfGenre(genre, sort, order)
                call.enqueue(object : retrofit2.Callback<GameReport> {
                    override fun onFailure(call: Call<GameReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<GameReport>,
                        response: Response<GameReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            GET_GAMES_OF_GENRE_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun getAllSubPlans(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val call = serviceAPI.getSubscriptionPlans()
            call.enqueue(object : retrofit2.Callback<SubPlanReport> {
                override fun onFailure(call: Call<SubPlanReport>, t: Throwable) {
                    Log.i("StreamingService", "call failed")
                }

                override fun onResponse(
                    call: Call<SubPlanReport>,
                    response: Response<SubPlanReport>
                ) {
                    val msgWithClient = Message.obtain(
                        null,
                        GET_ALL_SUB_PLANS_QUERY
                    )
                    val b = Bundle()
                    if (response.body() != null) {
                        b.putSerializable("data", response.body())
                    } else {
                        b.putString("error", response.errorBody().toString())
                    }
                    msgWithClient.data = b
                    replyTo.send(msgWithClient)
                }
            })
        }

        private fun getUserSubscriptionPlans(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val token = msg.data.getString("token")
            if (token != null) {
                val call = serviceAPI.getUserSubscriptionPlans(token)
                call.enqueue(object : retrofit2.Callback<UserSubPlanReport> {
                    override fun onFailure(call: Call<UserSubPlanReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<UserSubPlanReport>,
                        response: Response<UserSubPlanReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            GET_USER_SUB_PLANS_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun addUserSubPlan(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val token = msg.data.getString("token")
            val subPlanName = msg.data.getString("sub_plan_name")
            val activeFrom = msg.data.getString("active_from")
            val activeTo = msg.data.getString("active_to")

            if (token != null && subPlanName != null && activeFrom != null && activeTo != null) {
                val call = serviceAPI.addUserSubscriptionPlan(
                    token,
                    UserSubPlanBody(
                        subPlanName,
                        activeFrom,
                        activeTo
                    )
                )
                call.enqueue(object : retrofit2.Callback<AddReport> {
                    override fun onFailure(call: Call<AddReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<AddReport>,
                        response: Response<AddReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            ADD_USER_SUB_PLAN_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun getAllUserSessions(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val token = msg.data.getString("token")

            if (token != null) {
                val call = serviceAPI.getUserSessions(token)
                call.enqueue(object : retrofit2.Callback<GetMachineUsageReport> {
                    override fun onFailure(call: Call<GetMachineUsageReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<GetMachineUsageReport>,
                        response: Response<GetMachineUsageReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            GET_USER_SESSIONS_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun addUserSession(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val token = msg.data.getString("token")
            val gameTitle = msg.data.getString("game_title")
            val inUseFrom = msg.data.getString("in_use_from")
            val inUseTo = msg.data.getString("in_use_to")

            if (token != null && gameTitle != null && inUseFrom != null && inUseTo != null) {
                val call = serviceAPI.addUserSession(
                    token,
                    SessionData(
                        gameTitle,
                        "$inUseFrom;$inUseTo"
                    )
                )
                call.enqueue(object : retrofit2.Callback<AddMachineUsageReport> {
                    override fun onFailure(call: Call<AddMachineUsageReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<AddMachineUsageReport>,
                        response: Response<AddMachineUsageReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            ADD_USER_SESSION_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }

        private fun updateUser(msg: Message) {
            val serviceAPI = mRetrofit.create(StreamingServiceAPI::class.java)
            val replyTo = msg.replyTo

            val token = msg.data.getString("token")
            val password = msg.data.getString("password")
            val nickname = msg.data.getString("nickname")

            if (token != null) {
                val call = serviceAPI.updateUser(token, UpdateUserBody(nickname, password))
                call.enqueue(object : retrofit2.Callback<AuthorizationReport> {
                    override fun onFailure(call: Call<AuthorizationReport>, t: Throwable) {
                        Log.i("StreamingService", "call failed")
                    }

                    override fun onResponse(
                        call: Call<AuthorizationReport>,
                        response: Response<AuthorizationReport>
                    ) {
                        val msgWithClient = Message.obtain(
                            null,
                            UPDATE_USER_QUERY
                        )
                        val b = Bundle()
                        if (response.body() != null) {
                            b.putSerializable("data", response.body())
                        } else {
                            b.putString("error", response.errorBody().toString())
                        }
                        msgWithClient.data = b
                        replyTo.send(msgWithClient)
                    }
                })
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
    }

    companion object {
        const val SIGN_IN_QUERY = 1
        const val SIGN_UP_QUERY = 2
        const val GET_ALL_GAMES_QUERY = 3
        const val GET_USER_GAMES_QUERY = 4
        const val ADD_USER_GAME_QUERY = 5
        const val GET_GENRES_QUERY = 6
        const val GET_GAMES_OF_GENRE_QUERY = 7
        const val GET_ALL_SUB_PLANS_QUERY = 8
        const val GET_USER_SUB_PLANS_QUERY = 9
        const val ADD_USER_SUB_PLAN_QUERY = 10
        const val GET_USER_SESSIONS_QUERY = 11
        const val ADD_USER_SESSION_QUERY = 12
        const val UPDATE_USER_QUERY = 13
    }
}