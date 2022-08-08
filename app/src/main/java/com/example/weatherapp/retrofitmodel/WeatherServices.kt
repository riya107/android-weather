package com.example.weatherapp.retrofitmodel

import com.example.weatherapp.model.WeatherData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface WeatherServices {
    @Headers("Content-Type: application/json")
    @GET("/v1/current.json")
    fun getWeather(@Query("key") key:String,@Query("q") q:String) : Call<WeatherData>
}