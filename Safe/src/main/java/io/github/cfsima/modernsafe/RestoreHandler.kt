package io.github.cfsima.modernsafe

import android.util.Log
import io.github.cfsima.modernsafe.model.RestoreDataSet
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

class RestoreHandler : DefaultHandler() {

    private var inOisafe = false
    private var inSalt = false
    private var inMasterKey = false
    private var inCategory = false
    private var inEntry = false
    private var inRowId = false
    private var inDescription = false
    private var inWebsite = false
    private var inUsername = false
    private var inPassword = false
    private var inNote = false
    private var inUniqueName = false
    private var inPackageAccess = false

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
        namespaceURI: String?,
        localName: String,
        qName: String?,
        atts: Attributes,
    ) {
        if (localName == "OISafe") {
            inOisafe = true
            val attrValue = atts.getValue("version")
            var version = 0
            if (attrValue != null) {
                try {
                    version = attrValue.toInt()
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "version parse error", e)
                }
            }

            val date = atts.getValue("date")

            parsedData.version = version
            if (date != null) {
                parsedData.date = date
            }

            if (DEBUG) {
                Log.d(TAG, "found OISafe $version date $date")
            }

        } else if (inOisafe && (localName == "Salt")) {
            inSalt = true
            if (DEBUG) Log.d(TAG, "found Salt")

        } else if (inOisafe && localName == "MasterKey") {
            inMasterKey = true
            if (DEBUG) Log.d(TAG, "found MasterKey")

        } else if (inOisafe && localName == "Category") {
            inCategory = true
            val name = atts.getValue("name")
            parsedData.newCategory(name)
            if (DEBUG) Log.d(TAG, "found Category $name")

        } else if (inCategory && localName == "Entry") {
            inEntry = true
            parsedData.newEntry()
            if (DEBUG) Log.d(TAG, "found Entry")

        } else if (inEntry) {
            when (localName) {
                "RowID" -> inRowId = true
                "Description" -> inDescription = true
                "Website" -> inWebsite = true
                "Username" -> inUsername = true
                "Password" -> inPassword = true
                "Note" -> inNote = true
                "UniqueName" -> inUniqueName = true
                "PackageAccess" -> inPackageAccess = true
            }
        }
    }

    @Throws(SAXException::class)
    override fun endElement(namespaceURI: String?, localName: String, qName: String?) {
        if (localName == "OISafe") {
            inOisafe = false
        } else if (inOisafe && localName == "Salt") {
            inSalt = false
        } else if (inOisafe && localName == "MasterKey") {
            inMasterKey = false
        } else if (inOisafe && localName == "Category") {
            inCategory = false
            parsedData.storyCategory()
        } else if (inCategory && localName == "Entry") {
            inEntry = false
            parsedData.storeEntry()
        } else if (inEntry) {
            when (localName) {
                "RowID" -> inRowId = false
                "Description" -> inDescription = false
                "Website" -> inWebsite = false
                "Username" -> inUsername = false
                "Password" -> inPassword = false
                "Note" -> inNote = false
                "UniqueName" -> inUniqueName = false
                "PackageAccess" -> inPackageAccess = false
            }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val str = String(ch, start, length)
        if (inSalt) {
            parsedData.salt = str
        } else if (inMasterKey) {
            parsedData.masterKeyEncrypted = str
        } else if (inRowId) {
            parsedData.setRowID(str)
        } else if (inDescription) {
            parsedData.setDescription(str)
        } else if (inWebsite) {
            parsedData.setWebsite(str)
        } else if (inUsername) {
            parsedData.setUsername(str)
        } else if (inPassword) {
            parsedData.setPassword(str)
        } else if (inNote) {
            parsedData.setNote(str)
        } else if (inUniqueName) {
            parsedData.setUniqueName(str)
        } else if (inPackageAccess) {
            parsedData.setPackageAccess(str)
        }
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "Restore"
    }
}
