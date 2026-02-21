package io.github.cfsima.modernsafe

import android.util.Log
import io.github.cfsima.modernsafe.model.RestoreDataSet
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

class RestoreHandler : DefaultHandler() {

    private val debug = false
    private val TAG = "Restore"

    private var in_oisafe = false
    private var in_salt = false
    private var in_masterkey = false
    private var in_category = false
    private var in_entry = false
    private var in_rowid = false
    private var in_description = false
    private var in_website = false
    private var in_username = false
    private var in_password = false
    private var in_note = false
    private var in_uniquename = false
    private var in_packageaccess = false

    var parsedData = RestoreDataSet()
        private set

    @Throws(SAXException::class)
    override fun startDocument() {
        parsedData = RestoreDataSet()
    }

    @Throws(SAXException::class)
    override fun endDocument() {
        // Nothing to do
    }

    @Throws(SAXException::class)
    override fun startElement(
        namespaceURI: String?, localName: String,
        qName: String?, atts: Attributes
    ) {
        if (localName == "OISafe") {
            in_oisafe = true
            val attrValue = atts.getValue("version")
            var version = 0
            if (attrValue != null) {
                try {
                    version = attrValue.toInt()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }

            val date = atts.getValue("date")

            parsedData.version = version
            if (date != null) {
                parsedData.date = date
            }

            if (debug) {
                Log.d(TAG, "found OISafe $version date $date")
            }

        } else if (in_oisafe && localName == "Salt") {
            in_salt = true
            if (debug) Log.d(TAG, "found Salt")

        } else if (in_oisafe && localName == "MasterKey") {
            in_masterkey = true
            if (debug) Log.d(TAG, "found MasterKey")

        } else if (in_oisafe && localName == "Category") {
            in_category = true
            val name = atts.getValue("name")
            parsedData.newCategory(name)
            if (debug) Log.d(TAG, "found Category $name")

        } else if (in_category && localName == "Entry") {
            in_entry = true
            parsedData.newEntry()
            if (debug) Log.d(TAG, "found Entry")

        } else if (in_entry) {
            when (localName) {
                "RowID" -> in_rowid = true
                "Description" -> in_description = true
                "Website" -> in_website = true
                "Username" -> in_username = true
                "Password" -> in_password = true
                "Note" -> in_note = true
                "UniqueName" -> in_uniquename = true
                "PackageAccess" -> in_packageaccess = true
            }
        }
    }

    @Throws(SAXException::class)
    override fun endElement(namespaceURI: String?, localName: String, qName: String?) {
        if (localName == "OISafe") {
            in_oisafe = false
        } else if (in_oisafe && localName == "Salt") {
            in_salt = false
        } else if (in_oisafe && localName == "MasterKey") {
            in_masterkey = false
        } else if (in_oisafe && localName == "Category") {
            in_category = false
            parsedData.storyCategory()
        } else if (in_category && localName == "Entry") {
            in_entry = false
            parsedData.storeEntry()
        } else if (in_entry) {
            when (localName) {
                "RowID" -> in_rowid = false
                "Description" -> in_description = false
                "Website" -> in_website = false
                "Username" -> in_username = false
                "Password" -> in_password = false
                "Note" -> in_note = false
                "UniqueName" -> in_uniquename = false
                "PackageAccess" -> in_packageaccess = false
            }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val str = String(ch, start, length)
        if (in_salt) {
            parsedData.setSalt(str)
        } else if (in_masterkey) {
            parsedData.setMasterKeyEncrypted(str)
        } else if (in_rowid) {
            parsedData.setRowID(str)
        } else if (in_description) {
            parsedData.setDescription(str)
        } else if (in_website) {
            parsedData.setWebsite(str)
        } else if (in_username) {
            parsedData.setUsername(str)
        } else if (in_password) {
            parsedData.setPassword(str)
        } else if (in_note) {
            parsedData.setNote(str)
        } else if (in_uniquename) {
            parsedData.setUniqueName(str)
        } else if (in_packageaccess) {
            parsedData.setPackageAccess(str)
        }
    }
}
