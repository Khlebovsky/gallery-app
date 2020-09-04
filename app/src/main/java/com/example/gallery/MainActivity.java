package com.example.gallery;

import android.app.Dialog;
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
import android.net.ConnectivityManager.NetworkCallback;
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
	public static final HashMap<String,String> URLS_LINKS_STATUS=new HashMap<>();
	@NonNull
	public static final HashMap<String,String> ERROR_LIST=new HashMap<>();
	public static final int ACTION_DELAY_TIME=500;
	@Nullable
	public static File cacheDir;
	@Nullable
	public static File imagePreviewsDir;
	@Nullable
	public static File imageBytesDir;
	@Nullable
	public static File textfilesDir;
	@Nullable
	public static ImagesAdapter imagesAdapter;
	@Nullable
	public static GridView gridView;
	@Nullable
	public static ConnectivityManager connectivityManager;
	public static boolean isConnected;
	@Nullable
	public static SharedPreferences sharedPreferences;
	@Nullable
	public static File linksFile;
	static int width;
	static int height;
	static float imageWidth;
	static boolean isThemeChanged;
	static boolean isCheckedUpdates;
	@Nullable
	static Context context;
	@Nullable
	Parcelable state;
	@Nullable
	Bundle bundleGridViewState;
	@NonNull
	private static final String IMAGES_URL="https://khlebovsky.ru/images.txt";
	private static final int NETWORK_CALLBACK_API=24;
	private static final int GOOGLE_SERVICES_API=19;
	private static final int NIGHT_MODE_API=17;
	@Nullable
	private static Resources resources;
	@Nullable
	private static NetworkCallback networkCallback;
	@Nullable
	private static ConnectivityReceiver connectivityReceiver;

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
			showNoInternetAlertDialog();
		}
	}

	static void getErrorListFromSharedPrefs()
	{
		try
		{
			@Nullable
			final SharedPreferences sharedPrefs=sharedPreferences;
			@Nullable
			final String string=sharedPrefs!=null?sharedPrefs.getString("LinksStatusJson",null):null;
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
		if(resources!=null)
		{
			quantity+=resources.getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT?numInWidth<<1:numInWidth;
		}
		else
		{
			quantity+=numInWidth<<1;
		}
		return quantity;
	}

	public static void initCacheDirs(@Nullable final Context context)
	{
		if(context!=null)
		{
			try
			{
				if(Environment.isExternalStorageEmulated()||!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||(cacheDir=context.getExternalCacheDir())==null||!cacheDir.canWrite())
				{
					cacheDir=context.getCacheDir();
				}
			}
			catch(Throwable e)
			{
				cacheDir=context.getCacheDir();
			}
			@Nullable
			final File cacheDir_=cacheDir;
			imagePreviewsDir=new File(cacheDir_,"previews");
			imageBytesDir=new File(cacheDir_,"bytes");
			textfilesDir=new File(cacheDir_,"textfiles");
			linksFile=new File(textfilesDir,"links.txt");
		}
	}

	static void initClasses()
	{
		@Nullable
		final Context context_=context;
		ImagesAdapter.initResources(context_);
		ImagesDownloader.initStatic(context_);
		ClientServer.initContext(context_);
		ImageCustomView.initStatic(context_);
		LruMemoryCache.initMemoryCache(getQuantityItemsOnScreen());
	}

	void initGooglePlayServices()
	{
		if(Build.VERSION.SDK_INT<=GOOGLE_SERVICES_API)
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

	static void initNetworkStatusListener()
	{
		if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API)
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
		context=getApplicationContext();
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
		@Nullable
		final GridView gridView_=gridView;
		if(gridView_!=null)
		{
			gridView_.setColumnWidth((int)imageWidth);
			gridView_.setNumColumns(num);
		}
	}

	static void initTheme()
	{
		@Nullable
		final SharedPreferences sharedPreferences_=sharedPreferences;
		if(!isThemeChanged&&sharedPreferences_!=null&&Build.VERSION.SDK_INT >= NIGHT_MODE_API)
		{
			try
			{
				final boolean isNightMode=sharedPreferences_.getBoolean("isNightMode",false);
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
			final ConnectivityManager manager=connectivityManager;
			if(manager!=null)
			{
				@Nullable
				final NetworkInfo wifiInfo=manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				@NonNull
				final boolean wifiConnected=(wifiInfo!=null?wifiInfo.getState():null)==NetworkInfo.State.CONNECTED;
				@Nullable
				final NetworkInfo mobileInfo=manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				@NonNull
				final boolean mobileConnected=(mobileInfo!=null?mobileInfo.getState():null)==NetworkInfo.State.CONNECTED;
				return wifiConnected||mobileConnected;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		LruMemoryCache.resizeMemoryCache(getQuantityItemsOnScreen());
		bundleGridViewState=new Bundle();
		@Nullable
		GridView gridView_=gridView;
		state=gridView_!=null?gridView_.onSaveInstanceState():null;
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
		gridView_=gridView;
		if(gridView_!=null)
		{
			gridView_.setColumnWidth((int)imageWidth);
			gridView_.setNumColumns(num);
			if(state!=null)
			{
				gridView_.onRestoreInstanceState(state);
			}
			if(gridView_.getAdapter()!=null)
			{
				ImagesAdapter.callNotifyDataSetChanged();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		initSharedPrefs();
		initTheme();
		super.onCreate(savedInstanceState);
		setTheme(R.style.AppTheme);
		setContentView(R.layout.activity_main);
		initStatic();
		initNetworkStatusListener();
		initCacheDirs(context);
		DiskUtils.makeDirs();
		getErrorListFromSharedPrefs();
		initGooglePlayServices();
		initClasses();
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
			showNoInternetAlertDialog();
			updateAdapter();
		}
		if(gridView!=null)
		{
			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(final AdapterView<?> parent,View v,int position,long id)
				{
					if(URLS_LINKS_STATUS.containsKey(ImagesAdapter.URLS_LIST.get(position)))
					{
						@NonNull
						final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
						if(ImagesDownloader.NO_INTERNET_LINKS.contains(ImagesAdapter.URLS_LIST.get(position)))
						{
							builder.setTitle("Ошибка");
							builder.setMessage("Ошибка загрузки: \nНет подключения к интернету");
						}
						else if("progress".equals(URLS_LINKS_STATUS.get(ImagesAdapter.URLS_LIST.get(position))))
						{
							builder.setTitle("Загрузка");
							builder.setMessage("Картинка загружается: \nПопробуйте позже");
						}
						else
						{
							@Nullable
							final String error=(ERROR_LIST.get(ImagesAdapter.URLS_LIST.get(position)));
							builder.setTitle("Ошибка");
							builder.setMessage("Ошибка загрузки: \n"+error);
							builder.setNegativeButton("Удалить",new AlertDialogOnClickListener(position));
							builder.setNeutralButton("Перезагрузить",new AlertDialogOnClickListener(position));
						}
						builder.setPositiveButton("ОК",new AlertDialogOnClickListener());
						builder.show();
					}
					else
					{
						@NonNull
						final Intent intent=new Intent(MainActivity.this,FullImage.class);
						intent.putExtra("URL",ImagesAdapter.URLS_LIST.get(position));
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
							builder.setPositiveButton("ОК",new AlertDialogOnClickListener());
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
		if(Build.VERSION.SDK_INT >= NIGHT_MODE_API)
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
		unregiesterNetworkStateChecker();
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
		if(Build.VERSION.SDK_INT >= NIGHT_MODE_API)
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
		unregiesterNetworkStateChecker();
		bundleGridViewState=new Bundle();
		@Nullable
		final GridView gridView_=gridView;
		if(gridView_!=null)
		{
			state=gridView_.onSaveInstanceState();
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
			@Nullable
			final GridView gridView_=gridView;
			if(gridView_!=null)
			{
				gridView_.onRestoreInstanceState(state);
			}
		}
	}

	private void regiesterNetworkStateChecker(@Nullable final ConnectivityManager connectivityManager)
	{
		if(connectivityManager!=null)
		{
			try
			{
				@Nullable
				final NetworkCallback networkCallback_=networkCallback;
				if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API&&networkCallback_!=null)
				{
					connectivityManager.registerDefaultNetworkCallback(networkCallback_);
				}
				else
				{
					registerReceiver(connectivityReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				isConnected=true;
			}
		}
	}

	public static void relaodErrorLinks()
	{
		ImagesDownloader.REPEAT_NUM=1;
		ERROR_LIST.clear();
		ImagesAdapter.callNotifyDataSetChanged();
	}

	public static void saveCurrentTheme()
	{
		@Nullable
		final SharedPreferences sharedPrefs=sharedPreferences;
		if(sharedPrefs!=null&&Build.VERSION.SDK_INT >= NIGHT_MODE_API)
		{
			final int nightMode=AppCompatDelegate.getDefaultNightMode();
			if(nightMode==AppCompatDelegate.MODE_NIGHT_YES)
			{
				sharedPrefs.edit().putBoolean("isNightMode",true).commit();
			}
			else if(nightMode==AppCompatDelegate.MODE_NIGHT_NO)
			{
				sharedPrefs.edit().putBoolean("isNightMode",false).commit();
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
			@Nullable
			final SharedPreferences sharedPreferences_=sharedPreferences;
			if(sharedPreferences_!=null)
			{
				sharedPreferences_.edit().putString("LinksStatusJson",hashMapString).commit();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void showNoInternetAlertDialog()
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
		builder.setTitle("Нет интернета");
		builder.setMessage("Нет подключения к интернету. Включите WI-FI или сотовую связь и попробуйте снова.");
		builder.setPositiveButton("ОК",new AlertDialogOnClickListener());
		builder.show();
	}

	private void unregiesterNetworkStateChecker()
	{
		@Nullable
		final ConnectivityManager connectivityManager_=connectivityManager;
		if(connectivityManager_!=null)
		{
			try
			{
				@Nullable
				final NetworkCallback networkCallback_=networkCallback;
				if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API&&networkCallback_!=null)
				{
					connectivityManager_.unregisterNetworkCallback(networkCallback_);
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
	}

	public void updateAdapter()
	{
		//noinspection AnonymousInnerClassMayBeStatic
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				@Nullable
				final GridView gridView_=gridView;
				if(gridView_!=null)
				{
					if(gridView_.getAdapter()==null)
					{
						gridView_.setAdapter(null);
						imagesAdapter=new ImagesAdapter();
						gridView_.setAdapter(imagesAdapter);
						DiskUtils.optimizeDisk();
					}
					else
					{
						ImagesAdapter.callNotifyDataSetChanged();
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
					ERROR_LIST.remove(ImagesAdapter.URLS_LIST.get(position));
					ImagesAdapter.callNotifyDataSetChanged();
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
				final URL url=new URL(IMAGES_URL);
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
					if(!serverURLS.equals(ImagesAdapter.URLS_LIST))
					{
						isUpdated=true;
					}
					if(!serverURLS.equals(ImagesAdapter.URLS_LIST))
					{
						@NonNull
						final ArrayList<String> urlsToDelete=new ArrayList<>();
						for(final String string : ImagesAdapter.URLS_LIST)
						{
							if(!serverURLS.contains(string))
							{
								urlsToDelete.add(string);
							}
						}
						ImagesAdapter.URLS_LIST.clear();
						for(final String string : serverURLS)
						{
							ImagesAdapter.URLS_LIST.add(string);
						}
						ImagesAdapter.callNotifyDataSetChanged();
						if(!urlsToDelete.isEmpty())
						{
							for(final String urlToDelete : urlsToDelete)
							{
								@Nullable
								final String fileName=ImagesDownloader.URLS_FILE_NAMES.get(urlToDelete);
								DiskUtils.deleteImageFromDisk(fileName);
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
					if(showToast&&context!=null)
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
			if(isError&&showToast&&context!=null)
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
			ImagesDownloader.NO_INTERNET_LINKS.clear();
			ImagesAdapter.callNotifyDataSetChanged();
		}

		@Override
		public void onLost(@NonNull Network network)
		{
			super.onLost(network);
			isConnected=false;
		}
	}
}
