package app.project.monetra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.project.monetra.ai.GeminiClient
import app.project.monetra.ai.TransactionDraft
import app.project.monetra.ai.TransactionInterpreter
import app.project.monetra.data.CategoryCatalog
import app.project.monetra.data.TransactionRepository

class ChatActivity : AppCompatActivity() {

    private lateinit var etTransaction: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var cardAiProcessing: android.view.View
    private lateinit var cardVoiceError: android.view.View
    private lateinit var tvVoiceError: TextView
    private lateinit var tvAiProcessing: TextView

    private lateinit var repository: TransactionRepository
    private lateinit var interpreter: TransactionInterpreter

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val transcript = result.data?.getStringExtra(VoiceActivity.EXTRA_TRANSCRIPT).orEmpty()
            if (transcript.isNotBlank()) {
                etTransaction.setText(transcript)
                submitInput(fromVoice = true)
            }
        } else if (result.data?.getBooleanExtra(VoiceActivity.EXTRA_OPEN_MANUAL, false) == true) {
            showManualSheet()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchVoice()
        } else {
            showVoiceError("Izin mikrofon diperlukan untuk mencatat lewat suara.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = TransactionRepository(this)
        interpreter = TransactionInterpreter(
            GeminiClient(BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL)
        )

        bindViews()
        bindActions()
    }

    private fun bindViews() {
        etTransaction = findViewById(R.id.etTransaction)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        cardAiProcessing = findViewById(R.id.cardAiProcessing)
        cardVoiceError = findViewById(R.id.cardVoiceError)
        tvVoiceError = findViewById(R.id.tvVoiceError)
        tvAiProcessing = findViewById(R.id.tvAiProcessing)

        findViewById<TextView>(R.id.tvAiMode).text = if (interpreter.isAiConfigured()) {
            "Gemini siap mengenali nominal & kategori otomatis."
        } else {
            "Mode dasar aktif. Tambahkan GEMINI_API_KEY untuk mengaktifkan AI."
        }
    }

    private fun bindActions() {
        findViewById<android.view.View>(R.id.example1).setOnClickListener {
            fillExample("nasi padang 20rb")
        }
        findViewById<android.view.View>(R.id.example2).setOnClickListener {
            fillExample("tadi beli bensin 30 ribu")
        }
        findViewById<android.view.View>(R.id.example3).setOnClickListener {
            fillExample("dapat uang saku 500 ribu")
        }

        btnSend.setOnClickListener { submitInput() }
        btnMic.setOnClickListener { checkMicrophonePermission() }
        findViewById<ImageButton>(R.id.btnManual).setOnClickListener { showManualSheet() }
        findViewById<TextView>(R.id.btnOpenSettings).setOnClickListener { openAppSettings() }

        etTransaction.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitInput()
                true
            } else {
                false
            }
        }

        findViewById<android.view.View>(R.id.navBeranda).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navStatistik).setOnClickListener {
            Toast.makeText(this, "Statistik segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.navProfil).setOnClickListener {
            Toast.makeText(this, "Profil segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnSearch).setOnClickListener {
            Toast.makeText(this, "Pencarian segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnNotification).setOnClickListener {
            Toast.makeText(this, "Belum ada notifikasi baru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fillExample(text: String) {
        etTransaction.setText(text)
        etTransaction.setSelection(text.length)
        etTransaction.requestFocus()
    }

    private fun submitInput(fromVoice: Boolean = false) {
        val text = etTransaction.text.toString().trim()
        if (text.isBlank()) {
            etTransaction.error = "Tulis transaksi dulu"
            etTransaction.requestFocus()
            return
        }

        hideKeyboard()
        setLoading(true)
        cardVoiceError.visibility = android.view.View.GONE
        tvAiProcessing.text = if (interpreter.isAiConfigured()) {
            "Gemini sedang mengekstrak nominal, kategori, dan tanggal."
        } else {
            "Parser dasar sedang membaca nominal, kategori, dan tanggal."
        }

        interpreter.interpret(text) { draft ->
            setLoading(false)
            showConfirmation(draft, fromVoice)
        }
    }

    private fun showConfirmation(draft: TransactionDraft, fromVoice: Boolean = false) {
        val inputLabel = when {
            draft.source == TransactionDraft.SOURCE_MANUAL -> "Manual"
            draft.source == TransactionDraft.SOURCE_AI && fromVoice -> "AI · suara"
            draft.source == TransactionDraft.SOURCE_AI -> "AI · teks"
            fromVoice -> "Mode dasar · suara"
            else -> "Mode dasar"
        }

        TransactionConfirmationSheet(this, draft, inputLabel) { transaction ->
            repository.insert(transaction) {
                etTransaction.setText("")
                Toast.makeText(this, "Transaksi masuk ke catatan", Toast.LENGTH_SHORT).show()
            }
        }.show()
    }

    private fun showManualSheet() {
        val draft = TransactionDraft(
            type = app.project.monetra.data.TransactionEntity.TYPE_EXPENSE,
            category = CategoryCatalog.expense.first(),
            description = "",
            dateMillis = System.currentTimeMillis(),
            source = TransactionDraft.SOURCE_MANUAL
        )
        showConfirmation(draft)
    }

    private fun setLoading(loading: Boolean) {
        cardAiProcessing.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        btnSend.isEnabled = !loading
        btnMic.isEnabled = !loading
        etTransaction.isEnabled = !loading
    }

    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> launchVoice()

            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchVoice() {
        cardVoiceError.visibility = android.view.View.GONE
        voiceLauncher.launch(Intent(this, VoiceActivity::class.java))
    }

    private fun showVoiceError(message: String) {
        tvVoiceError.text = message
        cardVoiceError.visibility = android.view.View.VISIBLE
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(etTransaction.windowToken, 0)
    }
}
