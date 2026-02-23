package estreamj.framework

/**
 * Generic interface every cipher needs to implement.
 */
interface ICipher {
    /**
     * @return true: algorithm is patented, check with vendor for license
     * details / false: free to use in private and commerical applications
     */
    val isPatented: Boolean

    /**
     * @return key size in bytes
     */
    val keySize: Int

    /**
     * @return nonce size in bytes.
     */
    val nonceSize: Int

    /**
     * @return alignment of data needed during calls into process()
     */
    val wordSize: Int

    /**
     * Resets the instance, so it can be reused.
     *
     * @throws ESJException if any error occurs
     */
    @Throws(ESJException::class)
    fun reset()

    /**
     * Sets up a new key with the existing instance.
     *
     * @param mode see MODE_xxx
     * @param key  buffer with key material
     * @param ofs  where the key starts
     * @throws ESJException if any error occurs
     */
    @Throws(ESJException::class)
    fun setupKey(mode: Int, key: ByteArray, ofs: Int)

    /**
     * Sets up a new nonce with the existing cipher instance.
     *
     * @param nonce buffer with nonce material
     * @param ofs   where the nonce starts
     * @throws ESJException if any error occurs
     */
    @Throws(ESJException::class)
    fun setupNonce(nonce: ByteArray, ofs: Int)

    /**
     * Processes data.
     *
     * @param inbuf  input buffer
     * @param inOfs  where to start reading from the input buffer
     * @param outbuf output buffer
     * @param outOfs where to start writing in the output buffer
     * @param len    number of bytes to process, must be aligned to the cipher's
     *               word size except on the last call where an arbitrary size can be used
     * @throws ESJException in any error occured
     */
    @Throws(ESJException::class)
    fun process(
        inbuf: ByteArray,
        inOfs: Int,
        outbuf: ByteArray,
        outOfs: Int,
        len: Int
    )

    companion object {
        /**
         * mode: instance is used for encryption
         */
        const val MODE_ENCRYPT = 0

        /**
         * mode: instance is used for decryption
         */
        const val MODE_DECRYPT = 1
    }
}
