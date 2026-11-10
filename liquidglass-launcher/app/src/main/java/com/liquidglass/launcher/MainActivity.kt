package com.liquidglass.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Navy = Color(0xFF050B1C)
private val Cyan = Color(0xFF37D9FF)
private val Violet = Color(0xFF9B65FF)
private val Glass = Color(0xAA172746)
private const val ASSISTANT_BACKEND_URL = "https://SEU-SERVIDOR.example.com/api/assistant"

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechCallback: ((String) -> Unit)? = null
    private val askAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecognition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setContent { LiquidGlassHome(onSpeak = { speak(it) }, onListen = { callback ->
            speechCallback = callback
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoiceRecognition()
            else askAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }) }
    }

    private fun startVoiceRecognition() {
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga um comando")
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) { results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { speechCallback?.invoke(it) } }
            override fun onError(error: Int) { speechCallback?.invoke("") }
            override fun onReadyForSpeech(params: Bundle?) {}; override fun onBeginningOfSpeech() {}; override fun onRmsChanged(rmsdB: Float) {}; override fun onBufferReceived(buffer: ByteArray?) {}; override fun onEndOfSpeech() {}; override fun onPartialResults(partialResults: Bundle?) {}; override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "liquidglass-response") }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("pt", "BR") }
    override fun onDestroy() { speechRecognizer?.destroy(); tts?.shutdown(); super.onDestroy() }
}

private data class TvApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable?)

@Composable
private fun LiquidGlassHome(onSpeak: (String) -> Unit, onListen: (((String) -> Unit) -> Unit)) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf(emptyList<TvApp>()) }
    var assistantText by remember { mutableStateOf("Olá. Sou seu assistente. Pressione o microfone para falar comigo.") }
    var listening by remember { mutableStateOf(false) }
    var showAssistant by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .mapNotNull { info ->
                    val pkg = info.activityInfo.packageName
                    if (pkg == context.packageName) null else TvApp(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm))
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF06132C), Color(0xFF11113A), Color(0xFF070B1C))))) {
        // Decorative ambient light layers.
        Box(Modifier.fillMaxWidth().height(280.dp).align(Alignment.TopEnd).background(Brush.radialGradient(listOf(Color(0x663E8DFF), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Início", "Apps", "Jogos", "Arquivos", "Desenvolvimento", "Configurações").forEachIndexed { i, label ->
                    GlassNavItem(label, i == 0, when(i) { 0 -> Icons.Default.Home; 1 -> Icons.Default.GridView; 2 -> Icons.Default.SportsEsports; 3 -> Icons.Default.Folder; 4 -> Icons.Default.Code; else -> Icons.Default.Settings })
                }
                Spacer(Modifier.weight(1f))
                IconButtonGlass(Icons.Default.Search, "Pesquisar") { showAssistant = true }
                IconButtonGlass(Icons.Default.Mic, "Assistente por voz") {
                    listening = true; showAssistant = true
                    onListen { phrase ->
                        listening = false
                        if (phrase.isNotBlank()) {
                            val normalized = phrase.lowercase(Locale("pt", "BR"))
                            val requestedApp = if (normalized.startsWith("abrir ") || normalized.startsWith("abra ")) {
                                installedApps.firstOrNull { normalized.contains(it.label.lowercase(Locale("pt", "BR"))) }
                            } else null
                            if (requestedApp != null) {
                                context.packageManager.getLaunchIntentForPackage(requestedApp.packageName)?.let(context::startActivity)
                                assistantText = "Abrindo ${requestedApp.label}."
                                onSpeak(assistantText)
                            } else {
                                assistantText = "Você: $phrase"
                                loading = true
                                scope.launch {
                                    val answer = withContext(Dispatchers.IO) { requestAssistant(phrase) }
                                    assistantText = answer
                                    loading = false
                                    onSpeak(answer)
                                }
                            }
                        }
                    }
                }
                GlassPill("20:00", Modifier.widthIn(min = 78.dp))
            }

            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Boa noite,", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light)
                    Text("Programador", color = Cyan, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text("Seu centro de entretenimento e desenvolvimento", color = Color(0xFFD3DDF4), fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    GlassPill("◉  Assistente Liquid Glass  •  IA por voz")
                }
                Column(Modifier.weight(1.1f).padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SEUS APLICATIVOS", color = Color(0xFFB8C9E8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Acesse seus aplicativos favoritos com um único comando.", color = Color.White, fontSize = 21.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aplicativos", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${installedApps.size} instalados", color = Color(0xFF9EB1D1), fontSize = 13.sp)
            }
            if (installedApps.isEmpty()) {
                GlassPanel(Modifier.fillMaxWidth().height(118.dp)) { Text("Carregando aplicativos instalados…", color = Color.White) }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(installedApps.take(12), key = { it.packageName }) { app ->
                        AppCard(app) {
                            val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launch != null) context.startActivity(launch)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CategoryCard("Desenvolvimento", "Ferramentas e utilitários", Icons.Default.Code, Modifier.weight(1f))
                CategoryCard("Entretenimento", "Filmes, séries e música", Icons.Default.Movie, Modifier.weight(1f))
                CategoryCard("Configurações", "Preferências do launcher", Icons.Default.Tune, Modifier.weight(1f))
            }
        }

        if (showAssistant) {
            GlassPanel(Modifier.align(Alignment.BottomEnd).padding(34.dp).width(430.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Cyan)
                    Spacer(Modifier.width(10.dp)); Text("Assistente", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f)); TextButton(onClick = { showAssistant = false }) { Text("Fechar") }
                }
                Text(if (listening) "Ouvindo…" else assistantText, color = Color(0xFFE5EDFF), fontSize = 15.sp)
                if (loading) Text("A conexão com o backend de IA será ativada após configurar a URL do servidor.", color = Color(0xFF9EB1D1), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onListen { phrase ->
                        listening = false
                        if (phrase.isNotBlank()) {
                            assistantText = "Você: $phrase"
                            loading = true
                            scope.launch {
                                val answer = withContext(Dispatchers.IO) { requestAssistant(phrase) }
                                assistantText = answer
                                loading = false
                                onSpeak(answer)
                            }
                        }
                    } }) { Icon(Icons.Default.Mic, null); Text(" Falar") }
                    OutlinedButton(onClick = { onSpeak("Olá. O assistente de voz está funcionando.") }) { Icon(Icons.Default.VolumeUp, null); Text(" Testar voz") }
                }
            }
        }
    }
}

