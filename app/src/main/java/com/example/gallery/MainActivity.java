package com.example.gallery;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
	@NonNull
	public static final HashMap<String,String> LINKS_STATUS=new HashMap<>();
	@NonNull
	public static final ArrayList<String> URLS=new ArrayList<>();
	@NonNull
	public static final HashMap<String,String> ERROR_LIST=new HashMap<>();
	@Nullable
	public static File cache;
	@Nullable
	public static File previews;
	@Nullable
	public static File bytes;
	public static int quantity;
	@Nullable
	public static LruCache<String,Bitmap> memoryCache;
	@Nullable
	public static ImageAdapter imageAdapter;
	@Nullable
	public static GridView gridView;
	public static ConnectivityManager connectivityManager;
	public static volatile boolean isConnected;
	@Nullable
	public static SharedPreferences sharedPreferences;
	static int width;
	static int height;
	static float imageWidth;
	static boolean isThemeChanged;
	@Nullable
	Parcelable state;
	@Nullable
	Bundle bundleGridViewState;
	@NonNull
	private static final String TAG="LinksParserThread";
	@NonNull
	private static final String MAIN="MainThread";
	@NonNull
	private static final String IMAGESURL="https://khlebovsky.ru/images.txt";
	private static final int NETWORKCALLBACKAPI=24;
	private static final int ALERTDIALOGSTYLEAPI=21;
	private static NetworkCallback networkCallback;
	private static ConnectivityReceiver connectivityReceiver;

	public void alertNoInternet()
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogStyle);
		builder.setTitle("Нет интернета");
		builder.setMessage("Нет подключения к интернету. Включите WI-FI или сотовую связь и попробуйте снова.");
		builder.setPositiveButton("OK",new AlertDialogOnClickListener());
		builder.show();
	}

	public void changeTheme()
	{
		final int nightMode=AppCompatDelegate.getDefaultNightMode();
		if(nightMode==AppCompatDelegate.MODE_NIGHT_YES)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		}
		else
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
		}
		recreate();
	}

	public static void clearData()
	{
		ERROR_LIST.clear();
		ImageDownloader.FILE_NAMES.clear();
		LINKS_STATUS.clear();
		URLS.clear();
		ImageAdapter.URLS.clear();
	}

	public static int getMemorySize()
	{
		@NonNull
		final int width=MainActivity.width;
		@NonNull
		final int height=MainActivity.height;
		@NonNull
		final int pixelsPerByte=4;
		@NonNull
		final float imageSize=imageWidth*imageWidth*pixelsPerByte;
		quantity=(int)(((width/imageWidth)*(height/imageWidth))+((width/imageWidth)*2));
		//noinspection unused
		@NonNull
		final int maxsize=(int)(imageSize*quantity);
		return quantity;
	}

	public boolean isInternet()
	{
		@NonNull
		final ConnectivityManager connectivityManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		@Nullable
		final NetworkInfo wifiInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		@NonNull
		final boolean wifiConnected=(wifiInfo!=null?wifiInfo.getState():null)==NetworkInfo.State.CONNECTED;
		@Nullable
		final NetworkInfo mobileInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		@NonNull
		final boolean mobileConnected=(mobileInfo!=null?mobileInfo.getState():null)==NetworkInfo.State.CONNECTED;
		return wifiConnected||mobileConnected;
	}

	public static void makeDirs()
	{
		try
		{
			if(cache!=null&&!cache.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				cache.mkdirs();
			}
			if(previews!=null&&!previews.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				previews.mkdirs();
			}
			if(bytes!=null&&!bytes.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				bytes.mkdirs();
			}
			if(previews!=null&&previews.exists())
			{
				try
				{
					@Nullable
					final File[] files=previews.listFiles();
					if(files!=null&&files.length!=0)
					{
						for(final File file : files)
						{
							//noinspection ResultOfMethodCallIgnored
							file.delete();
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			if(bytes!=null&&bytes.exists())
			{
				try
				{
					@Nullable
					final File[] files=bytes.listFiles();
					if(files!=null&&files.length!=0)
					{
						for(final File file : files)
						{
							//noinspection ResultOfMethodCallIgnored
							file.delete();
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			try
			{
				@Nullable
				File[] files=null;
				if(cache!=null)
				{
					files=cache.listFiles();
				}
				if(files!=null)
				{
					for(final File file : files)
					{
						//noinspection ResultOfMethodCallIgnored
						file.delete();
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			@NonNull
			final File textfilesdir=new File(cache+File.separator+"textfiles");
			if(!textfilesdir.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				textfilesdir.mkdirs();
			}
			@NonNull
			final File linksfile=new File(textfilesdir,"links.txt");
			if(linksfile.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				linksfile.delete();
			}
			//noinspection ResultOfMethodCallIgnored
			linksfile.createNewFile();
		}
		catch(Exception e)
		{
			Log.d(MAIN,String.valueOf(e));
			e.printStackTrace();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		bundleGridViewState=new Bundle();
		state=gridView!=null?gridView.onSaveInstanceState():null;
		bundleGridViewState.putParcelable("GridViewState",state);
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		width=size.x;
		height=size.y;
		@NonNull
		final GridView gridView=findViewById(R.id.GridView);
		@NonNull
		final int num=(int)(width/imageWidth);
		gridView.setColumnWidth((int)imageWidth);
		gridView.setNumColumns(num);
		if(state!=null)
		{
			gridView.onRestoreInstanceState(state);
		}
		if(gridView.getAdapter()!=null)
		{
			if(imageAdapter!=null)
			{
				imageAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		sharedPreferences=getSharedPreferences("Gallery",MODE_PRIVATE);
		if(!isThemeChanged)
		{
			try
			{
				@NonNull
				final boolean isNightMode=sharedPreferences.getBoolean("isNightMode",false);
				if(isNightMode)
				{
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				}
				else
				{
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			isThemeChanged=true;
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		if(Build.VERSION.SDK_INT >= NETWORKCALLBACKAPI)
		{
			networkCallback=new NetworkCallback();
		}
		else
		{
			connectivityReceiver=new ConnectivityReceiver();
		}
		isConnected=isInternet();
		imageWidth=getResources().getDimensionPixelSize(R.dimen.imageWidth);
		gridView=findViewById(R.id.GridView);
		try
		{
			if(Environment.isExternalStorageEmulated()||!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||(cache=getBaseContext().getExternalCacheDir())==null||!cache.canWrite())
			{
				cache=getBaseContext().getCacheDir();
			}
		}
		catch(Throwable e)
		{
			cache=getBaseContext().getCacheDir();
		}
		previews=new File(cache,"previews");
		bytes=new File(cache,"bytes");
		try
		{
			@Nullable
			final String string=sharedPreferences!=null?sharedPreferences.getString("LinksStatusJson",null):null;
			//noinspection AnonymousInnerClassMayBeStatic
			@NonNull
			final Type type=new TypeToken<HashMap<String,String>>()
			{
			}.getType();
			final Gson gson=new Gson();
			@Nullable
			final HashMap<String,String> TEMP=gson.fromJson(string,type);
			if(TEMP!=null&&!TEMP.isEmpty())
			{
				//noinspection rawtypes
				for(final Map.Entry entry : TEMP.entrySet())
				{
					ERROR_LIST.put(entry.getKey().toString(),entry.getValue().toString());
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Log.d(MAIN,String.valueOf(e));
		}
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		width=size.x;
		height=size.y;
		@NonNull
		final int num=(int)(width/imageWidth);
		if(memoryCache==null)
		{
			//noinspection AnonymousInnerClassMayBeStatic
			memoryCache=new LruCache<String,Bitmap>(getMemorySize())
			{
				@Override
				protected int sizeOf(String key,Bitmap value)
				{
					return 1;
				}
			};
		}
		if(gridView!=null)
		{
			gridView.setColumnWidth((int)imageWidth);
			gridView.setNumColumns(num);
		}
		try
		{
			ProviderInstaller.installIfNeeded(getBaseContext());
		}
		catch(GooglePlayServicesRepairableException e)
		{
			e.printStackTrace();
		}
		catch(GooglePlayServicesNotAvailableException e)
		{
			e.printStackTrace();
		}
		ImageDownloader.initStatic(getBaseContext());
		if(isConnected)
		{
			try
			{
				@Nullable
				final File[] files=previews.listFiles();
				if(files==null||files.length==0)
				{
					startLinksParser();
				}
				else
				{
					updateGallery();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				startLinksParser();
			}
		}
		else
		{
			alertNoInternet();
			updateGallery();
		}
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(final AdapterView<?> parent,View v,int position,long id)
			{
				parent.setClickable(false);
				parent.setEnabled(false);
				if(LINKS_STATUS.containsKey(ImageAdapter.URLS.get(position)))
				{
					@NonNull
					final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogStyle);
					if(ImageDownloader.NO_INTERNET_LINKS.contains(ImageAdapter.URLS.get(position)))
					{
						builder.setTitle("Ошибка");
						builder.setMessage("Ошибка загрузки: \nНет подключения к интернету");
					}
					else if("progress".equals(LINKS_STATUS.get(ImageAdapter.URLS.get(position))))
					{
						builder.setTitle("Загрузка");
						builder.setMessage("Картинка загружается: \nПопробуйте позже");
					}
					else
					{
						@Nullable
						final String error=(ERROR_LIST.get(ImageAdapter.URLS.get(position)));
						builder.setTitle("Ошибка");
						builder.setMessage("Ошибка загрузки: \n"+error);
					}
					builder.setPositiveButton("OK",new AlertDialogOnClickListener());
					builder.show();
				}
				else
				{
					final Intent intent=new Intent(getApplicationContext(),FullImage.class);
					intent.putExtra("image",ImageDownloader.FILE_NAMES.get(ImageAdapter.URLS.get(position)));
					intent.setPackage(getPackageName());
					try
					{
						startActivity(intent);
					}
					catch(Throwable e)
					{
						@NonNull
						final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogStyle);
						builder.setTitle("Ошибка");
						builder.setMessage("Ошибка: \nПроизошла непредвиденная ошибка");
						builder.setPositiveButton("OK",new AlertDialogOnClickListener());
						builder.show();
						e.printStackTrace();
					}
				}
				//noinspection AnonymousInnerClassMayBeStatic
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						parent.setEnabled(true);
						parent.setClickable(true);
					}
				},200);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		@NonNull
		final MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.main,menu);
		final int nightMode=AppCompatDelegate.getDefaultNightMode();
		if(nightMode==AppCompatDelegate.MODE_NIGHT_YES)
		{
			menu.findItem(R.id.changeTheme).setTitle(R.string.lightTheme);
		}
		else
		{
			menu.findItem(R.id.changeTheme).setTitle(R.string.darkTheme);
		}
		return true;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		saveERRORLIST();
		saveCurrentTheme();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.update:
				startLinksParser();
				return true;
			case R.id.reloadErrorLinks:
				relaodErrorLinks();
				return true;
			case R.id.changeTheme:
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						changeTheme();
					}
				},500);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregiesterNetworkStateChecker(connectivityManager);
		bundleGridViewState=new Bundle();
		if(gridView!=null)
		{
			state=gridView.onSaveInstanceState();
		}
		bundleGridViewState.putParcelable("GridViewState",state);
		saveERRORLIST();
		saveCurrentTheme();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		regiesterNetworkStateChecker(connectivityManager);
		if(bundleGridViewState!=null)
		{
			state=bundleGridViewState.getParcelable("GridViewState");
			if(gridView!=null)
			{
				gridView.onRestoreInstanceState(state);
			}
		}
	}

	private void regiesterNetworkStateChecker(ConnectivityManager connectivityManager)
	{
		try
		{
			{
				if(Build.VERSION.SDK_INT >= NETWORKCALLBACKAPI)
				{
					connectivityManager.registerDefaultNetworkCallback(networkCallback);
				}
				else
				{
					registerReceiver(connectivityReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			isConnected=true;
		}
	}

	public static void relaodErrorLinks()
	{
		ImageDownloader.REPEAT_NUM=1;
		ERROR_LIST.clear();
		if(imageAdapter!=null)
		{
			imageAdapter.notifyDataSetChanged();
		}
	}

	public static void saveCurrentTheme()
	{
		final int nightMode=AppCompatDelegate.getDefaultNightMode();
		if(sharedPreferences!=null&&nightMode==AppCompatDelegate.MODE_NIGHT_YES)
		{
			sharedPreferences.edit().putBoolean("isNightMode",true).commit();
		}
		else if(sharedPreferences!=null&&nightMode==AppCompatDelegate.MODE_NIGHT_NO)
		{
			sharedPreferences.edit().putBoolean("isNightMode",false).commit();
		}
	}

	public static void saveERRORLIST()
	{
		try
		{
			final Gson gson=new Gson();
			@NonNull
			final String hashMapString=gson.toJson(ERROR_LIST);
			if(sharedPreferences!=null)
			{
				sharedPreferences.edit().putString("LinksStatusJson",hashMapString).commit();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Log.d(MAIN,String.valueOf(e));
		}
	}

	// TODO возможность поделиться картинкой - окно добавления (фулл картинки, сохранить + отмена) список + сервер
	// TODO возможность удалить картинку список + сервер
	// TODO написать скрипт для редактирования текстовика на сервере
	// TODO доработать визуалку
	// TODO возможность перезагрузить картинку при ошибке декодирования во время открытия
	// TODO возможность удалить картинку с ошибкой из диалога
	public void startLinksParser()
	{
		if(isConnected)
		{
			ImageDownloader.REPEAT_NUM=3;
			clearData();
			@NonNull
			final ProgressDialog progressDialog;
			progressDialog=Build.VERSION.SDK_INT >= ALERTDIALOGSTYLEAPI?new ProgressDialog(MainActivity.this,R.style.AlertDialogStyle):new ProgressDialog(MainActivity.this);
			progressDialog.setMessage("Обновление данных. Подождите...");
			progressDialog.setCancelable(false);
			progressDialog.show();
			@NonNull
			final LinksParser linksParser=new LinksParser(progressDialog);
			//noinspection AnonymousInnerClassMayBeStatic
			new Thread()
			{
				@Override
				public void run()
				{
					makeDirs();
					ImageDownloader.ThreadsCounter.resetThreadsCoutner();
					if(memoryCache!=null)
					{
						memoryCache.evictAll();
					}
					linksParser.start();
				}
			}.start();
		}
		else
		{
			alertNoInternet();
		}
	}

	private void unregiesterNetworkStateChecker(ConnectivityManager connectivityManager)
	{
		try
		{
			if(Build.VERSION.SDK_INT >= NETWORKCALLBACKAPI)
			{
				connectivityManager.unregisterNetworkCallback(networkCallback);
			}
			else
			{
				unregisterReceiver(connectivityReceiver);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void updateGallery()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				@NonNull
				final GridView gridView=findViewById(R.id.GridView);
				gridView.setAdapter(null);
				imageAdapter=new ImageAdapter(getBaseContext());
				gridView.setAdapter(imageAdapter);
			}
		});
	}

	static class AlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			dialog.dismiss();
		}
	}

	class LinksParser extends Thread
	{
		ProgressDialog progressDialog;

		LinksParser(ProgressDialog dialog)
		{
			progressDialog=dialog;
		}

		@Override
		public void run()
		{
			try
			{
				@NonNull
				final File textfilesdir=new File(cache,"textfiles");
				@NonNull
				final File linksfile=new File(textfilesdir,"links.txt");
				@NonNull
				final FileWriter downloader=new FileWriter(linksfile);
				@NonNull
				final URL url=new URL(IMAGESURL);
				try
				{
					@NonNull
					final OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(ConnectionSettings.getTLSSocketFactory(),ConnectionSettings.getTrustManager()[0]).build();
					@NonNull
					final Call call=client.newCall(new Request.Builder().url(url).get().build());
					@NonNull
					final Response response=call.execute();
					String line;
					@NonNull
					final BufferedReader in=new BufferedReader(response.body()!=null?response.body().charStream():null);
					while((line=in.readLine())!=null)
					{
						downloader.append(line).append("\n");
						URLS.add(line);
					}
				}
				catch(Exception e)
				{
					Log.d(TAG,String.valueOf(e));
				}
				downloader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			progressDialog.dismiss();
			updateGallery();
		}
	}

	@RequiresApi(api=Build.VERSION_CODES.LOLLIPOP)
	class NetworkCallback extends ConnectivityManager.NetworkCallback
	{
		@Override
		public void onAvailable(@NonNull Network network)
		{
			super.onAvailable(network);
			isConnected=true;
			ImageDownloader.NO_INTERNET_LINKS.clear();
			if(MainActivity.imageAdapter!=null)
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						imageAdapter.notifyDataSetChanged();
					}
				});
			}
		}

		@Override
		public void onLost(@NonNull Network network)
		{
			super.onLost(network);
			isConnected=false;
		}
	}
}
