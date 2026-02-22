package io.github.cfsima.modernsafe

import android.content.Context
import android.net.Uri
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class InputStreamData private constructor(
    val filename: String,
    val stream: InputStream
) {
    @Throws(FileNotFoundException::class)
    constructor(filename: String) : this(filename, FileInputStream(filename))

    @Throws(FileNotFoundException::class)
    constructor(documentUri: Uri, context: Context) : this(
        documentUri.toString(),
        context.contentResolver.openInputStream(documentUri)
            ?: throw FileNotFoundException("Could not open stream for $documentUri")
    )
}
