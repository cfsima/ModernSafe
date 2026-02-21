package io.github.cfsima.modernsafe

import android.content.Context
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import java.io.IOException
import java.io.Writer
import java.util.HashMap

object Export {
    @JvmStatic
    @Throws(IOException::class)
    fun exportDatabaseToWriter(context: Context, w: Writer) {
        val writer = CSVWriter(w, ',')

        val header = arrayOf(
            context.getString(R.string.category),
            context.getString(R.string.description),
            context.getString(R.string.website),
            context.getString(R.string.username),
            context.getString(R.string.password),
            context.getString(R.string.notes),
            context.getString(R.string.last_edited)
        )
        writer.writeNext(header)

        val categories: HashMap<Long, String> = Passwords.getCategoryIdToName()

        val rows: List<PassEntry> = Passwords.getPassEntries(0L, true, false)

        for (row in rows) {
            val rowEntries = arrayOf(
                categories[row.category],
                row.plainDescription,
                row.plainWebsite,
                row.plainUsername,
                row.plainPassword,
                row.plainNote,
                row.lastEdited
            )
            writer.writeNext(rowEntries)
        }
        writer.close()
    }
}
