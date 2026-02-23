package estreamj.framework

import estreamj.ciphers.trivium.Trivium
import java.util.Collections
import java.util.Vector

/**
 * The engine accumulates all the stream ciphers available. All implementations
 * register with the engine by themselves.
 */
object Engine {
    private val _cphMks = HashMap<String, ICipherMaker>()

    init {
        // Register Trivium explicitly, replacing reflection
        Trivium.register()
    }

    /**
     * @return the names of all ciphers registered; can be empty if no ciphers
     * have been registered so far
     */
    @JvmStatic
    fun getCipherNames(): Array<String> {
        synchronized(_cphMks) {
            return _cphMks.keys.toTypedArray().apply { sort() }
        }
    }

    /**
     * Creates a new cipher instance.
     *
     * @param name name of the cipher to make
     * @return new cipher instance
     * @throws ESJException if any error occured
     */
    @JvmStatic
    @Throws(ESJException::class)
    fun createCipher(name: String): ICipher {
        val maker: ICipherMaker?
        synchronized(_cphMks) {
            maker = _cphMks[name]
        }
        if (maker == null) {
            throw ESJException("no maker registered for cipher \"$name\"")
        }
        return maker.create()
    }

    /**
     * Called by cipher implementations to register their factories, usually
     * during startup time.
     *
     * @param cphMk the factory to register
     */
    @JvmStatic
    fun registerCipher(cphMk: ICipherMaker) {
        val name = cphMk.name
        synchronized(_cphMks) {
            if (_cphMks.containsKey(name)) {
                System.err.println("cipher \"$name\" has been registered already")
                return
            }
            _cphMks[name] = cphMk
        }
    }
}
