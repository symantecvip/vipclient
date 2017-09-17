package com.symantec.vip.helper;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SslSocketFactoryUtil {
	private static final Map<String, SSLContext> keyStoreToSSLContextMap = new ConcurrentHashMap<String, SSLContext>();
	private static final Object lock = new Object();

	public static SSLContext getSSLContext(String keyStorePath, String keyStorePassword) {
		SSLContext sslContext = null;
		if(keyStoreToSSLContextMap.containsKey(keyStorePath)) {
			sslContext = keyStoreToSSLContextMap.get(keyStorePath);
		} else {
			synchronized(lock) {
				// Check again to avoid duplicate creation.
				if(keyStoreToSSLContextMap.containsKey(keyStorePath)) {
					sslContext = keyStoreToSSLContextMap.get(keyStorePath);
				} else {
					sslContext = createSslContext(keyStorePath, keyStorePassword);
					keyStoreToSSLContextMap.put(keyStorePath, sslContext);
				}
			}
		}

		return sslContext;
	}

	public static SSLSocketFactory getSSLSocketFactory(String keyStorePath, String keyStorePassword) {
		return getSSLContext(keyStorePath, keyStorePassword).getSocketFactory();
	}

	private static SSLContext createSslContext(String keyStorePath, String keyStorePassword) {
		System.out.println("Creating SSLContext for key store " + keyStorePath);

		// Get an instance of the keystore.
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance("pkcs12");
		} catch (KeyStoreException kse) {
			throw new RuntimeException("Cannot get keystore for type PKCS12", kse);
		}

		// Open the key store file and load the key store.
		FileInputStream keyStoreInputStream = null;
		try {
			keyStoreInputStream = new FileInputStream(keyStorePath);
			keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());
		} catch (Exception e) {
			throw new RuntimeException("Exception thrown while loading the key store [" + keyStorePath + "].", e);
		} finally {
			if(keyStoreInputStream != null)
				try { keyStoreInputStream.close(); } catch (Exception e) {};
		}

		try {
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{new NaiveTrustManager()}, null);
            System.out.println("SSLContext provider: " + sslContext.getProvider().toString());
			return sslContext;
		} catch (Exception e) {
			throw new RuntimeException("Exception thrown while initializing key manager factory.", e);
		}
	}

	/**
	 * This Trust Manager is "naive" because it trusts everyone.
	 **/
	public static class NaiveTrustManager implements X509TrustManager {
		/**
		 * Doesn't throw an exception, so this is how it approves a certificate.
		 * @see X509TrustManager#checkClientTrusted(X509Certificate[], String)
		 **/
		@Override
		public void checkClientTrusted ( X509Certificate[] cert, String authType ) throws CertificateException {
		}

		/**
		 * Doesn't throw an exception, so this is how it approves a certificate.
		 * @see X509TrustManager#checkServerTrusted(X509Certificate[], String)
		 **/
		@Override
		public void checkServerTrusted ( X509Certificate[] cert, String authType ) throws CertificateException  {
            System.out.println("Trusting server with SubjectDN: " + cert[0].getSubjectDN().getName());
		}

		/**
		 * @see X509TrustManager#getAcceptedIssuers()
		 **/
		@Override
		public X509Certificate[] getAcceptedIssuers () {
			return null;  // I've seen someone return new X509Certificate[ 0 ];
		}
	}

	public static HostnameVerifier getAllIgnorningHostnameVerifier() {
		return new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
                System.out.println("AllIgnorningHostnameVerifier returning true for host name: " + hostname);
				return true;
			}
		};
	}
}
