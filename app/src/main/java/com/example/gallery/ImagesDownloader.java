package com.example.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.muddzdev.styleabletoast.StyleableToast;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ImagesDownloader
{
	@NonNull
	static final ArrayList<String> URLS_IN_PROGRESS=new ArrayList<>();
	@Nullable
	static Context context;
	private static final int CONTROL_NUMBER_OF_BYTES=50000;
	@NonNull
	private static final String IMAGES_URL="https://khlebovsky.ru/images.txt";
	private static final int ERROR_TIME_SLEEP=1500;
	@NonNull
	private static final String SHARED_PREFERENCES_CPU_THREADS_NUM_KEY="CpuThreadsNum";
	private static int maxThreadNum=2;

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
					final OkHttpClient client=ConnectionSettings.getOkHttpClient();
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
							final int originalBitmapWidth=options.outWidth;
							@NonNull
							final int originalBitmapHeight=options.outHeight;
							if(originalBitmapWidth==-1)
							{
								SaveImageActivity.showSaveImageAlertDialog("The file is not a picture");
							}
							else
							{
								final int reductionRatio=getReductionRadio(originalBitmapWidth,originalBitmapHeight);
								@NonNull
								final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
								bitmapOptions.inSampleSize=reductionRatio;
								@Nullable
								final Bitmap originalBitmap=BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length,bitmapOptions);
								if(originalBitmap!=null)
								{
									//noinspection AnonymousInnerClassMayBeStatic
									((SaveImageActivity)context).runOnUiThread(new Runnable()
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
												SaveImageActivity.showSaveImageAlertDialog("Decoding error");
											}
										}
									});
								}
								else
								{
									SaveImageActivity.showSaveImageAlertDialog("Decoding error");
								}
							}
						}
						catch(Exception e)
						{
							SaveImageActivity.showSaveImageAlertDialog("Connection error");
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
						SaveImageActivity.showSaveImageAlertDialog("Response code: "+response.code());
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					SaveImageActivity.showSaveImageAlertDialog("Unknown error");
				}
			}
		}.start();
	}

	static void downloadOrGetImageFromDisk(@NonNull final String url)
	{
		@Nullable
		final Context context=ImagesDownloader.context;
		if(context!=null)
		{
			@NonNull
			final String urlHashMD5=urlToHashMD5(url);
			@NonNull
			final File path=new File(DiskUtils.getImagePreviewsDir(DiskUtils.getCacheDir(context)),urlHashMD5);
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
								Application.URLS_FILE_NAMES.put(url,urlHashMD5);
							}
							else
							{
								Application.URLS_ERROR_LIST.put(url,"Decoding error");
								Application.URLS_FILE_NAMES.put(url,"error");
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
						for(int i=0;i<Application.DOWNLOADING_REPEAT_NUM;i++)
						{
							if(!Application.isInternetAvaliable)
							{
								Application.addUrlInUrlsStatusList(url,Application.NO_INTERNET);
								break;
							}
							try
							{
								@NonNull
								final OkHttpClient client=ConnectionSettings.getOkHttpClient();
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
									if(Application.NO_INTERNET.equals(Application.getUrlStatus(url)))
									{
										Application.removeUrlFromUrlsStatusList(url);
									}
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
												final File pathBytes=new File((DiskUtils.getImagesBytesDir(DiskUtils.getCacheDir(context))),urlHashMD5);
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
													final int imageWidth=context.getResources().getDimensionPixelSize(R.dimen.imageSize);
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
														Application.URLS_FILE_NAMES.put(url,urlHashMD5);
														@NonNull
														final File previewName=new File((DiskUtils.getImagePreviewsDir(DiskUtils.getCacheDir(context))),urlHashMD5);
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
							if(!Application.isInternetAvaliable)
							{
								Application.addUrlInUrlsStatusList(url,Application.NO_INTERNET);
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
							if(!Application.isInternetAvaliable)
							{
								Application.addUrlInUrlsStatusList(url,Application.NO_INTERNET);
								break;
							}
							if(i==Application.DOWNLOADING_REPEAT_NUM-1&&status!=null&&exception!=null)
							{
								Application.URLS_ERROR_LIST.put(url,exception);
								Application.addUrlInUrlsStatusList(url,status);
								Application.URLS_FILE_NAMES.put(url,"error");
							}
						}
						URLS_IN_PROGRESS.remove(url);
						ThreadsCounter.decreaseThreadsCounter();
						ImagesAdapter.callNotifyDataSetChanged();
					}
				}.start();
			}
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
		if(ThreadsCounter.getThreadCount()<maxThreadNum)
		{
			if(Application.URLS_FILE_NAMES.containsKey(url)&&URLS_IN_PROGRESS.contains(url))
			{
				URLS_IN_PROGRESS.remove(url);
				LruMemoryCache.removeBitmapFromMemoryCache(url);
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
						SaveImageActivity.showSaveImageAlertDialog("The picture does not exist");
					}
					else
					{
						@NonNull
						final File path=new File((DiskUtils.getImagesBytesDir(DiskUtils.getCacheDir(context))),fileName);
						@NonNull
						final BitmapFactory.Options options=new BitmapFactory.Options();
						options.inJustDecodeBounds=true;
						BitmapFactory.decodeFile(String.valueOf(path),options);
						@NonNull
						final int originalBitmapWidth=options.outWidth;
						@NonNull
						final int originalBitmapHeight=options.outHeight;
						if(originalBitmapWidth==-1)
						{
							SaveImageActivity.showSaveImageAlertDialog("The file is not a picture");
						}
						else
						{
							final int reductionRatio=getReductionRadio(originalBitmapWidth,originalBitmapHeight);
							@NonNull
							final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
							bitmapOptions.inSampleSize=reductionRatio;
							@Nullable
							final Bitmap originalBitmap=BitmapFactory.decodeFile(String.valueOf(path),bitmapOptions);
							if(originalBitmap!=null)
							{
								//noinspection AnonymousInnerClassMayBeStatic
								((SaveImageActivity)context).runOnUiThread(new Runnable()
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
											SaveImageActivity.showSaveImageAlertDialog("Decoding error");
										}
									}
								});
							}
							else
							{
								SaveImageActivity.showSaveImageAlertDialog("Decoding error");
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					SaveImageActivity.showSaveImageAlertDialog("Unknown error");
				}
			}
		}.start();
	}

	public static int getReductionRadio(int originalBitmapWidth,int originalBitmapHeight)
	{
		int reductionRatio=0;
		if(originalBitmapWidth>Application.MAX_BITMAP_SIZE||originalBitmapHeight>Application.MAX_BITMAP_SIZE)
		{
			reductionRatio=1;
			while(originalBitmapWidth>Application.MAX_BITMAP_SIZE||originalBitmapHeight>Application.MAX_BITMAP_SIZE)
			{
				reductionRatio<<=1;
				originalBitmapWidth/=2;
				originalBitmapHeight/=2;
			}
		}
		return reductionRatio;
	}

	public static void init(@NonNull final Context context)
	{
		ImagesDownloader.context=context;
		try
		{
			int cpuThreadsNum=SharedPreferences.getInt(context,SHARED_PREFERENCES_CPU_THREADS_NUM_KEY,0);
			if(cpuThreadsNum==0)
			{
				@NonNull
				final File systemDir=new File("/sys/devices/system/cpu/");
				@Nullable
				final File[] files=systemDir.listFiles();
				if(files!=null&&files.length!=0)
				{
					for(final File file : files)
					{
						@NonNull
						final String dir=file.getName();
						if(dir.matches("cpu"+"[0-9]"))
						{
							cpuThreadsNum++;
						}
					}
				}
				final int maxThreadNum=cpuThreadsNum>2?4:2;
				ImagesDownloader.maxThreadNum=maxThreadNum;
				SharedPreferences.putInt(context,SHARED_PREFERENCES_CPU_THREADS_NUM_KEY,maxThreadNum);
			}
			else
			{
				maxThreadNum=cpuThreadsNum;
			}
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

	static class CheckUpdatesThread extends Thread
	{
		final boolean showToast;

		CheckUpdatesThread(final boolean showToast)
		{
			this.showToast=showToast;
		}

		@Override
		public void run()
		{
			boolean isError=false;
			try
			{
				@Nullable
				final Context context=ImagesDownloader.context;
				@NonNull
				final URL url=new URL(IMAGES_URL);
				@NonNull
				final ArrayList<String> serverURLS=new ArrayList<>();
				try
				{
					boolean isUpdated=false;
					@NonNull
					final OkHttpClient client=ConnectionSettings.getOkHttpClient();
					@NonNull
					final Call call=client.newCall(new Request.Builder().url(url).get().build());
					@NonNull
					final Response response=call.execute();
					@NonNull
					final BufferedReader bufferedReader=new BufferedReader(response.body()!=null?response.body().charStream():null);
					@Nullable
					String line;
					while((line=bufferedReader.readLine())!=null)
					{
						serverURLS.add(line);
					}
					if(!serverURLS.equals(Application.URLS_LIST))
					{
						isUpdated=true;
					}
					if(!serverURLS.equals(Application.URLS_LIST))
					{
						@NonNull
						final ArrayList<String> urlsToDelete=new ArrayList<>();
						for(final String string : Application.URLS_LIST)
						{
							if(!serverURLS.contains(string))
							{
								urlsToDelete.add(string);
							}
						}
						Application.URLS_LIST.clear();
						for(final String string : serverURLS)
						{
							Application.URLS_LIST.add(string);
						}
						ImagesAdapter.callNotifyDataSetChanged();
						if(!urlsToDelete.isEmpty())
						{
							for(final String urlToDelete : urlsToDelete)
							{
								@Nullable
								final String fileName=Application.URLS_FILE_NAMES.get(urlToDelete);
								if(context!=null)
								{
									DiskUtils.deleteImageFromDisk(fileName,context);
								}
							}
						}
						try
						{
							if(context!=null)
							{
								@NonNull
								final FileWriter fileWriter=new FileWriter((DiskUtils.getLinksFile(DiskUtils.getCacheDir(context))));
								for(final String string : serverURLS)
								{
									fileWriter.write(string+'\n');
								}
								fileWriter.flush();
								fileWriter.close();
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					@Nullable
					final WeakReference<MainActivity> mainActivityWeakReference=Application.mainActivity;
					if(showToast&&mainActivityWeakReference!=null)
					{
						@Nullable
						final MainActivity mainActivity=mainActivityWeakReference.get();
						if(mainActivity!=null)
						{
							@NonNull
							final String updateResult=isUpdated?"Данные обновлены":"Обновлений не обнаружено";
							//noinspection AnonymousInnerClassMayBeStatic
							mainActivity.runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									StyleableToast.makeText(mainActivity,updateResult,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
								}
							});
						}
					}
				}
				catch(Exception e)
				{
					isError=true;
					e.printStackTrace();
				}
			}
			catch(Exception e)
			{
				isError=true;
				e.printStackTrace();
			}
			@Nullable
			final WeakReference<MainActivity> mainActivityWeakReference=Application.mainActivity;
			if(isError&&showToast&&mainActivityWeakReference!=null)
			{
				@Nullable
				final MainActivity mainActivity=mainActivityWeakReference.get();
				if(mainActivity!=null)
				{
					//noinspection AnonymousInnerClassMayBeStatic
					mainActivity.runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							StyleableToast.makeText(mainActivity,"Ошибка обновления данных",Toast.LENGTH_SHORT,R.style.ToastStyle).show();
						}
					});
				}
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
