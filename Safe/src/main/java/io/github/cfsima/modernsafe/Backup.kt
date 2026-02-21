package io.github.cfsima.modernsafe

import android.content.Context
import androidx.preference.PreferenceManager
import android.text.format.Time
import android.util.Log
import android.util.Xml
import io.github.cfsima.modernsafe.model.Passwords
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

class Backup(private val context: Context) {

    var result: String = ""
        private set

    fun write(filename: String, stream: OutputStream): Boolean {
        if (DEBUG) {
            Log.d(TAG, "write($filename,)")
        }

        try {
            val serializer: XmlSerializer = Xml.newSerializer()
            serializer.setOutput(stream, "utf-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startTag(null, "OISafe")

            serializer.attribute(null, "version", CURRENT_VERSION.toString())

            val dateFormatter = DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT,
                DateFormat.FULL
            )
            val today = Date()
            val dateOut = dateFormatter.format(today)

            serializer.attribute(null, "date", dateOut)

            val masterKeyEncrypted = Passwords.fetchMasterKeyEncrypted()
            if (masterKeyEncrypted != null) {
                serializer.startTag(null, "MasterKey")
                serializer.text(masterKeyEncrypted)
                serializer.endTag(null, "MasterKey")
            }

            val salt = Passwords.fetchSalt()
            if (salt != null) {
                serializer.startTag(null, "Salt")
                serializer.text(salt)
                serializer.endTag(null, "Salt")
            }

            val crows = Passwords.getCategoryEntries()

            var totalPasswords = 0

            for (crow in crows) {

                serializer.startTag(null, "Category")
                serializer.attribute(null, "name", crow.name)

                val rows = Passwords.getPassEntries(crow.id, false, false)

                for (row in rows) {
                    totalPasswords++

                    serializer.startTag(null, "Entry")

                    serializer.startTag(null, "RowID")
                    serializer.text(row.id.toString())
                    serializer.endTag(null, "RowID")

                    serializer.startTag(null, "Description")
                    serializer.text(row.description)
                    serializer.endTag(null, "Description")

                    serializer.startTag(null, "Website")
                    serializer.text(row.website)
                    serializer.endTag(null, "Website")

                    serializer.startTag(null, "Username")
                    serializer.text(row.username)
                    serializer.endTag(null, "Username")

                    serializer.startTag(null, "Password")
                    serializer.text(row.password)
                    serializer.endTag(null, "Password")

                    serializer.startTag(null, "Note")
                    serializer.text(row.note)
                    serializer.endTag(null, "Note")

                    if (row.uniqueName != null) {
                        serializer.startTag(null, "UniqueName")
                        serializer.text(row.uniqueName)
                        serializer.endTag(null, "UniqueName")
                    }

                    val packageAccess = Passwords.getPackageAccessEntries(row.id)
                    if (packageAccess != null) {
                        serializer.startTag(null, "PackageAccess")
                        val entry = packageAccess.joinToString(",") { it.packageAccess }
                        serializer.text("[$entry]")
                        serializer.endTag(null, "PackageAccess")
                    }

                    serializer.endTag(null, "Entry")
                }
                serializer.endTag(null, "Category")
            }

            serializer.endTag(null, "OISafe")
            serializer.endDocument()

            stream.close()

            val tz = TimeZone.getDefault()
            @Suppress("DEPRECATION")
            val julianDay = Time.getJulianDay(Date().time, tz.rawOffset.toLong())
            if (DEBUG) {
                Log.d(TAG, "julianDay=$julianDay")
            }

            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            sp.edit().putInt(Settings.PREFERENCE_LAST_BACKUP_JULIAN, julianDay).apply()

            result = context.getString(R.string.backup_complete) + " " +
                    totalPasswords + "\n" + filename
        } catch (e: IOException) {
            e.printStackTrace()
            result = context.getString(R.string.backup_failed) + " " +
                    e.localizedMessage
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "Backup"
        private const val DEBUG = false
        const val CURRENT_VERSION = 1
    }
}
