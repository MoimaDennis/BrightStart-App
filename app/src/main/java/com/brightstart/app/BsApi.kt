package com.brightstart.app
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class UserDto(val FullName: String?, val Email: String?, val Role: String?)
data class LoginResponse(val success: Boolean, val message: String?, val user: UserDto?, val pending: Boolean?)
data class SettingsDto(val theme: String, val text_size: String, val show_slider: Int)
data class SettingsResponse(val success: Boolean, val message: String?, val settings: SettingsDto?, val profile: UserDto?)
data class SimpleResponse(val success: Boolean, val message: String?)
data class StatsResponse(val success: Boolean, val students: Int, val volunteers: Int, val programs: Int,
                         val sessions: Int, val enrolments: Int, val donations: Int, val total_donations_amount: Double)
data class LoginBody(val email: String, val password: String)
data class RegisterBody(val full_name: String, val email: String, val username: String, val password: String, val role: String = "Volunteer")
data class SsoBody(val idToken: String)
data class SettingsBody(val theme: String, val text_size: String, val show_slider: Int)
data class AdminUserDto(val AdminID: Int, val FullName: String, val Email: String, val Role: String, val Verified: Int)
data class AdminUsersResponse(val success: Boolean, val data: List<AdminUserDto>)
data class AdminIdBody(val admin_id: Int)
data class StudentBody(val first_name: String, val last_name: String, val date_of_birth: String, val gender: String,
                       val community_area: String, val contact_number: String, val enrolment_date: String)

interface ApiService {
    @POST("login.php") suspend fun login(@Body b: LoginBody): LoginResponse
    @POST("register.php") suspend fun register(@Body b: RegisterBody): SimpleResponse
    @POST("sso_login.php") suspend fun sso(@Body b: SsoBody): Response<LoginResponse>
    @GET("get_settings.php") suspend fun getSettings(): SettingsResponse
    @POST("update_settings.php") suspend fun saveSettings(@Body b: SettingsBody): SimpleResponse
    @GET("dashboard_stats.php") suspend fun stats(): StatsResponse
    @GET suspend fun list(@Url path: String): ResponseBody
    @POST("logout.php") suspend fun logout(): ResponseBody
    @GET("get_admin_users.php") suspend fun adminUsers(): AdminUsersResponse
    @POST("approve_user.php") suspend fun approveUser(@Body b: AdminIdBody): SimpleResponse
    @POST("reject_user.php") suspend fun rejectUser(@Body b: AdminIdBody): SimpleResponse
    @POST("add_student.php") suspend fun addStudent(@Body b: StudentBody): SimpleResponse
}

// Keeps the PHP session cookie so settings calls stay "logged in"
class MemoryCookieJar : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val list = store.getOrPut(url.host) { mutableListOf() }
        cookies.forEach { n -> list.removeAll { it.name == n.name }; list.add(n) }
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = store[url.host] ?: emptyList()
    fun clear() = store.clear()
}

object BsApi {
    // CHANGE to your ngrok URL (keep the trailing slash)
    const val BASE_URL = "https://cartoon-decency-unblessed.ngrok-free.dev/brightstart-web/api/"
    val cookies = MemoryCookieJar()
    val api: ApiService by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().cookieJar(cookies)
            .addInterceptor { c -> c.proceed(c.request().newBuilder().addHeader("ngrok-skip-browser-warning", "true").build()) }
            .addInterceptor(log).build()
        Retrofit.Builder().baseUrl(BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
    }
}