package server.models

import com.google.gson.Gson

data class LoginModel(
    val username: String,
    val password: String
)


fun parseJsonToLoginModel(json: String): LoginModel {
    return Gson().fromJson(json, LoginModel::class.java)
}
