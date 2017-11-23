package org.apache.commons.httpclient.contrib.ssl

import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.scheme.LayeredSocketFactory
import org.apache.http.conn.scheme.SocketFactory
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager

class EasySSLSocketFactory : SocketFactory, LayeredSocketFactory {
    private var sslcontext: SSLContext? = null


    private val sslContext: SSLContext
        @Throws(IOException::class)
        get() {
            if (sslcontext == null) {
                sslcontext = createEasySSLContext()
            }
            return sslcontext as SSLContext
        }


    @Throws(IOException::class)
    private fun createEasySSLContext(): SSLContext {
        try {
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<TrustManager>(EasyX509TrustManager(null)), null)
            return context
        } catch (e: Exception) {
            throw IOException(e.message)
        }

    }


    /**
     * @see org.apache.http.conn.scheme.SocketFactory.connectSocket
     */


    @Throws(IOException::class, UnknownHostException::class, ConnectTimeoutException::class)
    override fun connectSocket(sock: Socket?, host: String, port: Int, localAddress: InetAddress?, localPort: Int,
                               params: HttpParams): Socket {
        var localPort = localPort
        val connTimeout = HttpConnectionParams.getConnectionTimeout(params)
        val soTimeout = HttpConnectionParams.getSoTimeout(params)
        val remoteAddress = InetSocketAddress(host, port)
        val sslsock = (sock ?: createSocket()) as SSLSocket

        if (localAddress != null || localPort > 0) {
            // we need to bind explicitly
            if (localPort < 0) {
                localPort = 0 // indicates "any"
            }
            val isa = InetSocketAddress(localAddress, localPort)
            sslsock.bind(isa)
        }

        sslsock.connect(remoteAddress, connTimeout)
        sslsock.soTimeout = soTimeout
        return sslsock
    }


    /**
     * @see org.apache.http.conn.scheme.SocketFactory.createSocket
     */
    @Throws(IOException::class)
    override fun createSocket(): Socket {
        return sslContext.socketFactory.createSocket()
    }


    /**
     * @see org.apache.http.conn.scheme.SocketFactory.isSecure
     */
    @Throws(IllegalArgumentException::class)
    override fun isSecure(socket: Socket): Boolean {
        return true
    }


    /**
     * @see org.apache.http.conn.scheme.LayeredSocketFactory.createSocket
     */
    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        return sslContext.socketFactory.createSocket(socket, host, port, autoClose)
    }


    // -------------------------------------------------------------------
    // javadoc in org.apache.http.conn.scheme.SocketFactory says :
    // Both Object.equals() and Object.hashCode() must be overridden
    // for the correct operation of some connection managers
    // -------------------------------------------------------------------

    override fun equals(obj: Any?): Boolean {
        return obj != null && obj.javaClass == EasySSLSocketFactory::class.java
    }


    override fun hashCode(): Int {
        return EasySSLSocketFactory::class.java.hashCode()
    }
}
