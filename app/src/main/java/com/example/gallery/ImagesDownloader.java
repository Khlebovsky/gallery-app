package com.example.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.*;
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

public final class ImagesDownloader
{
	@NonNull
	public static final HashMap<String,String> URLS_FILE_NAMES=new HashMap<>();
	@NonNull
	public static final ArrayList<String> NO_INTERNET_LINKS=new ArrayList<>();
	public static final int MAX_BITMAP_SIZE=4096;
	@NonNull
	static final ArrayList<String> URLS_IN_PROGRESS=new ArrayList<>();
	@Nullable
	static final File PREVIEWS=MainActivity.imagePreviewsDir;
	@Nullable
	static final File BYTES=MainActivity.imageBytesDir;
	static final int ERROR_TIME_SLEEP=1500;
	static final int CONTROL_NUMBER_OF_BYTES=50000;
	public static int REPEAT_NUM=3;
	static int imageWidth;
	private static int MAX_THREAD_NUM=2;
	@Nullable
	private static Resources resources;

	private ImagesDownloader()
	{
	}

	static void downloadImageToSaveScreen(@NonNull final String url,@NonNull final ImageView imageView,@NonNull final Context context)
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
							int bytes;
							if(inputStream!=null)
							{
								while((bytes=inputStream.read(buffer))>0)
								{
									byteArrayOutputStream.write(buffer,0,bytes);
								}
							}
							@NonNull
							final BitmapFactory.Options options=new BitmapFactory.Options();
							options.inJustDecodeBounds=true;
							BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,options);
							@NonNull
							int originalBitmapWidth=options.outWidth;
							@NonNull
							int originalBitmapHeight=options.outHeight;
							if(originalBitmapWidth==-1)
							{
								SaveImage.showSaveImageAlertDialog("The file is not a picture");
							}
							else
							{
								int reductionRatio=0;
								if(originalBitmapWidth>MAX_BITMAP_SIZE||originalBitmapHeight>MAX_BITMAP_SIZE)
								{
									reductionRatio=1;
									while(originalBitmapWidth>MAX_BITMAP_SIZE||originalBitmapHeight>MAX_BITMAP_SIZE)
									{
										reductionRatio<<=1;
										originalBitmapWidth/=2;
										originalBitmapHeight/=2;
									}
								}
								@NonNull
								final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
								bitmapOptions.inSampleSize=reductionRatio;
								@Nullable
								final Bitmap originalBitmap=BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,bitmapOptions);
								if(originalBitmap!=null)
								{
									//noinspection AnonymousInnerClassMayBeStatic
									((SaveImage)context).runOnUiThread(new Runnable()
									{
										@Override
										public void run()
										{
											try
											{
												imageView.setImageBitmap(originalBitmap);
												imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
												imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
											}
											catch(Exception e)
											{
												SaveImage.showSaveImageAlertDialog("Decoding error");
											}
										}
									});
								}
								else
								{
									SaveImage.showSaveImageAlertDialog("Decoding error");
								}
							}
						}
						catch(Exception e)
						{
							SaveImage.showSaveImageAlertDialog("Connection error");
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
						SaveImage.showSaveImageAlertDialog("Response code: "+response.code());
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					SaveImage.showSaveImageAlertDialog("Unknown error");
				}
			}
		}.start();
	}

	static void downloadOrGetImageFromDisk(@NonNull final String url)
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
					try
					{
						@Nullable
						final Bitmap bitmap=BitmapFactory.decodeFile(String.valueOf(path));
						if(bitmap!=null)
						{
							LruMemoryCache.addBitmapToMemoryCache(url,bitmap);
							URLS_FILE_NAMES.put(url,urlHashMD5);
						}
						else
						{
							MainActivity.ERROR_LIST.put(url,"Decoding error");
							URLS_FILE_NAMES.put(url,"error");
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					URLS_IN_PROGRESS.remove(url);
					ThreadsCounter.decreaseThreadsCounter();
					ImagesAdapter.callNotifyDataSetChanged();
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
					String exception=null;
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
								NO_INTERNET_LINKS.remove(url);
								@NonNull
								boolean isImage=true;
								try
								{
									if(response.body()!=null)
									{
										inputStream=response.body().byteStream();
									}
									@NonNull
									final byte[] buffer=new byte[4096];
									@NonNull
									int bytes;
									if(inputStream!=null)
									{
										while((bytes=inputStream.read(buffer))>0)
										{
											byteArrayOutputStream.write(buffer,0,bytes);
											if(byteArrayOutputStream.toByteArray().length>CONTROL_NUMBER_OF_BYTES)
											{
												try
												{
													@NonNull
													final BitmapFactory.Options options=new BitmapFactory.Options();
													options.inJustDecodeBounds=true;
													BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,options);
													@NonNull
													final int originalBitmapWidth=options.outWidth;
													if(originalBitmapWidth==-1)
													{
														isImage=false;
														break;
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
												exception="Decoding error";
												status="error";
											}
											else
											{
												int factor=0;
												while(originalBitmapWidth>imageWidth)
												{
													originalBitmapWidth/=2;
													factor+=2;
												}
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
													e.printStackTrace();
												}
												if(preview!=null)
												{
													LruMemoryCache.addBitmapToMemoryCache(url,preview);
													URLS_FILE_NAMES.put(url,urlHashMD5);
													@NonNull
													final File previewName=new File(PREVIEWS,urlHashMD5);
													try
													{
														@NonNull
														final FileOutputStream fileOutputStream=new FileOutputStream(previewName);
														preview.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
														fileOutputStream.flush();
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
											exception=String.valueOf(e);
											e.printStackTrace();
										}
									}
									else
									{
										status="error";
										exception="The file is not a picture";
									}
								}
								catch(Exception e)
								{
									status="error";
									exception=String.valueOf(e);
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
								exception="Response code: "+response.code();
							}
						}
						catch(Exception e)
						{
							status="error";
							exception=String.valueOf(e);
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
						if(i==REPEAT_NUM-1&&status!=null&&exception!=null)
						{
							MainActivity.ERROR_LIST.put(url,exception);
							MainActivity.URLS_LINKS_STATUS.put(url,status);
							URLS_FILE_NAMES.put(url,"error");
						}
					}
					URLS_IN_PROGRESS.remove(url);
					ThreadsCounter.decreaseThreadsCounter();
					ImagesAdapter.callNotifyDataSetChanged();
				}
			}.start();
		}
	}

	public static Bitmap getImageBitmap(@NonNull final String url)
	{
		@Nullable
		final Bitmap bitmap=LruMemoryCache.getBitmapFromMemoryCache(url);
		if(bitmap!=null)
		{
			return bitmap;
		}
		if(ThreadsCounter.getThreadCount()<MAX_THREAD_NUM)
		{
			if(URLS_FILE_NAMES.containsKey(url)&&URLS_IN_PROGRESS.contains(url))
			{
				URLS_IN_PROGRESS.remove(url);
				if(LruMemoryCache.memoryCache!=null)
				{
					LruMemoryCache.memoryCache.remove(url);
				}
				ImagesAdapter.callNotifyDataSetChanged();
			}
			if(!URLS_IN_PROGRESS.contains(url))
			{
				try
				{
					URLS_IN_PROGRESS.add(url);
					downloadOrGetImageFromDisk(url);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	static void getImageFromDiskToSaveScreen(@NonNull final String url,@NonNull final ImageView imageView,@NonNull final Context context)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@Nullable
					final String fileName=urlToHashMD5(url);
					if(url.isEmpty())
					{
						SaveImage.showSaveImageAlertDialog("The picture does not exist");
					}
					else
					{
						@NonNull
						final File path=new File(MainActivity.imageBytesDir,fileName);
						@NonNull
						final BitmapFactory.Options options=new BitmapFactory.Options();
						options.inJustDecodeBounds=true;
						BitmapFactory.decodeFile(String.valueOf(path),options);
						@NonNull
						int originalBitmapWidth=options.outWidth;
						@NonNull
						int originalBitmapHeight=options.outHeight;
						if(originalBitmapWidth==-1)
						{
							SaveImage.showSaveImageAlertDialog("The file is not a picture");
						}
						else
						{
							int reductionRatio=0;
							if(originalBitmapWidth>MAX_BITMAP_SIZE||originalBitmapHeight>MAX_BITMAP_SIZE)
							{
								reductionRatio=1;
								while(originalBitmapWidth>MAX_BITMAP_SIZE||originalBitmapHeight>MAX_BITMAP_SIZE)
								{
									reductionRatio<<=1;
									originalBitmapWidth/=2;
									originalBitmapHeight/=2;
								}
							}
							@NonNull
							final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
							bitmapOptions.inSampleSize=reductionRatio;
							@Nullable
							final Bitmap originalBitmap=BitmapFactory.decodeFile(String.valueOf(path),bitmapOptions);
							if(originalBitmap!=null)
							{
								//noinspection AnonymousInnerClassMayBeStatic
								((SaveImage)context).runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										try
										{
											imageView.setImageBitmap(originalBitmap);
											imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
											imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
										}
										catch(Exception e)
										{
											SaveImage.showSaveImageAlertDialog("Decoding error");
										}
									}
								});
							}
							else
							{
								SaveImage.showSaveImageAlertDialog("Decoding error");
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					SaveImage.showSaveImageAlertDialog("Unknown error");
				}
			}
		}.start();
	}

	public static void initStatic(@Nullable final Context context)
	{
		if(context!=null)
		{
			resources=context.getResources();
		}
		if(resources!=null)
		{
			imageWidth=resources.getDimensionPixelSize(R.dimen.imageWidth);
		}
		Log.d("123",String.valueOf(imageWidth));
		try
		{
			int cpuThreadsNum=0;
			@NonNull
			final String systemPath="/sys/devices/system/cpu/";
			@NonNull
			final File systemDir=new File(systemPath);
			@Nullable
			final File[] files=systemDir.listFiles();
			if(files!=null&&files.length!=0)
			{
				for(final File file : files)
				{
					@NonNull
					final String dir=file.toString().substring(systemPath.length());
					if(dir.matches("cpu"+"[0-9]"))
					{
						cpuThreadsNum++;
					}
				}
			}
			MAX_THREAD_NUM=cpuThreadsNum>2?4:2;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String urlToHashMD5(@NonNull final String url)
	{
		try
		{
			@NonNull
			final MessageDigest digest=java.security.MessageDigest.getInstance("MD5");
			digest.update(url.getBytes());
			@NonNull
			final byte[] messageDigest=digest.digest();
			@NonNull
			final StringBuilder hexStringBuilder=new StringBuilder();
			for(final byte messageByte : messageDigest)
			{
				hexStringBuilder.append(Integer.toHexString(0xFF&messageByte));
			}
			return hexStringBuilder.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return "";
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
	}
}
