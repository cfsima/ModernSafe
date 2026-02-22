package io.github.cfsima.modernsafe

import android.content.Context
import android.net.Uri
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

class OutputStreamData private constructor(
    val filename: String,
    val stream: OutputStream
) {
    @Throws(FileNotFoundException::class)
    constructor(filename: String) : this(filename, FileOutputStream(filename))

    @Throws(FileNotFoundException::class)
    constructor(documentUri: Uri, context: Context) : this(
        documentUri.toString(),
        context.contentResolver.openOutputStream(documentUri)
            ?: throw FileNotFoundException("Could not open stream for $documentUri")
    )
}
