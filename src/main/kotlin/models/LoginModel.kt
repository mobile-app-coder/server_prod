package server.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class LoginModel(
    val login: String,
    val password: String
)

object LoginModelGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            // Optional Gson configuration
            // .serializeNulls() // Uncomment if you want to serialize null values as well
            .create()
    }

    fun toJson(loginModel: LoginModel): String? = try {
        gson.toJson(loginModel)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    fun fromJson(jsonString: String): LoginModel? = try {
        gson.fromJson(jsonString, LoginModel::class.java)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    fun toJsonList(loginModels: List<LoginModel>): String? = try {
        gson.toJson(loginModels)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    fun fromJsonList(jsonString: String): List<LoginModel>? = try {
        val listType = object : TypeToken<List<LoginModel>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }
}

