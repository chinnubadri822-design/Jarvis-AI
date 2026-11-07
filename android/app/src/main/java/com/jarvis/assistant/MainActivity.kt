package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var speechRecognizer: SpeechRecognizer? = null
    private var pageReady = false
    private var pendingWake = false
    private lateinit var localAi: LocalAiEngine
    private val modelPickerRequest = 73

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingWake = intent?.getBooleanExtra(HeyJarvisWakeService.EXTRA_WAKE_DETECTED, false) == true
        localAi = LocalAiEngine(this,
            onState = { sendToPage("window.jarvisLocalAiState", it) },
            onReply = { sendToPage("window.jarvisLocalAiReply", it) },
            onError = { sendToPage("window.jarvisLocalAiError", it) }
        )
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                if (pendingWake) deliverWakeToPage()
            }
        }
        webView.addJavascriptInterface(JarvisBridge(), "JarvisNative")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/www/index.html")
        requestNotificationPermissionIfNeeded()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra(HeyJarvisWakeService.EXTRA_WAKE_DETECTED, false) == true) {
            pendingWake = true
            if (pageReady) deliverWakeToPage()
        }
    }

    private fun deliverWakeToPage() {
        pendingWake = false
        stopService(Intent(this, HeyJarvisWakeService::class.java))
        webView.evaluateJavascript(
            "window.jarvisWakeDetected && window.jarvisWakeDetected()",
            null
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    private fun sendToPage(functionName: String, value: String) {
        val safeValue = JSONObject.quote(value)
        runOnUiThread { webView.evaluateJavascript("$functionName($safeValue)", null) }
    }

    private fun startNativeVoice(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41) }
            return false
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendToPage("window.jarvisNativeVoiceError", "No speech recognition service is installed.")
            return false
        }
        runOnUiThread {
            stopNativeVoice()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    sendToPage("window.jarvisNativeVoiceState", "Listening. Speak now.")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    sendToPage("window.jarvisNativeVoiceState", "Processing speech…")
                }
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Speak clearly after the listening indicator appears, then try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Tap the microphone and speak right away."
                        SpeechRecognizer.ERROR_AUDIO -> "The microphone audio failed. Close other apps using the microphone and try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Wait a moment and try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required. Allow it in Android app settings."
                        else -> "Speech recognition stopped (code $error). Please try again."
                    }
                    sendToPage("window.jarvisNativeVoiceError", message)
                    stopNativeVoice()
                }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val phrase = words?.firstOrNull().orEmpty()
                    if (phrase.isNotBlank()) sendToPage("window.jarvisNativeVoiceResult", phrase)
                    else sendToPage("window.jarvisNativeVoiceError", "I couldn't hear a clear command. Please try again.")
                    stopNativeVoice()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                // Allow Android's installed recognition provider when an offline language pack is unavailable.
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            speechRecognizer?.startListening(intent)
        }
        return true
    }

    private fun stopNativeVoice() {
        speechRecognizer?.let {
            try { it.cancel() } catch (_: Exception) {}
            it.destroy()
        }
        speechRecognizer = null
    }

    private fun hasMicPermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun chooseLocalModel() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(picker, modelPickerRequest)
    }

    @Deprecated("Deprecated in Android but retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != modelPickerRequest || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                .orEmpty()
            if (!displayName.endsWith(".litertlm", ignoreCase = true)) {
                sendToPage("window.jarvisLocalAiError", "Choose a .litertlm model file.")
                return
            }
            sendToPage("window.jarvisLocalAiState", "Copying model into private app storage…")
            val destination = File(filesDir, "jarvis-model.litertlm")
            contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Could not read the selected file.")
            localAi.load(destination)
        } catch (error: Exception) {
            sendToPage("window.jarvisLocalAiError", "Could not import model: " + (error.message ?: "unknown error"))
        }
    }

    inner class JarvisBridge {
        @JavascriptInterface
        fun isAndroidApp(): Boolean = true

        @JavascriptInterface
        fun chooseLocalModel() {
            runOnUiThread { this@MainActivity.chooseLocalModel() }
        }

        @JavascriptInterface
        fun isLocalAiLoaded(): Boolean = localAi.isLoaded()

        @JavascriptInterface
        fun askLocalAI(prompt: String): Boolean {
            if (!localAi.isLoaded()) return false
            localAi.ask(prompt)
            return true
        }

        @JavascriptInterface
        fun startVoiceRecognition(): Boolean = startNativeVoice()

        @JavascriptInterface
        fun stopVoiceRecognition() {
            runOnUiThread { stopNativeVoice() }
        }

        @JavascriptInterface
        fun startClapService(): Boolean {
            if (!hasMicPermission()) {
                runOnUiThread { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41) }
                return false
            }
            stopService(Intent(this@MainActivity, HeyJarvisWakeService::class.java))
            val intent = Intent(this@MainActivity, ClapWakeService::class.java)
                .setAction(ClapWakeService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            return true
        }

        @JavascriptInterface
        fun stopClapService() {
            stopService(Intent(this@MainActivity, ClapWakeService::class.java))
        }

        @JavascriptInterface
        fun startHeyJarvisService(): Boolean {
            if (!hasMicPermission()) {
                runOnUiThread { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41) }
                return false
            }
            stopService(Intent(this@MainActivity, ClapWakeService::class.java))
            val intent = Intent(this@MainActivity, HeyJarvisWakeService::class.java)
                .setAction(HeyJarvisWakeService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            return true
        }

        @JavascriptInterface
        fun stopHeyJarvisService() {
            stopService(Intent(this@MainActivity, HeyJarvisWakeService::class.java))
        }
    }

    override fun onDestroy() {
        localAi.close()
        stopNativeVoice()
        webView.removeJavascriptInterface("JarvisNative")
        webView.destroy()
        super.onDestroy()
    }
}
