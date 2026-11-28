package app.project.monetra.ai

import app.project.monetra.data.TransactionEntity

data class TransactionDraft(
    val type: String = TransactionEntity.TYPE_EXPENSE,
    val amount: Long? = null,
    val category: String? = null,
    val description: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val source: String = SOURCE_RULE,
    val needsReview: Boolean = false,
    val note: String? = null
) {
    companion object {
        const val SOURCE_AI = "AI"
        const val SOURCE_RULE = "RULE"
        const val SOURCE_MANUAL = "MANUAL"
    }
}
