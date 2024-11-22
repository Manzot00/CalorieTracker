package com.example.calorietracker.api

import com.example.calorietracker.models.RegistrationResponse
import com.example.calorietracker.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MyAPIService {
    @POST("/register")
    suspend fun registerUser(@Body user: User) : Response<RegistrationResponse>
}