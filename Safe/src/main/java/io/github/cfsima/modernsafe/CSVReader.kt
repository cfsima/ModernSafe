package io.github.cfsima.modernsafe

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader

/**
 * A very simple CSV reader released under a commercial-friendly license.
 *
 * @author Glen Smith
 */
class CSVReader(
    reader: Reader,
    private val separator: Char = DEFAULT_SEPARATOR,
    private val quotechar: Char = DEFAULT_QUOTE_CHARACTER,
    private val skipLines: Int = DEFAULT_SKIP_LINES
) {
    private val br = BufferedReader(reader)
    private var hasNext = true
    private var linesSkiped = false

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     * file.
     *
     * @throws IOException
     * if bad things happen during the read
     */
    @Throws(IOException::class)
    fun readAll(): List<Array<String>> {
        val allElements = ArrayList<Array<String>>()
        while (hasNext) {
            val nextLineAsTokens = readNext()
            if (nextLineAsTokens != null) {
                allElements.add(nextLineAsTokens)
            }
        }
        return allElements
    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate
     * entry.
     *
     * @throws IOException
     * if bad things happen during the read
     */
    @Throws(IOException::class)
    fun readNext(): Array<String>? {
        val nextLine = getNextLine()
        return if (hasNext) parseLine(nextLine) else null
    }

    /**
     * Reads the next line from the file.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException
     * if bad things happen during the read
     */
    @Throws(IOException::class)
    private fun getNextLine(): String? {
        if (!this.linesSkiped) {
            for (i in 0 until skipLines) {
                br.readLine()
            }
            this.linesSkiped = true
        }
        val nextLine = br.readLine()
        if (nextLine == null) {
            hasNext = false
        }
        return if (hasNext) nextLine else null
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine
     * the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException if bad things happen during the read
     */
    @Throws(IOException::class)
    private fun parseLine(nextLine: String?): Array<String>? {
        if (nextLine == null) {
            return null
        }

        val tokensOnThisLine = ArrayList<String>()
        var sb = StringBuffer()
        var inQuotes = false
        var currentLine = nextLine

        do {
            if (inQuotes) {
                // continuing a quoted section, reappend newline
                sb.append("\n")
                currentLine = getNextLine()
                if (currentLine == null) {
                    break
                }
            }

            var i = 0
            while (i < currentLine!!.length) {
                val c = currentLine!![i]
                if (c == quotechar) {
                    // this gets complex... the quote may end a quoted block, or escape another quote.
                    // do a 1-char lookahead:
                    if (inQuotes // we are in quotes, therefore there can be escaped quotes in here.
                        && currentLine!!.length > (i + 1) // there is indeed another character to check.
                        && currentLine!![i + 1] == quotechar // ..and that char. is a quote also.
                    ) {
                        // we have two quote chars in a row == one quote char, so consume them both and
                        // put one on the token. we do *not* exit the quoted text.
                        sb.append(currentLine!![i + 1])
                        i++
                    } else {
                        inQuotes = !inQuotes
                        // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                        if (i > 2 //not on the begining of the line
                            && currentLine!![i - 1] != this.separator //not at the begining of an escape sequence
                            && currentLine!!.length > (i + 1) &&
                            currentLine!![i + 1] != this.separator //not at the	end of an escape sequence
                        ) {
                            sb.append(c)
                        }
                    }
                } else if (c == separator && !inQuotes) {
                    tokensOnThisLine.add(sb.toString())
                    sb = StringBuffer() // start work on next token
                } else {
                    sb.append(c)
                }
                i++
            }
        } while (inQuotes)

        tokensOnThisLine.add(sb.toString())
        return tokensOnThisLine.toTypedArray()
    }

    /**
     * Closes the underlying reader.
     *
     * @throws IOException if the close fails
     */
    @Throws(IOException::class)
    fun close() {
        br.close()
    }

    companion object {
        /** The default separator to use if none is supplied to the constructor.  */
        const val DEFAULT_SEPARATOR = ','

        /**
         * The default quote character to use if none is supplied to the
         * constructor.
         */
        const val DEFAULT_QUOTE_CHARACTER = '"'

        /**
         * The default line to start reading.
         */
        const val DEFAULT_SKIP_LINES = 0
    }
}
