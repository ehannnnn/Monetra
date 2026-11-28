package app.project.monetra

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.project.monetra.data.CategoryCatalog
import app.project.monetra.data.FormatUtils
import app.project.monetra.data.TransactionEntity
import app.project.monetra.data.TransactionRepository

data class TransactionItem(
    val title: String,
    val meta: String,
    val amount: String,
    val isIncome: Boolean,
    val iconBgColorRes: Int,
    val iconRes: Int
)

class DashboardActivity : AppCompatActivity() {

    private lateinit var repository: TransactionRepository
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyTransactions: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = TransactionRepository(this)
        rvTransactions = findViewById(R.id.rvRecentTransactions)
        tvEmptyTransactions = findViewById(R.id.tvEmptyTransactions)
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)

        rvTransactions.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.fabCatat).setOnClickListener { openChat() }
        findViewById<View>(R.id.navChat).setOnClickListener { openChat() }
        findViewById<View>(R.id.navStatistik).setOnClickListener {
            Toast.makeText(this, "Statistik segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.navProfil).setOnClickListener {
            Toast.makeText(this, "Profil segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnSeeAllTransactions).setOnClickListener {
            Toast.makeText(this, "Riwayat lengkap segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnMonthFilter).setOnClickListener {
            Toast.makeText(this, "Filter bulan segera hadir", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnNotification).setOnClickListener {
            Toast.makeText(this, "Belum ada notifikasi baru", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnProfileAvatar).setOnClickListener {
            Toast.makeText(this, "Profil segera hadir", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnAddCategoryHome)?.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        repository.loadAll { transactions ->
            updateSummary(transactions)
            renderRecent(transactions.take(5))
        }
    }

    private fun updateSummary(transactions: List<TransactionEntity>) {
        val income = transactions
            .filter { it.type == TransactionEntity.TYPE_INCOME }
            .sumOf { it.amount }
        val expense = transactions
            .filter { it.type == TransactionEntity.TYPE_EXPENSE }
            .sumOf { it.amount }

        tvTotalBalance.text = FormatUtils.rupiah(income - expense)
        tvTotalIncome.text = FormatUtils.rupiah(income)
        tvTotalExpense.text = FormatUtils.rupiah(expense)
    }

    private fun renderRecent(transactions: List<TransactionEntity>) {
        val items = transactions.map { transaction ->
            val isIncome = transaction.type == TransactionEntity.TYPE_INCOME
            TransactionItem(
                title = transaction.description,
                meta = "${transaction.category} • ${FormatUtils.relativeDate(transaction.dateMillis)}",
                amount = FormatUtils.signedRupiah(transaction.amount, isIncome),
                isIncome = isIncome,
                iconBgColorRes = CategoryCatalog.colorFor(transaction.category),
                iconRes = CategoryCatalog.iconFor(transaction.category)
            )
        }

        rvTransactions.adapter = TransactionAdapter(items)
        tvEmptyTransactions.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openChat() {
        startActivity(
            Intent(this, ChatActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }
}

class TransactionAdapter(private val items: List<TransactionItem>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

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

        holder.tvTitle.text = item.title
        holder.tvMeta.text = item.meta
        holder.tvAmount.text = item.amount

        val amountColor = if (item.isIncome) R.color.monetra_green else R.color.monetra_red
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, amountColor))

        holder.iconContainer.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, item.iconBgColorRes))

        holder.ivCategoryIcon.setImageResource(item.iconRes)
        holder.ivCategoryIcon.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.monetra_white))
    }

    override fun getItemCount() = items.size
}
