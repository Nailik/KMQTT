package socket.tls

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import openssl.*
import socket.IOException

actual class TLSClientEngine : TLSEngine {

    private val context: CPointer<SSL>
    private val readBio: CPointer<BIO>
    private val writeBio: CPointer<BIO>

    init {
        val readBio = BIO_new(BIO_s_mem()) ?: throw IOException("Failed allocating read BIO")

        val writeBio = BIO_new(BIO_s_mem())
        if (writeBio == null) {
            BIO_free(readBio)
            throw IOException("Failed allocating read BIO")
        }

        val method = TLS_client_method()
        val sslContext = SSL_CTX_new(method)!!

        val clientContext = SSL_new(sslContext)
        if (clientContext == null) {
            BIO_free(readBio)
            BIO_free(writeBio)
            throw IOException("Failed allocating read BIO")
        }

        SSL_set_bio(clientContext, readBio, writeBio)
        context = clientContext
        this.readBio = readBio
        this.writeBio = writeBio
    }

    override val isInitFinished: Boolean
        get() = SSL_is_init_finished(context) != 0

    override val bioShouldRetry: Boolean
        get() = BIO_test_flags(writeBio, BIO_FLAGS_SHOULD_RETRY) == 0

    override fun accept(): Int {
        return SSL_accept(context)
    }

    override fun write(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_write(context, buffer, length)
    }

    override fun read(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_read(context, buffer, length)
    }

    override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int {
        return BIO_read(writeBio, buffer, length)
    }

    override fun getError(result: Int): TLSError {
        return when (SSL_get_error(context, result)) {
            SSL_ERROR_NONE -> TLSError.OK
            SSL_ERROR_WANT_READ -> TLSError.WANT_READ
            SSL_ERROR_ZERO_RETURN, SSL_ERROR_SYSCALL -> TLSError.ERROR
            else -> TLSError.ERROR
        }
    }

    override fun close() {
        SSL_free(context)
    }
}