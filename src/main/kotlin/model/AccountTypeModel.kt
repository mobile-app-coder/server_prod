package model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class AccountType(
    val id: Int,
    val name: String,
    val description: String,
    val features: String
)

object AccountTypeGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

    // Convert AccountType object to JSON string
    fun toJson(accountType: AccountType): String? = try {
        gson.toJson(accountType)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert JSON string to AccountType object
    fun fromJson(jsonString: String): AccountType? = try {
        gson.fromJson(jsonString, AccountType::class.java)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert a list of AccountType objects to JSON string
    fun toJsonList(accountTypes: List<AccountType>): String? = try {
        gson.toJson(accountTypes)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert JSON string to list of AccountType objects
    fun fromJsonList(jsonString: String): List<AccountType>? = try {
        val listType = object : TypeToken<List<AccountType>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }
}