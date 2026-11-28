package app.project.monetra

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import app.project.monetra.ai.TransactionDraft
import app.project.monetra.data.CategoryCatalog
import app.project.monetra.data.FormatUtils
import app.project.monetra.data.TransactionEntity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class TransactionConfirmationSheet(
    private val context: Context,
    private val draft: TransactionDraft,
    private val inputLabel: String? = null,
    private val onSave: (TransactionEntity) -> Unit
) {
    private var selectedType: String = draft.type
    private var selectedCategory: String =
        draft.category?.takeIf { it in CategoryCatalog.forType(selectedType) }
            ?: CategoryCatalog.forType(selectedType).first()
    private var selectedDate: Long = draft.dateMillis

    private lateinit var dialog: BottomSheetDialog
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnExpense: TextView
    private lateinit var btnIncome: TextView
    private lateinit var btnCategory: TextView
    private lateinit var btnDate: TextView
    private lateinit var tvCharCount: TextView
    private lateinit var tvReviewNote: TextView

    fun show() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.sheet_transaction_confirmation, null)

        dialog = BottomSheetDialog(context).apply {
            setContentView(view)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        bindViews(view)
        renderDraft()
        bindActions()
        dialog.show()
    }

    private fun bindViews(view: View) {
        etAmount = view.findViewById(R.id.etAmount)
        etDescription = view.findViewById(R.id.etDescription)
        btnExpense = view.findViewById(R.id.btnExpense)
        btnIncome = view.findViewById(R.id.btnIncome)
        btnCategory = view.findViewById(R.id.btnCategory)
        btnDate = view.findViewById(R.id.btnDate)
        tvCharCount = view.findViewById(R.id.tvCharCount)
        tvReviewNote = view.findViewById(R.id.tvReviewNote)
    }

    private fun renderDraft() {
        val sourceLabel = inputLabel ?: when (draft.source) {
            TransactionDraft.SOURCE_AI -> "AI · teks"
            TransactionDraft.SOURCE_MANUAL -> "Manual"
            else -> "Mode dasar"
        }
        dialog.findViewById<TextView>(R.id.tvSourceBadge)?.text = sourceLabel
        dialog.findViewById<TextView>(R.id.tvSheetSubtitle)?.text =
            if (draft.source == TransactionDraft.SOURCE_MANUAL) {
                "Isi form transaksi baru."
            } else {
                "Monetra sudah mengisi form dari teks kamu."
            }

        etAmount.setText(draft.amount?.toString().orEmpty())
        etDescription.setText(draft.description)
        updateTypeButtons()
        updateCategory()
        updateDate()
        updateCharCount()

        if (draft.needsReview || !draft.note.isNullOrBlank()) {
            tvReviewNote.visibility = View.VISIBLE
            tvReviewNote.text = draft.note ?: "Periksa kembali data sebelum menyimpan."
        } else {
            tvReviewNote.visibility = View.GONE
        }
    }

    private fun bindActions() {
        btnExpense.setOnClickListener {
            selectedType = TransactionEntity.TYPE_EXPENSE
            ensureCategoryForType()
            updateTypeButtons()
            updateCategory()
        }

        btnIncome.setOnClickListener {
            selectedType = TransactionEntity.TYPE_INCOME
            ensureCategoryForType()
            updateTypeButtons()
            updateCategory()
        }

        btnCategory.setOnClickListener {
            val categories = CategoryCatalog.forType(selectedType)
            AlertDialog.Builder(context)
                .setTitle("Pilih kategori")
                .setItems(categories.toTypedArray()) { _, which ->
                    selectedCategory = categories[which]
                    updateCategory()
                }
                .show()
        }

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val picker = DatePickerDialog(
                context,
                { _, year, month, day ->
                    selectedDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    updateDate()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.maxDate = System.currentTimeMillis()
            picker.show()
        }

        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateCharCount()
        })

        dialog.findViewById<TextView>(R.id.btnCancel)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.btnSave)?.setOnClickListener { save() }
    }

    private fun ensureCategoryForType() {
        val categories = CategoryCatalog.forType(selectedType)
        if (selectedCategory !in categories) {
            selectedCategory = categories.first()
        }
    }

    private fun updateTypeButtons() {
        if (selectedType == TransactionEntity.TYPE_INCOME) {
            btnIncome.setBackgroundResource(R.drawable.bg_card_income)
            btnIncome.setTextColor(ContextCompat.getColor(context, R.color.monetra_black))
            btnExpense.setBackgroundResource(R.drawable.bg_brutal_button_white)
            btnExpense.setTextColor(ContextCompat.getColor(context, R.color.monetra_black))
        } else {
            btnExpense.setBackgroundResource(R.drawable.bg_brutal_button_red)
            btnExpense.setTextColor(ContextCompat.getColor(context, R.color.monetra_white))
            btnIncome.setBackgroundResource(R.drawable.bg_brutal_button_white)
            btnIncome.setTextColor(ContextCompat.getColor(context, R.color.monetra_black))
        }
    }

    private fun updateCategory() {
        btnCategory.text = selectedCategory
    }

    private fun updateDate() {
        btnDate.text = FormatUtils.date(selectedDate)
    }

    private fun updateCharCount() {
        tvCharCount.text = "${etDescription.text.length}/100"
    }

    private fun save() {
        val amount = etAmount.text.toString().trim().toLongOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Nominal wajib diisi"
            etAmount.requestFocus()
            return
        }

        val description = etDescription.text.toString().trim().ifBlank { selectedCategory }
        val transaction = TransactionEntity(
            type = selectedType,
            amount = amount,
            category = selectedCategory,
            description = description,
            dateMillis = selectedDate,
            source = draft.source
        )

        onSave(transaction)
        dialog.dismiss()
    }
}
