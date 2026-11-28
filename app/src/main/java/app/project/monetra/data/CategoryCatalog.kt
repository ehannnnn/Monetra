package app.project.monetra.data

import app.project.monetra.R

object CategoryCatalog {
    val expense = listOf(
        "Makanan & Minuman",
        "Transportasi",
        "Belanja",
        "Tagihan",
        "Hiburan",
        "Pendidikan",
        "Lainnya"
    )

    val income = listOf(
        "Gaji Bulanan",
        "Freelance",
        "Lainnya"
    )

    val all: List<String> = (expense + income).distinct()

    fun forType(type: String): List<String> =
        if (type == TransactionEntity.TYPE_INCOME) income else expense

    fun iconFor(category: String): Int = when (category) {
        "Makanan & Minuman" -> R.drawable.ic_cat_food
        "Transportasi" -> R.drawable.ic_cat_fuel
        "Belanja" -> R.drawable.ic_cat_bag
        "Tagihan" -> R.drawable.ic_cat_bill
        "Hiburan" -> R.drawable.ic_cat_gamepad
        "Pendidikan" -> R.drawable.ic_cat_grad
        "Gaji Bulanan" -> R.drawable.ic_cat_cash
        "Freelance" -> R.drawable.ic_cat_sparkle
        else -> R.drawable.ic_cat_more
    }

    fun colorFor(category: String): Int = when (category) {
        "Makanan & Minuman" -> R.color.monetra_purple
        "Transportasi" -> R.color.monetra_green
        "Belanja" -> R.color.monetra_yellow
        "Tagihan" -> R.color.monetra_red
        "Hiburan" -> R.color.monetra_purple
        "Pendidikan" -> R.color.monetra_green
        "Gaji Bulanan" -> R.color.monetra_green
        "Freelance" -> R.color.monetra_blue
        else -> R.color.monetra_gray_text
    }
}
