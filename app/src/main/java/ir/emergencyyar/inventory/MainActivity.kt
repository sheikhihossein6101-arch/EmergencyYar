package ir.emergencyyar.inventory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ibm.icu.util.PersianCalendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

data class StockItem(
    val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Int,
    val location: String,
    val expiryIso: String,
    val dateType: String
)

class InventoryDb(context: Context) : SQLiteOpenHelper(context, "emergency_inventory.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            quantity INTEGER NOT NULL,
            location TEXT NOT NULL,
            expiry_iso TEXT NOT NULL,
            date_type TEXT NOT NULL
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun allItems(): List<StockItem> {
        val result = mutableListOf<StockItem>()
        readableDatabase.rawQuery("SELECT * FROM items ORDER BY expiry_iso ASC", null).use { c ->
            while (c.moveToNext()) result.add(StockItem(
                c.getLong(0), c.getString(1), c.getString(2), c.getInt(3),
                c.getString(4), c.getString(5), c.getString(6)
            ))
        }
        return result
    }
    fun save(item: StockItem): Long {
        val v = ContentValues().apply {
            put("name", item.name); put("category", item.category)
            put("quantity", item.quantity); put("location", item.location)
            put("expiry_iso", item.expiryIso); put("date_type", item.dateType)
        }
        return if (item.id == 0L) writableDatabase.insert("items", null, v) else {
            writableDatabase.update("items", v, "id=?", arrayOf(item.id.toString())).toLong()
            item.id
        }
    }
    fun remove(id: Long) { writableDatabase.delete("items", "id=?", arrayOf(id.toString())) }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = InventoryDb(this)
        setContent { MaterialTheme { InventoryApp(db) } }
    }
}

private fun isoToMillis(iso: String): Long? = try {
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    f.parse(iso)?.time
} catch (_: Exception) { null }

private fun todayIso(): String {
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return f.format(Date())
}

private fun expiryStatus(iso: String): Int {
    val item = isoToMillis(iso) ?: return 2
    val today = isoToMillis(todayIso()) ?: return 2
    val days = ((item - today) / 86400000L).toInt()
    return when { days < 0 -> 0; days <= 90 -> 1; else -> 2 }
}

