package estreamj.ciphers.trivium

import estreamj.framework.ESJException
import estreamj.framework.Engine
import estreamj.framework.ICipher
import estreamj.framework.ICipherMaker
import estreamj.framework.Utils

class Trivium : ICipher {
    private val key = ByteArray(10)
    private val s = IntArray(10)

    override val keySize: Int
        get() = KEY_SIZE_BITS ushr 3

    override val nonceSize: Int
        get() = IV_SIZE_BITS ushr 3

    override val wordSize: Int
        get() = 4

    override val isPatented: Boolean
        get() = false

    @Throws(ESJException::class)
    override fun reset() {
        // key is cached already, nothing to do here
    }

    @Throws(ESJException::class)
    override fun setupKey(mode: Int, key: ByteArray, ofs: Int) {
        System.arraycopy(key, ofs, this.key, 0, this.key.size)
    }

    @Throws(ESJException::class)
    override fun setupNonce(nonce: ByteArray, ofs: Int) {
        val key = this.key
        val s = this.s

        var s11 = Utils.readInt32LE(key, 0)
        var s12 = Utils.readInt32LE(key, 4)
        var s13 = (key[8].toInt() and 0xff) or ((key[9].toInt() shl 8) and 0xff00)
        var s21 = Utils.readInt32LE(nonce, ofs)
        var s22 = Utils.readInt32LE(nonce, ofs + 4)
        var s23 = (nonce[ofs + 8].toInt() and 0xff) or ((nonce[ofs + 9].toInt() shl 8) and 0xff00)
        var s31 = 0
        var s32 = 0
        var s33 = 0
        var s34 = 0x07000

        for (i in 0 until 4 * 9) {
            var t1: Int
            var t2: Int
            var t3: Int

            t1 = ((s13 shl 96 - 66) or (s12 ushr 66 - 64)) xor ((s13 shl 96 - 93) or (s12 ushr 93 - 64))
            t2 = ((s23 shl 96 - 69) or (s22 ushr 69 - 64)) xor ((s23 shl 96 - 84) or (s22 ushr 84 - 64))
            t3 = ((s33 shl 96 - 66) or (s32 ushr 66 - 64)) xor ((s34 shl 128 - 111) or (s33 ushr 111 - 96))

            t1 = t1 xor ((((s13 shl 96 - 91) or (s12 ushr 91 - 64)) and ((s13 shl 96 - 92) or (s12 ushr 92 - 64))) xor ((s23 shl 96 - 78) or (s22 ushr 78 - 64)))
            t2 = t2 xor ((((s23 shl 96 - 82) or (s22 ushr 82 - 64)) and ((s23 shl 96 - 83) or (s22 ushr 83 - 64))) xor ((s33 shl 96 - 87) or (s32 ushr 87 - 64)))
            t3 = t3 xor ((((s34 shl 128 - 109) or (s33 ushr 109 - 96)) and ((s34 shl 128 - 110) or (s33 ushr 110 - 96))) xor ((s13 shl 96 - 69) or (s12 ushr 69 - 64)))

            s13 = s12
            s12 = s11
            s11 = t3
            s23 = s22
            s22 = s21
            s21 = t1
            s34 = s33
            s33 = s32
            s32 = s31
            s31 = t2
        }

        s[0] = s11
        s[1] = s12
        s[2] = s13
        s[3] = s21
        s[4] = s22
        s[5] = s23
        s[6] = s31; s[7] = s32; s[8] = s33; s[9] = s34
    }

