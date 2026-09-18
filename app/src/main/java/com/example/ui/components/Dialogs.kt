package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.DpsScheme
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.ProjectEntry
import com.example.data.model.Transaction
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun AccountDropdownField(
    label: String,
    accounts: List<Account>,
    selectedAccount: String,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedAccount,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select Account",
                    modifier = Modifier.clickable { expanded = true }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name) },
                    onClick = {
                        onAccountSelected(acc.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (from: String, to: String, amount: Double, fee: Double, date: String, note: String) -> Unit
) {
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "ক্যাশ") }
    var toAccount by remember { mutableStateOf(accounts.getOrNull(1)?.name ?: "বিকাশ") }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("0") }
    var dateText by remember { mutableStateOf(DateUtils.today()) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🔄 একাউন্ট ট্রান্সফার",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "এক মাধ্যম থেকে অন্য মাধ্যমে টাকা স্থানান্তর করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AccountDropdownField(
                    label = "উৎস মাধ্যম (যেখান থেকে কাটবে)",
                    accounts = accounts,
                    selectedAccount = fromAccount,
                    onAccountSelected = { fromAccount = it }
                )

                AccountDropdownField(
                    label = "গন্তব্য মাধ্যম (যেখানে জমা হবে)",
                    accounts = accounts,
                    selectedAccount = toAccount,
                    onAccountSelected = { toAccount = it }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMessage = null },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )

                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("ট্রান্সফার ফি / চার্জ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("নোট / বিবরণ (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: ক্যাশআউট, সেন্ডমানি") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val fee = feeText.toDoubleOrNull() ?: 0.0
                    if (fromAccount == toAccount) {
                        errorMessage = "উৎস ও গন্তব্য মাধ্যম আলাদা হতে হবে"
                        return@Button
                    }
                    if (amount <= 0) {
                        errorMessage = "সঠিক টাকার পরিমাণ লিখুন"
                        return@Button
                    }
                    onConfirm(fromAccount, toAccount, amount, fee, dateText, noteText)
                },
                modifier = Modifier.testTag("transfer_confirm_button")
            ) {
                Text("ট্রান্সফার করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun SettlePersonDialog(
    person: Person,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, accountName: String, date: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format("%.2f", person.balance)) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "ক্যাশ") }
    var dateText by remember { mutableStateOf(DateUtils.today()) }
    var error by remember { mutableStateOf<String?>(null) }

    val isRec = person.type == "receivable"
    val actionTitle = if (isRec) "পাওনা আদায় নিষ্পত্তি" else "দেনা পরিশোধ নিষ্পত্তি"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(actionTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${person.name}-এর অবশিষ্ট স্থিতি: ${CurrencyFormatter.formatBDT(person.balance)}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                AccountDropdownField(
                    label = if (isRec) "যে মাধ্যমে টাকা গ্রহণ করবেন" else "যে মাধ্যম থেকে টাকা দিবেন",
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAccountSelected = { selectedAccount = it }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("পরিশোধের পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("settle_amount_input")
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "সঠিক পরিমাণ লিখুন"
                        return@Button
                    }
                    if (amount > person.balance) {
                        error = "পরিশোধের পরিমাণ অবশিষ্ট স্থিতির চেয়ে বেশি হতে পারে না"
                        return@Button
                    }
                    onConfirm(amount, selectedAccount, dateText)
                },
                modifier = Modifier.testTag("settle_confirm_button")
            ) {
                Text("নিষ্পত্তি নিশ্চিত করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun ExtendDueDateDialog(
    person: Person,
    onDismiss: () -> Unit,
    onConfirm: (newDueDate: String) -> Unit
) {
    var newDueDate by remember { mutableStateOf(DateUtils.daysFromNow(14)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("সময়সীমা বৃদ্ধি (${person.name})", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("বর্তমান পরিশোধের তারিখ: ${person.dueDate}")
                OutlinedTextField(
                    value = newDueDate,
                    onValueChange = { newDueDate = it },
                    label = { Text("নতুন পরিশোধের তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().testTag("new_due_date_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newDueDate) },
                modifier = Modifier.testTag("extend_confirm_button")
            ) {
                Text("আপডেট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun DpsPaymentDialog(
    dps: DpsScheme,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (accountName: String, amount: Double, date: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format("%.2f", dps.monthlyAmount)) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "ক্যাশ") }
    var dateText by remember { mutableStateOf(DateUtils.today()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ডিপিএস কিস্তি জমা (${dps.bankName})", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("নির্ধারিত কিস্তি: ${CurrencyFormatter.formatBDT(dps.monthlyAmount)} | জমা দিন: ${dps.dayOfMonth} তারিখ")
                AccountDropdownField(
                    label = "যে মাধ্যম থেকে কিস্তি দিবেন",
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAccountSelected = { selectedAccount = it }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("কিস্তির পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("dps_pay_amount_input")
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("জমার তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(selectedAccount, amount, dateText)
                    }
                },
                modifier = Modifier.testTag("dps_pay_confirm_button")
            ) {
                Text("কিস্তি জমা করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun AddPersonDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, type: String, amount: Double, accountName: String, txDate: String, dueDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("receivable") } // receivable or payable
    var amountText by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "ক্যাশ") }
    var txDate by remember { mutableStateOf(DateUtils.today()) }
    var dueDate by remember { mutableStateOf(DateUtils.daysFromNow(7)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("👥 নতুন ব্যক্তি যোগ (খাতা)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("ব্যক্তির নাম *") },
                    placeholder = { Text("যেমন: করিম সাহেব") },
                    modifier = Modifier.fillMaxWidth().testTag("person_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                    placeholder = { Text("017xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("person_phone_input")
                )

                Text("লেনদেনের ধরন:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == "receivable",
                        onClick = { type = "receivable" }
                    )
                    Text("আমি পাবো (পাওনা)", modifier = Modifier.clickable { type = "receivable" })
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = type == "payable",
                        onClick = { type = "payable" }
                    )
                    Text("সে পাবে (দেনা)", modifier = Modifier.clickable { type = "payable" })
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("person_amount_input")
                )

                AccountDropdownField(
                    label = "সম্পর্কিত মাধ্যম",
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAccountSelected = { selectedAccount = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = txDate,
                        onValueChange = { txDate = it },
                        label = { Text("লেনদেনের তারিখ") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("পরিশোধের তারিখ") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isBlank()) {
                        error = "নাম আবশ্যক"
                        return@Button
                    }
                    if (amount <= 0) {
                        error = "সঠিক পরিমাণ লিখুন"
                        return@Button
                    }
                    onConfirm(name, phone, type, amount, selectedAccount, txDate, dueDate)
                },
                modifier = Modifier.testTag("add_person_confirm_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun AddDpsDialog(
    onDismiss: () -> Unit,
    onConfirm: (bankName: String, monthlyAmount: Double, dayOfMonth: Int, startDate: String, durationYears: Int) -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    var monthlyAmountText by remember { mutableStateOf("") }
    var dayOfMonthText by remember { mutableStateOf("10") }
    var startDate by remember { mutableStateOf(DateUtils.today()) }
    var durationYearsText by remember { mutableStateOf("5") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🏦 নতুন ডিপিএস স্কিম", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; error = null },
                    label = { Text("ব্যাংক / প্রতিষ্ঠানের নাম *") },
                    placeholder = { Text("যেমন: সোনালী ব্যাংক, ব্র্যাক ব্যাংক") },
                    modifier = Modifier.fillMaxWidth().testTag("dps_bank_input")
                )

                OutlinedTextField(
                    value = monthlyAmountText,
                    onValueChange = { monthlyAmountText = it; error = null },
                    label = { Text("প্রতি মাসের কিস্তি (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("dps_monthly_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayOfMonthText,
                        onValueChange = { dayOfMonthText = it },
                        label = { Text("জমার দিন (১-৩১)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationYearsText,
                        onValueChange = { durationYearsText = it },
                        label = { Text("মেয়াদ (বছর)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("শুরুর তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monthly = monthlyAmountText.toDoubleOrNull() ?: 0.0
                    val day = dayOfMonthText.toIntOrNull() ?: 10
                    val years = durationYearsText.toIntOrNull() ?: 5
                    if (bankName.isBlank()) {
                        error = "ব্যাংক নাম লিখুন"
                        return@Button
                    }
                    if (monthly <= 0) {
                        error = "সঠিক কিস্তির পরিমাণ লিখুন"
                        return@Button
                    }
                    onConfirm(bankName, monthly, day.coerceIn(1, 31), startDate, years)
                },
                modifier = Modifier.testTag("add_dps_confirm_button")
            ) {
                Text("ডিপিএস খুলুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📁 নতুন প্রজেক্ট তৈরি", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("প্রজেক্টের নাম *") },
                    placeholder = { Text("যেমন: বাড়ি নির্মাণ, ফ্ল্যাট সংস্কার, ফ্রিল্যান্স") },
                    modifier = Modifier.fillMaxWidth().testTag("project_name_input")
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "প্রজেক্টের নাম দিন"
                        return@Button
                    }
                    onConfirm(name)
                },
                modifier = Modifier.testTag("create_project_confirm_button")
            ) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun ProjectEntryDialog(
    project: Project,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (type: String, accountName: String, amount: Double, date: String, desc: String) -> Unit
) {
    var type by remember { mutableStateOf("expense") } // expense or income
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "ক্যাশ") }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(DateUtils.today()) }
    var descText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("প্রজেক্ট এন্ট্রি (${project.name})", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == "expense",
                        onClick = { type = "expense" }
                    )
                    Text("প্রজেক্ট খরচ (-)", modifier = Modifier.clickable { type = "expense" })
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = type == "income",
                        onClick = { type = "income" }
                    )
                    Text("প্রজেক্ট আয় (+)", modifier = Modifier.clickable { type = "income" })
                }

                AccountDropdownField(
                    label = "পেমেন্ট মাধ্যম",
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAccountSelected = { selectedAccount = it }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("project_entry_amount_input")
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("বিবরণ *") },
                    placeholder = { Text("যেমন: সিমেন্ট কেনা, রাজমিস্ত্রি মজুরি...") },
                    modifier = Modifier.fillMaxWidth().testTag("project_entry_desc_input")
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "সঠিক পরিমাণ লিখুন"
                        return@Button
                    }
                    if (descText.isBlank()) {
                        error = "বিবরণ আবশ্যক"
                        return@Button
                    }
                    onConfirm(type, selectedAccount, amount, dateText, descText)
                },
                modifier = Modifier.testTag("project_entry_confirm_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, initialBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("💳 নতুন মাধ্যম যোগ", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("মাধ্যমের নাম *") },
                    placeholder = { Text("যেমন: রকেট, উপায়, সিটি ব্যাংক") },
                    modifier = Modifier.fillMaxWidth().testTag("new_account_name_input")
                )
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("প্রারম্ভিক স্থিতি (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "মাধ্যমের নাম দিন"
                        return@Button
                    }
                    val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                    onConfirm(name, initBal)
                },
                modifier = Modifier.testTag("save_account_confirm_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    onDelete: () -> Unit
) {
    var type by remember { mutableStateOf(transaction.type) }
    var account by remember { mutableStateOf(transaction.accountName) }
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var date by remember { mutableStateOf(transaction.date) }
    var desc by remember { mutableStateOf(transaction.description) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ লেনদেন সম্পাদন করুন") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "expense" }) {
                        RadioButton(selected = type == "expense", onClick = { type = "expense" })
                        Text("ব্যয়")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "income" }) {
                        RadioButton(selected = type == "income", onClick = { type = "income" })
                        Text("আয়")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "pending" }) {
                        RadioButton(selected = type == "pending", onClick = { type = "pending" })
                        Text("পেন্ডিং")
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                AccountDropdownField(
                    label = "হিসাব মাধ্যম *",
                    accounts = accounts,
                    selectedAccount = account,
                    onAccountSelected = { account = it }
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it; error = null },
                    label = { Text("বিবরণ *") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("এই লেনদেনটি মুছে ফেলুন")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "সঠিক পরিমাণ দিন"
                        return@Button
                    }
                    if (desc.isBlank()) {
                        error = "বিবরণ আবশ্যক"
                        return@Button
                    }
                    onConfirm(
                        transaction.copy(
                            type = type,
                            accountName = account,
                            amount = amount,
                            date = date.trim(),
                            description = desc.trim()
                        )
                    )
                }
            ) {
                Text("আপডেট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun EditProjectNameDialog(
    project: Project,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ প্রজেক্টের নাম পরিবর্তন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("প্রজেক্টের নাম *") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "প্রজেক্টের নাম দিন"
                        return@Button
                    }
                    onConfirm(name.trim())
                }
            ) {
                Text("পরিবর্তন করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun EditProjectEntryDialog(
    entry: ProjectEntry,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (ProjectEntry) -> Unit,
    onDelete: () -> Unit
) {
    var type by remember { mutableStateOf(entry.type) }
    var account by remember { mutableStateOf(entry.accountName) }
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var date by remember { mutableStateOf(entry.date) }
    var desc by remember { mutableStateOf(entry.description) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ প্রজেক্ট এন্ট্রি সম্পাদন") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "income" }) {
                        RadioButton(selected = type == "income", onClick = { type = "income" })
                        Text("আয়")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "expense" }) {
                        RadioButton(selected = type == "expense", onClick = { type = "expense" })
                        Text("ব্যয়")
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                AccountDropdownField(
                    label = "মাধ্যম *",
                    accounts = accounts,
                    selectedAccount = account,
                    onAccountSelected = { account = it }
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it; error = null },
                    label = { Text("বিবরণ *") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("এই এন্ট্রি মুছে ফেলুন")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "সঠিক পরিমাণ দিন"
                        return@Button
                    }
                    if (desc.isBlank()) {
                        error = "বিবরণ আবশ্যক"
                        return@Button
                    }
                    onConfirm(
                        entry.copy(
                            type = type,
                            accountName = account,
                            amount = amount,
                            date = date.trim(),
                            description = desc.trim()
                        )
                    )
                }
            ) {
                Text("আপডেট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun DateRangeReportDialog(
    transactions: List<Transaction>,
    projectEntries: List<ProjectEntry>,
    onDismiss: () -> Unit,
    onShareReport: (String) -> Unit
) {
    var startDate by remember { mutableStateOf(DateUtils.today().substring(0, 8) + "01") }
    var endDate by remember { mutableStateOf(DateUtils.today()) }

    val filteredTx = remember(transactions, startDate, endDate) {
        transactions.filter { it.date in startDate..endDate }
    }
    val filteredProjectEntries = remember(projectEntries, startDate, endDate) {
        projectEntries.filter { it.date in startDate..endDate }
    }

    val totalIncome = remember(filteredTx) {
        filteredTx.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTx) {
        filteredTx.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalPending = remember(filteredTx) {
        filteredTx.filter { it.type == "pending" }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val projIncome = remember(filteredProjectEntries) {
        filteredProjectEntries.filter { it.type == "income" }.sumOf { it.amount }
    }
    val projExpense = remember(filteredProjectEntries) {
        filteredProjectEntries.filter { it.type == "expense" }.sumOf { it.amount }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📊 তারিখ অনুযায়ী হিসাব বিবরণী") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("শুরু তারিখ") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("শেষ তারিখ") },
                        modifier = Modifier.weight(1f)
                    )
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ব্যক্তিগত হিসাব সারসংক্ষেপ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট আয়:")
                            Text(CurrencyFormatter.formatBDT(totalIncome), color = AccentEmerald, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট ব্যয়:")
                            Text(CurrencyFormatter.formatBDT(totalExpense), color = AccentRose, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("নিট স্থিতি:")
                            Text(CurrencyFormatter.formatBDT(netBalance), color = if (netBalance >= 0) AccentEmerald else AccentRose, fontWeight = FontWeight.ExtraBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("পেন্ডিং বিল:")
                            Text(CurrencyFormatter.formatBDT(totalPending), color = AccentAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("প্রজেক্ট হিসাব সারসংক্ষেপ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রজেক্ট আয়:")
                            Text(CurrencyFormatter.formatBDT(projIncome), color = AccentEmerald, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রজেক্ট ব্যয়:")
                            Text(CurrencyFormatter.formatBDT(projExpense), color = AccentRose, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("প্রজেক্ট লাভ/ক্ষতি:")
                            Text(CurrencyFormatter.formatBDT(projIncome - projExpense), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Text(
                    "মোট ${filteredTx.size} টি ব্যক্তিগত এবং ${filteredProjectEntries.size} টি প্রজেক্ট লেনদেন রেকর্ড পাওয়া গেছে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reportText = buildString {
                        appendLine("📊 FundFlow - আর্থিক বিবরণী রিপোর্ট")
                        appendLine("সময়কাল: $startDate থেকে $endDate")
                        appendLine("----------------------------------")
                        appendLine("📈 ব্যক্তিগত মোট আয়: ${CurrencyFormatter.formatBDT(totalIncome)}")
                        appendLine("📉 ব্যক্তিগত মোট ব্যয়: ${CurrencyFormatter.formatBDT(totalExpense)}")
                        appendLine("💵 নিট উদ্বৃত্ত: ${CurrencyFormatter.formatBDT(netBalance)}")
                        appendLine("⌛ পেন্ডিং বিল: ${CurrencyFormatter.formatBDT(totalPending)}")
                        appendLine("----------------------------------")
                        appendLine("🏗️ প্রজেক্ট আয়: ${CurrencyFormatter.formatBDT(projIncome)}")
                        appendLine("🏗️ প্রজেক্ট ব্যয়: ${CurrencyFormatter.formatBDT(projExpense)}")
                        appendLine("🏗️ প্রজেক্ট লাভ/ক্ষতি: ${CurrencyFormatter.formatBDT(projIncome - projExpense)}")
                        appendLine("----------------------------------")
                        appendLine("তৈরির তারিখ: ${DateUtils.today()}")
                    }
                    onShareReport(reportText)
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("শেয়ার বা প্রিন্ট রিপোর্ট")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun GoogleSheetsSyncDialog(
    currentUrl: String,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onSyncNow: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📊 Google Sheets সিঙ্ক ও ব্যাকআপ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "গুগল শিটের Web App URL এর মাধ্যমে আপনার ফান্ডফ্লোর সমস্ত হিসাব সরাসরি স্প্রেডশিটে ব্যাকআপ সংরক্ষণ করতে পারেন।",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        onSaveUrl(it)
                    },
                    label = { Text("Google Apps Script Web App URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "✓ আপনার গুগল শিট সাফল্যের সাথে কানেক্ট করা আছে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSyncNow(urlText) },
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পাঠানো হচ্ছে...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📤 সব হিসাব গুগল শিটে পাঠান")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun CloudSyncDialog(
    currentPin: String,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
    onPush: (String) -> Unit,
    onPull: (String) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var pinText by remember { mutableStateOf(currentPin) }
    var showImportField by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("☁️ ক্লাউড সিঙ্ক ও পিন কোড") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "যেকোনো ৩-৬ ডিজিটের পিন (যেমন ১২৩৪) ব্যবহার করে আপনার সমস্ত হিসাব ক্লাউডে ব্যাকআপ রাখতে পারেন এবং অন্য ডিভাইসে রিস্টোর করতে পারেন।",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        pinText = it
                        onSavePin(it)
                    },
                    label = { Text("আপনার ব্যাকআপ পিন (PIN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("যেমন: 1234") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onPush(pinText) },
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("আপলোড")
                    }

                    Button(
                        onClick = { onPull(pinText) },
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("রিস্টোর")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("অফলাইন ফাইল ব্যাকআপ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportJson,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("JSON কপি")
                    }
                    OutlinedButton(
                        onClick = { showImportField = !showImportField },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("JSON পেস্ট")
                    }
                }

                if (showImportField) {
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("ব্যাকআপ JSON টেক্সট দিন") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                onImportJson(importJsonText)
                                showImportField = false
                                importJsonText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ইম্পোর্ট সম্পন্ন করুন")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("সম্পন্ন")
            }
        }
    )
}
