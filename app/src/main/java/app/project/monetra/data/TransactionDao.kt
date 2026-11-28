package app.project.monetra.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {
    @Insert
    fun insert(transaction: TransactionEntity): Long

    @Update
    fun update(transaction: TransactionEntity)

    @Delete
    fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC LIMIT :limit")
    fun getRecent(limit: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY dateMillis DESC, id DESC")
    fun getByCategory(category: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    fun deleteById(id: Long)
}
