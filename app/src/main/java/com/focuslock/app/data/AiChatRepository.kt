package com.focuslock.app.data

import com.focuslock.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ChatMessage(val role: String, val text: String) // role: "user" or "model"

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiContent(val role: String, val parts: List<GeminiPart>)

@Serializable
private data class GeminiRequest(val contents: List<GeminiContent>)

@Serializable
private data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

/**
 * Talks to the free Google Gemini API for the AI Assistant tab. The API key is injected at
 * build time via BuildConfig (see app/build.gradle.kts) from a GitHub Actions secret - it is
 * never written into any source file, so it never ends up in this public repo.
 */
class AiChatRepository {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun send(history: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No AI key is set up yet. Ask whoever maintains the app to add the GEMINI_API_KEY secret on GitHub.")
            )
        }
        try {
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20000
                readTimeout = 30000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val requestBody = GeminiRequest(
                contents = history.map { GeminiContent(role = it.role, parts = listOf(GeminiPart(it.text))) }
            )
            connection.outputStream.use { it.write(json.encodeToString(GeminiRequest.serializer(), requestBody).toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                return@withContext Result.failure(Exception("The AI service returned an error (code $code)."))
            }

            val parsed = json.decodeFromString(GeminiResponse.serializer(), responseText)
            val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                Result.failure(Exception("No response came back - try asking again."))
            } else {
                Result.success(text.trim())
            }
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't reach the AI service. Check your internet connection and try again."))
        }
    }
}
