/*
 * Copyright (C) 2009 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.cfsima.modernsafe.model

import android.content.Context
import android.util.Log
import android.widget.Toast
import io.github.cfsima.modernsafe.CryptoHelper
import io.github.cfsima.modernsafe.CryptoHelperException
import io.github.cfsima.modernsafe.DBHelper
import io.github.cfsima.modernsafe.R
import io.github.cfsima.modernsafe.password.Master
import java.text.DateFormat
import java.util.Collections
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Abstraction layer for storing encrypted and decrypted versions
 * of all PassEntry and CategoryEntry data.
 * Handles fetching and storing with DBHelper
 * and encrypt/decrypt of data.
 *
 * @author Randy McEoin
 */
object Passwords {

    private const val DEBUG = false
    private const val TAG = "Passwords"

    // Use ConcurrentHashMap for thread safety, though full synchronization on methods is safer for logic involving multiple steps
    private var passEntries: MutableMap<Long, PassEntry>? = null
    private var categoryEntries: MutableMap<Long, CategoryEntry>? = null
    private var packageAccessEntries: MutableMap<Long, ArrayList<PackageAccessEntry>>? = null

    private var ch: CryptoHelper? = null
    @Volatile
    var isCryptoInitialized: Boolean = false
        private set

    private var dbHelper: DBHelper? = null

