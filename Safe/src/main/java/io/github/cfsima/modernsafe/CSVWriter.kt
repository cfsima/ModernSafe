package io.github.cfsima.modernsafe

import java.io.IOException
import java.io.PrintWriter
import java.io.Writer

/**
 * A very simple CSV writer released under a commercial-friendly license.
 *
 * @author Glen Smith
 */
class CSVWriter(
    writer: Writer,
    private val separator: Char = DEFAULT_SEPARATOR,
    private val quotechar: Char = DEFAULT_QUOTE_CHARACTER,
    private val escapechar: Char = DEFAULT_ESCAPE_CHARACTER,
    private val lineEnd: String = DEFAULT_LINE_END
) {
    private val rawWriter: Writer = writer
    private val pw: PrintWriter = PrintWriter(writer)

    /**
     * Writes the entire list to a CSV file. The list is assumed to be a
     * String[]
     *
     * @param allLines
     * a List of String[], with each String[] representing a line of
     * the file.
     */
    fun writeAll(allLines: List<Array<out String?>>) {
        for (nextLine in allLines) {
            writeNext(nextLine)
        }
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine
     * a string array with each comma-separated element as a separate
     * entry.
     */
    fun writeNext(nextLine: Array<out String?>?) {
        if (nextLine == null) {
            return
        }

        val sb = StringBuffer()
        for (i in nextLine.indices) {
            if (i != 0) {
                sb.append(separator)
            }

            val nextElement = nextLine[i]
            if (nextElement == null) {
                continue
            }

            if (quotechar != NO_QUOTE_CHARACTER) {
                sb.append(quotechar)
            }

            for (j in 0 until nextElement.length) {
                val nextChar = nextElement[j]
                if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
                    sb.append(escapechar).append(nextChar)
                } else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
                    sb.append(escapechar).append(nextChar)
                } else {
                    sb.append(nextChar)
                }
            }

            if (quotechar != NO_QUOTE_CHARACTER) {
                sb.append(quotechar)
            }
        }

        sb.append(lineEnd)
        pw.write(sb.toString())
    }

    /**
     * Flush underlying stream to writer.
     *
     * @throws IOException if bad things happen
     */
    @Throws(IOException::class)
    fun flush() {
        pw.flush()
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException if bad things happen
     */
    @Throws(IOException::class)
    fun close() {
        pw.flush()
        pw.close()
        rawWriter.close()
    }

    companion object {
        /** The character used for escaping quotes.  */
        const val DEFAULT_ESCAPE_CHARACTER = '"'

        /** The default separator to use if none is supplied to the constructor.  */
        const val DEFAULT_SEPARATOR = ','

        /**
         * The default quote character to use if none is supplied to the
         * constructor.
         */
        const val DEFAULT_QUOTE_CHARACTER = '"'

        /** The quote constant to use when you wish to suppress all quoting.  */
        const val NO_QUOTE_CHARACTER = '\u0000'

        /** The escape constant to use when you wish to suppress all escaping.  */
        const val NO_ESCAPE_CHARACTER = '\u0000'

        /** Default line terminator uses platform encoding.  */
        const val DEFAULT_LINE_END = "\n"
    }
}
