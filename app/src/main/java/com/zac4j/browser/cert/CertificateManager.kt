package com.zac4j.browser.cert

import android.text.TextUtils
import java.math.BigInteger
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Created by Zaccc on 2018/8/14.
 */
class CertificateManager : X509TrustManager {

    companion object {
        const val PUB_KEY = "30820122300d06092a864886f70d0101" +
            "0105000382010f003082010a0282010100b35ea8adaf4cb6db86068a836f3c85" +
            "5a545b1f0cc8afb19e38213bac4d55c3f2f19df6dee82ead67f70a990131b6bc" +
            "ac1a9116acc883862f00593199df19ce027c8eaaae8e3121f7f329219464e657" +
            "2cbf66e8e229eac2992dd795c4f23df0fe72b6ceef457eba0b9029619e0395b8" +
            "609851849dd6214589a2ceba4f7a7dcceb7ab2a6b60c27c69317bd7ab2135f50" +
            "c6317e5dbfb9d1e55936e4109b7b911450c746fe0d5d07165b6b23ada7700b00" +
            "33238c858ad179a82459c4718019c111b4ef7be53e5972e06ca68a112406da38" +
            "cf60d2f4fda4d1cd52f1da9fd6104d91a34455cd7b328b02525320a35253147b" +
            "e0b7a5bc860966dc84f10d723ce7eed5430203010001"
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        TODO(
            "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        if (chain == null || chain.isEmpty()) {
            throw IllegalArgumentException("checkServerTrusted: X509Certificate is empty")
        }

        if (TextUtils.isEmpty(authType) || "rsa" == authType?.toLowerCase()) {
            throw CertificateException("checkServerTrusted: AuthType is not RSA")
        }

        try {
            val tmf = TrustManagerFactory.getInstance("X509")
            tmf.init(null as KeyStore)

            for (trustManager in tmf.trustManagers) {
                (trustManager as X509TrustManager).checkServerTrusted(chain, authType)
            }
        } catch (e: Exception) {
            throw CertificateException(e)
        }

        // Hack ahead: BigInteger and toString(). We know a DER encoded Public Key begins
        // with 0x30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is no leading 0x00 to drop.
        val publicKey = chain[0].publicKey
        val encoded = BigInteger(1, publicKey.encoded).toString(16)

        // Pin it
        val expected = PUB_KEY == encoded
        if (!expected) {
            throw CertificateException("checkServerTrusted: unexpected public key")
        }
    }
}