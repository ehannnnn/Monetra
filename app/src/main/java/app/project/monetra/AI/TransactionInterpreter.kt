package app.project.monetra.ai

import android.os.Handler
import android.os.Looper
import app.project.monetra.data.FormatUtils
import java.util.concurrent.Executors

class TransactionInterpreter(
    private val geminiClient: GeminiClient
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isAiConfigured(): Boolean = geminiClient.isConfigured()

    fun interpret(text: String, onResult: (TransactionDraft) -> Unit) {
        executor.execute {
            val ruleDraft = RuleBasedParser.parse(text)
            val aiDraft = if (geminiClient.isConfigured()) {
                geminiClient.interpret(text)
            } else {
                null
            }

            val result = if (aiDraft != null) reconcile(aiDraft, ruleDraft) else ruleDraft
            mainHandler.post { onResult(result) }
        }
    }

    private fun reconcile(aiDraft: TransactionDraft, ruleDraft: TransactionDraft): TransactionDraft {
        var needsReview = aiDraft.needsReview
        var note = aiDraft.note

        if (aiDraft.amount != null && ruleDraft.amount != null && aiDraft.amount != ruleDraft.amount) {
            needsReview = true
            note = "Periksa nominal: AI membaca ${FormatUtils.rupiah(aiDraft.amount)}, " +
                    "parser membaca ${FormatUtils.rupiah(ruleDraft.amount)}."
        }

        if (aiDraft.amount == null) {
            needsReview = true
            note = note ?: "Nominal belum jelas. Periksa sebelum menyimpan."
        }

        return aiDraft.copy(needsReview = needsReview, note = note)
    }
}
