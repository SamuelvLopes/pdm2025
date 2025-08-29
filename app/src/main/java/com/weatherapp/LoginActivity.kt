package com.weatherapp

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherapp.ui.theme.WeatherAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.weatherapp.ui.components.DataField
import com.weatherapp.ui.components.PasswordField


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginPage(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPage(modifier: Modifier = Modifier) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current


    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally,
    ) {
        Text(
            text = "Bem-vindo/a!", fontSize = 24.sp
        )

        Spacer(modifier = modifier.size(24.dp))

        DataField(
            label = "Digite seu e-mail",
            value = email,
            onValueChange = { email = it },
            modifier = modifier.fillMaxWidth(0.9f),
            keyboardType = KeyboardType.Email
        )

        PasswordField(
            label = "Digite sua senha",
            value = password,
            onValueChange = { password = it },
            modifier = modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = modifier.size(24.dp))

        Row(modifier = modifier) {
            Button(
                onClick = {
                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(activity!!) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(activity, "Login OK!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(activity, "Login FALHOU!", Toast.LENGTH_LONG).show()
                            }
                        }

                }, enabled = email.isNotEmpty() && password.isNotEmpty()
            ) {
                Text("Login")
            }

            Spacer(modifier = modifier.size(12.dp))

            Button(
                onClick = { email = ""; password = "" }) {
                Text("Limpar")
            }
        }

        Button(
            onClick = {
                context.startActivity(Intent(context, RegisterActivity::class.java))
            }) {
            Text("Registrar")
        }
    }
}