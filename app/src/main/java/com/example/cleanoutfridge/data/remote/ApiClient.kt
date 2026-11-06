package com.example.cleanoutfridge.data.remote

import com.example.cleanoutfridge.BuildConfig
import com.example.cleanoutfridge.data.remote.api.DashboardApi
import com.example.cleanoutfridge.data.remote.api.FridgeItemApi
import com.example.cleanoutfridge.data.remote.api.IngredientApi
import com.example.cleanoutfridge.data.remote.api.NotificationApi
import com.example.cleanoutfridge.data.remote.api.RecipeApi
import com.example.cleanoutfridge.data.remote.api.RecommendationApi
import com.example.cleanoutfridge.data.remote.api.VisionApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

/**
 * Single Retrofit setup shared by every `*Api` interface, matching the spec's
 * "Base URL: /api/v1" (§3) contract. Repositories should go through [ApiClient.<name>Api]
 * rather than building their own Retrofit instance.
 *
 * [BASE_URL] points at a local dev server by default (10.0.2.2 is the Android emulator's
 * alias for the host machine's localhost). Override it to your deployed backend once
 * available — e.g. by reading it from BuildConfig/gradle properties per build variant.
 */
object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/api/v1/"

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    /** AI photo analysis can take longer than a normal CRUD call, so it gets its own timeouts. */
    private val visionOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val visionRetrofit: Retrofit = retrofit.newBuilder()
        .client(visionOkHttpClient)
        .build()

    val ingredientApi: IngredientApi by lazy { retrofit.create(IngredientApi::class.java) }
    val fridgeItemApi: FridgeItemApi by lazy { retrofit.create(FridgeItemApi::class.java) }
    val notificationApi: NotificationApi by lazy { retrofit.create(NotificationApi::class.java) }
    val recipeApi: RecipeApi by lazy { retrofit.create(RecipeApi::class.java) }
    val recommendationApi: RecommendationApi by lazy { retrofit.create(RecommendationApi::class.java) }
    val dashboardApi: DashboardApi by lazy { retrofit.create(DashboardApi::class.java) }
    val visionApi: VisionApi by lazy { visionRetrofit.create(VisionApi::class.java) }
}
