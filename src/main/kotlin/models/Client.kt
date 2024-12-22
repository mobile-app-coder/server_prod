package models

class Client(
    val id: Int = 0,
    val name: String,
    val date: String,
    val address: String,
    val email: String,
    val phone: String,
    val login: String,
    val password: String
) {


    override fun toString(): String {
        return "Client(id=$id, name='$name', date='$date', address='$address', email='$email', phone='$phone', login='$login')"
    }
}
