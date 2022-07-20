package com.example.whetherapp.pojo

import com.google.gson.annotations.SerializedName

data class Sys(
    @SerializedName("type") val temp : Int,
    @SerializedName("id") val id : Int,
    @SerializedName("country") val country : String,
    @SerializedName("sunrise") val sunrise : Int,
    @SerializedName("sunset") val sunset : Int,
)
