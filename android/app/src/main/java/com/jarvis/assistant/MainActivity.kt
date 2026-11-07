package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(JarvisBridge(), "JarvisNative")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/www/index.html")
        requestNotificationPermissionIfNeeded()
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
                    sendToPage("window.jarvisNativeVoiceError", "Voice input ended (code $error). Please try again.")
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
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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

    inner class JarvisBridge {
        @JavascriptInterface
        fun isAndroidApp(): Boolean = true

        @JavascriptInterface
        fun startVoiceRecognition(): Boolean = startNativeVoice()

        @JavascriptInterface
        fun stopVoiceRecognition() {
            runOnUiThread { stopNativeVoice() }
        }

        @JavascriptInterface
        fun startClapService(): Boolean {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41) }
                return false
            }
            val intent = Intent(this@MainActivity, ClapWakeService::class.java)
                .setAction(ClapWakeService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            return true
        }

        @JavascriptInterface
        fun stopClapService() {
            stopService(Intent(this@MainActivity, ClapWakeService::class.java))
        }
    }

    override fun onDestroy() {
        stopNativeVoice()
        webView.removeJavascriptInterface("JarvisNative")
        webView.destroy()
        super.onDestroy()
    }
}
