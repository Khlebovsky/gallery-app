package com.example.gallery;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.*;
import android.util.LruCache;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import com.google.android.gms.security.ProviderInstaller;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.muddzdev.styleabletoast.StyleableToast;
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
	public static final HashMap<String,String> ERROR_LIST=new HashMap<>();
	public static final int ACTION_DELAY_TIME=500;
	@Nullable
	public static File cache;
	@Nullable
	public static File previews;
	@Nullable
	public static File bytes;
	@Nullable
	public static File textfiles;
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
	public static Resources resources;
	static int width;
	static int height;
	static float imageWidth;
	static boolean isThemeChanged;
	static boolean isCheckedUpdates;
	@Nullable
	Parcelable state;
	@Nullable
	Bundle bundleGridViewState;
	@NonNull
	private static final String IMAGESURL="https://khlebovsky.ru/images.txt";
	private static final int NETWORKCALLBACKAPI=24;
	private static final int GOOGLESERVICESAPI=19;
	private static final int NIGHTMODEAPI=17;
	private static NetworkCallback networkCallback;
	private static ConnectivityReceiver connectivityReceiver;

	public void alertNoInternet()
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
		builder.setTitle("Нет интернета");
		builder.setMessage("Нет подключения к интернету. Включите WI-FI или сотовую связь и попробуйте снова.");
		builder.setPositiveButton("Ок",new AlertDialogOnClickListener());
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

	static void checkDisk()
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@NonNull
					final ArrayList<String> diskFiles=new ArrayList<>();
					for(final String string : ImageAdapter.URLS)
					{
						diskFiles.add(ImageDownloader.urlToHashMD5(string));
					}
					if(bytes!=null&&bytes.exists())
					{
						@NonNull
						final String parentDir=bytes+"/";
						@Nullable
						final File[] files=bytes.listFiles();
						if(files!=null&&files.length!=0)
						{
							for(final File file : files)
							{
								@NonNull
								final String fileName=file.toString().substring(parentDir.length());
								if(!diskFiles.contains(fileName))
								{
									ImageDownloader.deleteImageFromDisk(fileName);
								}
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

	void checkUpdates(final boolean showToast)
	{
		if(isConnected)
		{
			@NonNull
			final CheckUpdatesThread checkUpdatesThread=new CheckUpdatesThread(showToast);
			checkUpdatesThread.start();
		}
		else
		{
			alertNoInternet();
		}
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

	static int getQuantityItemsOnScreen()
	{
		final int numInWidth=(int)(width/imageWidth);
		final int numInHeight=(int)(height/imageWidth);
		int quantity=numInWidth*numInHeight;
		quantity+=resources.getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT?numInWidth<<1:numInWidth;
		return quantity;
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
		textfiles=new File(cache,"textfiles");
		linksFile=new File(textfiles,"links.txt");
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
			memoryCache=new LruCache<String,Bitmap>(getQuantityItemsOnScreen())
			{
				@Override
				protected int sizeOf(String key,Bitmap value)
				{
					return 1;
				}
			};
		}
	}

	static void initNetworkStatusListener()
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
		resources=getResources();
		setTitle(resources.getString(R.string.app_name));
		context=getBaseContext();
		imageWidth=resources.getDimensionPixelSize(R.dimen.imageWidth);
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
		if(!isThemeChanged&&sharedPreferences!=null&&Build.VERSION.SDK_INT >= NIGHTMODEAPI)
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
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			if(linksFile!=null&&!linksFile.exists())
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
				if(!linksfile.exists())
				{
					//noinspection ResultOfMethodCallIgnored
					linksfile.createNewFile();
				}
			}
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
		resizeMemoryCache();
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
		final int num=(int)(width/imageWidth);
		gridView.setColumnWidth((int)imageWidth);
		gridView.setNumColumns(num);
		if(state!=null)
		{
			gridView.onRestoreInstanceState(state);
		}
		if(gridView.getAdapter()!=null)
		{
			ImageDownloader.callNotifyDataSetChanged();
		}
	}

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
		makeDirs();
		getErrorLinksFromSharedPrefs();
		initMemoryCache();
		initGooglePlayServices();
		ImageDownloader.initStatic();
		if(isConnected)
		{
			if(isCheckedUpdates)
			{
				updateAdapter();
			}
			else
			{
				updateAdapter();
				checkUpdates(false);
				isCheckedUpdates=true;
			}
		}
		else
		{
			alertNoInternet();
			updateAdapter();
		}
		if(gridView!=null)
		{
			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(final AdapterView<?> parent,View v,int position,long id)
				{
					if(LINKS_STATUS.containsKey(ImageAdapter.URLS.get(position)))
					{
						@NonNull
						final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
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
						builder.setPositiveButton("Ок",new AlertDialogOnClickListener());
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
							final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
							builder.setTitle("Ошибка");
							builder.setMessage("Ошибка: \nПроизошла непредвиденная ошибка");
							builder.setPositiveButton("Ок",new AlertDialogOnClickListener());
							builder.show();
							e.printStackTrace();
						}
					}
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
		if(Build.VERSION.SDK_INT >= NIGHTMODEAPI)
		{
			final int nightMode=AppCompatDelegate.getDefaultNightMode();
			if(nightMode==AppCompatDelegate.MODE_NIGHT_YES)
			{
				menu.findItem(R.id.changeTheme).setTitle(R.string.light_theme);
			}
			else
			{
				menu.findItem(R.id.changeTheme).setTitle(R.string.dark_theme);
			}
		}
		return true;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		try
		{
			FullImage.hideDefaultNotification();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		saveErrorList();
		saveCurrentTheme();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if(Build.VERSION.SDK_INT >= NIGHTMODEAPI)
		{
			switch(item.getItemId())
			{
				case R.id.update:
					checkUpdates(true);
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
					},ACTION_DELAY_TIME);
					return true;
			}
		}
		else
		{
			switch(item.getItemId())
			{
				case R.id.update:
					checkUpdates(true);
					return true;
				case R.id.reloadErrorLinks:
					relaodErrorLinks();
					return true;
			}
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
		ImageDownloader.callNotifyDataSetChanged();
	}

	static void resizeMemoryCache()
	{
		if(memoryCache!=null)
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				memoryCache.resize(getQuantityItemsOnScreen());
			}
		}
		else
		{
			initMemoryCache();
		}
	}

	public static void saveCurrentTheme()
	{
		if(sharedPreferences!=null&&Build.VERSION.SDK_INT >= NIGHTMODEAPI)
		{
			final int nightMode=AppCompatDelegate.getDefaultNightMode();
			if(nightMode==AppCompatDelegate.MODE_NIGHT_YES)
			{
				sharedPreferences.edit().putBoolean("isNightMode",true).commit();
			}
			else if(nightMode==AppCompatDelegate.MODE_NIGHT_NO)
			{
				sharedPreferences.edit().putBoolean("isNightMode",false).commit();
			}
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

	public void updateAdapter()
	{
		//noinspection AnonymousInnerClassMayBeStatic
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if(gridView!=null)
				{
					if(gridView.getAdapter()==null)
					{
						gridView.setAdapter(null);
						imageAdapter=new ImageAdapter();
						gridView.setAdapter(imageAdapter);
						checkDisk();
					}
					else
					{
						ImageDownloader.callNotifyDataSetChanged();
					}
				}
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
					ImageDownloader.callNotifyDataSetChanged();
					break;
			}
		}
	}

	private class CheckUpdatesThread extends Thread
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
				@NonNull
				final URL url=new URL(IMAGESURL);
				@NonNull
				final ArrayList<String> serverURLS=new ArrayList<>();
				try
				{
					boolean isUpdated=false;
					@NonNull
					final OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(ConnectionSettings.getTLSSocketFactory(),ConnectionSettings.getTrustManager()[0]).build();
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
					if(!serverURLS.equals(ImageAdapter.URLS))
					{
						isUpdated=true;
					}
					if(!serverURLS.equals(ImageAdapter.URLS))
					{
						@NonNull
						final ArrayList<String> urlsToDelete=new ArrayList<>();
						for(final String string : ImageAdapter.URLS)
						{
							if(!serverURLS.contains(string))
							{
								urlsToDelete.add(string);
							}
						}
						ImageAdapter.URLS.clear();
						for(final String string : serverURLS)
						{
							ImageAdapter.URLS.add(string);
						}
						ImageDownloader.callNotifyDataSetChanged();
						if(!urlsToDelete.isEmpty())
						{
							for(final String urlToDelete : urlsToDelete)
							{
								@Nullable
								final String fileName=ImageDownloader.FILE_NAMES.get(urlToDelete);
								ImageDownloader.deleteImageFromDisk(fileName);
							}
						}
						try
						{
							@NonNull
							final FileWriter fileWriter=new FileWriter(linksFile);
							for(final String string : serverURLS)
							{
								fileWriter.write(string+'\n');
							}
							fileWriter.flush();
							fileWriter.close();
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					if(showToast)
					{
						@NonNull
						final String updateResult=isUpdated?"Данные обновлены":"Обновлений не обнаружено";
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								StyleableToast.makeText(context,updateResult,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
							}
						});
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
			if(isError&&showToast)
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						StyleableToast.makeText(context,"Ошибка обновления данных",Toast.LENGTH_SHORT,R.style.ToastStyle).show();
					}
				});
			}
		}
	}

	@RequiresApi(api=Build.VERSION_CODES.LOLLIPOP)
	static class NetworkCallback extends ConnectivityManager.NetworkCallback
	{
		@Override
		public void onAvailable(@NonNull Network network)
		{
			super.onAvailable(network);
			isConnected=true;
			ImageDownloader.NO_INTERNET_LINKS.clear();
			ImageDownloader.callNotifyDataSetChanged();
		}

		@Override
		public void onLost(@NonNull Network network)
		{
			super.onLost(network);
			isConnected=false;
		}
	}
}
