package app.project.monetra.ai

import app.project.monetra.data.CategoryCatalog
import app.project.monetra.data.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiClient(
    private val apiKey: String,
    private val model: String
) {
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun interpret(text: String, now: Long = System.currentTimeMillis()): TransactionDraft? {
        if (!isConfigured()) return null

        return try {
            val connection = (URL(endpoint()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val payload = JSONObject().apply {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt(now)))
                    )
                )
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", "Kalimat pengguna: \"$text\"")
                                )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.1)
                        put("responseMimeType", "application/json")
                    }
                )
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode !in 200..299) {
                connection.errorStream?.close()
                connection.disconnect()
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val root = JSONObject(response)
            val candidates = root.optJSONArray("candidates") ?: return null
            val parts = candidates.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: return null
            val rawText = parts.optJSONObject(0)?.optString("text").orEmpty()
            if (rawText.isBlank()) return null

            parseDraft(JSONObject(stripCodeFence(rawText)), now)
        } catch (_: Exception) {
            null
        }
    }

    private fun endpoint(): String =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    private fun parseDraft(json: JSONObject, now: Long): TransactionDraft {
        val type = when (json.optString("type").uppercase(Locale.US)) {
            TransactionEntity.TYPE_INCOME -> TransactionEntity.TYPE_INCOME
            else -> TransactionEntity.TYPE_EXPENSE
        }

        val amount = json.optLong("amount", 0L).takeIf { it > 0L }
        val allowedCategories = CategoryCatalog.forType(type)
        val rawCategory = json.optString("category").trim()
        val category = rawCategory.takeIf { it in allowedCategories } ?: "Lainnya"
        val description = json.optString("description").trim().ifBlank { category }
        val dateMillis = parseDate(json.optString("date"), now)
        val confidence = json.optDouble("confidence", 0.0).toFloat()
        val note = json.optString("note").trim().ifBlank { null }
        val needsReview = amount == null || confidence < 0.5f

        return TransactionDraft(
            type = type,
            amount = amount,
            category = category,
            description = description,
            dateMillis = dateMillis,
            source = TransactionDraft.SOURCE_AI,
            needsReview = needsReview,
            note = note ?: if (amount == null) "Nominal belum jelas. Periksa sebelum menyimpan." else null
        )
    }

    private fun parseDate(raw: String, now: Long): Long {
        if (raw.isBlank()) return now
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val parsed = format.parse(raw)?.time ?: return now
            if (parsed > now) now else parsed
        } catch (_: Exception) {
            now
        }
    }

    private fun stripCodeFence(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun systemPrompt(now: Long): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        return """
            Anda adalah mesin ekstraksi transaksi keuangan pribadi berbahasa Indonesia.
            Kembalikan HANYA JSON valid, tanpa markdown dan tanpa penjelasan.

            Skema JSON:
            {
              "type": "EXPENSE" atau "INCOME",
              "amount": angka bulat rupiah (0 jika tidak jelas),
              "category": nama kategori,
              "description": deskripsi singkat,
              "date": "YYYY-MM-DD" atau "",
              "confidence": angka 0..1,
              "note": catatan singkat jika ada yang ambigu
            }

            Aturan:
            - Pahami bahasa sehari-hari: "20rb", "30 ribu", "1,5 juta", "gaji 4jt".
            - "tadi", "barusan", "hari ini" berarti hari ini. "kemarin" berarti kemarin.
            - Jangan menebak nominal jika tidak disebutkan. Isi amount 0 dan jelaskan di note.
            - Tanggal tidak boleh melewati hari ini.
            - Pilih kategori hanya dari daftar berikut.
            - Pengeluaran: ${CategoryCatalog.expense.joinToString(", ")}.
            - Pemasukan: ${CategoryCatalog.income.joinToString(", ")}.
            - Tanggal hari ini: $today.
        """.trimIndent()
    }
}
