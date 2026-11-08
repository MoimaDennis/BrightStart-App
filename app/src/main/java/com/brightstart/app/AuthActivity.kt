package com.brightstart.app
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthActivity : AppCompatActivity() {
    private val TAG = "AuthActivity"
    private var registerMode = false
    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvToggle: TextView
    private lateinit var progress: ProgressBar

    private val googleClient by lazy {
        GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build())
    }

    // Google SSO: Google account -> Firebase -> Firebase ID token -> the website's sso_login.php
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            try {
                val acct = GoogleSignIn.getSignedInAccountFromIntent(r.data).getResult(ApiException::class.java)
                val cred = GoogleAuthProvider.getCredential(acct.idToken, null)
                val user = FirebaseAuth.getInstance().signInWithCredential(cred).await().user!!
                val idToken = user.getIdToken(true).await().token!!
                val resp = BsApi.api.sso(SsoBody(idToken))
                val body = resp.body() ?: Gson().fromJson(resp.errorBody()?.string(), LoginResponse::class.java)
                FirebaseAuth.getInstance().signOut()
                if (body?.success == true) goHome() else toast(body?.message ?: "Google sign-in failed")
            } catch (e: Exception) {
                Log.e(TAG, "Google SSO failed", e); toast("Google sign-in failed")
            }
            progress.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(
            if (Session.dark(this)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_auth)
        etName = findViewById(R.id.etName); etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail); etPassword = findViewById(R.id.etPassword)
        btnSubmit = findViewById(R.id.btnSubmit); tvToggle = findViewById(R.id.tvToggle)
        progress = findViewById(R.id.progress)

        tvToggle.setOnClickListener {
            registerMode = !registerMode
            val v = if (registerMode) View.VISIBLE else View.GONE
            etName.visibility = v; etUsername.visibility = v
            btnSubmit.text = if (registerMode) "Register" else "Log in"
            tvToggle.text = if (registerMode) "Have an account? Log in" else "No account? Register"
        }
        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw = etPassword.text.toString()
            if (!Validator.isValidEmail(email)) { etEmail.error = "Enter a valid email"; return@setOnClickListener }
            if (!Validator.isValidPassword(pw)) { etPassword.error = "Min 8 characters, with a letter and a number"; return@setOnClickListener }
            if (registerMode) {
                val name = etName.text.toString().trim()
                val un = etUsername.text.toString().trim()
                if (name.length < 2) { etName.error = "Enter your full name"; return@setOnClickListener }
                if (un.length < 4) { etUsername.error = "Min 4 characters"; return@setOnClickListener }
                callApi { toast(BsApi.api.register(RegisterBody(name, email, un, pw)).message ?: "Done") }
            } else {
                callApi {
                    val r = BsApi.api.login(LoginBody(email, pw))
                    if (r.success) { Log.d(TAG, "Login ok"); goHome() } else toast(r.message ?: "Login failed")
                }
            }
        }
        findViewById<Button>(R.id.btnGoogle).setOnClickListener {
            googleClient.signOut().addOnCompleteListener { googleLauncher.launch(googleClient.signInIntent) }
        }
    }

    private fun callApi(block: suspend () -> Unit) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try { block() } catch (e: Exception) { Log.e(TAG, "API error", e); toast("Cannot reach server. Is ngrok running?") }
            progress.visibility = View.GONE
        }
    }
    private fun goHome() { startActivity(Intent(this, MainActivity::class.java)); finish() }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}

