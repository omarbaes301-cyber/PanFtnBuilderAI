package com.panftn.builderai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONArray
import org.json.JSONObject

const val WORKER_URL = "https://panftn-ai.omarbaes301.workers.dev"

data class Msg(val text: String, val fromUser: Boolean, val copiable: Boolean = false)

fun callWorker(token: String, history: List<Msg>): String {
    val arr = JSONArray()
    for (m in history.dropWhile { !it.fromUser }) {
        arr.put(JSONObject().put("role", if (m.fromUser) "user" else "model").put("text", m.text))
    }
    val body = JSONObject().put("messages", arr).toString()
    val c = URL(WORKER_URL).openConnection() as HttpURLConnection
    try {
        c.requestMethod = "POST"
        c.connectTimeout = 15000
        c.readTimeout = 60000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("x-app-token", token)
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code == 401) return "Contraseña incorrecta 🔒 Cámbiala con el botón de arriba."
        if (code !in 200..299) {
            val err = try { JSONObject(text).optString("error", text) } catch (e: Exception) { text }
            return "Error ($code): $err"
        }
        return JSONObject(text).optString("reply", "(sin respuesta)")
    } finally {
        c.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { ChatScreen() }
            }
        }
    }
}

@Composable
fun ChatScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE) }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var editToken by remember { mutableStateOf(token.isEmpty()) }
    val msgs = remember {
        mutableStateListOf(Msg("¡Qué onda! Soy PAN-FTN Builder AI 🤙 Pon tu contraseña arriba y escríbeme.", false))
    }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(msgs.size) { listState.animateScrollToItem(msgs.lastIndex) }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        if (editToken) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    token = token.trim()
                    prefs.edit().putString("token", token).apply()
                    if (token.isNotEmpty()) editToken = false
                }) { Text("Guardar") }
            }
        } else {
            TextButton(onClick = { editToken = true }) { Text("🔑 Cambiar contraseña") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(msgs) { m ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(m.text)
                            if (m.copiable) {
                                TextButton(onClick = { clipboard.setText(AnnotatedString(m.text)) }) {
                                    Text("Copiar")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (loading) Text("Pensando... 🤔")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe aquí...") }
            )
            Spacer(Modifier.width(8.dp))
            Button(enabled = !loading, onClick = {
                val t = input.trim()
                if (t.isNotEmpty()) {
                    if (token.isEmpty()) {
                        msgs.add(Msg("Primero guarda tu contraseña arriba 🔑", false))
                    } else {
                        msgs.add(Msg(t, true))
                        input = ""
                        loading = true
                        val snapshot = msgs.toList()
                        val tk = token
                        thread {
                            val reply = try {
                                callWorker(tk, snapshot)
                            } catch (e: Exception) {
                                "No pude conectar 😕 Revisa tu internet."
                            }
                            msgs.add(Msg(reply, false, true))
                            loading = false
                        }
                    }
                }
            }) { Text("Enviar") }
        }
    }
}
