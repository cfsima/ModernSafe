/* $Id$
 *
 * Copyright 2007-2008 Steven Osborn
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
package io.github.cfsima.modernsafe

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDiskIOException
import android.util.Log
import io.github.cfsima.modernsafe.model.CategoryEntry
import io.github.cfsima.modernsafe.model.PassEntry
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * DBHelper class.
 * <p/>
 * The overall theme of this class was borrowed from the Notepad
 * example Open Handset Alliance website.  It's essentially a very
 * primitive database layer.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
class DBHelper(private val context: Context) {

    private var db: SQLiteDatabase? = null
    var needsUpgrade: Boolean = false
        private set

    companion object {
        private const val DEBUG = false
        private const val TAG = "DBHelper"

        private const val DATABASE_NAME = "safe"
        private const val TABLE_DBVERSION = "dbversion"
        private const val TABLE_PASSWORDS = "passwords"
        private const val TABLE_CATEGORIES = "categories"
        private const val TABLE_MASTER_KEY = "master_key"
        private const val TABLE_SALT = "salt"
        private const val TABLE_PACKAGE_ACCESS = "package_access"
        private const val TABLE_CIPHER_ACCESS = "cipher_access"
        private const val DATABASE_VERSION = 4

        private const val DBVERSION_CREATE =
            "create table $TABLE_DBVERSION (" +
                    "version integer not null);"

        private const val PASSWORDS_CREATE =
            "create table $TABLE_PASSWORDS (" +
                    "id integer primary key autoincrement, " +
                    "category integer not null, " +
                    "password text not null, " +
                    "description text not null, " +
                    "username text, " +
                    "website text, " +
                    "note text, " +
                    "unique_name text, " + //might be null
                    "lastdatetimeedit text);"

        private const val PASSWORDS_DROP =
            "drop table $TABLE_PASSWORDS;"

        private const val PACKAGE_ACCESS_CREATE =
            "create table $TABLE_PACKAGE_ACCESS (" +
                    "id integer not null, " +
                    "package text not null);"

        private const val PACKAGE_ACCESS_DROP =
            "drop table $TABLE_PACKAGE_ACCESS;"

        private const val CATEGORIES_CREATE =
            "create table $TABLE_CATEGORIES (" +
                    "id integer primary key autoincrement, " +
                    "name text not null, " +
                    "lastdatetimeedit text);"

        private const val CATEGORIES_DROP =
            "drop table $TABLE_CATEGORIES;"

        private const val MASTER_KEY_CREATE =
            "create table $TABLE_MASTER_KEY (" +
                    "encryptedkey text not null);"

        private const val SALT_CREATE =
            "create table $TABLE_SALT (" +
                    "salt text not null);"

        private const val CIPHER_ACCESS_CREATE =
            "create table $TABLE_CIPHER_ACCESS (" +
                    "id integer primary key autoincrement, " +
                    "packagename text not null, " +
                    "expires integer not null, " +
                    "dateadded text not null);"

        // private const val CIPHER_ACCESS_DROP = "drop table $TABLE_CIPHER_ACCESS;"

        var needsPrePopulation = false
            private set

        fun clearPrePopulate() {
            needsPrePopulation = false
        }
    }

    init {
        try {
            db = context.openOrCreateDatabase(DATABASE_NAME, 0, null)

            // avoid journals in the file system as it gives access to the passwords.
            // FIXME: if you can get hold of a memory dump you could still get access to the passwords.
            db?.rawQuery("PRAGMA journal_mode=MEMORY", null)?.close()

            // Check for the existence of the DBVERSION table
            val c = db?.query(
                "sqlite_master", arrayOf("name"),
                "type='table' and name='$TABLE_DBVERSION'", null, null, null, null
            )

            val numRows = c?.count ?: 0
            c?.close()

            if (numRows < 1) {
                db?.let { createDatabase(it) }
            } else {
                var version = 0
                val vc = db?.query(
                    true, TABLE_DBVERSION, arrayOf("version"),
                    null, null, null, null, null, null
                )
                if (vc != null && vc.count > 0) {
                    vc.moveToFirst()
                    version = vc.getInt(0)
                }
                vc?.close()

                if (version != DATABASE_VERSION) {
                    needsUpgrade = true
                    Log.e(TAG, "database version mismatch")
                }
            }

        } catch (e: SQLiteDiskIOException) {
            Log.d(TAG, "SQLite DiskIO exception: " + e.localizedMessage)
            if (DEBUG) {
                Log.d(TAG, "SQLite DiskIO exception: db=$db")
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    fun isDatabaseOpen(): Boolean {
        val isOpen = (db != null)
        if (DEBUG) {
            Log.d(TAG, "isDatabaseOpen==$isOpen")
        }
        return isOpen
    }

    private fun createDatabase(db: SQLiteDatabase) {
        try {
            db.execSQL(DBVERSION_CREATE)
            val args = ContentValues()
            args.put("version", DATABASE_VERSION)
            db.insert(TABLE_DBVERSION, null, args)

            db.execSQL(CATEGORIES_CREATE)
            needsPrePopulation = true

            db.execSQL(PASSWORDS_CREATE)
            db.execSQL(PACKAGE_ACCESS_CREATE)
            db.execSQL(CIPHER_ACCESS_CREATE)
            db.execSQL(MASTER_KEY_CREATE)
            db.execSQL(SALT_CREATE)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    fun deleteDatabase() {
        try {
            db?.execSQL(PASSWORDS_DROP)
            db?.execSQL(PASSWORDS_CREATE)

            db?.execSQL(CATEGORIES_DROP)
            db?.execSQL(CATEGORIES_CREATE)

            db?.execSQL(PACKAGE_ACCESS_DROP)
            db?.execSQL(PACKAGE_ACCESS_CREATE)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    /**
     * Close database connection
     */
    fun close() {
        if (db == null) {
            return
        }
        try {
            db?.close()
        } catch (e: SQLException) {
            Log.d(TAG, "close exception: " + e.localizedMessage)
        }
    }

    fun fetchVersion(): Int {
        var version = 0
        try {
            db?.query(
                true, TABLE_DBVERSION,
                arrayOf("version"),
                null, null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    version = c.getInt(0)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return version
    }

    // //////// Salt Functions ////////////////

    /**
     * Store the salt
     *
     * @return String version of salt
     */
    fun fetchSalt(): String {
        var salt = ""
        if (db == null) {
            return salt
        }
        try {
            db?.query(
                true, TABLE_SALT, arrayOf("salt"),
                null, null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    salt = c.getString(0)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return salt
    }

    /**
     * Store the salt into the database.
     *
     * @param salt String version of the salt
     */
    fun storeSalt(salt: String) {
        val args = ContentValues()
        try {
            db?.delete(TABLE_SALT, "1=1", null)
            args.put("salt", salt)
            db?.insert(TABLE_SALT, null, args)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    // //////// Master Key Functions ////////////////

    /**
     * @return The master key.   If none is set, then return an empty string.
     */
    fun fetchMasterKey(): String {
        var key = ""
        try {
            db?.query(
                true, TABLE_MASTER_KEY, arrayOf("encryptedkey"),
                null, null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    key = c.getString(0)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return key
    }

    fun storeMasterKey(masterKey: String) {
        val args = ContentValues()
        try {
            db?.delete(TABLE_MASTER_KEY, "1=1", null)
            args.put("encryptedkey", masterKey)
            db?.insert(TABLE_MASTER_KEY, null, args)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    // ////////Category Functions ////////////////

    /**
     * Doesn't add the category if it already exists.
     *
     * @param entry
     * @return row id of the added category
     */
    fun addCategory(entry: CategoryEntry): Long {
        val initialValues = ContentValues()

        var rowID: Long = -1
        if (db == null) {
            return rowID
        }

        try {
            db?.query(
                true, TABLE_CATEGORIES, arrayOf("id", "name"),
                "name=?", arrayOf(entry.name), null, null, null, null
            ).use { c ->
                if (c != null && c.count > 0) {
                    c.moveToFirst()
                    rowID = c.getLong(0)
                } else { // there's not already such a category...
                    initialValues.put("name", entry.name)
                    rowID = db?.insert(TABLE_CATEGORIES, null, initialValues) ?: -1
                }
            }
        } catch (e: SQLException) {
             Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return rowID
    }

    /**
     * @param id id of a category to delete
     */
    fun deleteCategory(id: Long) {
        try {
            db?.delete(TABLE_CATEGORIES, "id=?", arrayOf(id.toString()))

        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    /**
     * @return a list of all categories
     */
    fun fetchAllCategoryRows(): List<CategoryEntry> {
        val ret = ArrayList<CategoryEntry>()
        if (db == null) {
            return ret
        }
        try {
            db?.query(
                TABLE_CATEGORIES, arrayOf("id", "name"),
                null, null, null, null, null
            )?.use { c ->
                val numRows = c.count
                c.moveToFirst()
                for (i in 0 until numRows) {
                    val row = CategoryEntry()
                    row.id = c.getLong(0)
                    row.name = c.getString(1)
                    ret.add(row)
                    c.moveToNext()
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return ret
    }

    /**
     * @param id
     * @return A CategoryEntry.  If Id was not found then CategoryEntry.id will equal -1.
     */
    fun fetchCategory(id: Long): CategoryEntry {
        val row = CategoryEntry()
        try {
            db?.query(
                true, TABLE_CATEGORIES, arrayOf("id", "name"),
                "id=$id", null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    row.id = c.getLong(0)
                    row.name = c.getString(1)
                } else {
                    row.id = -1
                    row.name = ""
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return row
    }

    fun getCategoryCount(id: Long): Int {
        var count = 0
        try {
            db?.rawQuery("SELECT count(*) FROM $TABLE_PASSWORDS WHERE category=$id", null)?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    count = c.getInt(0)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return count
    }

    /**
     * @param id
     * @param entry
     */
    fun updateCategory(id: Long, entry: CategoryEntry) {
        val args = ContentValues()
        args.put("name", entry.name)

        try {
            db?.update(TABLE_CATEGORIES, args, "id=$id", null)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    // //////// Password Functions ////////////////

    /**
     * @param categoryId if -1 then count all passwords? Logic says > 0 for specific category.
     * Note: original comment said "-1 count all", logic said "categoryId > 0" -> filter.
     * If categoryId <= 0, it counts all.
     */
    fun countPasswords(categoryId: Long): Int {
        var count = 0
        try {
            val selection = if (categoryId > 0) "category=?" else null
            val selectionArgs = if (categoryId > 0) arrayOf(categoryId.toString()) else null
            db?.query(
                TABLE_PASSWORDS, arrayOf("count(*)"),
                selection, selectionArgs, null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    count = c.getInt(0)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        //Log.i(TAG,"count="+count);
        return count
    }

    /**
     * @return A list of all password entries filtered by the CategoryId.
     * If CategoryId is 0, then return all entries in the database.
     */
    fun fetchAllRows(categoryId: Long): List<PassEntry> {
        val ret = ArrayList<PassEntry>()
        if (db == null) {
            return ret
        }
        try {
            val selection = if (categoryId == 0L) null else "category=?"
            val selectionArgs = if (categoryId == 0L) null else arrayOf(categoryId.toString())
            db?.query(
                TABLE_PASSWORDS, arrayOf(
                    "id", "password", "description", "username", "website",
                    "note", "category", "unique_name", "lastdatetimeedit"
                ),
                selection, selectionArgs, null, null, null
            )?.use { c ->
                val numRows = c.count
                c.moveToFirst()
                for (i in 0 until numRows) {
                    val row = PassEntry()
                    row.id = c.getLong(0)
                    row.password = c.getString(1)
                    row.description = c.getString(2)
                    row.username = c.getString(3)
                    row.website = c.getString(4)
                    row.note = c.getString(5)
                    row.category = c.getLong(6)
                    row.uniqueName = c.getString(7)
                    row.lastEdited = c.getString(8)
                    ret.add(row)
                    c.moveToNext()
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return ret
    }

    /**
     * @param id
     * @return The password entry matching the Id.  If not found, then
     * the returned PassEntry.id will equal -1.
     */
    fun fetchPassword(id: Long): PassEntry {
        val row = PassEntry()
        try {
            db?.query(
                true, TABLE_PASSWORDS, arrayOf(
                    "id", "password", "description", "username", "website",
                    "note", "category", "unique_name", "lastdatetimeedit"
                ),
                "id=$id", null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    row.id = c.getLong(0)
                    row.password = c.getString(1)
                    row.description = c.getString(2)
                    row.username = c.getString(3)
                    row.website = c.getString(4)
                    row.note = c.getString(5)
                    row.category = c.getLong(6)
                    row.uniqueName = c.getString(7)
                    row.lastEdited = c.getString(8)
                } else {
                    row.id = -1
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return row
    }

    fun fetchPassword(uniqueName: String): PassEntry {
        val row = PassEntry()
        row.id = -1
        try {
            db?.query(
                true, TABLE_PASSWORDS, arrayOf(
                    "id", "password", "description", "username", "website",
                    "note", "category", "unique_name", "lastdatetimeedit"
                ),
                "unique_name=?", arrayOf(uniqueName),
                null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    row.id = c.getLong(0)
                    row.password = c.getString(1)
                    row.description = c.getString(2)
                    row.username = c.getString(3)
                    row.website = c.getString(4)
                    row.note = c.getString(5)
                    row.category = c.getLong(6)
                    row.uniqueName = c.getString(7)
                    row.lastEdited = c.getString(8)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return row
    }

    fun fetchPackageAccess(passwordID: Long): ArrayList<String> {
        val pkgs = ArrayList<String>()
        try {
            db?.query(
                true, TABLE_PACKAGE_ACCESS, arrayOf("package"),
                "id=$passwordID", null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    while (!c.isAfterLast) {
                        pkgs.add(c.getString(0))
                        c.moveToNext()
                    }
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return pkgs
    }

    /**
     * Fetch all the package access data into one HashMap.
     *
     * @return HashMap<Long id, ArrayList<String> package>
     */
    fun fetchPackageAccessAll(): HashMap<Long, ArrayList<String>> {
        val pkgsAll = HashMap<Long, ArrayList<String>>()

        if (db == null) {
            return pkgsAll
        }
        try {
            db?.query(
                true, TABLE_PACKAGE_ACCESS, arrayOf("id"),
                null, null, null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    while (!c.isAfterLast) {
                        val id = c.getLong(0)
                        val pkgs = fetchPackageAccess(id)
                        if (pkgs.isNotEmpty()) {
                            pkgsAll[id] = pkgs
                        }
                        c.moveToNext()
                    }
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return pkgsAll
    }

    fun addPackageAccess(passwordID: Long, packageToAdd: String) {
        val packageAccessValues = ContentValues()
        packageAccessValues.put("id", passwordID)
        packageAccessValues.put("package", packageToAdd)
        try {
            db?.insert(TABLE_PACKAGE_ACCESS, null, packageAccessValues)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    /**
     * @param id
     * @param entry
     * @return Id on success, -1 on failure
     */
    fun updatePassword(id: Long, entry: PassEntry): Long {
        val args = ContentValues()
        args.put("description", entry.description)
        args.put("username", entry.username)
        args.put("password", entry.password)
        args.put("website", entry.website)
        args.put("note", entry.note)
        args.put("unique_name", entry.uniqueName)
        val dateFormatter = DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.FULL
        )
        val today = Date()
        val dateOut = dateFormatter.format(today)
        args.put("lastdatetimeedit", dateOut)
        try {
            db?.update(TABLE_PASSWORDS, args, "id=$id", null)
        } catch (e: SQLException) {
            Log.d(TAG, "updatePassword: SQLite exception: " + e.localizedMessage)
            return -1
        }
        return id
    }

    /**
     * Only update the category field of the password entry.
     *
     * @param id            the id of the password entry
     * @param newCategoryId the updated category id
     */
    fun updatePasswordCategory(id: Long, newCategoryId: Long) {
        if (id < 0 || newCategoryId < 0) {
            //make sure values appear valid
            return
        }
        val args = ContentValues()

        args.put("category", newCategoryId)

        try {
            db?.update(TABLE_PASSWORDS, args, "id=$id", null)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    /**
     * Add a password entry to the database.
     * PassEntry.id should be set to 0, unless a specific
     * row id is desired.
     *
     * @param entry PassEntry
     * @return long row id of newly added entry, equal to -1 if an error occurred
     */
    fun addPassword(entry: PassEntry): Long {
        var id: Long = -1
        val initialValues = ContentValues()
        if (entry.id != 0L) {
            initialValues.put("id", entry.id)
        }
        initialValues.put("category", entry.category)
        initialValues.put("password", entry.password)
        initialValues.put("description", entry.description)
        initialValues.put("username", entry.username)
        initialValues.put("website", entry.website)
        initialValues.put("note", entry.note)
        initialValues.put("unique_name", entry.uniqueName)
        val dateFormatter = DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.FULL
        )
        val today = Date()
        val dateOut = dateFormatter.format(today)
        initialValues.put("lastdatetimeedit", dateOut)

        try {
            id = db?.insertOrThrow(TABLE_PASSWORDS, null, initialValues) ?: -1
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
            id = -1
        }
        return id
    }

    /**
     * @param id
     */
    fun deletePassword(id: Long) {
        try {
            db?.delete(TABLE_PASSWORDS, "id=$id", null)
            db?.delete(TABLE_PACKAGE_ACCESS, "id=$id", null)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    // ////////Cipher Access Functions ////////////////

    /**
     * Add a package to the list of packages allowed to use the encrypt/decrypt
     * cipher services.
     *
     * @param packageToAdd
     * @param expiration   set to 0 if no expiration, otherwise epoch time
     */
    fun addCipherAccess(packageToAdd: String, expiration: Long) {
        val initialValues = ContentValues()
        initialValues.put("packagename", packageToAdd)
        initialValues.put("expires", expiration)
        val dateFormatter = DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.FULL
        )
        val today = Date()
        val dateOut = dateFormatter.format(today)
        initialValues.put("dateadded", dateOut)
        try {
            db?.insert(TABLE_CIPHER_ACCESS, null, initialValues)
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    /**
     * Fetch the cipher access for a package.   This determines if the package
     * is allowed to use encrypt/decrypt services.
     *
     * @param packageName
     * @return -1 if not found, 0 if no expiration, otherwise epoch date of expiration
     */
    fun fetchCipherAccess(packageName: String): Long {
        var expires: Long = -1 // default to not found
        try {
            db?.query(
                true, TABLE_CIPHER_ACCESS, arrayOf("expires"),
                "packagename=?", arrayOf(packageName), null, null, null, null
            )?.use { c ->
                if (c.count > 0) {
                    c.moveToFirst()
                    expires = c.getLong(0)
                }
            }
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
        return expires
    }

    /**
     * Begin a transaction on an open database.
     *
     * @return true if successful
     */
    fun beginTransaction(): Boolean {
        try {
            db?.execSQL("begin transaction;")
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
            return false
        }
        return true
    }

    /**
     * Commit all changes since the begin transaction on an
     * open database.
     */
    fun commit() {
        try {
            db?.execSQL("commit;")
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }

    /**
     * Rollback all changes since the begin transaction on an
     * open database.
     */
    fun rollback() {
        try {
            db?.execSQL("rollback;")
        } catch (e: SQLException) {
            Log.d(TAG, "SQLite exception: " + e.localizedMessage)
        }
    }
}
