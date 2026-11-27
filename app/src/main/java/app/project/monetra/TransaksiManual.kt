package app.project.monetra

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.TextViewCompat
import java.io.File
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransaksiManual : AppCompatActivity() {

    private var isExpenseMode = true
    private var selectedCategoryView: LinearLayout? = null
    private var selectedCategoryText: TextView? = null

    private var photoUri: Uri? = null

    // 1. Launcher Ambil Foto via Uri File (Standar Android 8+)
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            val layoutAttachmentDefault = findViewById<View>(R.id.layoutAttachmentDefault)
            val layoutAttachmentPreview = findViewById<View>(R.id.layoutAttachmentPreview)
            val ivReceiptPreview = findViewById<ImageView>(R.id.ivReceiptPreview)

            // Tampilkan foto hasil jepretan dari Uri
            ivReceiptPreview.setImageURI(photoUri)

            layoutAttachmentDefault.visibility = View.GONE
            layoutAttachmentPreview.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Foto dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Launcher Izin Runtime Kamera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCameraWithFileProvider()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCameraWithFileProvider()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCameraWithFileProvider() {
        try {
            val photoFile = File.createTempFile("struk_", ".jpg", externalCacheDir ?: cacheDir)
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            // 🟢 Tambahkan null-check sebelum di-launch
            photoUri?.let { uri ->
                takePictureLauncher.launch(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi_manual)

        // ================= INISIALISASI COMPONENT VIEW =================
        val btnTypeExpense = findViewById<Button>(R.id.btnTypeExpense)
        val btnTypeIncome = findViewById<Button>(R.id.btnTypeIncome)
        val sectionKategori = findViewById<LinearLayout>(R.id.sectionKategori)
        val layoutAttachmentContainer = findViewById<View>(R.id.layoutAttachmentContainer)
        val lblSelectedCategory = findViewById<TextView>(R.id.lblSelectedCategory)

        // Attachment Views
        val layoutAttachmentDefault = findViewById<View>(R.id.layoutAttachmentDefault)
        val layoutAttachmentPreview = findViewById<View>(R.id.layoutAttachmentPreview)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnDeletePhoto = findViewById<ImageButton>(R.id.btnDeletePhoto)
        val btnRetakePhoto = findViewById<ImageButton>(R.id.btnRetakePhoto)

        // List Kategori
        val categories = listOf(
            Triple(findViewById<LinearLayout>(R.id.catFood), findViewById<TextView>(R.id.tvCatFoodText), "Makanan & Minuman"),
            Triple(findViewById<LinearLayout>(R.id.catTransport), findViewById<TextView>(R.id.tvCatTransportText), "Transportasi"),
            Triple(findViewById<LinearLayout>(R.id.catShopping), findViewById<TextView>(R.id.tvCatShoppingText), "Belanja"),
            Triple(findViewById<LinearLayout>(R.id.catBills), findViewById<TextView>(R.id.tvCatBillsText), "Tagihan"),
            Triple(findViewById<LinearLayout>(R.id.catEntertainment), findViewById<TextView>(R.id.tvCatEntertainmentText), "Hiburan"),
            Triple(findViewById<LinearLayout>(R.id.catEducation), findViewById<TextView>(R.id.tvCatEducationText), "Pendidikan"),
            Triple(findViewById<LinearLayout>(R.id.catOthers), findViewById<TextView>(R.id.tvCatOthersText), "Lainnya")
        )

        // Set default kategori aktif ke "Makanan & Minuman"
        selectedCategoryView = null
        selectedCategoryText = null
        lblSelectedCategory.text = "Belum Dipilih"


        // Helper Tint Warna Tombol & Icon Panah
        fun updateButtonState(button: Button, isActive: Boolean, activeBgRes: Int, activeTextColor: Int, iconTintActive: Int, iconTintInactive: Int) {
            if (isActive) {
                button.setBackgroundResource(activeBgRes)
                button.setTextColor(activeTextColor)
                TextViewCompat.setCompoundDrawableTintList(button, ContextCompat.getColorStateList(this, iconTintActive))
            } else {
                button.setBackgroundResource(R.drawable.bg_brutal_button_white)
                button.setTextColor(Color.BLACK)
                TextViewCompat.setCompoundDrawableTintList(button, ContextCompat.getColorStateList(this, iconTintInactive))
            }
        }

        // ================= LOGIKA TIPE TRANSAKSI =================
        btnTypeExpense.setOnClickListener {
            if (!isExpenseMode) {
                isExpenseMode = true

                updateButtonState(btnTypeExpense, true, R.drawable.bg_brutal_button_red, Color.WHITE, android.R.color.white, android.R.color.black)
                updateButtonState(btnTypeIncome, false, R.drawable.bg_card_income, Color.BLACK, android.R.color.white, android.R.color.black)
                TextViewCompat.setCompoundDrawableTintList(btnTypeIncome, ContextCompat.getColorStateList(this, android.R.color.holo_green_dark))

                sectionKategori.visibility = View.VISIBLE
                layoutAttachmentContainer.visibility = View.VISIBLE
            }
        }

        btnTypeIncome.setOnClickListener {
            if (isExpenseMode) {
                isExpenseMode = false

                updateButtonState(btnTypeIncome, true, R.drawable.bg_card_income, Color.BLACK, android.R.color.black, android.R.color.black)
                updateButtonState(btnTypeExpense, false, R.drawable.bg_brutal_button_red, Color.BLACK, android.R.color.white, android.R.color.black)

                sectionKategori.visibility = View.GONE
                layoutAttachmentContainer.visibility = View.GONE
            }
        }

        // ================= LOGIKA SELEKSI KATEGORI =================
        for (category in categories) {
            val view = category.first
            val textView = category.second
            val categoryName = category.third

            view.setOnClickListener {
                // 1. Reset kategori yang sebelumnya terpilih kembali ke Putih (jika ada)
                selectedCategoryView?.let { prevView ->
                    val prevLayerList = ContextCompat.getDrawable(this@TransaksiManual, R.drawable.bg_brutal_card)?.mutate() as? android.graphics.drawable.LayerDrawable
                    val prevShapeCard = prevLayerList?.getDrawable(1) as? android.graphics.drawable.GradientDrawable
                    prevShapeCard?.setColor(Color.WHITE)
                    prevView.background = prevLayerList
                }
                selectedCategoryText?.setTextColor(Color.BLACK)

                // 2. Set kategori baru yang baru saja diklik menjadi Ungu
                val activeLayerList = ContextCompat.getDrawable(this@TransaksiManual, R.drawable.bg_brutal_card)?.mutate() as? android.graphics.drawable.LayerDrawable
                val activeShapeCard = activeLayerList?.getDrawable(1) as? android.graphics.drawable.GradientDrawable
                activeShapeCard?.setColor(ContextCompat.getColor(this@TransaksiManual, R.color.monetra_purple))

                view.background = activeLayerList
                textView.setTextColor(Color.WHITE)

                // 3. Simpan reference kategori aktif saat ini
                selectedCategoryView = view
                selectedCategoryText = textView
                lblSelectedCategory.text = categoryName
            }
        }

        // ================= LOGIKA PILIH TANGGAL TRANSAKSI =================
        val tvDateTitle = findViewById<TextView>(R.id.tvDateTitle)
        val btnChangeDate = findViewById<View>(R.id.btnChangeDate) // Bisa berupa Button, ImageButton, atau Layout

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("id", "ID")) // Format: "17 Nov 2026"

        // Set teks default saat activity pertama dibuka
        tvDateTitle.text = "Hari ini, ${dateFormat.format(calendar.time)}"

        btnChangeDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Update objek calendar dengan tanggal yang dipilih user
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                    // Cek apakah tanggal yang dipilih sama dengan hari ini
                    val today = Calendar.getInstance()
                    val isToday = selectedYear == today.get(Calendar.YEAR) &&
                            selectedMonth == today.get(Calendar.MONTH) &&
                            selectedDay == today.get(Calendar.DAY_OF_MONTH)

                    val formattedDate = dateFormat.format(calendar.time)

                    // Jika hari ini tampilkan "Hari ini, 17 Nov 2026", jika bukan tampilkan tanggalnya saja
                    tvDateTitle.text = if (isToday) "Hari ini, $formattedDate" else formattedDate
                },
                year,
                month,
                day
            )

            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // ================= LOGIKA LAMPIRAN STRUK & KAMERA =================
        btnTakePhoto.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        btnRetakePhoto.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        btnDeletePhoto.setOnClickListener {
            photoUri = null
            layoutAttachmentPreview.visibility = View.GONE
            layoutAttachmentDefault.visibility = View.VISIBLE
        }
    }
}