    @Throws(ESJException::class)
    override fun process(
        inbuf: ByteArray,
        inOfs: Int,
        outbuf: ByteArray,
        outOfs: Int,
        len: Int
    ) {
        var s11 = s[0]; var s12 = s[1]; var s13 = s[2]
        var s21 = s[3]; var s22 = s[4]; var s23 = s[5]
        var s31 = s[6]; var s32 = s[7]; var s33 = s[8]; var s34 = s[9]

        var currentOutOfs = outOfs
        var currentInOfs = inOfs
        var outEnd = currentOutOfs + (len and 3.inv())

        while (currentOutOfs < outEnd) {
            var t1: Int; var t2: Int; var t3: Int; var reg: Int

            t1 = ((s13 shl 96 - 66) or (s12 ushr 66 - 64)) xor ((s13 shl 96 - 93) or (s12 ushr 93 - 64))
            t2 = ((s23 shl 96 - 69) or (s22 ushr 69 - 64)) xor ((s23 shl 96 - 84) or (s22 ushr 84 - 64))
            t3 = ((s33 shl 96 - 66) or (s32 ushr 66 - 64)) xor ((s34 shl 128 - 111) or (s33 ushr 111 - 96))

            reg = t1 xor t2 xor t3
            outbuf[currentOutOfs] = (inbuf[currentInOfs].toInt() xor reg).toByte()
            outbuf[currentOutOfs + 1] = (inbuf[currentInOfs + 1].toInt() xor (reg ushr 8)).toByte()
            outbuf[currentOutOfs + 2] = (inbuf[currentInOfs + 2].toInt() xor (reg ushr 16)).toByte()
            outbuf[currentOutOfs + 3] = (inbuf[currentInOfs + 3].toInt() xor (reg ushr 24)).toByte()

            t1 = t1 xor ((((s13 shl 96 - 91) or (s12 ushr 91 - 64)) and ((s13 shl 96 - 92) or (s12 ushr 92 - 64))) xor ((s23 shl 96 - 78) or (s22 ushr 78 - 64)))
            t2 = t2 xor ((((s23 shl 96 - 82) or (s22 ushr 82 - 64)) and ((s23 shl 96 - 83) or (s22 ushr 83 - 64))) xor ((s33 shl 96 - 87) or (s32 ushr 87 - 64)))
            t3 = t3 xor ((((s34 shl 128 - 109) or (s33 ushr 109 - 96)) and ((s34 shl 128 - 110) or (s33 ushr 110 - 96))) xor ((s13 shl 96 - 69) or (s12 ushr 69 - 64)))

            s13 = s12; s12 = s11; s11 = t3
            s23 = s22; s22 = s21; s21 = t1
            s34 = s33; s33 = s32; s32 = s31; s31 = t2

            currentOutOfs += 4
            currentInOfs += 4
        }

        outEnd = currentOutOfs + (len and 3)
        if (currentOutOfs < outEnd) {
            var t1: Int; var t2: Int; var t3: Int; var reg: Int

            t1 = ((s13 shl 96 - 66) or (s12 ushr 66 - 64)) xor ((s13 shl 96 - 93) or (s12 ushr 93 - 64))
            t2 = ((s23 shl 96 - 69) or (s22 ushr 69 - 64)) xor ((s23 shl 96 - 84) or (s22 ushr 84 - 64))
            t3 = ((s33 shl 96 - 66) or (s32 ushr 66 - 64)) xor ((s34 shl 128 - 111) or (s33 ushr 111 - 96))

            reg = t1 xor t2 xor t3
            while (currentOutOfs < outEnd) {
                outbuf[currentOutOfs] = (inbuf[currentInOfs].toInt() xor reg).toByte()
                reg = reg ushr 8
                currentOutOfs++
                currentInOfs++
            }

            t1 = t1 xor ((((s13 shl 96 - 91) or (s12 ushr 91 - 64)) and ((s13 shl 96 - 92) or (s12 ushr 92 - 64))) xor ((s23 shl 96 - 78) or (s22 ushr 78 - 64)))
            t2 = t2 xor ((((s23 shl 96 - 82) or (s22 ushr 82 - 64)) and ((s23 shl 96 - 83) or (s22 ushr 83 - 64))) xor ((s33 shl 96 - 87) or (s32 ushr 87 - 64)))
            t3 = t3 xor ((((s34 shl 128 - 109) or (s33 ushr 109 - 96)) and ((s34 shl 128 - 110) or (s33 ushr 110 - 96))) xor ((s13 shl 96 - 69) or (s12 ushr 69 - 64)))

            s13 = s12; s12 = s11; s11 = t3
            s23 = s22; s22 = s21; s21 = t1
            s34 = s33; s33 = s32; s32 = s31; s31 = t2
        }

        s[0] = s11; s[1] = s12; s[2] = s13
        s[3] = s21; s[4] = s22; s[5] = s23
        s[6] = s31; s[7] = s32; s[8] = s33; s[9] = s34
    }

    class Maker : ICipherMaker {
        @Throws(ESJException::class)
        override fun create(): ICipher {
            return Trivium()
        }

        override val name: String
            get() = "Trivium"
    }

    companion object {
        const val KEY_SIZE_BITS = 80
        const val IV_SIZE_BITS = 80

        fun register() {
            Engine.registerCipher(Maker())
        }
    }
}
