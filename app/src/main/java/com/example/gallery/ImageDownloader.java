package com.example.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.ortiz.touchview.TouchImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ImageDownloader
{
	@NonNull
	public static final GalleryHandler GALLERY_HANDLER=new GalleryHandler();
	@NonNull
	public static final HashMap<String,String> FILE_NAMES=new HashMap<>();
	@NonNull
	public static final ArrayList<String> NO_INTERNET_LINKS=new ArrayList<>();
	@NonNull
	static final ArrayList<String> URLS_IN_PROGRESS=new ArrayList<>();
	@Nullable
	static final File PREVIEWS=MainActivity.previews;
	@Nullable
	static final File BYTES=MainActivity.bytes;
	public static int REPEAT_NUM=3;
	static int imageWidth;
	static int imageHeight;
	static int ERROR_TIME_SLEEP=1500;
	@NonNull
	private static final String TAG="ImageDownloader";
	private static int MAX_THREAD_NUM=2;

	private ImageDownloader()
	{
	}

	public static void addBitmapToMemoryCache(String key,Bitmap bitmap)
	{
		if(getBitmapFromMemoryCache(key)==null)
		{
			try
			{
				if(MainActivity.memoryCache!=null)
				{
					MainActivity.memoryCache.put(key,bitmap);
				}
			}
			catch(Exception e)
			{
				Log.d(TAG,String.valueOf(e));
			}
		}
	}

	// TODO доделать логику скачивания картинки
	static void downloadImageFromSharing(final String url,final TouchImageView touchImageView,final Context context)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@NonNull
					final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).sslSocketFactory(ConnectionSettings.getTLSSocketFactory(),ConnectionSettings.getTrustManager()[0]).build();
					@NonNull
					final Call call=client.newCall(new Request.Builder().url(url).get().build());
					@NonNull
					final Response response=call.execute();
					@NonNull
					final ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
					@Nullable
					InputStream inputStream=null;
					if(response.code()==200||response.code()==201)
					{
						try
						{
							if(response.body()!=null)
							{
								inputStream=response.body().byteStream();
							}
							@NonNull
							final byte[] buffer=new byte[4096];
							@NonNull
							int n;
							if(inputStream!=null)
							{
								while((n=inputStream.read(buffer))>0)
								{
									byteArrayOutputStream.write(buffer,0,n);
								}
							}
							@Nullable
							final Bitmap originalBitmap=BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length);
							if(originalBitmap!=null)
							{
								//noinspection AnonymousInnerClassMayBeStatic
								((SharedImage)context).runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										touchImageView.setImageBitmap(originalBitmap);
										touchImageView.setDoubleTapScale(3);
										touchImageView.setMaxZoom(10);
										touchImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
										touchImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
									}
								});
							}
							else
							{
								//noinspection AnonymousInnerClassMayBeStatic
								((SharedImage)context).runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										touchImageView.setImageResource(R.drawable.ic_error);
									}
								});
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						finally
						{
							if(inputStream!=null)
							{
								inputStream.close();
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	static void downloadOrGetImageFromCache(final String url)
	{
		@NonNull
		final String urlHashMD5=urlToHashMD5(url);
		@NonNull
		final File path=new File(PREVIEWS,urlHashMD5);
		if(path.exists())
		{
			new Thread()
			{
				@Override
				public void run()
				{
					ThreadsCounter.increaseThreadsCounter();
					@Nullable
					Bitmap bitmap;
					try
					{
						bitmap=BitmapFactory.decodeFile(String.valueOf(path));
					}
					catch(Exception e)
					{
						Log.d(TAG,String.valueOf(e));
						e.printStackTrace();
						bitmap=null;
					}
					if(bitmap!=null)
					{
						addBitmapToMemoryCache(url,bitmap);
						FILE_NAMES.put(url,urlHashMD5);
						@NonNull
						final Message message=GALLERY_HANDLER.obtainMessage();
						GALLERY_HANDLER.sendMessage(message);
					}
					else
					{
						MainActivity.ERROR_LIST.put(url,"Decoding error");
						FILE_NAMES.put(url,"error");
					}
					URLS_IN_PROGRESS.remove(url);
					ThreadsCounter.decreaseThreadsCounter();
				}
			}.start();
		}
		else
		{
			new Thread()
			{
				@Override
				public void run()
				{
					ThreadsCounter.increaseThreadsCounter();
					@Nullable
					String exeption=null;
					@Nullable
					String status=null;
					if(PREVIEWS!=null&&!PREVIEWS.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						PREVIEWS.mkdirs();
					}
					if(BYTES!=null&&!BYTES.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						BYTES.mkdirs();
					}
					for(int i=0;i<REPEAT_NUM;i++)
					{
						if(!MainActivity.isConnected)
						{
							NO_INTERNET_LINKS.add(url);
							break;
						}
						try
						{
							@NonNull
							final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).sslSocketFactory(ConnectionSettings.getTLSSocketFactory(),ConnectionSettings.getTrustManager()[0]).build();
							@NonNull
							final Call call=client.newCall(new Request.Builder().url(url).get().build());
							@NonNull
							final Response response=call.execute();
							@NonNull
							final ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
							@Nullable
							InputStream inputStream=null;
							if(response.code()==200||response.code()==201)
							{
								@NonNull
								boolean isImage=true;
								NO_INTERNET_LINKS.remove(url);
								try
								{
									if(response.body()!=null)
									{
										inputStream=response.body().byteStream();
									}
									@NonNull
									final byte[] buffer=new byte[4096];
									@NonNull
									int n;
									if(inputStream!=null)
									{
										while((n=inputStream.read(buffer))>0)
										{
											byteArrayOutputStream.write(buffer,0,n);
											if(byteArrayOutputStream.toByteArray().length>30000)
											{
												try
												{
													@NonNull
													final BitmapFactory.Options options=new BitmapFactory.Options();
													options.inJustDecodeBounds=true;
													//noinspection unused
													@Nullable
													final Bitmap originalBitmap=BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,options);
													@NonNull
													final int originalBitmapWidth=options.outWidth;
													if(originalBitmapWidth==-1)
													{
														isImage=false;
													}
												}
												catch(Exception e)
												{
													e.printStackTrace();
												}
											}
										}
									}
									if(isImage)
									{
										try
										{
											@NonNull
											final File pathBytes=new File(BYTES,urlHashMD5);
											@NonNull
											final OutputStream outputStream=new FileOutputStream(pathBytes);
											byteArrayOutputStream.writeTo(outputStream);
											outputStream.flush();
											outputStream.close();
											@NonNull
											final BitmapFactory.Options options=new BitmapFactory.Options();
											options.inJustDecodeBounds=true;
											BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,options);
											@NonNull
											int originalBitmapWidth=options.outWidth;
											if(originalBitmapWidth==-1)
											{
												exeption="Decoding error";
												status="error";
											}
											else
											{
												@NonNull
												int factor=0;
												while(originalBitmapWidth>imageWidth)
												{
													originalBitmapWidth/=2;
													factor+=2;
												}
												@NonNull
												final float heightRatio=(float)originalBitmapWidth/imageWidth*100;
												if(heightRatio<75)
												{
													factor-=2;
												}
												@NonNull
												final BitmapFactory.Options previewOptions=new BitmapFactory.Options();
												previewOptions.inSampleSize=factor;
												@Nullable
												Bitmap preview=null;
												try
												{
													preview=BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,previewOptions);
												}
												catch(Exception e)
												{
													Log.d(TAG,String.valueOf(e));
													e.printStackTrace();
												}
												if(preview!=null)
												{
													addBitmapToMemoryCache(url,preview);
													FILE_NAMES.put(url,urlHashMD5);
													@NonNull
													final File previewName=new File(PREVIEWS,urlHashMD5);
													try
													{
														@NonNull
														final FileOutputStream fileOutputStream=new FileOutputStream(previewName);
														preview.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
														fileOutputStream.close();
														break;
													}
													catch(Exception e)
													{
														e.printStackTrace();
													}
												}
											}
										}
										catch(Exception e)
										{
											status="error";
											exeption=String.valueOf(e);
											e.printStackTrace();
										}
									}
									else
									{
										status="error";
										exeption="The file is not a picture";
									}
								}
								catch(Exception e)
								{
									status="error";
									exeption=String.valueOf(e);
									e.printStackTrace();
								}
								finally
								{
									if(inputStream!=null)
									{
										inputStream.close();
									}
								}
							}
							else
							{
								status="Connection error";
								exeption="Response code: "+response.code();
							}
						}
						catch(Exception e)
						{
							status="error";
							exeption=String.valueOf(e);
							Log.d(TAG,"error - "+url+' '+e);
							e.printStackTrace();
						}
						if(!MainActivity.isConnected)
						{
							NO_INTERNET_LINKS.add(url);
							break;
						}
						try
						{
							sleep(ERROR_TIME_SLEEP);
						}
						catch(InterruptedException e)
						{
							e.printStackTrace();
						}
						if(!MainActivity.isConnected)
						{
							NO_INTERNET_LINKS.add(url);
							break;
						}
						if(i==REPEAT_NUM-1&&status!=null&&exeption!=null)
						{
							MainActivity.ERROR_LIST.put(url,exeption);
							MainActivity.LINKS_STATUS.put(url,status);
							FILE_NAMES.put(url,"error");
						}
					}
					URLS_IN_PROGRESS.remove(url);
					ThreadsCounter.decreaseThreadsCounter();
					@NonNull
					final Message message=GALLERY_HANDLER.obtainMessage();
					GALLERY_HANDLER.sendMessage(message);
				}
			}.start();
		}
	}

	public static Bitmap getBitmapFromMemoryCache(String key)
	{
		return MainActivity.memoryCache!=null?MainActivity.memoryCache.get(key):null;
	}

	public static Bitmap getImageBitmap(final String url)
	{
		@Nullable
		final Bitmap bitmap;
		bitmap=getBitmapFromMemoryCache(url);
		if(bitmap!=null)
		{
			return bitmap;
		}
		if(ThreadsCounter.getThreadCount()<MAX_THREAD_NUM&&!URLS_IN_PROGRESS.contains(url))
		{
			URLS_IN_PROGRESS.add(url);
			downloadOrGetImageFromCache(url);
			return null;
		}
		return null;
	}

	public static void initStatic(@NonNull Context mContext)
	{
		imageWidth=mContext.getResources().getDimensionPixelSize(R.dimen.imageWidth);
		//noinspection SuspiciousNameCombination
		imageHeight=imageWidth;
		try
		{
			@NonNull
			int numOfCPUThreads=0;
			@NonNull
			final String systemPath="/sys/devices/system/cpu/";
			@NonNull
			final File systemDir=new File(systemPath);
			//noinspection ConstantConditions
			@NonNull
			final File[] files=systemDir.listFiles();
			if(files!=null&&files.length!=0)
			{
				for(final File file : files)
				{
					@NonNull
					final String dir=file.toString().substring(systemPath.length());
					if(dir.matches("cpu"+"[0-9]"))
					{
						numOfCPUThreads++;
					}
				}
			}
			MAX_THREAD_NUM=numOfCPUThreads>2?4:2;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String urlToHashMD5(String url)
	{
		try
		{
			@NonNull
			final MessageDigest digest=java.security.MessageDigest.getInstance("MD5");
			digest.update(url.getBytes());
			@NonNull
			final byte[] messageDigest=digest.digest();
			@NonNull
			final StringBuilder hexString=new StringBuilder();
			for(final byte b : messageDigest)
			{
				hexString.append(Integer.toHexString(0xFF&b));
			}
			return hexString.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}

	static class GalleryHandler extends Handler
	{
		GalleryHandler()
		{
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(@NonNull Message msg)
		{
			if(MainActivity.imageAdapter!=null)
			{
				MainActivity.imageAdapter.notifyDataSetChanged();
			}
		}
	}

	public static final class ThreadsCounter
	{
		private static int currentThreadNum;

		private ThreadsCounter()
		{
			currentThreadNum=0;
		}

		public static synchronized void decreaseThreadsCounter()
		{
			currentThreadNum--;
		}

		public static synchronized int getThreadCount()
		{
			return currentThreadNum;
		}

		public static synchronized void increaseThreadsCounter()
		{
			currentThreadNum++;
		}

		public static synchronized void resetThreadsCoutner()
		{
			currentThreadNum=0;
		}
	}
}