private fun isoToDisplay(iso: String, dateType: String): String {
    val millis = isoToMillis(iso) ?: return iso
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply { timeInMillis = millis }
    return if (dateType == "شمسی") {
val pc = PersianCalendar().apply {
    timeZone = TimeZone.getTimeZone("UTC")
    timeInMillis = millis
}        "%04d/%02d/%02d".format(Locale.US, pc.get(Calendar.YEAR), pc.get(Calendar.MONTH) + 1, pc.get(Calendar.DAY_OF_MONTH))
    } else {
        "%04d/%02d/%02d".format(Locale.US, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
}

```kotlin
private fun parseExpiry(input: String, type: String): String? {
    val clean = input.trim()
        .replace('-', '/')
        .replace('۰', '0')
        .replace('۱', '1')
        .replace('۲', '2')
        .replace('۳', '3')
        .replace('۴', '4')
        .replace('۵', '5')
        .replace('۶', '6')
        .replace('۷', '7')
        .replace('۸', '8')
        .replace('۹', '9')

    val parts = clean.split('/')
    if (parts.size != 3) return null

    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null

    return try {
        val millis: Long

        if (type == "شمسی") {
            val pc = PersianCalendar()
            pc.timeZone = com.ibm.icu.util.TimeZone.getTimeZone("UTC")
            pc.clear()
            pc.isLenient = false
            pc.set(PersianCalendar.YEAR, y)
            pc.set(PersianCalendar.MONTH, m - 1)
            pc.set(PersianCalendar.DAY_OF_MONTH, d)
            millis = pc.timeInMillis
        } else {
            val c = Calendar.getInstance(
                java.util.TimeZone.getTimeZone("UTC"),
                Locale.US
            )
            c.clear()
            c.isLenient = false
            c.set(y, m - 1, d)
            millis = c.timeInMillis
        }

        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            .format(Date(millis))
    } catch (_: Exception) {
        null
    }
}
```    val clean = input.trim().replace('-', '/').replace('۰','0').replace('۱','1').replace('۲','2')
        .replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9')
    val parts = clean.split('/')
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return try {
        val millis = if (type == "شمسی") {
            val pc = PersianCalendar(TimeZone.getTimeZone("UTC"), Locale("fa", "IR"))
            pc.clear()
            pc.setLenient(false)
            pc.set(PersianCalendar.YEAR, y)
            pc.set(PersianCalendar.MONTH, m - 1)
            pc.set(PersianCalendar.DAY_OF_MONTH, d)
            pc.timeInMillis
        } else {
            val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
            c.clear(); c.isLenient = false
            c.set(y, m - 1, d)
            c.timeInMillis
        }
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp(db: InventoryDb) {
    var refresh by remember { mutableIntStateOf(0) }
    val items = remember(refresh) { db.allItems() }
    var editing by remember { mutableStateOf<StockItem?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("همه") }
    val expired = items.count { expiryStatus(it.expiryIso) == 0 }
    val soon = items.count { expiryStatus(it.expiryIso) == 1 }

    Scaffold(
        topBar = { TopAppBar(title = { Text("اورژانس‌یار", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("مدیریت موجودی دارو و تجهیزات", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("کل اقلام", items.size.toString(), Modifier.weight(1f))
                SummaryCard("نزدیک انقضا", soon.toString(), Modifier.weight(1f))
                SummaryCard("منقضی", expired.toString(), Modifier.weight(1f))
            }
            if (expired > 0 || soon > 0) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text("هشدار: $expired قلم منقضی و $soon قلم با مهلت ۹۰ روز یا کمتر دارید. پیش از استفاده، تاریخ و سلامت اقلام را بررسی کنید.",
                        Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("جست‌وجوی نام یا محل نگهداری") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("همه", "دارو", "تجهیزات", "هشدارها").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هنوز موردی ثبت نشده است.\nبرای افزودن دارو یا تجهیزات، دکمه + را بزنید.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items.filter {
                        (it.name.contains(query, true) || it.location.contains(query, true)) &&
                        (filter == "همه" || (filter == "دارو" && it.category == "دارو") ||
                         (filter == "تجهیزات" && it.category == "تجهیزات") ||
                         (filter == "هشدارها" && expiryStatus(it.expiryIso) != 2))
                    }, key = { it.id }) { item ->
                        StockCard(item, onEdit = { editing = item; showForm = true }, onDelete = {
                            db.remove(item.id); refresh++
                        })
                    }
                }
            }
        }
    }
    if (showForm) {
        ItemDialog(editing, onDismiss = { showForm = false }, onSave = { item ->
            db.save(item); refresh++; showForm = false
        })
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StockCard(item: StockItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val status = expiryStatus(item.expiryIso)
    val statusText = when (status) { 0 -> "منقضی‌شده"; 1 -> "نزدیک انقضا (۹۰ روز)"; else -> "وضعیت عادی" }
    val color = when (status) { 0 -> MaterialTheme.colorScheme.errorContainer; 1 -> MaterialTheme.colorScheme.tertiaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant }
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${item.category} • تعداد: ${item.quantity}")
            Text("محل نگهداری: ${item.location}")
            Text("انقضا (${item.dateType}): ${isoToDisplay(item.expiryIso, item.dateType)}")
            Text(statusText, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("ویرایش") }
                TextButton(onClick = onDelete) { Text("حذف") }
            }
        }
    }
}

@Composable
private fun ItemDialog(item: StockItem?, onDismiss: () -> Unit, onSave: (StockItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var category by remember(item) { mutableStateOf(item?.category ?: "دارو") }
    var quantity by remember(item) { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var location by remember(item) { mutableStateOf(item?.location ?: "") }
    var dateType by remember(item) { mutableStateOf(item?.dateType ?: "شمسی") }
    var expiry by remember(item) { mutableStateOf(item?.let { isoToDisplay(it.expiryIso, it.dateType) } ?: "") }
    var error by remember(item) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "افزودن قلم جدید" else "ویرایش قلم") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام دارو یا تجهیزات") }, singleLine = true)
                Text("نوع قلم")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == "دارو", onClick = { category = "دارو" }, label = { Text("دارو") })
                    FilterChip(selected = category == "تجهیزات", onClick = { category = "تجهیزات" }, label = { Text("تجهیزات") })
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text("تعداد موجودی") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("محل نگهداری") }, singleLine = true)
                Text("نوع تقویم تاریخ انقضا")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = dateType == "شمسی", onClick = {
                        if (dateType != "شمسی" && expiry.isNotBlank()) {
                            val iso = parseExpiry(expiry, dateType)
                            expiry = iso?.let { isoToDisplay(it, "شمسی") } ?: expiry
                        }
                        dateType = "شمسی"
                    }, label = { Text("شمسی") })
                    FilterChip(selected = dateType == "میلادی", onClick = {
                        if (dateType != "میلادی" && expiry.isNotBlank()) {
                            val iso = parseExpiry(expiry, dateType)
                            expiry = iso?.let { isoToDisplay(it, "میلادی") } ?: expiry
                        }
                        dateType = "میلادی"
                    }, label = { Text("میلادی") })
                }
                OutlinedTextField(expiry, { expiry = it }, label = { Text("تاریخ انقضا (سال/ماه/روز)") }, placeholder = { Text(if (dateType == "شمسی") "۱۴۰۵/۱۲/۲۹" else "2027/12/31") }, singleLine = true)
                Text("هشدار برنامه برای انقضای ۹۰ روز آینده و موارد منقضی‌شده نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = quantity.toIntOrNull()
                val iso = parseExpiry(expiry, dateType)
                when {
                    name.isBlank() -> error = "نام قلم را وارد کنید."
                    qty == null || qty < 0 -> error = "تعداد باید عدد صفر یا بیشتر باشد."
                    location.isBlank() -> error = "محل نگهداری را وارد کنید."
                    iso == null -> error = "تاریخ معتبر نیست؛ آن را به شکل سال/ماه/روز وارد کنید."
                    else -> onSave(StockItem(item?.id ?: 0L, name.trim(), category, qty, location.trim(), iso, dateType))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
