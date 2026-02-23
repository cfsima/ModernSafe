package estreamj.framework

import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.util.Arrays

object Utils {
    fun fillPattern123(buf: ByteArray, ofs: Int, len: Int) {
        var currentOfs = ofs
        var counter = 0
        val end = currentOfs + len
        while (currentOfs < end) {
            buf[currentOfs++] = (counter++).toByte()
        }
    }

    fun checkPattern123(buf: ByteArray, ofs: Int, len: Int): Boolean {
        var currentOfs = ofs
        var counter = 0
        val end = currentOfs + len
        while (currentOfs < end) {
            if (buf[currentOfs] != counter.toByte()) {
                return false
            }
            counter++
            currentOfs++
        }
        return true
    }

    fun makeOutputBuffer(len: Int, extraLen: Int): ByteArray {
        val result = ByteArray(len + extraLen)
        Arrays.fill(result, 0xcc.toByte())
        return result
    }

    fun arraysEquals(
        a: ByteArray, ofsA: Int, b: ByteArray, ofsB: Int, len: Int
    ): Boolean {
        var currentOfsA = ofsA
        var currentOfsB = ofsB
        val end = currentOfsA + len
        while (currentOfsA < end) {
            if (b[currentOfsB++] != a[currentOfsA++]) {
                return false
            }
        }
        return true
    }

    fun readInt32LE(data: ByteArray, ofs: Int): Int {
        return (data[ofs + 3].toInt() shl 24) or
                ((data[ofs + 2].toInt() and 0xff) shl 16) or
                ((data[ofs + 1].toInt() and 0xff) shl 8) or
                (data[ofs].toInt() and 0xff)
    }

    fun writeInt32LE(value: Int, data: ByteArray, ofs: Int) {
        data[ofs] = value.toByte()
        data[ofs + 1] = (value ushr 8).toByte()
        data[ofs + 2] = (value ushr 16).toByte()
        data[ofs + 3] = (value ushr 24).toByte()
    }

    fun readInt32BE(data: ByteArray, ofs: Int): Int {
        return (data[ofs].toInt() shl 24) or
                ((data[ofs + 1].toInt() and 0xff) shl 16) or
                ((data[ofs + 2].toInt() and 0xff) shl 8) or
                (data[ofs + 3].toInt() and 0xff)
    }

    fun writeInt32BE(value: Int, data: ByteArray, ofs: Int) {
        data[ofs + 3] = value.toByte()
        data[ofs + 2] = (value ushr 8).toByte()
        data[ofs + 1] = (value ushr 16).toByte()
        data[ofs] = (value ushr 24).toByte()
    }

    fun hexStrToBytes(hex: String): ByteArray? {
        val len = hex.length
        if (1 == (len and 1)) {
            return null
        }
        val result = ByteArray(len shr 1)
        var r = 0
        var pos = 0
        while (pos < len) {
            var nReg = 0
            for (nI in 0..1) {
                nReg = nReg shl 4
                val c = hex[pos++].lowercaseChar()
                if (c in '0'..'9') {
                    nReg = nReg or (c - '0')
                } else if (c in 'a'..'f') {
                    nReg = nReg or ((c - 'a') + 10)
                } else {
                    return null
                }
            }
            result[r++] = nReg.toByte()
        }
        return result
    }

    val HEXTAB = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    fun hexDump(
        shouldRead: InputStream,
        ps: PrintStream,
        maxRead: Int,
        bytesPerLine: Int
    ): Int {
        var read: Int
        var chr: Int
        var i: Int
        var result: Int
        var pad: CharArray
        val left: StringBuilder
        val right: StringBuilder

        var bPerLine = bytesPerLine
        if (1 > bPerLine) {
            bPerLine = 1
        }

        left = StringBuilder()
        right = StringBuilder()

        result = 0

        read = 0
        i = 0
        while (true) {
            if (-1 != maxRead) {
                if (maxRead <= read) {
                    break
                }
            }

            try {
                chr = shouldRead.read()
                if (-1 == chr) {
                    break
                }
            } catch (ioe: IOException) {
                break
            }

            result++

            if (0 < i++) {
                left.append(' ')
            }

            left.append(HEXTAB[chr ushr 4])
            left.append(HEXTAB[chr and 0x0f])

            right.append(if (chr < ' '.code) '.' else chr.toChar())

            if (0 == (i % bPerLine)) {
                ps.print(left.toString())
                ps.print("    ")
                ps.println(right.toString())

                left.setLength(0)
                right.setLength(0)

                i = 0
            }
        }

        if (0 < i) {
            pad = CharArray(((bPerLine - i) * 3) + 4)
            Arrays.fill(pad, ' ')

            ps.print(left.toString())
            ps.print(pad)
            ps.println(right.toString())
        }

        return result
    }

    fun swapByteOrder32(data: ByteArray, ofs: Int, len: Int): ByteArray {
        val end = ofs + len
        var tmp: Byte
        var currentOfs = ofs

        while (currentOfs < end) {
            tmp = data[currentOfs]
            data[currentOfs] = data[currentOfs + 3]
            data[currentOfs + 3] = tmp

            tmp = data[currentOfs + 1]
            data[currentOfs + 1] = data[currentOfs + 2]
            data[currentOfs + 2] = tmp

            currentOfs += 4
        }
        return data
    }
}
