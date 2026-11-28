package app.project.monetra.ai

import app.project.monetra.data.CategoryCatalog
import app.project.monetra.data.TransactionEntity
import java.util.Calendar
import kotlin.math.min

object RuleBasedParser {

    private data class AmountMatch(
        val value: Long,
        val hasUnit: Boolean,
        val text: String
    )

    private val amountRegex = Regex(
        """(?i)(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d+)?)\s*(rb|ribu|k|jt|juta|m)?\b"""
    )

    private val incomeWords = listOf(
        "gaji", "uang saku", "pemasukan", "dapat", "terima", "bonus", "freelance",
        "jual", "cashback", "refund", "masuk", "dividen", "bunga", "honor"
    )

    private val expenseWords = listOf(
        "beli", "bayar", "makan", "minum", "bensin", "parkir", "belanja", "tagihan",
        "pengeluaran", "keluar", "jajan", "kopi", "nasi", "ongkos", "tarif", "sewa",
        "cicilan", "listrik", "air", "kuota", "pulsa", "transport", "ojek"
    )

    private val categoryKeywords = linkedMapOf(
        "Makanan & Minuman" to listOf(
            "makan", "minum", "nasi", "kopi", "ayam", "bakso", "mie", "resto",
            "kafe", "jajan", "camilan", "roti", "padang", "sate", "soto", "martabak"
        ),
        "Transportasi" to listOf(
            "bensin", "pertalite", "parkir", "ojek", "grab", "gojek", "bus", "angkot",
            "kereta", "taksi", "transport", "motor", "mobil", "tol", "tiket"
        ),
        "Belanja" to listOf(
            "belanja", "baju", "sepatu", "skincare", "kosmetik", "shopee", "tokopedia",
            "market", "indomaret", "alfamart", "supermarket"
        ),
        "Tagihan" to listOf(
            "tagihan", "listrik", "air", "internet", "kuota", "pulsa", "kos", "sewa",
            "cicilan", "iuran", "langganan"
        ),
        "Hiburan" to listOf(
            "nonton", "film", "game", "netflix", "spotify", "hiburan", "liburan",
            "konser", "bioskop"
        ),
        "Pendidikan" to listOf(
            "buku", "kuliah", "kursus", "sekolah", "pendidikan", "print", "fotokopi",
            "semester", "ujian"
        )
    )

    fun parse(input: String, now: Long = System.currentTimeMillis()): TransactionDraft {
        val text = input.trim()
        val lower = text.lowercase()
        val amountMatches = amountRegex.findAll(text).mapNotNull { match ->
            val raw = match.groupValues[1]
            val unit = match.groupValues[2].ifBlank { null }
            parseAmount(raw, unit)?.let { value ->
                AmountMatch(value, unit != null, match.value)
            }
        }.toList()

        val amount = amountMatches
            .sortedWith(compareByDescending<AmountMatch> { it.hasUnit }.thenByDescending { it.value })
            .firstOrNull()
            ?.value

        val incomeScore = incomeWords.count { lower.contains(it) }
        val expenseScore = expenseWords.count { lower.contains(it) }
        val type = when {
            incomeScore > expenseScore -> TransactionEntity.TYPE_INCOME
            incomeScore > 0 && expenseScore == 0 -> TransactionEntity.TYPE_INCOME
            else -> TransactionEntity.TYPE_EXPENSE
        }

        val category = detectCategory(lower, type)
        val description = buildDescription(text, amountMatches, category)
        val dateMillis = parseDate(lower, now)
        val needsReview = amount == null
        val note = when {
            amount == null -> "Nominal belum jelas. Periksa sebelum menyimpan."
            else -> null
        }

        return TransactionDraft(
            type = type,
            amount = amount,
            category = category,
            description = description,
            dateMillis = dateMillis,
            source = TransactionDraft.SOURCE_RULE,
            needsReview = needsReview,
            note = note
        )
    }

    private fun parseAmount(raw: String, unit: String?): Long? {
        val normalized = normalizeNumber(raw)
        val number = normalized.toDoubleOrNull() ?: return null
        val multiplier = when (unit?.lowercase()) {
            "rb", "ribu", "k" -> 1_000L
            "jt", "juta", "m" -> 1_000_000L
            else -> 1L
        }
        val value = (number * multiplier).toLong()
        return value.takeIf { it > 0 }
    }

    private fun normalizeNumber(raw: String): String {
        val lastDot = raw.lastIndexOf('.')
        val lastComma = raw.lastIndexOf(',')

        return when {
            lastDot >= 0 && lastComma >= 0 -> {
                if (lastDot > lastComma) {
                    raw.replace(",", "")
                } else {
                    raw.replace(".", "").replace(',', '.')
                }
            }
            lastDot >= 0 -> {
                val decimals = raw.length - lastDot - 1
                if (decimals == 3) raw.replace(".", "") else raw
            }
            lastComma >= 0 -> {
                val decimals = raw.length - lastComma - 1
                if (decimals == 3) raw.replace(",", "") else raw.replace(',', '.')
            }
            else -> raw
        }
    }

    private fun detectCategory(lower: String, type: String): String {
        if (type == TransactionEntity.TYPE_INCOME) {
            return when {
                lower.contains("gaji") -> "Gaji Bulanan"
                lower.contains("freelance") || lower.contains("honor") -> "Freelance"
                else -> "Lainnya"
            }
        }

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return "Lainnya"
    }

    private fun buildDescription(
        text: String,
        amounts: List<AmountMatch>,
        category: String
    ): String {
        var description = text
        amounts.forEach { description = description.replace(it.text, " ") }

        val stopWords = listOf(
            "tadi", "barusan", "kemarin lusa", "kemarin", "hari ini", "sekarang",
            "tanggal\\s+\\d{1,2}", "rp", "beli", "bayar", "dapat", "terima"
        )
        stopWords.forEach { word ->
            description = description.replace(Regex("(?i)\\b$word\\b"), " ")
        }

        description = description.replace(Regex("\\s+"), " ").trim(' ', ',', '.', '-')
        return description.ifBlank { category }
    }

    private fun parseDate(lower: String, now: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        when {
            lower.contains("kemarin lusa") -> calendar.add(Calendar.DAY_OF_YEAR, -2)
            lower.contains("kemarin") -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            lower.contains("minggu lalu") -> calendar.add(Calendar.DAY_OF_YEAR, -7)
        }

        val dayMatch = Regex("""(?i)tanggal\s+(\d{1,2})""").find(lower)
        if (dayMatch != null) {
            val requestedDay = dayMatch.groupValues[1].toIntOrNull() ?: return calendar.timeInMillis
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
            }
            candidate.set(
                Calendar.DAY_OF_MONTH,
                min(requestedDay, candidate.getActualMaximum(Calendar.DAY_OF_MONTH))
            )
            if (candidate.timeInMillis > now) {
                candidate.add(Calendar.MONTH, -1)
            }
            return candidate.timeInMillis
        }

        return calendar.timeInMillis
    }

    fun categoriesFor(type: String): List<String> = CategoryCatalog.forType(type)
}
