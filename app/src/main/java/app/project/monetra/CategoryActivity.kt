package app.project.monetra

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class CategoryModel(
    val name: String,
    var txCount: Int,
    val iconRes: Int,
    val accentColorRes: Int,
    val isCustom: Boolean = false,
    val isIncome: Boolean = false
)

data class CategoryTransaction(
    val id: String,
    val title: String,
    val meta: String,
    val amount: String,
    val isIncome: Boolean,
    val iconRes: Int = R.drawable.ic_cat_sparkle
)

class CategoryActivity : AppCompatActivity() {

    private val categories = mutableListOf(
        CategoryModel("Makanan & Minuman", 34, R.drawable.ic_cat_food, R.color.monetra_purple, false, false),
        CategoryModel("Transportasi", 18, R.drawable.ic_cat_fuel, R.color.monetra_green, false, false),
        CategoryModel("Belanja", 15, R.drawable.ic_cat_bag, R.color.monetra_yellow, false, false),
        CategoryModel("Tagihan", 6, R.drawable.ic_cat_bill, R.color.monetra_red, false, false),
        CategoryModel("Hiburan", 9, R.drawable.ic_cat_gamepad, R.color.monetra_purple, false, false),
        CategoryModel("Pendidikan", 4, R.drawable.ic_cat_grad, R.color.monetra_green, false, false),
        CategoryModel("Lainnya", 7, R.drawable.ic_cat_more, R.color.monetra_gray_text, false, false),
        CategoryModel("Skincare", 12, R.drawable.ic_cat_sparkle, R.color.monetra_yellow, true, false),
        CategoryModel("Gaji Bulanan", 5, R.drawable.ic_cat_cash, R.color.monetra_green, false, true),
        CategoryModel("Freelance", 8, R.drawable.ic_cat_sparkle, R.color.monetra_blue, true, true)
    )

    private val categoryTransactions = mutableMapOf<String, MutableList<CategoryTransaction>>(
        "Makanan & Minuman" to mutableListOf(
            CategoryTransaction("1", "Nasi Padang", "Hari ini, 12:30", "-Rp20.000", false, R.drawable.ic_cat_food),
            CategoryTransaction("2", "Kopi Susu", "Kemarin, 16:45", "-Rp18.000", false, R.drawable.ic_cat_coffee)
        ),
        "Transportasi" to mutableListOf(
            CategoryTransaction("3", "Bensin Pertalite", "Kemarin, 08:15", "-Rp35.000", false, R.drawable.ic_cat_fuel)
        ),
        "Skincare" to mutableListOf(
            CategoryTransaction("4", "Serum Wajah", "2 hari lalu", "-Rp75.000", false, R.drawable.ic_cat_sparkle)
        )
    )

    private var showingIncome = false
    private lateinit var adapter: CategoryAdapter
    private lateinit var rvCategories: RecyclerView
    private lateinit var tvActiveCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        rvCategories = findViewById(R.id.rvCategories)
        tvActiveCount = findViewById(R.id.tvActiveCount)
        rvCategories.layoutManager = GridLayoutManager(this, 2)

        val tabPengeluaran = findViewById<TextView>(R.id.tabPengeluaran)
        val tabPemasukan = findViewById<TextView>(R.id.tabPemasukan)

        tabPengeluaran.setOnClickListener {
            showingIncome = false
            tabPengeluaran.setBackgroundResource(R.drawable.bg_brutal_button_red)
            tabPengeluaran.setTextColor(ContextCompat.getColor(this, R.color.monetra_white))
            tabPemasukan.setBackgroundResource(R.drawable.bg_brutal_button_white)
            tabPemasukan.setTextColor(ContextCompat.getColor(this, R.color.monetra_black))
            updateList()
        }

        tabPemasukan.setOnClickListener {
            showingIncome = true
            tabPemasukan.setBackgroundResource(R.drawable.bg_brutal_button_red)
            tabPemasukan.setTextColor(ContextCompat.getColor(this, R.color.monetra_white))
            tabPengeluaran.setBackgroundResource(R.drawable.bg_brutal_button_white)
            tabPengeluaran.setTextColor(ContextCompat.getColor(this, R.color.monetra_black))
            updateList()
        }

