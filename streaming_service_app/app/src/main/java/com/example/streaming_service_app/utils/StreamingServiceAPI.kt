package com.example.streaming_service_app.utils

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable

interface StreamingServiceAPI {
    @POST("/auth/signup")
    fun signUp(@Body body: SignUpClient): Call<AuthorizationReport>

    @POST("/auth/signin")
    fun signIn(@Body body: SignInClient): Call<AuthorizationReport>

    @GET("/games")
    fun getGames(@Query("sort") sort: String?, @Query("order") order: String?): Call<GameReport>

    @GET("/games/my")
    fun getUserGames(@Header("token") token: String, @Query("sort") sort: String?, @Query("order") order: String?): Call<GameReport>

    @POST("/games/my")
    fun addUserGame(@Header("token") token: String, @Body body: NewGame): Call<AddReport>

    @GET("/genres")
    fun getGenres(): Call<GetGenresReport>

    @GET("/games/genre/{chosen_genre}")
    fun getGamesOfGenre(
        @Path("chosen_genre") chosenGenre: String, @Query("sort") sort: String?, @Query(
            "order"
        ) order: String?
    ): Call<GameReport>

    @GET("/subscription_plans")
    fun getSubscriptionPlans(): Call<SubPlanReport>

    @GET("/subscription_plans/my")
    fun getUserSubscriptionPlans(@Header("token") token: String): Call<UserSubPlanReport>

    @POST("/subscription_plans/my")
    fun addUserSubscriptionPlan(@Header("token") token: String, @Body body: UserSubPlanBody): Call<AddReport>

    @GET("/sessions/my")
    fun getUserSessions(@Header("token") token: String): Call<GetMachineUsageReport>

    @POST("/sessions/my")
    fun addUserSession(@Header("token") token: String, @Body body: SessionData): Call<AddMachineUsageReport>

    @POST("/users/me")
    fun updateUser(@Header("token") token:String, @Body body: UpdateUserBody): Call<AuthorizationReport>
}

class AuthorizationReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: AuthorizationData? = null
}

class GetGenresReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: List<Genre>? = null
}

class GameReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: List<Game>? = null
}

class AddReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
}

class SubPlanReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: List<SubscriptionPlan>? = null
}

class UserSubPlanReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: List<UserSubscriptionPlan>? = null
}

class AddMachineUsageReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: MachineUsage? = null
}

class GetMachineUsageReport: Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null
    @SerializedName("data")
    @Expose
    var data: List<MachineUsage>? = null
}

class AuthorizationData : Serializable {
    @SerializedName("id")
    @Expose
    var id: Int? = null
    @SerializedName("nickname")
    @Expose
    var nickname: String? = null
    @SerializedName("email")
    @Expose
    var email: String? = null
    @SerializedName("token")
    @Expose
    var token: String? = null
}

class Game : Serializable {
    @SerializedName("id")
    @Expose
    var id: Int? = null
    @SerializedName("title")
    @Expose
    var title: String? = null
    @SerializedName("price")
    @Expose
    var price: Int? = null
    @SerializedName("purchase_date")
    @Expose
    var purchaseDate: String? = null
    @SerializedName("minutes_played")
    @Expose
    var minutesPlayed: Int? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        val otherGame = other as Game
        return this.title == otherGame.title
    }

    override fun hashCode(): Int {
        return id ?: 0
    }
}

class Genre : Serializable {
    @SerializedName("name")
    @Expose
    var name: String? = null
}

class SubscriptionPlan : Serializable {
    @SerializedName("id")
    @Expose
    var id: Int? = null
    @SerializedName("name")
    @Expose
    var name: String? = null
    @SerializedName("price")
    @Expose
    var price: Int? = null
}

class UserSubscriptionPlan : Serializable {
    @SerializedName("name")
    @Expose
    var name: String? = null
    @SerializedName("active_from")
    @Expose
    var activeFrom: String? = null
    @SerializedName("active_to")
    @Expose
    var activeTo: String? = null
}

class MachineUsage {
    @SerializedName("id")
    @Expose
    var id: Int? = null
    @SerializedName("owned_game_id")
    @Expose
    var ownedGameId: Int? = null
    @SerializedName("machine_id")
    @Expose
    var machineId: Int? = null
    @SerializedName("in_use_from")
    @Expose
    var inUseFrom: String? = null
    @SerializedName("in_use_to")
    @Expose
    var inUseTo: String? = null
    @SerializedName("title")
    @Expose
    var gameTitle: String? = null
}

class SignUpClient(val email: String, val password: String, val nickname: String)

class SignInClient(val email: String, val password: String)

class NewGame(val game_title: String, val purchase_date: String)

class UserSubPlanBody(val plan_name: String, val active_from: String, val active_to: String)

class SessionData(val game_title: String, val usage_time: String)

class UpdateUserBody(val nickname: String?, val password: String?)