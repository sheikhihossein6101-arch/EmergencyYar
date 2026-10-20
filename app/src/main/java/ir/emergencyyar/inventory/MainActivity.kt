    }
    val color = when (status) {
        0 -> MaterialTheme.colorScheme.errorContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
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
private fun ItemDialog(
    item: StockItem?,
    ambulanceCode: String,
    onDismiss: () -> Unit,
    onSave: (StockItem) -> Unit
) {
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var category by remember(item) { mutableStateOf(item?.category ?: "دارو") }
    var quantity by remember(item) { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var location by remember(item) { mutableStateOf(item?.location ?: "") }
    var dateType by remember(item) { mutableStateOf(item?.dateType ?: "شمسی") }
    var expiry by remember(item) {
        mutableStateOf(item?.let { isoToDisplay(it.expiryIso, it.dateType) } ?: "")
    }
    var error by remember(item) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "افزودن قلم جدید" else "ویرایش قلم") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("کد آمبولانس: $ambulanceCode", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("نام دارو یا تجهیزات") }, singleLine = true
                )
                Text("نوع قلم")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == "دارو", onClick = { category = "دارو" }, label = { Text("دارو") })
                    FilterChip(selected = category == "تجهیزات", onClick = { category = "تجهیزات" }, label = { Text("تجهیزات") })
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = normalizeDigits(it) },
                    label = { Text("تعداد موجودی") }, singleLine = true
                )
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    label = { Text("محل نگهداری") }, singleLine = true
                )
                Text("نوع تقویم تاریخ انقضا")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dateType == "شمسی",
                        onClick = {
                            if (dateType != "شمسی" && expiry.isNotBlank()) {
                                parseExpiry(expiry, dateType)?.let { expiry = isoToDisplay(it, "شمسی") }
                            }
                            dateType = "شمسی"
                        }, label = { Text("شمسی") }
                    )
                    FilterChip(
                        selected = dateType == "میلادی",
                        onClick = {
                            if (dateType != "میلادی" && expiry.isNotBlank()) {
                                parseExpiry(expiry, dateType)?.let { expiry = isoToDisplay(it, "میلادی") }
                            }
                            dateType = "میلادی"
                        }, label = { Text("میلادی") }
                    )
                }
                OutlinedTextField(
                    value = expiry, onValueChange = { expiry = it },
                    label = { Text("تاریخ انقضا (سال/ماه/روز)") },
                    placeholder = { Text(if (dateType == "شمسی") "۱۴۰۵/۱۲/۲۹" else "2027/12/31") },
                    singleLine = true
                )
                Text("هشدار انقضا برای ۹۰ روز آینده و موارد منقضی‌شده نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = normalizeDigits(quantity).toIntOrNull()
                val iso = parseExpiry(expiry, dateType)
                when {
                    name.isBlank() -> error = "نام قلم را وارد کنید."
                    qty == null || qty < 0 -> error = "تعداد باید عدد صفر یا بیشتر باشد."
                    location.isBlank() -> error = "محل نگهداری را وارد کنید."
                    iso == null -> error = "تاریخ معتبر نیست؛ سال/ماه/روز را وارد کنید."
                    else -> onSave(
                        StockItem(
                            id = item?.id ?: 0L,
                            name = name.trim(),
                            category = category,
                            quantity = qty,
                            location = location.trim(),
                            expiryIso = iso,
                            dateType = dateType,
                            ambulanceCode = ambulanceCode
                        )
                    )
                }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