private fun requestAssistant(message: String): String {
    return try {
        val connection = (URL(ASSISTANT_BACKEND_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        connection.outputStream.use { it.write(JSONObject().put("message", message).toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        if (connection.responseCode in 200..299) json.optString("answer", "Não recebi uma resposta válida.")
        else json.optString("error", "O servidor de IA retornou um erro.")
    } catch (_: Exception) {
        "Não foi possível conectar ao assistente. Configure o endereço HTTPS do backend no projeto Android."
    }
}

@Composable private fun GlassNavItem(label: String, selected: Boolean, icon: ImageVector) {
    var focused by remember { mutableStateOf(false) }
    val border = if (selected || focused) Cyan.copy(alpha = .8f) else Color.White.copy(alpha = .13f)
    Row(Modifier.padding(2.dp).background(if (selected || focused) Brush.horizontalGradient(listOf(Color(0xAA147AC0), Color(0x663E4A91))) else Brush.horizontalGradient(listOf(Color(0x661A2A48), Color(0x44111B32))), RoundedCornerShape(30.dp)).border(1.dp, border, RoundedCornerShape(30.dp)).onFocusChanged { focused = it.isFocused }.focusable().padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(label, color = Color.White, fontSize = 13.sp)
    }
}

@Composable private fun IconButtonGlass(icon: ImageVector, description: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(onClick = onClick, modifier = Modifier.size(44.dp).onFocusChanged { focused = it.isFocused }, shape = CircleShape, color = if (focused) Color(0xAA147AC0) else Glass, border = BorderStroke(1.dp, Color.White.copy(alpha = .22f))) { Box(contentAlignment = Alignment.Center) { Icon(icon, description, tint = Color.White) } }
}

@Composable private fun GlassPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(40.dp), color = Glass, border = BorderStroke(1.dp, Color.White.copy(alpha = .2f))) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.White, fontSize = 13.sp) }
}

@Composable private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.shadow(18.dp, RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xCC253D63), Color(0xAA111D38))), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable private fun AppCard(app: TvApp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "focus-scale")
    val stroke by animateColorAsState(if (focused) Cyan else Color.White.copy(alpha = .2f), label = "focus-border")
    Surface(onClick = onClick, modifier = Modifier.width(148.dp).height(118.dp).scale(scale).onFocusChanged { focused = it.isFocused }, shape = RoundedCornerShape(22.dp), color = Color(0xAA172743), border = BorderStroke(if (focused) 2.dp else 1.dp, stroke), shadowElevation = if (focused) 18.dp else 4.dp) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(34.dp).background(Brush.linearGradient(listOf(Cyan.copy(alpha = .8f), Violet.copy(alpha = .8f))), CircleShape), contentAlignment = Alignment.Center) { Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            Text(app.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text("Abrir", color = if (focused) Cyan else Color(0xFFAABBD7), fontSize = 11.sp)
        }
    }
}

@Composable private fun CategoryCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassPanel(modifier.height(88.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Cyan, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(12.dp)); Column { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Color(0xFFB8C9E8), fontSize = 11.sp) } }
    }
}
