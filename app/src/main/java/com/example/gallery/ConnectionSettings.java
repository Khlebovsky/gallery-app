package com.example.gallery;

import android.content.Context;
import android.os.Build;
import com.google.android.gms.security.ProviderInstaller;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;

public final class ConnectionSettings
{
	private static final int GOOGLE_SERVICES_API=19;
	@NonNull
	private static final String TLS_VERSION_1_1="TLSv1.1";
	@NonNull
	private static final String TLS_VERSION_1_2="TLSv1.2";

	private ConnectionSettings()
	{
	}

	public static OkHttpClient getOkHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		@NonNull
		final OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(getTLSSocketFactory(),getTrustManager()[0]).build();
		return client;
	}

	public static SSLSocketFactory getTLSSocketFactory() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException
	{
		@NonNull
		final SSLContext ctx=SSLContext.getInstance("TLS");
		ctx.init(null,getTrustManager(),null);
		@NonNull
		final SSLSocketFactory sslSocketFactory=ctx.getSocketFactory();
		@NonNull
		final SSLSocketFactory tlsSocketFactory=Build.VERSION.SDK_INT<=Build.VERSION_CODES.KITKAT?new TLSSocketFactory(sslSocketFactory):sslSocketFactory;
		return tlsSocketFactory;
	}

	public static X509TrustManager[] getTrustManager() throws KeyStoreException, NoSuchAlgorithmException
	{
		@Nullable
		final X509TrustManager[] myTrustManager={
			new MyTrustedManager()
		};
		return myTrustManager;
	}

	public static void initGooglePlayServices(@NonNull final Context context)
	{
		if(Build.VERSION.SDK_INT<=GOOGLE_SERVICES_API)
		{
			try
			{
				ProviderInstaller.installIfNeeded(context);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static class MyTrustedManager implements X509TrustManager
	{
		@NonNull
		public static final X509Certificate[] X_509_CERTIFICATES=new X509Certificate[0];
		@Nullable
		static TrustManager[] trustManagers;
		@Nullable
		X509TrustManager x509TrustManager;

		public MyTrustedManager() throws NoSuchAlgorithmException, KeyStoreException
		{
			@NonNull
			final TrustManagerFactory trustManagerFactory=TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init((KeyStore)null);
			trustManagers=trustManagerFactory.getTrustManagers();
			for(final TrustManager trustManager : trustManagers)
			{
				if(trustManager instanceof X509TrustManager)
				{
					x509TrustManager=(X509TrustManager)trustManager;
				}
			}
		}

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
		{
			@Nullable
			final X509TrustManager x509TrustManager_=x509TrustManager;
			if(x509TrustManager_!=null)
			{
				x509TrustManager_.checkServerTrusted(x509Certificates,s);
			}
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
		{
			@Nullable
			final X509TrustManager x509TrustManager_=x509TrustManager;
			if(x509TrustManager_!=null)
			{
				x509TrustManager_.checkServerTrusted(x509Certificates,s);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers()
		{
			return X_509_CERTIFICATES;
		}
	}

	public static class TLSSocketFactory extends SSLSocketFactory
	{
		@NonNull
		private final SSLSocketFactory internalSSLSocketFactory;

		public TLSSocketFactory(@NonNull SSLSocketFactory delegate)
		{
			internalSSLSocketFactory=delegate;
		}

		@Override
		public Socket createSocket(Socket s,String host,int port,boolean autoClose) throws IOException
		{
			return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s,host,port,autoClose));
		}

		@Override
		public Socket createSocket(String host,int port) throws IOException
		{
			return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host,port));
		}

		@Override
		public Socket createSocket(String host,int port,InetAddress localHost,int localPort) throws IOException
		{
			return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host,port,localHost,localPort));
		}

		@Override
		public Socket createSocket(InetAddress host,int port) throws IOException
		{
			return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host,port));
		}

		@Override
		public Socket createSocket(InetAddress address,int port,InetAddress localAddress,int localPort) throws IOException
		{
			return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address,port,localAddress,localPort));
		}

		private static Socket enableTLSOnSocket(@NonNull Socket socket)
		{
			if((socket instanceof SSLSocket)&&isTLSServerEnabled((SSLSocket)socket))
			{
				((SSLSocket)socket).setEnabledProtocols(new String[]{TLS_VERSION_1_1,TLS_VERSION_1_2});
			}
			return socket;
		}

		@Override
		public String[] getDefaultCipherSuites()
		{
			return internalSSLSocketFactory.getDefaultCipherSuites();
		}

		@Override
		public String[] getSupportedCipherSuites()
		{
			return internalSSLSocketFactory.getSupportedCipherSuites();
		}

		private static boolean isTLSServerEnabled(@NonNull SSLSocket sslSocket)
		{
			for(final String protocol : sslSocket.getSupportedProtocols())
			{
				if(TLS_VERSION_1_1.equals(protocol)||TLS_VERSION_1_2.equals(protocol))
				{
					return true;
				}
			}
			return false;
		}
	}
}