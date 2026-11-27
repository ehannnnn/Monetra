
package app.project.monetra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.Locale

class VoiceActivity : AppCompatActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent

    private lateinit var btnMicrophone: ImageButton
    private lateinit var tvTranscript: TextView
    private lateinit var tvListeningStatus: TextView

    private lateinit var audioVisualizer: VoiceVisualizer

    private lateinit var tvAudioLevel: TextView

    private lateinit var tvRecordingTimer: TextView

    private lateinit var btnCancelVoice: TextView

    private val timerHandler = Handler(Looper.getMainLooper())
    private var recordingStartTime = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsedSeconds =
                (SystemClock.elapsedRealtime() - recordingStartTime) / 1000

            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60

            tvRecordingTimer.text = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
            )

            timerHandler.postDelayed(this, 1000)
        }
    }

    // Meminta izin mikrofon
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {
                startListening()
            } else {
                tvListeningStatus.text = "Izin mikrofon diperlukan"
                Toast.makeText(
                    this,
                    "Izinkan akses mikrofon untuk menggunakan fitur ini",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice)

        // Menghubungkan komponen XML
        btnMicrophone = findViewById(R.id.btnMicrophone)
        tvTranscript = findViewById(R.id.tvTranscript)
        tvListeningStatus = findViewById(R.id.tvListeningStatus)
        audioVisualizer = findViewById(R.id.audioVisualizer)
        tvAudioLevel = findViewById(R.id.tvAudioLevel)
        tvRecordingTimer = findViewById(R.id.tvRecordingTimer)

        btnCancelVoice = findViewById(R.id.btnCancelVoice)

        btnCancelVoice.setOnClickListener {
            cancelListening()
        }

        // Mengatur SpeechRecognizer
        setupSpeechRecognizer()

        // Tombol mikrofon
        btnMicrophone.setOnClickListener {
            checkMicrophonePermission()
        }

        // Mengatur system bars
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    // Memeriksa izin mikrofon
    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startListening()
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }

    // Konfigurasi SpeechRecognizer
    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvListeningStatus.text =
                "Layanan pengenalan suara tidak tersedia"
            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        recognizerIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "id-ID"
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
                )
            }

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    tvListeningStatus.text =
                        "Sedang mendengarkan..."
                }

                override fun onBeginningOfSpeech() {
                    tvListeningStatus.text =
                        "Suara terdeteksi"
                    startRecordingTimer()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    audioVisualizer.updateAudioLevel(rmsdB)
                    tvAudioLevel.text = "Audio Sensitif ${rmsdB.toInt()} dB"
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    tvListeningStatus.text =
                        "Memproses suara..."
                    stopRecordingTimer()
                }

                override fun onError(error: Int) {
                    tvListeningStatus.text =
                        "Pengenalan suara berhenti"

                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        Toast.makeText(
                            this@VoiceActivity,
                            "Suara tidak berhasil dikenali. Coba lagi.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    if (!matches.isNullOrEmpty()) {
                        tvTranscript.text = matches[0]
                    }

                    tvListeningStatus.text =
                        "Selesai mengenali suara"
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    if (!matches.isNullOrEmpty()) {
                        tvTranscript.text = matches[0]
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )
    }

    // Memulai pengenalan suara
    private fun startListening() {
        if (speechRecognizer == null) {
            setupSpeechRecognizer()
        }

        tvTranscript.text = "Silakan mulai berbicara..."
        speechRecognizer?.startListening(recognizerIntent)
    }

    private fun startRecordingTimer() {
        recordingStartTime = SystemClock.elapsedRealtime()
        tvRecordingTimer.text = "00:00"

        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.post(timerRunnable)
    }

    private fun stopRecordingTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun cancelListening() {
        // Menghentikan pengenalan suara
        speechRecognizer?.cancel()

        // Menghentikan timer
        stopRecordingTimer()
        tvRecordingTimer.text = "00:00"

        // Menghapus transkripsi
        tvTranscript.text = "Silakan mulai berbicara..."

        // Mengembalikan status
        tvListeningStatus.text = "Pengenalan suara dibatalkan"

        // Mengembalikan level audio
        tvAudioLevel.text = "Audio Sensitif 0 dB"

        // Mengembalikan visualizer
        audioVisualizer.updateAudioLevel(-2f)
    }

    // Membersihkan SpeechRecognizer
    override fun onDestroy() {
        stopRecordingTimer()

        speechRecognizer?.destroy()
        speechRecognizer = null

        super.onDestroy()
    }
}
