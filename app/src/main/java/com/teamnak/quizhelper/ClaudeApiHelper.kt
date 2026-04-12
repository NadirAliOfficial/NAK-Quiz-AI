package com.teamnak.quizhelper

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ClaudeApiHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun askQuestion(apiKey: String, questionText: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val prompt = """Below is text from a quiz screen.

Step 1: Find the answer options listed on the screen.
Step 2: Pick the correct one.
Step 3: Copy its exact text — nothing else.

The answer must be copied exactly from the options. Do not write any word that is not already in the screen text.

Screen text:
$questionText"""

        val body = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("max_tokens", 40)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a quiz assistant. You MUST only reply with text that exists word-for-word in the screen text provided. Never generate new words. Always pick the correct answer from the visible options only.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val key = apiKey // Set your Groq API key in the app settings

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    onError("${response.code}: ${responseBody?.take(200) ?: "null"}")
                    return
                }
                try {
                    val answer = JSONObject(responseBody)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                    onResult(answer)
                } catch (e: Exception) {
                    onError("Parse error: ${e.message}")
                }
            }
        })
    }
}
