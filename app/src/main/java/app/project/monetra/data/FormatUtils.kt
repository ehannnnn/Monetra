package app.project.monetra.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
    private val idLocale = Locale("id", "ID")
    private val dayFormat = SimpleDateFormat("d MMM yyyy", idLocale)
    private val timeFormat = SimpleDateFormat("HH:mm", idLocale)

    fun rupiah(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        return "$sign Rp" + "%,d".format(abs(amount)).replace(',', '.')
    }

    fun signedRupiah(amount: Long, isIncome: Boolean): String =
        (if (isIncome) "+" else "-") + rupiah(abs(amount))

    fun date(millis: Long): String = dayFormat.format(Date(millis))

    fun relativeDate(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> "Hari ini, ${timeFormat.format(Date(millis))}"
            isYesterday -> "Kemarin, ${timeFormat.format(Date(millis))}"
            else -> date(millis)
        }
    }
}
