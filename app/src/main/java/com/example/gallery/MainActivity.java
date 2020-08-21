package com.example.gallery;

import android.app.Dialog;
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
import android.util.LruCache;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
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
	public static Context context;
	@Nullable
	public static File linksFile;
	static int width;
	static int height;
	static float imageWidth;
	static boolean isThemeChanged;
	@Nullable
	Parcelable state;
	@Nullable
	Bundle bundleGridViewState;
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="LinksParserThread";
	@SuppressWarnings("unused")
	@NonNull
	private static final String MAIN="MainThread";
	@NonNull
	private static final String IMAGESURL="https://khlebovsky.ru/images.txt";
	private static final int NETWORKCALLBACKAPI=24;
	private static final int ALERTDIALOGSTYLEAPI=21;
	private static final int GOOGLESERVICESAPI=19;
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

	static void getErrorLinksFromSharedPrefs()
	{
		try
		{
			@Nullable
			final String string=sharedPreferences!=null?sharedPreferences.getString("LinksStatusJson",null):null;
			if(string!=null)
			{
				@NonNull
				final Type type=new TypeToken<HashMap<String,String>>()
				{
				}.getType();
				@NonNull
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
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void initCacheDirs(@NonNull Context context)
	{
		try
		{
			if(Environment.isExternalStorageEmulated()||!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||(cache=context.getExternalCacheDir())==null||!cache.canWrite())
			{
				cache=context.getCacheDir();
			}
		}
		catch(Throwable e)
		{
			cache=context.getCacheDir();
		}
		previews=new File(cache,"previews");
		bytes=new File(cache,"bytes");
		@NonNull
		final File textfilesdir=new File(cache,"textfiles");
		linksFile=new File(textfilesdir,"links.txt");
	}

	void initGooglePlayServices()
	{
		if(Build.VERSION.SDK_INT<=GOOGLESERVICESAPI)
		{
			try
			{
				ProviderInstaller.installIfNeeded(getBaseContext());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	static void initMemoryCache()
	{
		if(memoryCache==null)
		{
			@NonNull
			final int width=MainActivity.width;
			@NonNull
			final int height=MainActivity.height;
			quantity=(int)(((width/imageWidth)*(height/imageWidth))+((width/imageWidth)*4));
			memoryCache=new LruCache<String,Bitmap>(quantity)
			{
				@Override
				protected int sizeOf(String key,Bitmap value)
				{
					return 1;
				}
			};
		}
	}

	void initNetworkStatusListener()
	{
		if(Build.VERSION.SDK_INT >= NETWORKCALLBACKAPI)
		{
			networkCallback=new NetworkCallback();
		}
		else
		{
			connectivityReceiver=new ConnectivityReceiver();
		}
	}

	void initSharedPrefs()
	{
		if(sharedPreferences==null)
		{
			sharedPreferences=getSharedPreferences("Gallery",MODE_PRIVATE);
		}
	}

	void initStatic()
	{
		setTitle(getResources().getString(R.string.app_name));
		context=getBaseContext();
		imageWidth=getResources().getDimensionPixelSize(R.dimen.imageWidth);
		gridView=findViewById(R.id.GridView);
		connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		isConnected=isInternet();
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		width=size.x;
		height=size.y;
		@NonNull
		final int num=(int)(width/imageWidth);
		if(gridView!=null)
		{
			gridView.setColumnWidth((int)imageWidth);
			gridView.setNumColumns(num);
		}
	}

	static void initTheme()
	{
		if(!isThemeChanged&&sharedPreferences!=null)
		{
			try
			{
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
	}

	public static boolean isInternet()
	{
		try
		{
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
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;
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
			final File textfilesdir=new File(cache,"textfiles");
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

	// TODO исправить проблему с залипанием картинок на загрузке (!)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		initSharedPrefs();
		initTheme();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initStatic();
		initNetworkStatusListener();
		initCacheDirs(context);
		getErrorLinksFromSharedPrefs();
		initMemoryCache();
		initGooglePlayServices();
		ImageDownloader.initStatic(getBaseContext());
		if(isConnected)
		{
			try
			{
				if(previews!=null)
				{
					@Nullable
					final File[] files=previews.listFiles();
					if(files!=null&&files.length!=0)
					{
						updateGallery();
					}
					else
					{
						startLinksParser();
					}
				}
				else
				{
					startLinksParser();
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
		if(gridView!=null)
		{
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
							builder.setNegativeButton("Удалить",new AlertDialogOnClickListener(position));
							builder.setNeutralButton("Перезагрузить",new AlertDialogOnClickListener(position));
						}
						builder.setPositiveButton("OK",new AlertDialogOnClickListener());
						builder.show();
					}
					else
					{
						@NonNull
						final Intent intent=new Intent(MainActivity.this,FullImage.class);
						intent.putExtra("URL",ImageAdapter.URLS.get(position));
						intent.putExtra("Num",position);
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		@NonNull
		final MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.main_menu,menu);
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
		saveErrorList();
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
		saveErrorList();
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

	private void regiesterNetworkStateChecker(@NonNull ConnectivityManager connectivityManager)
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

	public static void saveErrorList()
	{
		try
		{
			@NonNull
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
		}
	}

	public void startLinksParser()
	{
		if(isConnected)
		{
			@NonNull
			final ProgressDialog progressDialog=Build.VERSION.SDK_INT >= ALERTDIALOGSTYLEAPI?new ProgressDialog(MainActivity.this,R.style.AlertDialogStyle):new ProgressDialog(MainActivity.this);
			progressDialog.setMessage("Обновление данных. Подождите...");
			progressDialog.setCancelable(false);
			progressDialog.show();
			clearData();
			makeDirs();
			ImageDownloader.ThreadsCounter.resetThreadsCoutner();
			ImageDownloader.REPEAT_NUM=3;
			@NonNull
			final LinksParser linksParser=new LinksParser(progressDialog);
			//noinspection AnonymousInnerClassMayBeStatic
			new Thread()
			{
				@Override
				public void run()
				{
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

	private void unregiesterNetworkStateChecker(@NonNull ConnectivityManager connectivityManager)
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
				gridView=findViewById(R.id.GridView);
				gridView.setAdapter(null);
				imageAdapter=new ImageAdapter(getBaseContext());
				gridView.setAdapter(imageAdapter);
			}
		});
	}

	static class AlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		int position;

		AlertDialogOnClickListener(int position)
		{
			this.position=position;
		}

		AlertDialogOnClickListener()
		{
		}

		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			switch(which)
			{
				case Dialog.BUTTON_POSITIVE:
					dialog.dismiss();
					break;
				case Dialog.BUTTON_NEGATIVE:
					ClientServer.deleteImage(position);
					break;
				case DialogInterface.BUTTON_NEUTRAL:
					ERROR_LIST.remove(ImageAdapter.URLS.get(position));
					if(imageAdapter!=null)
					{
						imageAdapter.notifyDataSetChanged();
					}
					break;
			}
		}
	}

	class LinksParser extends Thread
	{
		@NonNull
		final ProgressDialog progressDialog;

		LinksParser(@NonNull ProgressDialog dialog)
		{
			progressDialog=dialog;
		}

		@Override
		public void run()
		{
			try
			{
				@NonNull
				final FileWriter downloader=new FileWriter(linksFile);
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
					@NonNull
					final BufferedReader bufferedReader=new BufferedReader(response.body()!=null?response.body().charStream():null);
					String line;
					while((line=bufferedReader.readLine())!=null)
					{
						downloader.append(line).append("\n");
						URLS.add(line);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				downloader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			updateGallery();
			progressDialog.dismiss();
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
