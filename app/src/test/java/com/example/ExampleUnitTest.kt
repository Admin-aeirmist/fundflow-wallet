package com.example

import com.example.data.model.Account
import com.example.data.model.Transaction
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testJsonSerializationAndParsing() {
        val account = Account(id = 1, name = "বিকাশ", isDefault = true)
        val tx = Transaction(
            id = 1,
            type = "income",
            accountName = "বিকাশ",
            amount = 50000.0,
            date = "2026-03-01",
            description = "মাসিক বেতন"
        )

        val rootJson = JSONObject().apply {
            val accArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", account.id)
                    put("name", account.name)
                    put("isDefault", account.isDefault)
                })
            }
            val txArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type)
                    put("accountName", tx.accountName)
                    put("amount", tx.amount)
                    put("date", tx.date)
                    put("description", tx.description)
                })
            }
            put("accounts", accArray)
            put("transactions", txArray)
        }

        val jsonString = rootJson.toString()
        assertTrue(jsonString.contains("বিকাশ"))
        assertTrue(jsonString.contains("50000"))

        val parsed = JSONObject(jsonString)
        val parsedAccs = parsed.getJSONArray("accounts")
        val parsedTxs = parsed.getJSONArray("transactions")

        assertEquals(1, parsedAccs.length())
        assertEquals("বিকাশ", parsedAccs.getJSONObject(0).getString("name"))

        assertEquals(1, parsedTxs.length())
        assertEquals(50000.0, parsedTxs.getJSONObject(0).getDouble("amount"), 0.001)
    }
}
