package estreamj.framework

/**
 * Simple cipher factory.
 */
interface ICipherMaker {
    /**
     * @return the name of cipher, which is used for queries - so it must be
     * unique
     */
    val name: String

    /**
     * Create a new cipher instance.
     *
     * @return new instance, which can also be of the type ICipherMAC, use the
     * "instanceof" keyword to find out what you are dealing with
     * @throws ESJException if any error occured
     */
    @Throws(ESJException::class)
    fun create(): ICipher
}
