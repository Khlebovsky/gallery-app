package com.example.gallery;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.*;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
	int width;
	int height;
	int num;
	float imageWidth;
	boolean hasUpdatesChecked;
	@Nullable
	ImagesAdapter imagesAdapter;
	@Nullable
	ConnectivityManager connectivityManager;
	@Nullable
	GridView gridView;
	@Nullable
	Parcelable state;
	@Nullable
	Bundle bundleGridViewState;
	@Nullable
	NetworkCallback networkCallback;
	@Nullable
	ConnectivityReceiver connectivityReceiver;
	private static final int NETWORK_CALLBACK_API=24;
	@NonNull
	private static final String SHARED_PREFERENCES_ERROR_LIST_KEY="LinksStatusJson";
	@NonNull
	private static final String GRID_VIEW_STATE_KEY="GridViewState";
	@NonNull
	private static final String SHARED_PREFERENCES_NIGHT_MODE_KEY="isNightMode";

	@RequiresApi(Application.NIGHT_MODE_API)
	void changeTheme()
	{
		final int currentTheme=AppCompatDelegate.getDefaultNightMode();
		final int theme=currentTheme==AppCompatDelegate.MODE_NIGHT_YES?AppCompatDelegate.MODE_NIGHT_NO:AppCompatDelegate.MODE_NIGHT_YES;
		AppCompatDelegate.setDefaultNightMode(theme);
		recreate();
		final boolean isNightMode=theme==AppCompatDelegate.MODE_NIGHT_YES;
		SharedPreferences.putBoolean(getApplicationContext(),SHARED_PREFERENCES_NIGHT_MODE_KEY,isNightMode);
	}

	void checkNetworkStatus()
	{
		if(isInternetConnected())
		{
			Application.isInternetAvaliable=true;
			Application.NO_INTERNET_LINKS.clear();
			ImagesAdapter.callNotifyDataSetChanged();
			checkUpdates(false);
		}
		else
		{
			Application.isInternetAvaliable=false;
		}
	}

	void checkUpdates(final boolean showToast)
	{
		if(Application.isInternetAvaliable)
		{
			new ImagesDownloader.CheckUpdatesThread(showToast).start();
		}
		else
		{
			showNoInternetAlertDialog();
		}
	}

	void getErrorListFromSharedPrefs()
	{
		try
		{
			@Nullable
			final String string=SharedPreferences.getString(getApplicationContext(),SHARED_PREFERENCES_ERROR_LIST_KEY,null);
			if(string!=null)
			{
				//noinspection AnonymousInnerClassMayBeStatic
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
						Application.URLS_ERROR_LIST.put(entry.getKey().toString(),entry.getValue().toString());
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	int getQuantityItemsOnScreen()
	{
		final int numInWidth=(int)(width/imageWidth);
		final int numInHeight=(int)(height/imageWidth);
		int quantity=numInWidth*numInHeight;
		@NonNull
		final Resources resources=getResources();
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

	void initGridView()
	{
		// TODO упростить
		num=(int)(width/imageWidth);
		@NonNull
		final GridView gridView=findViewById(R.id.GridView);
		gridView.setColumnWidth((int)imageWidth);
		gridView.setNumColumns(num);
		gridView.setAdapter(null);
		imagesAdapter=new ImagesAdapter(getLayoutInflater());
		gridView.setAdapter(imagesAdapter);
		gridView.setOnItemClickListener(this);
		this.gridView=gridView;
	}

	// TODO в регистрацию
	void initNetworkStatusListener()
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

	void initObjects()
	{
		Application.mainActivity=new WeakReference<>(MainActivity.this);
		@NonNull
		final Resources resources=getResources();
		setTitle(resources.getString(R.string.app_name));
		imageWidth=resources.getDimensionPixelSize(R.dimen.imageHeight);
		connectivityManager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		Application.isInternetAvaliable=isInternetConnected();
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		// TODO проверить Configuration.onScreenDp
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		width=size.x;
		height=size.y;
	}

	boolean isInternetConnected()
	{
		try
		{
			// TODO в метод
			@Nullable
			final ConnectivityManager connectivityManager=this.connectivityManager;
			if(connectivityManager!=null)
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
		// TODO упростить
		bundleGridViewState=new Bundle();
		@Nullable
		GridView gridView_=gridView;
		state=gridView_!=null?gridView_.onSaveInstanceState():null;
		bundleGridViewState.putParcelable(GRID_VIEW_STATE_KEY,state);
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		// TODO упростить
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		width=size.x;
		height=size.y;
		num=(int)(width/imageWidth);
		gridView_=gridView;
		if(gridView_!=null)
		{
			gridView_.setColumnWidth((int)imageWidth);
			gridView_.setNumColumns(num);
			if(state!=null)
			{
				gridView_.onRestoreInstanceState(state);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Application.initTheme(getApplicationContext());
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initObjects();
		initNetworkStatusListener();
		getErrorListFromSharedPrefs();
		ConnectionSettings.initGooglePlayServices(this);
		DiskUtils.updateUrlsList();
		DiskUtils.initCacheDirs(this);
		DiskUtils.optimizeDisk();
		ImagesDownloader.init();
		LruMemoryCache.initMemoryCache(getQuantityItemsOnScreen());
		initGridView();
		if(isInternetConnected())
		{
			if(!hasUpdatesChecked)
			{
				checkUpdates(false);
				hasUpdatesChecked=true;
			}
		}
		else
		{
			showNoInternetAlertDialog();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		@NonNull
		final MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.main_menu,menu);
		// TODO проверить кнопку на null
		if(Build.VERSION.SDK_INT >= Application.NIGHT_MODE_API)
		{
			final int nightMode=AppCompatDelegate.getDefaultNightMode();
			menu.findItem(R.id.changeTheme).setTitle(nightMode==AppCompatDelegate.MODE_NIGHT_YES?R.string.light_theme:R.string.dark_theme);
		}
		return true;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterNetworkStateChecker();
		saveErrorList();
	}

	@Override
	public void onItemClick(AdapterView<?> parent,View view,int position,long id)
	{
		@Nullable
		final String url=Application.URLS_LIST.get(position);
		// TODO слить URLS_LINKS_STATUS и NO_INTERNET_LINKS
		if(Application.URLS_LINKS_STATUS.containsKey(url))
		{
			@NonNull
			final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
			if(Application.NO_INTERNET_LINKS.contains(url))
			{
				builder.setTitle("Ошибка");
				builder.setMessage("Ошибка загрузки: \nНет подключения к интернету");
			}
			// TODO константы и строки
			else if("progress".equals(Application.URLS_LINKS_STATUS.get(url)))
			{
				builder.setTitle("Загрузка");
				builder.setMessage("Картинка загружается: \nПопробуйте позже");
			}
			else
			{
				@Nullable
				final String error=(Application.URLS_ERROR_LIST.get(url));
				builder.setTitle("Ошибка");
				builder.setMessage("Ошибка загрузки: \n"+error);
				builder.setNegativeButton("Удалить",new ErrorLinksAlertDialogOnClickListener(position));
				builder.setNeutralButton("Перезагрузить",new ErrorLinksAlertDialogOnClickListener(position));
			}
			builder.setPositiveButton("ОК",new ErrorLinksAlertDialogOnClickListener());
			builder.show();
		}
		else
		{
			@NonNull
			final Intent intent=new Intent(MainActivity.this,FullImageActivity.class);
			// TODO константы в FullImage
			intent.putExtra("URL",url);
			intent.putExtra("Num",position);
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
				builder.setPositiveButton("ОК",new ErrorLinksAlertDialogOnClickListener());
				builder.show();
				e.printStackTrace();
			}
		}
	}

	@TargetApi(Application.NIGHT_MODE_API)
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.update:
				checkUpdates(true);
				return true;
			case R.id.reloadErrorLinks:
				reloadErrorLinks();
				return true;
			case R.id.changeTheme:
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						changeTheme();
					}
				},Application.ACTION_DELAY_TIME);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterNetworkStateChecker();
		saveErrorList();
		bundleGridViewState=new Bundle();
		@Nullable
		final GridView gridView_=gridView;
		if(gridView_!=null)
		{
			state=gridView_.onSaveInstanceState();
		}
		bundleGridViewState.putParcelable(GRID_VIEW_STATE_KEY,state);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// TODO параметр геттер
		registerNetworkStateChecker(connectivityManager);
		if(bundleGridViewState!=null)
		{
			state=bundleGridViewState.getParcelable(GRID_VIEW_STATE_KEY);
			@Nullable
			final GridView gridView_=gridView;
			if(gridView_!=null)
			{
				gridView_.onRestoreInstanceState(state);
			}
		}
	}

	void registerNetworkStateChecker(@Nullable final ConnectivityManager connectivityManager)
	{
		if(connectivityManager!=null)
		{
			try
			{
				if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API)
				{
					@Nullable
					final NetworkCallback networkCallback_=networkCallback;
					if(networkCallback_!=null)
					{
						connectivityManager.registerDefaultNetworkCallback(networkCallback_);
					}
				}
				else
				{
					registerReceiver(connectivityReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Application.isInternetAvaliable=true;
			}
		}
	}

	static void reloadErrorLinks()
	{
		Application.DOWNLOADING_REPEAT_NUM=1;
		Application.URLS_ERROR_LIST.clear();
		ImagesAdapter.callNotifyDataSetChanged();
	}

	void saveErrorList()
	{
		@NonNull
		final Gson gson=new Gson();
		@NonNull
		final String hashMapString=gson.toJson(Application.URLS_ERROR_LIST);
		SharedPreferences.putString(getApplicationContext(),SHARED_PREFERENCES_ERROR_LIST_KEY,hashMapString);
	}

	void showNoInternetAlertDialog()
	{
		// TODO строки + кнопки в ресурсы, билдер в метод
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(this,R.style.AlertDialogTheme);
		builder.setTitle("Нет интернета");
		builder.setMessage("Нет подключения к интернету. Включите WI-FI или сотовую связь и попробуйте снова.");
		builder.setPositiveButton("ОК",new ErrorLinksAlertDialogOnClickListener());
		builder.show();
	}

	void unregisterNetworkStateChecker()
	{
		// TODO упростить
		@Nullable
		final ConnectivityManager connectivityManager_=connectivityManager;
		if(connectivityManager_!=null)
		{
			try
			{
				if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API)
				{
					@Nullable
					final NetworkCallback networkCallback_=networkCallback;
					if(networkCallback_!=null)
					{
						connectivityManager_.unregisterNetworkCallback(networkCallback_);
					}
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

	void updateAdapter()
	{
		if(imagesAdapter!=null)
		{
			imagesAdapter.notifyDataSetChanged();
		}
	}

	class ConnectivityReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context,Intent intent)
		{
			checkNetworkStatus();
		}
	}

	class ErrorLinksAlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		int position;

		ErrorLinksAlertDialogOnClickListener(final int position)
		{
			this.position=position;
		}

		ErrorLinksAlertDialogOnClickListener()
		{
		}

		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			switch(which)
			{
				case Dialog.BUTTON_NEGATIVE:
					ClientServer.deleteImage(position);
					break;
				case Dialog.BUTTON_NEUTRAL:
					Application.URLS_ERROR_LIST.remove(Application.URLS_LIST.get(position));
					updateAdapter();
					break;
			}
		}
	}

	@RequiresApi(api=Build.VERSION_CODES.LOLLIPOP)
	class NetworkCallback extends ConnectivityManager.NetworkCallback
	{
		@Override
		public void onAvailable(@NonNull Network network)
		{
			checkNetworkStatus();
		}

		@Override
		public void onLost(@NonNull Network network)
		{
			checkNetworkStatus();
		}
	}
}