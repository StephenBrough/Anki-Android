package org.apache.commons.httpclient.contrib.ssl

import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Date

import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class EasyX509TrustManager
/**
 * Constructor for EasyX509TrustManager.
 */
@Throws(NoSuchAlgorithmException::class, KeyStoreException::class)
constructor(keystore: KeyStore?) : X509TrustManager {
    private var standardTrustManager: X509TrustManager? = null


    init {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keystore)
        val trustmanagers = factory.trustManagers
        if (trustmanagers.isEmpty()) {
            throw NoSuchAlgorithmException("no trust manager found")
        }
        standardTrustManager = trustmanagers[0] as X509TrustManager
    }


    /**
     * @see javax.net.ssl.X509TrustManager.checkClientTrusted
     */
    @Throws(CertificateException::class)
    override fun checkClientTrusted(certificates: Array<X509Certificate>, authType: String) {
        standardTrustManager!!.checkClientTrusted(certificates, authType)
    }


    /**
     * @see javax.net.ssl.X509TrustManager.checkServerTrusted
     */
    @Throws(CertificateException::class)
    override fun checkServerTrusted(certificates: Array<X509Certificate>, authType: String) {
        // Clean up the certificates chain and build a new one.
        // Theoretically, we shouldn't have to do this, but various web servers
        // in practice are mis-configured to have out-of-order certificates or
        // expired self-issued root certificate.
        var chainLength = certificates.size
        if (certificates.size > 1) {
            // 1. we clean the received certificates chain.
            // We start from the end-entity certificate, tracing down by matching
            // the "issuer" field and "subject" field until we can't continue.
            // This helps when the certificates are out of order or
            // some certificates are not related to the site.
            var currIndex: Int
            currIndex = 0
            while (currIndex < certificates.size) {
                var foundNext = false
                for (nextIndex in currIndex + 1 until certificates.size) {
                    if (certificates[currIndex].issuerDN == certificates[nextIndex].subjectDN) {
                        foundNext = true
                        // Exchange certificates so that 0 through currIndex + 1 are in proper order
                        if (nextIndex != currIndex + 1) {
                            val tempCertificate = certificates[nextIndex]
                            certificates[nextIndex] = certificates[currIndex + 1]
                            certificates[currIndex + 1] = tempCertificate
                        }
                        break
                    }
                }
                if (!foundNext) {
                    break
                }
                ++currIndex
            }

            // 2. we exam if the last traced certificate is self issued and it is expired.
            // If so, we drop it and pass the rest to checkServerTrusted(), hoping we might
            // have a similar but unexpired trusted root.
            chainLength = currIndex + 1
            val lastCertificate = certificates[chainLength - 1]
            val now = Date()
            if (lastCertificate.subjectDN == lastCertificate.issuerDN && now.after(lastCertificate.notAfter)) {
                --chainLength
            }
        }

        standardTrustManager!!.checkServerTrusted(certificates, authType)
    }


    /**
     * @see javax.net.ssl.X509TrustManager.getAcceptedIssuers
     */
    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return standardTrustManager!!.acceptedIssuers
    }
}
