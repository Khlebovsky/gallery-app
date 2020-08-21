package com.example.gallery;

import android.os.Build;
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

public final class ConnectionSettings
{
	private ConnectionSettings()
	{
	}

	public static SSLSocketFactory getTLSSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
	{
		@NonNull
		final SSLContext ctx=SSLContext.getInstance("TLS");
		ctx.init(null,getTrustManager(),null);
		@NonNull
		final SSLSocketFactory sslSocketFactory=ctx.getSocketFactory();
		@Nullable
		final SSLSocketFactory tlsSocketFactory=Build.VERSION.SDK_INT<=Build.VERSION_CODES.KITKAT?new TLSSocketFactory(sslSocketFactory):sslSocketFactory;
		return tlsSocketFactory;
	}

	public static X509TrustManager[] getTrustManager()
	{
		@Nullable
		X509TrustManager[] myTrustManager=null;
		try
		{
			myTrustManager=new X509TrustManager[]{
				new MyTrustedManager()
			};
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return myTrustManager;
	}

	public static class MyTrustedManager implements X509TrustManager
	{
		@NonNull
		public static final X509Certificate[] X_509_CERTIFICATES=new X509Certificate[0];
		static TrustManager[] tms;
		X509TrustManager pkixTrustManager;

		public MyTrustedManager() throws NoSuchAlgorithmException, KeyStoreException
		{
			@NonNull
			final TrustManagerFactory factory=TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			factory.init((KeyStore)null);
			tms=factory.getTrustManagers();
			for(final TrustManager tm : tms)
			{
				if(tm instanceof X509TrustManager)
				{
					pkixTrustManager=(X509TrustManager)tm;
				}
			}
		}

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
		{
			pkixTrustManager.checkServerTrusted(x509Certificates,s);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates,String s)
		{
			try
			{
				pkixTrustManager.checkServerTrusted(x509Certificates,s);
			}
			catch(Throwable e)
			{
				e.printStackTrace();
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
				((SSLSocket)socket).setEnabledProtocols(new String[]{"TLSv1.1","TLSv1.2"});
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
				if("TLSv1.1".equals(protocol)||"TLSv1.2".equals(protocol))
				{
					return true;
				}
			}
			return false;
		}
	}
}