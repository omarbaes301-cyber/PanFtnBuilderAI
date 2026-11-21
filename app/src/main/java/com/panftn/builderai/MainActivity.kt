package com.panftn.builderai

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

data class Msg(val text: String, val fromUser: Boolean, val copiable: Boolean = false)

// Paso 1: respuestas de prueba, todavía sin IA real.
const val LUA_DEMO = "-- Aqui esta, papito\n" +
    "-- Donde va: ServerScriptService > Script > HolaMundo\n" +
    "print(\"Hola desde PAN-FTN Builder AI!\")"

fun fakeReply(input: String): Msg =
    if (input.contains("script", ignoreCase = true)) Msg(LUA_DEMO, false, true)
    else Msg("¡Qué onda, compa! 😎 Aún soy una prueba sin IA real. Escribe \"script\" para ver cómo te entrego uno.", false)

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
    val msgs = remember {
        mutableStateListOf(Msg("¡Qué onda! Soy PAN-FTN Builder AI 🤙 (versión de prueba)", false))
    }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(msgs.size) { listState.animateScrollToItem(msgs.lastIndex) }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
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
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 300.dp)) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe aquí...") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val t = input.trim()
                if (t.isNotEmpty()) {
                    msgs.add(Msg(t, true))
                    msgs.add(fakeReply(t))
                    input = ""
                }
            }) { Text("Enviar") }
        }
    }
}
