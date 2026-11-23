package lat.pam.monetra

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

data class TransactionItem(
    val title: String,
    val meta: String,
    val amount: String,
    val isIncome: Boolean,
    val iconBgColorRes: Int,
    val iconRes: Int
)

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rvTransactions = findViewById<RecyclerView>(R.id.rvRecentTransactions)
        val fabCatat = findViewById<View>(R.id.fabCatat)

        val sampleTransactions = listOf(
            TransactionItem("Nasi Padang", "Makanan & Minuman • Hari ini, 12:30", "-Rp20.000", false, R.color.monetra_purple, R.drawable.ic_cat_food),
            TransactionItem("Bensin Pertalite", "Transportasi • Kemarin, 08:15", "-Rp35.000", false, R.color.monetra_green, R.drawable.ic_cat_fuel),
            TransactionItem("Uang saku bulanan", "Pemasukan • 15 Nov, 10:00", "+Rp500.000", true, R.color.monetra_green, R.drawable.ic_cat_cash),
            TransactionItem("Parkir motor kampus", "Transportasi • 14 Nov, 14:20", "-Rp2.000", false, R.color.monetra_blue, R.drawable.ic_cat_park_p),
            TransactionItem("Kopi susu gula aren", "Makanan & Minuman • 13 Nov, 16:45", "-Rp18.000", false, R.color.monetra_pink, R.drawable.ic_cat_coffee)
        )

        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = TransactionAdapter(sampleTransactions)

        fabCatat.setOnClickListener {
            Toast.makeText(this, "Buka form catat transaksi", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnAddCategoryHome)?.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
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
