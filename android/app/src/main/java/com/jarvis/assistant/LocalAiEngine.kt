package com.jarvis.assistant

import android.content.Context
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Owns the on-device LiteRT-LM engine and conversation on one background thread.
 * The model file is selected by the user and stored inside the app's private files directory.
 */
class LocalAiEngine(
    context: Context,
    private val onState: (String) -> Unit,
    private val onReply: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var modelFile: File? = null

    fun load(file: File) {
        executor.execute {
            closeModel()
            if (!file.exists() || file.length() < 1024L || !file.extension.equals("litertlm", true)) {
                onError("Please select a valid .litertlm model file.")
                return@execute
            }
            onState("Loading local model… this can take a while.")
            var candidate: Engine? = null
            try {
                candidate = Engine(
                    EngineConfig(
                        modelPath = file.absolutePath,
                        cacheDir = appContext.cacheDir.absolutePath
                    )
                )
                candidate.initialize()
                val chat = candidate.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(
                            "You are JARVIS, a helpful, respectful, concise personal assistant. " +
                            "Answer clearly, admit uncertainty, and never claim to perform device actions " +
                            "unless the app explicitly confirms them. You run locally on the user's device."
                        )
                    )
                )
                engine = candidate
                conversation = chat
                modelFile = file
                onState("LOCAL AI ONLINE")
            } catch (error: Exception) {
                try { candidate?.close() } catch (_: Exception) {}
                onError("Could not load this model: ${error.message ?: "unsupported model or device"}.")
            }
        }
    }

    fun ask(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        executor.execute {
            val chat = conversation
            if (chat == null) {
                onError("Load a .litertlm model first to enable offline AI.")
                return@execute
            }
            onState("Thinking on this device…")
            try {
                val response = chat.sendMessage(clean)
                onReply(response.text.ifBlank { "I couldn't create a response. Please try again." })
                onState("LOCAL AI ONLINE")
            } catch (error: Exception) {
                onError("Local model error: ${error.message ?: "please try again"}.")
                onState("LOCAL AI READY")
            }
        }
    }

    fun isLoaded(): Boolean = conversation != null

    private fun closeModel() {
        try { conversation?.close() } catch (_: Exception) {}
        conversation = null
        try { engine?.close() } catch (_: Exception) {}
        engine = null
        modelFile = null
    }

    fun close() {
        executor.execute { closeModel() }
        executor.shutdown()
    }
}
