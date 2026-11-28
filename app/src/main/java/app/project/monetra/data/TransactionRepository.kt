package app.project.monetra.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

class TransactionRepository(context: Context) {
    private val dao = MonetraDatabase.get(context).transactionDao()

    fun insert(transaction: TransactionEntity, onSaved: (Long) -> Unit = {}) {
        executor.execute {
            val id = dao.insert(transaction)
            mainHandler.post { onSaved(id) }
        }
    }

    fun update(transaction: TransactionEntity, onUpdated: () -> Unit = {}) {
        executor.execute {
            dao.update(transaction)
            mainHandler.post { onUpdated() }
        }
    }

    fun delete(transaction: TransactionEntity, onDeleted: () -> Unit = {}) {
        executor.execute {
            dao.delete(transaction)
            mainHandler.post { onDeleted() }
        }
    }

    fun loadAll(onLoaded: (List<TransactionEntity>) -> Unit) {
        executor.execute {
            val items = dao.getAll()
            mainHandler.post { onLoaded(items) }
        }
    }

    fun loadRecent(limit: Int, onLoaded: (List<TransactionEntity>) -> Unit) {
        executor.execute {
            val items = dao.getRecent(limit)
            mainHandler.post { onLoaded(items) }
        }
    }

    fun loadByCategory(category: String, onLoaded: (List<TransactionEntity>) -> Unit) {
        executor.execute {
            val items = dao.getByCategory(category)
            mainHandler.post { onLoaded(items) }
        }
    }

    fun deleteById(id: Long, onDeleted: () -> Unit = {}) {
        executor.execute {
            dao.deleteById(id)
            mainHandler.post { onDeleted() }
        }
    }

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
