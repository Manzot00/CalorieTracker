package com.example.calorietracker.api

import com.example.calorietracker.models.DailyGoals
import com.example.calorietracker.models.FoodDetailResponse
import com.example.calorietracker.models.FoodSearchResponse
import com.example.calorietracker.models.RegistrationResponse
import com.example.calorietracker.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MyAPIService {
    @POST("/register")
    suspend fun registerUser(@Body user: User) : Response<RegistrationResponse>

    @GET("/get_dailyGoals/{userId}")
    suspend fun getDailyGoals(
        @Path("userId") userId: String,
        @Header("Authorization") token: String
    ) : Response<DailyGoals>

    @PUT("/update_dailyGoals/{userId}")
    suspend fun updateDailyGoals(
        @Path("userId") userId: String,
        @Header("Authorization") token: String,
        @Body dailyGoals: DailyGoals
    ): Response<String>

    @GET("/search_foods")
    suspend fun searchFoods(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Response<List<FoodSearchResponse>>

    @GET("/get_food/{foodId}")
    suspend fun getFood(
        @Path("foodId") foodId: Long,
        @Header("Authorization") token: String
    ): Response<FoodDetailResponse>

}