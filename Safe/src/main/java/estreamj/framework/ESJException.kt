package estreamj.framework

class ESJException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    companion object {
        private const val serialVersionUID = 6375271013846829471L
    }
}
