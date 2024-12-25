package model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class User(
    val id: String? = null,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val address: String,
    val email: String,
    val phone: String,
    val login: String? = null,
    val password: String? = null
) {
    override fun toString(): String {
        return "User(name='$firstName', date='$dateOfBirth', address='$address', email='$email', phone='$phone', login='$login', password='$password')"
    }
}

object UserGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

    fun toJson(user: User): String? = try {
        gson.toJson(user)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    fun fromJson(jsonString: String): User? = try {
        gson.fromJson(jsonString, User::class.java)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    fun toJsonList(users: List<User>): String? = try {
        gson.toJson(users)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    fun fromJsonList(jsonString: String): List<User>? = try {
        val listType = object : TypeToken<List<User>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }
}
