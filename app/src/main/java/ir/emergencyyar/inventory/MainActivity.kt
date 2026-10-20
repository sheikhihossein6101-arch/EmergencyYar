
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

class InventoryDb(context: Context) :
    SQLiteOpenHelper(context, "emergency_inventory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                location TEXT NOT NULL,
                expiry_iso TEXT NOT NULL,
                date_type TEXT NOT NULL
            )"""
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // اطلاعات قبلی حفظ می‌شوند.
    }

    fun allItems(): List<StockItem> {
        val result = mutableListOf<StockItem>()

        readableDatabase.rawQuery(
            "SELECT * FROM items ORDER BY expiry_iso ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    StockItem(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        category = cursor.getString(2),
                        quantity = cursor.getInt(3),
                        location = cursor.getString(4),
                        expiryIso = cursor.getString(5),
                        dateType = cursor.getString(6)
                    )
                )
            }
        }

        return result
    }

    fun save(item: StockItem): Boolean {
        val values = ContentValues().apply {
            put("name", item.name.trim())
            put("category", item.category)
            put("quantity", item.quantity)
            put("location", item.location.trim())
            put("expiry_iso", item.expiryIso)
            put("date_type", item.dateType)
        }

        return try {
            if (item.id == 0L) {
                writableDatabase.insert("items", null, values) != -1L
            } else {
                writableDatabase.update(
                    "items",
                    values,
                    "id=?",
                    arrayOf(item.id.toString())
                ) > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    fun remove(id: Long): Boolean {
        return try {
            writableDatabase.delete(
                "items",
                "id=?",
                arrayOf(id.toString())
            ) > 0
        } catch (_: Exception) {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = InventoryDb(applicationContext)

        setContent {
            MaterialTheme {
                InventoryApp(db)
            }
        }
    }
}

private fun isoToMillis(iso: String): Long? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val date = format.parse(iso) ?: return null

        if (format.format(date) != iso) null else date.time
    } catch (_: Exception) {
        null
    }
}

private fun todayIso(): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return format.format(Date())
}

private fun expiryStatus(iso: String): Int {
    val expiry = isoToMillis(iso) ?: return 2
    val today = isoToMillis(todayIso()) ?: return 2
    val days = (expiry - today) / 86400000L

    return when {
        days < 0 -> 0
        days <= 90 -> 1
        else -> 2
    }
}

private fun isoToDisplay(
    iso: String,
    dateType: String
): String {
    val millis = isoToMillis(iso) ?: return iso

    return if (dateType == "شمسی") {
        val calendar = PersianCalendar(
            com.ibm.icu.util.TimeZone.getTimeZone("UTC"),
            Locale("fa", "IR")
        )
        calendar.timeInMillis = millis

        String.format(
            Locale.US,
            "%04d/%02d/%02d",
            calendar.get(PersianCalendar.YEAR),
            calendar.get(PersianCalendar.MONTH) + 1,
            calendar.get(PersianCalendar.DAY_OF_MONTH)
        )
    } else {
        val calendar = Calendar.getInstance(
            TimeZone.getTimeZone("UTC"),
            Locale.US
        )
        calendar.timeInMillis = millis

        String.format(
            Locale.US,
            "%04d/%02d/%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}

private fun parseExpiry(
    input: String,
    type: String
): String? {
    val clean = input.trim()
        .replace('-', '/')
        .replace('۰', '0').replace('۱', '1')
        .replace('۲', '2').replace('۳', '3')
        .replace('۴', '4').replace('۵', '5')
        .replace('۶', '6').replace('۷', '7')
        .replace('۸', '8').replace('۹', '9')
        .replace('٠', '0').replace('١', '1')
        .replace('٢', '2').replace('٣', '3')
        .replace('٤', '4').replace('٥', '5')
        .replace('٦', '6').replace('٧', '7')
        .replace('٨', '8').replace('٩', '9')

    val parts = clean.split('/')
    if (parts.size != 3) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31) return null

    return try {
        val millis: Long

        if (type == "شمسی") {
            val calendar = PersianCalendar(
                com.ibm.icu.util.TimeZone.getTimeZone("UTC"),
                Locale("fa", "IR")
            )
            calendar.clear()
            calendar.isLenient = false
            calendar.set(PersianCalendar.YEAR, year)
            calendar.set(PersianCalendar.MONTH, month - 1)
            calendar.set(PersianCalendar.DAY_OF_MONTH, day)

            millis = calendar.timeInMillis

            if (calendar.get(PersianCalendar.YEAR) != year ||
                calendar.get(PersianCalendar.MONTH) != month - 1 ||
                calendar.get(PersianCalendar.DAY_OF_MONTH) != day
            ) return null
        } else {
            val calendar = Calendar.getInstance(
                TimeZone.getTimeZone("UTC"),
                Locale.US
            )
            calendar.clear()
            calendar.isLenient = false
            calendar.set(year, month - 1, day)

            millis = calendar.timeInMillis
        }

        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    } catch (_: Exception) {
        null
    }
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
    var message by remember { mutableStateOf("") }

    val expired = items.count { expiryStatus(it.expiryIso) == 0 }
    val soon = items.count { expiryStatus(it.expiryIso) == 1 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("اورژانس‌یار", fontWeight = FontWeight.Bold)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showForm = true
                }
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "مدیریت موجودی دارو و تجهیزات",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    "کل اقلام",
                    items.size.toString(),
                    Modifier.weight(1f)
                )
                SummaryCard(
                    "نزدیک انقضا",
                    soon.toString(),
                    Modifier.weight(1f)
                )
                SummaryCard(
                    "منقضی",
                    expired.toString(),
                    Modifier.weight(1f)
                )
            }

            if (expired > 0 || soon > 0) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "هشدار: $expired قلم منقضی و $soon قلم با مهلت ۹۰ روز یا کمتر دارید. پیش از استفاده، تاریخ و سلامت اقلام را بررسی کنید.",
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("جست‌وجوی نام یا محل نگهداری") },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("همه", "دارو", "تجهیزات", "هشدارها").forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val filteredItems = items.filter {
                (it.name.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true)) &&
                    (
                        filter == "همه" ||
                            (filter == "دارو" && it.category == "دارو") ||
                            (filter == "تجهیزات" && it.category == "تجهیزات") ||
                            (filter == "هشدارها" &&
                                expiryStatus(it.expiryIso) != 2)
                    )
            }

            if (filteredItems.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (items.isEmpty()) {
                            "هنوز موردی ثبت نشده است.\nبرای افزودن دارو یا تجهیزات، دکمه + را بزنید."
                        } else {
                            "موردی پیدا نشد."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        StockCard(
                            item = item,
                            onEdit = {
                                editing = item
                                showForm = true
                            },
                            onDelete = {
                                if (db.remove(item.id)) {
                                    refresh++
                                    message = "قلم حذف شد."
                                } else {
                                    message = "حذف انجام نشد. دوباره تلاش کنید."
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            title = { Text("پیام برنامه") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { message = "" }) {
                    Text("باشه")
                }
            }
        )
    }

    if (showForm) {
        ItemDialog(
            item = editing,
            onDismiss = { showForm = false },
            onSave = { item ->
                if (db.save(item)) {
                    refresh++
                    showForm = false
                    message = "اطلاعات با موفقیت ذخیره شد."
                } else {
                    message = "ذخیره انجام نشد. فضای گوشی و اطلاعات واردشده را بررسی کنید."
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StockCard(
    item: StockItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val status = expiryStatus(item.expiryIso)

    val statusText = when (status) {
        0 -> "منقضی‌شده"
        1 -> "نزدیک انقضا (۹۰ روز)"
        else -> "وضعیت عادی"
    }

    val color = when (status) {
        0 -> MaterialTheme.colorScheme.errorContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("${item.category} • تعداد: ${item.quantity}")
            Text("محل نگهداری: ${item.location}")
            Text("انقضا (${item.dateType}): ${isoToDisplay(item.expiryIso, item.dateType)}")
            Text(statusText, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("ویرایش")
                }
                TextButton(onClick = onDelete) {
                    Text("حذف")
                }
            }
        }
    }
}

@Composable
private fun ItemDialog(
    item: StockItem?,
    onDismiss: () -> Unit,
    onSave: (StockItem) -> Unit
) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var category by remember(item) { mutableStateOf(item?.category ?: "دارو") }
    var quantity by remember(item) {
        mutableStateOf(item?.quantity?.toString() ?: "1")
    }
    var location by remember(item) { mutableStateOf(item?.location ?: "") }
    var dateType by remember(item) { mutableStateOf(item?.dateType ?: "شمسی") }
    var expiry by remember(item) {
        mutableStateOf(
            item?.let { isoToDisplay(it.expiryIso, it.dateType) } ?: ""
        )
    }
    var error by remember(item) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) "افزودن قلم جدید" else "ویرایش قلم")
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دارو یا تجهیزات") },
                    singleLine = true
                )

                Text("نوع قلم")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == "دارو",
                        onClick = { category = "دارو" },
                        label = { Text("دارو") }
                    )
                    FilterChip(
                        selected = category == "تجهیزات",
                        onClick = { category = "تجهیزات" },
                        label = { Text("تجهیزات") }
                    )
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                            .replace('۰', '0').replace('۱', '1')
                            .replace('۲', '2').replace('۳', '3')
                            .replace('۴', '4').replace('۵', '5')
                            .replace('۶', '6').replace('۷', '7')
                            .replace('۸', '8').replace('۹', '9')
                    },
                    label = { Text("تعداد موجودی") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("محل نگهداری") },
                    singleLine = true
                )

                Text("نوع تقویم تاریخ انقضا")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dateType == "شمسی",
                        onClick = {
                            if (dateType != "شمسی" && expiry.isNotBlank()) {
                                val iso = parseExpiry(expiry, dateType)
                                if (iso != null) {
                                    expiry = isoToDisplay(iso, "شمسی")
                                }
                            }
                            dateType = "شمسی"
                        },
                        label = { Text("شمسی") }
                    )

                    FilterChip(
                        selected = dateType == "میلادی",
                        onClick = {
                            if (dateType != "میلادی" && expiry.isNotBlank()) {
                                val iso = parseExpiry(expiry, dateType)
                                if (iso != null) {
                                    expiry = isoToDisplay(iso, "میلادی")
                                }
                            }
                            dateType = "میلادی"
                        },
                        label = { Text("میلادی") }
                    )
                }

                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it },
                    label = { Text("تاریخ انقضا (سال/ماه/روز)") },
                    placeholder = {
                        Text(if (dateType == "شمسی") "۱۴۰۵/۱۲/۲۹" else "2027/12/31")
                    },
                    singleLine = true
                )

                Text(
                    "هشدار انقضا برای ۹۰ روز آینده و موارد منقضی‌شده نمایش داده می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedQuantity = quantity
                        .replace('۰', '0').replace('۱', '1')
                        .replace('۲', '2').replace('۳', '3')
                        .replace('۴', '4').replace('۵', '5')
                        .replace('۶', '6').replace('۷', '7')
                        .replace('۸', '8').replace('۹', '9')
                        .replace('٠', '0').replace('١', '1')
                        .replace('٢', '2').replace('٣', '3')
                        .replace('٤', '4').replace('٥', '5')
                        .replace('٦', '6').replace('٧', '7')
                        .replace('٨', '8').replace('٩', '9')

                    val qty = normalizedQuantity.toIntOrNull()
                    val iso = parseExpiry(expiry, dateType)

                    when {
                        name.isBlank() ->
                            error = "نام قلم را وارد کنید."

                        qty == null || qty < 0 ->
                            error = "تعداد باید عدد صفر یا بیشتر باشد."

                        location.isBlank() ->
                            error = "محل نگهداری را وارد کنید."

                        iso == null ->
                            error = "تاریخ معتبر نیست؛ سال/ماه/روز را وارد کنید."

                        else -> onSave(
                            StockItem(
                                id = item?.id ?: 0L,
                                name = name.trim(),
                                category = category,
                                quantity = qty,
                                location = location.trim(),
                                expiryIso = iso,
                                dateType = dateType
                            )
                        )
                    }
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
