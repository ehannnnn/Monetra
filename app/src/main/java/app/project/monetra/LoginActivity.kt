package app.project.monetra

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import android.widget.ImageButton
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.os.Handler
import android.os.Looper
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
class LoginActivity : AppCompatActivity() {

    private val pinInput = StringBuilder()

    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var isBackspaceHeld = false

    private val backspaceRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceHeld) {
                deletePinDigit()
                backspaceHandler.postDelayed(this, 100)
            }
        }
    }

    private lateinit var pinDots: List<View>
    private lateinit var errorLayout: View
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupKeypad()
        setupUnlockButton()
        setupFingerprint()

        updatePinIndicators()
    }

    private fun setupFingerprint() {
        val fingerprintButton =
            findViewById<ImageButton>(R.id.btnKeyFingerprint)

        applyPressAnimation(fingerprintButton)

        fingerprintButton.setOnClickListener {
            val biometricManager = BiometricManager.from(this)

            when (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    showBiometricPrompt()
                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Toast.makeText(
                        this,
                        "Perangkat tidak memiliki sensor biometrik.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(
                        this,
                        "Daftarkan sidik jari terlebih dahulu di pengaturan perangkat.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    Toast.makeText(
                        this,
                        "Fingerprint tidak tersedia saat ini.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    Toast.makeText(
                        this@LoginActivity,
                        "Autentikasi biometrik berhasil!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Toast.makeText(
                        this@LoginActivity,
                        "Sidik jari tidak cocok. Coba lagi.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    if (
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(
                            this@LoginActivity,
                            errString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Monetra")
            .setSubtitle("Verifikasi identitas menggunakan biometrik")
            .setNegativeButtonText("Batal")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun initViews() {
        pinDots = listOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4),
            findViewById(R.id.pinDot5),
            findViewById(R.id.pinDot6)
        )

        errorLayout = findViewById(R.id.layoutPinError)
        errorText = findViewById(R.id.tvErrorText)
    }

    private fun setupKeypad() {
        val numberButtons = mapOf(
            R.id.btnKey0 to "0",
            R.id.btnKey1 to "1",
            R.id.btnKey2 to "2",
            R.id.btnKey3 to "3",
            R.id.btnKey4 to "4",
            R.id.btnKey5 to "5",
            R.id.btnKey6 to "6",
            R.id.btnKey7 to "7",
            R.id.btnKey8 to "8",
            R.id.btnKey9 to "9"
        )

        numberButtons.forEach { (buttonId, number) ->
            val button = findViewById<AppCompatButton>(buttonId)

            button.setOnClickListener {
                addPinDigit(number)
            }

            applyPressAnimation(button)
        }

        val backspaceButton = findViewById<View>(R.id.btnKeyBackspace)

        backspaceButton.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    isBackspaceHeld = true
                    deletePinDigit()

                    backspaceHandler.postDelayed(backspaceRunnable, 400)
                    view.isPressed = true
                    true
                }

                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    isBackspaceHeld = false
                    backspaceHandler.removeCallbacks(backspaceRunnable)
                    view.isPressed = false
                    view.performClick()
                    true
                }

                else -> true
            }
        }

        val offset = 3f * resources.displayMetrics.density

        backspaceButton.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    isBackspaceHeld = true
                    deletePinDigit()
                    backspaceHandler.postDelayed(backspaceRunnable, 400)

                    view.animate().translationX(offset).translationY(offset)
                        .setDuration(80).start()
                    view.isPressed = true
                    true
                }

                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    isBackspaceHeld = false
                    backspaceHandler.removeCallbacks(backspaceRunnable)

                    view.animate().translationX(0f).translationY(0f)
                        .setDuration(100).start()
                    view.isPressed = false
                    view.performClick()
                    true
                }

                else -> true
            }
        }
    }

    private fun addPinDigit(number: String) {
        if (pinInput.length >= 6) return

        pinInput.append(number)

        hideError()
        updatePinIndicators()
    }

    private fun deletePinDigit() {
        if (pinInput.isEmpty()) return

        pinInput.deleteCharAt(pinInput.length - 1)

        hideError()
        updatePinIndicators()
    }

    private fun updatePinIndicators() {
        pinDots.forEachIndexed { index, dot ->
            if (index < pinInput.length) {
                if (dot.visibility != View.VISIBLE) {
                    dot.visibility = View.VISIBLE

                    dot.layoutParams = dot.layoutParams.apply {
                        width = (14 * resources.displayMetrics.density).toInt()
                        height = (14 * resources.displayMetrics.density).toInt()
                    }

                    ValueAnimator.ofInt(
                        (14 * resources.displayMetrics.density).toInt(),
                        (8 * resources.displayMetrics.density).toInt()
                    ).apply {
                        duration = 250
                        interpolator = OvershootInterpolator(1.5f)

                        addUpdateListener { animator ->
                            val size = animator.animatedValue as Int
                            dot.layoutParams = dot.layoutParams.apply {
                                width = size
                                height = size
                            }
                        }

                        start()
                    }
                }
            } else {
                dot.visibility = View.GONE
            }
        }
    }

    private fun applyPressAnimation(view: View) {
        val offset = 3f * resources.displayMetrics.density

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .translationX(offset)
                        .translationY(offset)
                        .setDuration(80)
                        .start()
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .setDuration(100)
                        .start()
                }
            }

            false
        }
    }

    private fun setupUnlockButton() {
        findViewById<AppCompatButton>(R.id.btnUnlock).setOnClickListener {
            if (pinInput.length < 6) {
                showError("Masukkan PIN lengkap 6 digit.")
            } else {
                showError("PIN sudah lengkap. Verifikasi belum diaktifkan.")
            }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorLayout.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorLayout.visibility = View.GONE
    }
}