        findViewById<View>(R.id.btnTambahKategoriBottom).setOnClickListener {
            showAddCategoryDialog()
        }

        updateList()
    }

    private fun updateList() {
        val filtered = categories.filter { it.isIncome == showingIncome }
        tvActiveCount.text = "● ${filtered.size} KATEGORI AKTIF"
        adapter = CategoryAdapter(filtered,
            onItemClick = { cat -> showCategoryDetailDialog(cat) },
            onDeleteClick = { cat -> showDeleteCategoryDialog(cat) }
        )
        rvCategories.adapter = adapter
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val btnPengeluaran = dialogView.findViewById<TextView>(R.id.btnModalPengeluaran)
        val btnPemasukan = dialogView.findViewById<TextView>(R.id.btnModalPemasukan)

        val tvColorName = dialogView.findViewById<TextView>(R.id.tvColorName)
        val previewIvIcon = dialogView.findViewById<ImageView>(R.id.previewIvIcon)
        val previewTvName = dialogView.findViewById<TextView>(R.id.previewTvName)
        val previewTvType = dialogView.findViewById<TextView>(R.id.previewTvType)
        val previewIconBox = dialogView.findViewById<FrameLayout>(R.id.previewIconBox)

        var isModalIncome = showingIncome
        var selectedIconRes = R.drawable.ic_cat_sparkle
        var selectedColorRes = R.color.monetra_purple
        var colorNameStr = "Tab Ungu"

        fun updatePreview() {
            previewIvIcon.setImageResource(selectedIconRes)
            previewIconBox.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.monetra_white))
            previewIvIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.monetra_black))
            tvColorName.text = "● $colorNameStr"

            val nameText = etName.text.toString().trim()
            previewTvName.text = if (nameText.isEmpty()) "Skincare" else nameText
            previewTvType.text = if (isModalIncome) "Kategori Pemasukan" else "Kategori Pengeluaran"
            previewTvType.setTextColor(ContextCompat.getColor(this, if (isModalIncome) R.color.monetra_green else R.color.monetra_red))
        }

        fun updateModalTabs() {
            if (isModalIncome) {
                btnPemasukan.setBackgroundResource(R.drawable.bg_brutal_button_red)
                btnPemasukan.setTextColor(ContextCompat.getColor(this, R.color.monetra_white))
                btnPengeluaran.setBackgroundResource(R.drawable.bg_brutal_button_white)
                btnPengeluaran.setTextColor(ContextCompat.getColor(this, R.color.monetra_black))
            } else {
                btnPengeluaran.setBackgroundResource(R.drawable.bg_brutal_button_red)
                btnPengeluaran.setTextColor(ContextCompat.getColor(this, R.color.monetra_white))
                btnPemasukan.setBackgroundResource(R.drawable.bg_brutal_button_white)
                btnPemasukan.setTextColor(ContextCompat.getColor(this, R.color.monetra_black))
            }
            updatePreview()
        }
        updateModalTabs()

        btnPengeluaran.setOnClickListener { isModalIncome = false; updateModalTabs() }
        btnPemasukan.setOnClickListener { isModalIncome = true; updateModalTabs() }

        etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Icon click listeners
        val icons = listOf(
            Pair(dialogView.findViewById<FrameLayout>(R.id.iconOpt1), R.drawable.ic_cat_sparkle),
            Pair(dialogView.findViewById<FrameLayout>(R.id.iconOpt2), R.drawable.ic_cat_food),
            Pair(dialogView.findViewById<FrameLayout>(R.id.iconOpt3), R.drawable.ic_cat_bag),
            Pair(dialogView.findViewById<FrameLayout>(R.id.iconOpt4), R.drawable.ic_cat_fuel),
            Pair(dialogView.findViewById<FrameLayout>(R.id.iconOpt5), R.drawable.ic_cat_bill),
            Pair(dialogView.findViewById<FrameLayout>(R.id.iconOpt6), R.drawable.ic_cat_gamepad)
        )
        for ((box, res) in icons) {
            box?.setOnClickListener {
                selectedIconRes = res
                icons.forEach { it.first?.backgroundTintList = null }
                box.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.monetra_yellow_light))
                updatePreview()
            }
        }

        // Color click listeners
        val colors = listOf(
            Triple(dialogView.findViewById<View>(R.id.colorOptGreen), R.color.monetra_green, "Tab Hijau"),
            Triple(dialogView.findViewById<View>(R.id.colorOptRed), R.color.monetra_red, "Tab Merah"),
            Triple(dialogView.findViewById<View>(R.id.colorOptPurple), R.color.monetra_purple, "Tab Ungu"),
            Triple(dialogView.findViewById<View>(R.id.colorOptYellow), R.color.monetra_yellow, "Tab Kuning")
        )
        for ((v, colorRes, name) in colors) {
            v?.setOnClickListener {
                selectedColorRes = colorRes
                colorNameStr = name
                updatePreview()
            }
        }

        dialogView.findViewById<ImageView>(R.id.btnCloseDialog).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.btnCancelModal).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<TextView>(R.id.btnSaveModal).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newCat = CategoryModel(
                name = name,
                txCount = 0,
                iconRes = selectedIconRes,
                accentColorRes = selectedColorRes,
                isCustom = true,
                isIncome = isModalIncome
            )
            categories.add(newCat)
            showingIncome = isModalIncome
            updateList()
            Toast.makeText(this, "Kategori '$name' berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteCategoryDialog(cat: CategoryModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_category, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvDelCatName).text = "Hapus '${cat.name}' beserta riwayatnya?"
        dialogView.findViewById<TextView>(R.id.btnCancelDeleteCat).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.btnConfirmDeleteCat).setOnClickListener {
            categories.remove(cat)
            categoryTransactions.remove(cat.name)
            updateList()
            Toast.makeText(this, "Kategori '${cat.name}' dihapus", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCategoryDetailDialog(cat: CategoryModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_detail, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvDetailTitle).text = cat.name
        val tvCount = dialogView.findViewById<TextView>(R.id.tvDetailCount)
        val rvTx = dialogView.findViewById<RecyclerView>(R.id.rvCategoryTransactions)
        rvTx.layoutManager = LinearLayoutManager(this)

        val txList = categoryTransactions.getOrPut(cat.name) {
            mutableListOf()
        }

        fun refreshDetail() {
            tvCount.text = "${txList.size} transaksi tercatat"
            cat.txCount = txList.size
            updateList()
            rvTx.adapter = CategoryTransactionAdapter(txList) { tx ->
                showDeleteTransactionDialog(tx, txList) {
                    refreshDetail()
                }
            }
        }
        refreshDetail()

        dialogView.findViewById<TextView>(R.id.btnAddTxInFolder).setOnClickListener {
            showAddTransactionDialog(cat, txList) {
                refreshDetail()
            }
        }

        dialogView.findViewById<ImageView>(R.id.btnCloseDetail).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.btnCloseDetailBottom).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showAddTransactionDialog(cat: CategoryModel, txList: MutableList<CategoryTransaction>, onAdded: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvAddTxTitle = dialogView.findViewById<TextView>(R.id.tvAddTxTitle)
        tvAddTxTitle.text = if (cat.isIncome) "Tambah Pemasukan" else "Tambah Transaksi"

        val etTitle = dialogView.findViewById<EditText>(R.id.etTxTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etTxAmount)

        dialogView.findViewById<ImageView>(R.id.btnCloseAddTx).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.btnCancelAddTx).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<TextView>(R.id.btnSaveAddTx).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Mohon isi nama dan nominal transaksi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rawAmount = amountStr.toLongOrNull() ?: 0L
            val prefix = if (cat.isIncome) "+Rp" else "-Rp"
            val formattedAmount = "$prefix%,d".format(rawAmount).replace(',', '.')

            val newTx = CategoryTransaction(
                id = System.currentTimeMillis().toString(),
                title = title,
                meta = "${cat.name} • Baru saja",
                amount = formattedAmount,
                isIncome = cat.isIncome,
                iconRes = cat.iconRes
            )
            txList.add(newTx)
            onAdded()
            Toast.makeText(this, "Transaksi '$title' ditambahkan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteTransactionDialog(tx: CategoryTransaction, txList: MutableList<CategoryTransaction>, onDeleted: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_transaction, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvDelTxTitle).text = tx.title
        val tvAmt = dialogView.findViewById<TextView>(R.id.tvDelTxAmount)
        tvAmt.text = tx.amount
        tvAmt.setTextColor(ContextCompat.getColor(this, if (tx.isIncome) R.color.monetra_green else R.color.monetra_red))

        dialogView.findViewById<TextView>(R.id.btnCancelDeleteTx).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.btnConfirmDeleteTx).setOnClickListener {
            txList.remove(tx)
            onDeleted()
            Toast.makeText(this, "Transaksi '${tx.title}' berhasil dihapus", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}

class CategoryAdapter(
    private val items: List<CategoryModel>,
    private val onItemClick: (CategoryModel) -> Unit,
    private val onDeleteClick: (CategoryModel) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val folderTabColor: View = view.findViewById(R.id.folderTabColor)
        val catIconBox: FrameLayout = view.findViewById(R.id.catIconBox)
        val ivCatIcon: ImageView = view.findViewById(R.id.ivCatIcon)
        val tvBadgeBawaan: View = view.findViewById(R.id.tvBadgeBawaan)
        val customActions: View = view.findViewById(R.id.customActions)
        val btnDeleteCat: ImageView = view.findViewById(R.id.btnDeleteCat)
        val tvCatName: TextView = view.findViewById(R.id.tvCatName)
        val tvBadgeKustom: TextView = view.findViewById(R.id.tvBadgeKustom)
        val tvTxCount: TextView = view.findViewById(R.id.tvTxCount)
        val dotAccent: View = view.findViewById(R.id.dotAccent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val colorInt = ContextCompat.getColor(context, item.accentColorRes)

        holder.folderTabColor.backgroundTintList = ColorStateList.valueOf(colorInt)
        holder.dotAccent.backgroundTintList = ColorStateList.valueOf(colorInt)
        holder.catIconBox.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.monetra_white))
        holder.ivCatIcon.setImageResource(item.iconRes)
        holder.ivCatIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.monetra_black))
        holder.tvCatName.text = item.name
        holder.tvTxCount.text = "${item.txCount} transaksi"

        if (item.isCustom) {
            holder.tvBadgeBawaan.visibility = View.GONE
            holder.customActions.visibility = View.VISIBLE
            holder.tvBadgeKustom.visibility = View.VISIBLE
        } else {
            holder.tvBadgeBawaan.visibility = View.VISIBLE
            holder.customActions.visibility = View.GONE
            holder.tvBadgeKustom.visibility = View.GONE
        }

        holder.btnDeleteCat.setOnClickListener { onDeleteClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}

class CategoryTransactionAdapter(
    private val items: List<CategoryTransaction>,
    private val onItemClick: (CategoryTransaction) -> Unit
) : RecyclerView.Adapter<CategoryTransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout = view.findViewById(R.id.iconContainer)
        val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTransactionTitle)
        val tvMeta: TextView = view.findViewById(R.id.tvTransactionMeta)
        val tvAmount: TextView = view.findViewById(R.id.tvTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.monetra_white))
        holder.ivCategoryIcon.setImageResource(item.iconRes)
        holder.ivCategoryIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.monetra_black))

        holder.tvTitle.text = item.title
        holder.tvMeta.text = item.meta
        holder.tvAmount.text = item.amount
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, if (item.isIncome) R.color.monetra_green else R.color.monetra_red))
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}
