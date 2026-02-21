/* $
 *
 * Copyright 2008 Randy McEoin
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

import android.util.Log
import java.util.ArrayList

class RestoreDataSet {

    companion object {
        private const val DEBUG = false
        private const val TAG = "RestoreDataSet"
    }

    var version: Int = 0
    var date: String? = null
    var salt: String = ""
        get() = field.trim()
        set(value) {
            field = if (field.isEmpty()) value else field + value
        }

    var masterKeyEncrypted: String = ""
        get() = field.trim()
        set(value) {
            field = if (field.isEmpty()) value else field + value
        }

    private var currentCategoryId: Long = 0
    private var currentCategory: CategoryEntry? = null
    private var categoryEntries = ArrayList<CategoryEntry>()
    private var currentEntry: PassEntry? = null
    private var currentRowID: String = ""
    private var currentPackageAccess: String = ""
    private var passEntries = ArrayList<PassEntry>()
    var totalEntries: Int = 0
        private set

    val categories: ArrayList<CategoryEntry> get() {
        return categoryEntries
    }

    fun newCategory(extractedCategory: String) {
        currentCategory = CategoryEntry()
        currentCategoryId++
        currentCategory?.id = currentCategoryId
        currentCategory?.name = extractedCategory
    }

    fun storyCategory() {
        currentCategory?.let {
            categoryEntries.add(it)
            currentCategory = null
        }
    }

    val pass: ArrayList<PassEntry> get() {
        return passEntries
    }

    fun newEntry() {
        currentEntry = PassEntry()
        currentEntry?.category = currentCategoryId
        currentRowID = ""
        currentEntry?.description = ""
        currentEntry?.website = ""
        currentEntry?.username = ""
        currentEntry?.password = ""
        currentEntry?.note = ""
        currentEntry?.uniqueName = ""
        currentEntry?.packageAccess = null
        currentPackageAccess = ""
    }

    fun storeEntry() {
        val entry = currentEntry ?: return

        // only add an entry if we had all the fields
        if (DEBUG) {
            Log.d(
                TAG, "${entry.description} ${entry.website} ${entry.username} ${entry.password} ${entry.note} $currentPackageAccess"
            )
        }

        if (entry.description.isNotEmpty()) {
            try {
                entry.id = currentRowID.toLong()
            } catch (e: NumberFormatException) {
                entry.id = 0
            }
            if (currentPackageAccess.isNotEmpty()) {
                // strip the brackets [ and ]
                // Assuming format "[pkg1, pkg2]"
                if (currentPackageAccess.length > 2) {
                    val packageList = currentPackageAccess.substring(
                        1,
                        currentPackageAccess.length - 1
                    )
                    val packages = packageList.split(",").map { it.trim() }
                    entry.packageAccess = ArrayList(packages)
                    if (DEBUG) {
                        Log.d(TAG, "packageAccess=${entry.packageAccess}")
                    }
                }
            }
            passEntries.add(entry)
            totalEntries++
        }
        currentEntry = null
    }

    fun setRowID(extractedRowID: String) {
        if (DEBUG) {
            Log.d(TAG, "setRowID($extractedRowID)")
        }
        if (currentEntry != null) {
            currentRowID += extractedRowID
        }
    }

    fun setDescription(extractedDescription: String) {
        if (DEBUG) {
            Log.d(TAG, "setDescription($extractedDescription)")
        }
        currentEntry?.let {
            it.description = (it.description) + extractedDescription
        }
    }

    fun setWebsite(extractedWebsite: String) {
        if (DEBUG) {
            Log.d(TAG, "setWebsite($extractedWebsite)")
        }
        currentEntry?.let {
            it.website = (it.website ?: "") + extractedWebsite
        }
    }

    fun setUsername(extractedUsername: String) {
        currentEntry?.let {
            it.username = (it.username ?: "") + extractedUsername
        }
    }

    fun setPassword(extractedPassword: String) {
        currentEntry?.let {
            it.password = (it.password ?: "") + extractedPassword
        }
    }

    fun setNote(extractedNote: String) {
        currentEntry?.let {
            it.note = (it.note ?: "") + extractedNote
        }
    }

    fun setUniqueName(extractedUniqueName: String) {
        currentEntry?.let {
            it.uniqueName = (it.uniqueName ?: "") + extractedUniqueName
        }
    }

    fun setPackageAccess(extractedPackageAccess: String) {
        if (DEBUG) {
            Log.d(TAG, "setPackageAccess($extractedPackageAccess)")
        }
        currentPackageAccess += extractedPackageAccess
    }
}