    @Synchronized
    fun Initialize(ctx: Context): Boolean {
        if (DEBUG) {
            Log.d(TAG, "Initialize()")
        }

        if (ch == null) {
            ch = CryptoHelper()
        }
        if (!isCryptoInitialized &&
            Master.salt != null &&
            Master.masterKey != null
        ) {
            try {
                InitCrypto(
                    CryptoHelper.EncryptionMedium,
                    Master.salt!!, Master.masterKey!!
                )
                isCryptoInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    ctx, "CategoryList: " + ctx.getString(R.string.crypto_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (dbHelper == null) {
            dbHelper = DBHelper(ctx)
            if (!dbHelper!!.isDatabaseOpen()) {
                return false
            }
        }
        if (passEntries == null) {
            passEntries = ConcurrentHashMap()
            initPassEntries()
        }
        if (categoryEntries == null) {
            categoryEntries = ConcurrentHashMap()
            initCategoryEntries()
        }
        if (packageAccessEntries == null) {
            packageAccessEntries = ConcurrentHashMap()
            initPackageAccess()
        }
        return true
    }

    /**
     * Force a fresh load from the database.
     */
    @Synchronized
    fun Reset() {
        categoryEntries?.clear()
        initCategoryEntries()
        passEntries?.clear()
        initPassEntries()
        packageAccessEntries?.clear()
        initPackageAccess()
    }

    @Throws(Exception::class)
    fun InitCrypto(strength: Int, salt: String, masterKey: String) {
        if (DEBUG) {
            Log.d(TAG, "InitCrypto($strength,$salt,$masterKey)")
        }
        try {
            ch?.init(strength, salt)
            ch?.setPassword(masterKey)
        } catch (e1: CryptoHelperException) {
            e1.printStackTrace()
            throw Exception(
                "Error with Passwords.InitCrypto: " +
                        e1.localizedMessage
            )
        }
    }

    @Synchronized
    fun deleteAll() {
        dbHelper?.deleteDatabase()
        Reset()
    }

    val prePopulate: Boolean
        get() = DBHelper.needsPrePopulation

    fun clearPrePopulate() {
        DBHelper.clearPrePopulate()
    }

    fun fetchSalt(): String {
        return dbHelper?.fetchSalt() ?: ""
    }

    fun fetchMasterKeyEncrypted(): String {
        return dbHelper?.fetchMasterKey() ?: ""
    }

    ///////////////////////////////////////////////////
    ///////////// Category Functions //////////////////
    ///////////////////////////////////////////////////

    private fun initCategoryEntries() {
        val catRows = dbHelper?.fetchAllCategoryRows() ?: return
        for (catRow in catRows) {
            catRow.nameNeedsDecrypt = true
            catRow.plainNameNeedsEncrypt = false
            categoryEntries?.put(catRow.id, catRow)
        }
    }

    @Synchronized
    fun getCategoryEntries(): List<CategoryEntry> {
        val categories = categoryEntries?.values ?: return emptyList()
        val iterator = categories.iterator()
        while (iterator.hasNext()) {
            val catEntry = iterator.next()
            // run through and ensure all entries are decrypted
            getCategoryEntry(catEntry.id)
        }
        val catList = ArrayList(categories)
        Collections.sort(catList) { o1, o2 ->
            val o1PlainName = plainName(o1)
            val o2PlainName = plainName(o2)
            o1PlainName.compareTo(o2PlainName, ignoreCase = true)
        }
        return catList
    }

    private fun plainName(categoryEntry: CategoryEntry?): String {
        return categoryEntry?.plainName ?: ""
    }

    fun getCategoryNames(): List<String> {
        val items = ArrayList<String>()
        val catList = getCategoryEntries()
        for (row in catList) {
            row.plainName?.let { items.add(it) }
        }
        return items
    }

    @Synchronized
    fun getCategoryIdToName(): HashMap<Long, String> {
        val categoryMap = HashMap<Long, String>()
        val categories = categoryEntries?.values ?: return categoryMap
        for (catEntry in categories) {
            getCategoryEntry(catEntry.id)
            catEntry.plainName?.let { categoryMap[catEntry.id] = it }
        }
        return categoryMap
    }

    @Synchronized
    fun getCategoryNameToId(): HashMap<String, Long> {
        val categoryMap = HashMap<String, Long>()
        val categories = categoryEntries?.values ?: return categoryMap
        for (catEntry in categories) {
            getCategoryEntry(catEntry.id)
            catEntry.plainName?.let {
                categoryMap[it] = catEntry.id
                if (DEBUG) {
                    Log.d(TAG, "map $it to ${catEntry.id}")
                }
            }
        }
        return categoryMap
    }

    @Synchronized
    fun getCategoryEntryByName(category: String): CategoryEntry? {
        val categories = categoryEntries?.values ?: return null
        for (catEntry in categories) {
            // Ensure decrypted first? Logic implies we iterate over loaded entries.
            // But if name is encrypted, we can't compare 'category' (plain) with 'catEntry.name' (encrypted).
            // We need 'plainName'. So we must ensure it's decrypted.
            getCategoryEntry(catEntry.id)
            if (catEntry.plainName == category) {
                return catEntry
            }
        }
        return null
    }

    @Synchronized
    fun getCategoryEntry(id: Long): CategoryEntry? {
        val catEntry = categoryEntries?.get(id) ?: return null

        if (catEntry.nameNeedsDecrypt) {
            if (DEBUG) {
                Log.d(TAG, "decrypt cat")
            }
            try {
                catEntry.plainName = ch?.decrypt(catEntry.name)
            } catch (e: CryptoHelperException) {
                Log.e(TAG, e.toString())
            }
            catEntry.nameNeedsDecrypt = false
            categoryEntries?.put(id, catEntry)
        }
        // Update count from DB? This seems inefficient to do on every get, but follows legacy logic.
        catEntry.count = dbHelper?.getCategoryCount(id) ?: 0
        return catEntry
    }

    @Synchronized
    fun putCategoryEntry(catEntry: CategoryEntry): Long {
        if (catEntry.plainNameNeedsEncrypt) {
            if (DEBUG) {
                Log.d(TAG, "encrypt cat")
            }
            try {
                catEntry.name = ch?.encrypt(catEntry.plainName ?: "") ?: ""
            } catch (e: CryptoHelperException) {
                Log.e(TAG, e.toString())
            }
            catEntry.plainNameNeedsEncrypt = false
        }
        if (catEntry.id == -1L) {
            catEntry.id = dbHelper?.addCategory(catEntry) ?: -1L
        } else {
            dbHelper?.updateCategory(catEntry.id, catEntry)
        }
        categoryEntries?.put(catEntry.id, catEntry)
        return catEntry.id
    }

    @Synchronized
    fun updateCategoryCount(id: Long) {
        val catEntry = categoryEntries?.get(id) ?: return
        catEntry.count = dbHelper?.getCategoryCount(id) ?: 0
    }

    @Synchronized
    fun deleteCategoryEntry(id: Long) {
        if (DEBUG) {
            Log.d(TAG, "deleteCategoryEntry($id)")
        }
        dbHelper?.deleteCategory(id)
        categoryEntries?.remove(id)
    }

    ///////////////////////////////////////////////////
    ///////////// Password Functions //////////////////
    ///////////////////////////////////////////////////

    private fun initPassEntries() {
        val passRows = dbHelper?.fetchAllRows(0) ?: return
        for (passRow in passRows) {
            passRow.needsDecryptDescription = true
            passRow.needsDecrypt = true
            passRow.needsEncrypt = false
            passEntries?.put(passRow.id, passRow)
        }
    }

    @Synchronized
    fun getPassEntries(categoryId: Long, decrypt: Boolean, descriptionOnly: Boolean): List<PassEntry> {
        if (DEBUG) {
            Log.d(TAG, "getPassEntries($categoryId,$decrypt,$descriptionOnly)")
        }
        val passwords = passEntries?.values ?: return emptyList()
        val passList = ArrayList<PassEntry>()
        for (passEntry in passwords) {
            if (categoryId == 0L || passEntry.category == categoryId) {
                getPassEntry(passEntry.id, decrypt, descriptionOnly)
                passList.add(passEntry)
            }
        }
        if (decrypt) {
            Collections.sort(passList) { o1, o2 ->
                o1.plainDescription.compareTo(o2.plainDescription, ignoreCase = true)
            }
        }
        return passList
    }

    @Synchronized
    fun getPassEntry(id: Long?, decrypt: Boolean, descriptionOnly: Boolean): PassEntry? {
        if (DEBUG) {
            Log.d(TAG, "getPassEntry($id)")
        }
        if (id == null || passEntries == null) {
            return null
        }
        val passEntry = passEntries?.get(id) ?: return null

        if (!decrypt) {
            return passEntry
        }
        if (passEntry.needsDecryptDescription) {
            try {
                passEntry.plainDescription = ch?.decrypt(passEntry.description) ?: ""
            } catch (e: CryptoHelperException) {
                Log.e(TAG, e.toString())
            }
            passEntry.needsDecryptDescription = false
            passEntries?.put(id, passEntry)
        }
        if (!descriptionOnly && passEntry.needsDecrypt) {
            if (DEBUG) {
                Log.d(TAG, "decrypt pass")
            }
            try {
                // Ensure non-null decryption results if original was encrypted (which it should be)
                // If original was null (shouldn't be for description/password), decrypt handles empty string?
                // Legacy logic relied on catch blocks or implicit behavior.
                passEntry.plainDescription = ch?.decrypt(passEntry.description) ?: ""
                passEntry.plainWebsite = ch?.decrypt(passEntry.website ?: "")
                passEntry.plainUsername = ch?.decrypt(passEntry.username ?: "")
                passEntry.plainPassword = ch?.decrypt(passEntry.password)
                passEntry.plainNote = ch?.decrypt(passEntry.note ?: "")
                passEntry.plainUniqueName = ch?.decrypt(passEntry.uniqueName ?: "")
            } catch (e: CryptoHelperException) {
                Log.e(TAG, e.toString())
            }
            passEntry.needsDecrypt = false
            passEntries?.put(id, passEntry)
        }
        return passEntry
    }

    @Synchronized
    fun findPassWithUniqueName(plainUniqueName: String): PassEntry? {
        val uniqueName: String
        try {
            uniqueName = ch?.encrypt(plainUniqueName) ?: ""
        } catch (e: CryptoHelperException) {
            Log.e(TAG, e.toString())
            return null
        }
        val passwords = passEntries?.values ?: return null
        for (passEntry in passwords) {
            if (passEntry.uniqueName == uniqueName) {
                return getPassEntry(passEntry.id, decrypt = true, descriptionOnly = false)
            }
        }
        return null
    }

    /**
     * @param passEntry the entry to placed into the password cache.  If id is 0,
     *                  then it will be added, otherwise it will update existing entry.
     * @return long row id of newly added or updated entry,
     * equal to -1 if a sql error occurred
     */
    @Synchronized
    fun putPassEntry(passEntry: PassEntry): Long {
        if (DEBUG) {
            Log.d(TAG, "putPassEntry(${passEntry.id})")
        }
        if (passEntry.needsEncrypt) {
            if (DEBUG) {
                Log.d(TAG, "encrypt pass")
            }
            try {
                passEntry.description = ch?.encrypt(passEntry.plainDescription) ?: ""
                passEntry.website = ch?.encrypt(passEntry.plainWebsite ?: "")
                passEntry.username = ch?.encrypt(passEntry.plainUsername ?: "")
                passEntry.password = ch?.encrypt(passEntry.plainPassword ?: "") ?: ""
                passEntry.note = ch?.encrypt(passEntry.plainNote ?: "")
                passEntry.uniqueName = ch?.encrypt(passEntry.plainUniqueName ?: "")
            } catch (e: CryptoHelperException) {
                Log.e(TAG, e.toString())
            }
            passEntry.needsEncrypt = false
        }
        // Format the current time.
        val date = Date()
        val df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.LONG)
        passEntry.lastEdited = df.format(date)

        if (passEntry.id == 0L) {
            passEntry.id = dbHelper?.addPassword(passEntry) ?: -1L
            if (passEntry.id == -1L) {
                // error adding
                return -1
            }
            updateCategoryCount(passEntry.category)
        } else {
            val success = dbHelper?.updatePassword(passEntry.id, passEntry) ?: -1L
            if (success == -1L) {
                return -1
            }
        }
        passEntries?.put(passEntry.id, passEntry)
        return passEntry.id
    }

    @Synchronized
    fun deletePassEntry(id: Long) {
        if (DEBUG) {
            Log.d(TAG, "deletePassEntry($id)")
        }
        val passEntry = getPassEntry(id, decrypt = false, descriptionOnly = false) ?: return
        val categoryId = passEntry.category
        dbHelper?.deletePassword(id)
        passEntries?.remove(id)
        updateCategoryCount(categoryId)
    }

    @Synchronized
    fun updatePassCategory(passId: Long, categoryId: Long) {
        val passEntry = passEntries?.get(passId)
        if (passEntry != null) {
            passEntry.category = categoryId
            dbHelper?.updatePasswordCategory(passId, categoryId)
        }
    }

    /**
     * @param categoryId if -1 then count all passwords? Logic: DBHelper uses >0 check.
     * Note: DBHelper countPasswords logic: if categoryId > 0 selection "category=...", else null (all).
     */
    fun countPasswords(categoryId: Long): Int {
        return dbHelper?.countPasswords(categoryId) ?: 0
    }

    ///////////////////////////////////////////////////
    ////////// Package Access Functions ///////////////
    ///////////////////////////////////////////////////

    private fun initPackageAccess() {
        val dbPackageAccess = dbHelper?.fetchPackageAccessAll() ?: return
        if (dbPackageAccess.isNotEmpty()) {
            for ((key, dbPackageNames) in dbPackageAccess) {
                val packageNames = ArrayList<PackageAccessEntry>()
                for (packageName in dbPackageNames) {
                    val packageAccess = PackageAccessEntry()
                    packageAccess.packageAccess = packageName
                    packageAccess.needsDecrypt = true
                    packageAccess.needsEncrypt = false
                    packageNames.add(packageAccess)
                }
                packageAccessEntries?.put(key, packageNames)
            }
        }
    }

    @Synchronized
    fun getPackageAccess(id: Long): ArrayList<String>? {
        var packageAccess: ArrayList<String>? = null
        if (packageAccessEntries?.containsKey(id) == true) {
            val packageAccessEntryList = packageAccessEntries?.get(id) ?: return null
            packageAccess = ArrayList()
            for (packEntry in packageAccessEntryList) {
                if (packEntry.needsDecrypt) {
                    try {
                        packEntry.plainPackageAccess = ch?.decrypt(packEntry.packageAccess) ?: ""
                    } catch (e: CryptoHelperException) {
                        Log.e(TAG, e.toString())
                    }
                }
                packageAccess.add(packEntry.plainPackageAccess)
            }
        }
        return packageAccess
    }

    @Synchronized
    fun getPackageAccessEntries(id: Long): ArrayList<PackageAccessEntry>? {
        return packageAccessEntries?.get(id)
    }

    @Synchronized
    fun addPackageAccess(id: Long, packageName: String) {
        val encryptedPackageName: String
        try {
            encryptedPackageName = ch?.encrypt(packageName) ?: ""
            dbHelper?.addPackageAccess(id, encryptedPackageName)
        } catch (e: CryptoHelperException) {
            Log.e(TAG, e.toString())
            return
        }

        val packageNames: ArrayList<PackageAccessEntry> = if (packageAccessEntries?.containsKey(id) == true) {
            packageAccessEntries?.get(id) ?: ArrayList()
        } else {
            ArrayList()
        }
        val newPackageAccessEntry = PackageAccessEntry()
        newPackageAccessEntry.plainPackageAccess = packageName
        newPackageAccessEntry.packageAccess = encryptedPackageName
        newPackageAccessEntry.needsDecrypt = false
        newPackageAccessEntry.needsEncrypt = false
        packageNames.add(newPackageAccessEntry)
        packageAccessEntries?.put(id, packageNames)
    }
}
