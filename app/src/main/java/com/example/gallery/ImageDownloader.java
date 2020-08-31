package com.example.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

public final class ImageDownloader
{
	@NonNull
	public static final GalleryHandler GALLERY_HANDLER=new GalleryHandler();
	@NonNull
	public static final HashMap<String,String> FILE_NAMES=new HashMap<>();
	@NonNull
	public static final ArrayList<String> NO_INTERNET_LINKS=new ArrayList<>();
	public static final int MAX_BITMAP_SIZE=4096;
	@NonNull
	static final ArrayList<String> URLS_IN_PROGRESS=new ArrayList<>();
	@Nullable
	static final File PREVIEWS=MainActivity.previews;
	@Nullable
	static final File BYTES=MainActivity.bytes;
	static final int ERROR_TIME_SLEEP=1500;
	static final int CONTROL_NUMBER_OF_BYTES=50000;
	public static int REPEAT_NUM=3;
	static int imageWidth;
	private static int MAX_THREAD_NUM=2;

	private ImageDownloader()
	{
	}

	public static void addBitmapToMemoryCache(@NonNull String key,@NonNull Bitmap bitmap)
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
				e.printStackTrace();
			}
		}
	}

	public static void addStringToLinksfile(@NonNull final String url)
	{
		if(MainActivity.linksFile!=null)
		{
			@NonNull
			final ArrayList<String> LinksFileString=new ArrayList<>();
			try
			{
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
				String string;
				while((string=bufferedReader.readLine())!=null)
				{
					LinksFileString.add(string);
				}
				LinksFileString.add(url);
				bufferedReader.close();
				@NonNull
				final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(MainActivity.linksFile));
				for(final String str : LinksFileString)
				{
					bufferedWriter.write(str+'\n');
				}
				bufferedWriter.flush();
				bufferedReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			callNotifyDataSetChanged();
		}
	}

	public static void callNotifyDataSetChanged()
	{
		@NonNull
		final Message message=ImageDownloader.GALLERY_HANDLER.obtainMessage();
		ImageDownloader.GALLERY_HANDLER.sendMessage(message);
	}

	static void deleteImageFromDisk(@Nullable final String fileName)
	{
		if(fileName!=null)
		{
			try
			{
				@NonNull
				final File fullImage=new File(MainActivity.bytes,fileName);
				if(fullImage.exists())
				{
					//noinspection ResultOfMethodCallIgnored
					fullImage.delete();
				}
				@NonNull
				final File preview=new File(MainActivity.previews,fileName);
				if(preview.exists())
				{
					//noinspection ResultOfMethodCallIgnored
					preview.delete();
				}
				ImageDownloader.FILE_NAMES.remove(fileName);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	static void downloadImageFromSharing(@NonNull final String url,@NonNull final ImageView imageView,@NonNull final Context context)
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
								SharedImage.showSharedImageAlertDialog("The file is not a picture");
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
									((SharedImage)context).runOnUiThread(new Runnable()
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
												SharedImage.showSharedImageAlertDialog("Decoding error");
											}
										}
									});
								}
								else
								{
									SharedImage.showSharedImageAlertDialog("Decoding error");
								}
							}
						}
						catch(Exception e)
						{
							SharedImage.showSharedImageAlertDialog("Connection error");
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
						SharedImage.showSharedImageAlertDialog("Response code: "+response.code());
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					SharedImage.showSharedImageAlertDialog("Unknown error");
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
							addBitmapToMemoryCache(url,bitmap);
							FILE_NAMES.put(url,urlHashMD5);
						}
						else
						{
							MainActivity.ERROR_LIST.put(url,"Decoding error");
							FILE_NAMES.put(url,"error");
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					URLS_IN_PROGRESS.remove(url);
					ThreadsCounter.decreaseThreadsCounter();
					callNotifyDataSetChanged();
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
													addBitmapToMemoryCache(url,preview);
													FILE_NAMES.put(url,urlHashMD5);
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
					callNotifyDataSetChanged();
				}
			}.start();
		}
	}

	public static Bitmap getBitmapFromMemoryCache(@NonNull String key)
	{
		return MainActivity.memoryCache!=null?MainActivity.memoryCache.get(key):null;
	}

	public static Bitmap getImageBitmap(@NonNull final String url)
	{
		@Nullable
		final Bitmap bitmap=getBitmapFromMemoryCache(url);
		if(bitmap!=null)
		{
			return bitmap;
		}
		if(ThreadsCounter.getThreadCount()<MAX_THREAD_NUM)
		{
			if(FILE_NAMES.containsKey(url)&&URLS_IN_PROGRESS.contains(url))
			{
				URLS_IN_PROGRESS.remove(url);
				if(MainActivity.memoryCache!=null)
				{
					MainActivity.memoryCache.remove(url);
				}
				callNotifyDataSetChanged();
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

	static void getImageFromSharing(@NonNull final String url,@NonNull final ImageView imageView,@NonNull final Context context)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@Nullable
					final String fileName=FILE_NAMES.get(url);
					if(fileName!=null)
					{
						@NonNull
						final File path=new File(MainActivity.bytes,fileName);
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
							SharedImage.showSharedImageAlertDialog("The file is not a picture");
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
								((SharedImage)context).runOnUiThread(new Runnable()
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
											SharedImage.showSharedImageAlertDialog("Decoding error");
										}
									}
								});
							}
							else
							{
								SharedImage.showSharedImageAlertDialog("Decoding error");
							}
						}
					}
					else
					{
						SharedImage.showSharedImageAlertDialog("The picture does not exist");
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					SharedImage.showSharedImageAlertDialog("Unknown error");
				}
			}
		}.start();
	}

	public static void initStatic()
	{
		imageWidth=MainActivity.resources.getDimensionPixelSize(R.dimen.imageWidth);
		try
		{
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

	public static void removeStringFromLinksfile(int numToDelete)
	{
		if(MainActivity.linksFile!=null)
		{
			@NonNull
			final ArrayList<String> LinksFileString=new ArrayList<>();
			try
			{
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
				String string;
				while((string=bufferedReader.readLine())!=null)
				{
					if(!string.equals(ImageAdapter.URLS.get(numToDelete)))
					{
						LinksFileString.add(string);
					}
				}
				bufferedReader.close();
				@NonNull
				final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(MainActivity.linksFile));
				for(final String str : LinksFileString)
				{
					bufferedWriter.write(str+'\n');
				}
				bufferedWriter.flush();
				bufferedReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			callNotifyDataSetChanged();
		}
	}

	public static void updateUrlsList()
	{
		ImageAdapter.URLS.clear();
		try
		{
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
			String url;
			while((url=bufferedReader.readLine())!=null)
			{
				ImageAdapter.URLS.add(url);
				MainActivity.LINKS_STATUS.put(url,"progress");
			}
			bufferedReader.close();
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
	}
}
