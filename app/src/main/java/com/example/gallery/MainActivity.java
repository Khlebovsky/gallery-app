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
import android.util.Log;
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
	float imageWidth;
	boolean hasUpdatesChecked;
	@Nullable
	ImagesAdapter imagesAdapter;
	@Nullable
	GridView gridView;
	@Nullable
	NetworkCallback networkCallback;
	@Nullable
	ConnectivityReceiver connectivityReceiver;
	private static final int NETWORK_CALLBACK_API=24;
	@NonNull
	private static final String SHARED_PREFERENCES_ERROR_LIST_KEY="LinksStatusJson";
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
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if(isInternetConnected(Application.getConnectivityManager(getApplicationContext())))
				{
					Application.isInternetAvaliable=true;
					Application.removeNoInternetUrlsFromUrlsStatusList();
					ImagesAdapter.callNotifyDataSetChanged();
					checkUpdates(false);
				}
				else
				{
					Application.isInternetAvaliable=false;
				}
			}
		},Application.CHECK_NETWORK_STATUS_DELAY_TIME);
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

	int getGridViewColumnsNum()
	{
		return getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT?(int)(width/imageWidth):(int)(height/imageWidth);
	}

	int getQuantityItemsOnScreen()
	{
		final float imageWidth=this.imageWidth;
		final int numInWidth;
		final int numInHeight;
		if(getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT)
		{
			numInWidth=(int)(width/imageWidth);
			numInHeight=(int)(height/imageWidth);
		}
		else
		{
			numInWidth=(int)(height/imageWidth);
			numInHeight=(int)(width/imageWidth);
		}
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
		@NonNull
		final ImagesAdapter imagesAdapter=new ImagesAdapter(getLayoutInflater());
		final float imageWidth=this.imageWidth;
		@NonNull
		final GridView gridView=findViewById(R.id.GridView);
		gridView.setColumnWidth((int)imageWidth);
		gridView.setNumColumns(getGridViewColumnsNum());
		gridView.setAdapter(null);
		gridView.setAdapter(imagesAdapter);
		gridView.setOnItemClickListener(this);
		this.imagesAdapter=imagesAdapter;
		this.gridView=gridView;
	}

	void initObjects()
	{
		Application.mainActivity=new WeakReference<>(MainActivity.this);
		@NonNull
		final Resources resources=getResources();
		setTitle(resources.getString(R.string.app_name));
		imageWidth=resources.getDimensionPixelSize(R.dimen.imageSize);
		Application.isInternetAvaliable=isInternetConnected(Application.getConnectivityManager(this));
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		width=size.x;
		height=size.y;
	}

	static boolean isInternetConnected(@NonNull final ConnectivityManager connectivityManager)
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
			Log.d("123",String.valueOf(wifiConnected||mobileConnected));
			return wifiConnected||mobileConnected;
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
		// TODO логика сохранения прокрутки
		super.onConfigurationChanged(newConfig);
		LruMemoryCache.resizeMemoryCache(getQuantityItemsOnScreen());
		@Nullable
		final GridView gridView=this.gridView;
		if(gridView!=null)
		{
			final int firstVisiblePosition=gridView.getFirstVisiblePosition();
			gridView.setColumnWidth((int)imageWidth);
			gridView.setNumColumns(getGridViewColumnsNum());
			gridView.setSelection(firstVisiblePosition);
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
		registerNetworkStateChecker(Application.getConnectivityManager(this));
		getErrorListFromSharedPrefs();
		ConnectionSettings.initGooglePlayServices(this);
		DiskUtils.updateUrlsList(this);
		DiskUtils.optimizeDisk(this);
		ImagesDownloader.init(this);
		LruMemoryCache.initMemoryCache(getQuantityItemsOnScreen());
		ClientServer.init(this);
		initGridView();
		if(isInternetConnected(Application.getConnectivityManager(this)))
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
		if(menu.findItem(R.id.changeTheme)!=null)
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
		unregisterNetworkStateChecker(Application.getConnectivityManager(this));
		saveErrorList();
	}

	@Override
	public void onItemClick(AdapterView<?> parent,View view,int position,long id)
	{
		@NonNull
		final Resources resources=getResources();
		@Nullable
		final String url=Application.URLS_LIST.get(position);
		if(url!=null)
		{
			if(Application.isUrlInUrlsStatusList(url))
			{
				@Nullable
				final String status=Application.getUrlStatus(url);
				@NonNull
				final AlertDialog.Builder builder=Application.getAlertDialogBuilder(this);
				if(Application.NO_INTERNET.equals(status))
				{
					builder.setTitle(resources.getString(R.string.dialog_title_error));
					builder.setMessage(resources.getString(R.string.dialog_message_internet_error));
				}
				else if(Application.PROGRESS.equals(status))
				{
					builder.setTitle(resources.getString(R.string.dialog_title_loading));
					builder.setMessage(resources.getString(R.string.dialog_message_loading));
				}
				else
				{
					@Nullable
					final String error=(Application.URLS_ERROR_LIST.get(url));
					builder.setTitle(resources.getString(R.string.dialog_title_error));
					builder.setMessage(resources.getString(R.string.dialog_message_error,error));
					builder.setNegativeButton(resources.getString(R.string.dialog_button_delete),new ErrorLinksAlertDialogOnClickListener(position));
					builder.setNeutralButton(resources.getString(R.string.dialog_button_reload),new ErrorLinksAlertDialogOnClickListener(position));
				}
				builder.setPositiveButton(resources.getString(R.string.dialog_button_ok),new ErrorLinksAlertDialogOnClickListener());
				builder.show();
			}
			else
			{
				@NonNull
				final Intent intent=new Intent(MainActivity.this,FullImageActivity.class);
				intent.putExtra(FullImageActivity.INTENT_EXTRA_NAME_URL,url);
				intent.putExtra(FullImageActivity.INTENT_EXTRA_NAME_NUM,position);
				try
				{
					startActivity(intent);
				}
				catch(Throwable e)
				{
					@NonNull
					final AlertDialog.Builder builder=Application.getAlertDialogBuilder(this);
					builder.setTitle(resources.getString(R.string.dialog_title_error));
					builder.setMessage(resources.getString(R.string.dialog_message_unknown_error));
					builder.setPositiveButton(resources.getString(R.string.dialog_button_ok),new ErrorLinksAlertDialogOnClickListener());
					builder.show();
					e.printStackTrace();
				}
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
		unregisterNetworkStateChecker(Application.getConnectivityManager(this));
		saveErrorList();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		registerNetworkStateChecker(Application.getConnectivityManager(this));
	}

	void registerNetworkStateChecker(@NonNull final ConnectivityManager connectivityManager)
	{
		try
		{
			if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API)
			{
				@Nullable
				NetworkCallback networkCallback=this.networkCallback;
				if(networkCallback==null)
				{
					networkCallback=new NetworkCallback();
				}
				connectivityManager.registerDefaultNetworkCallback(networkCallback);
				this.networkCallback=networkCallback;
			}
			else
			{
				@Nullable
				ConnectivityReceiver connectivityReceiver=this.connectivityReceiver;
				if(connectivityReceiver==null)
				{
					connectivityReceiver=new ConnectivityReceiver();
				}
				registerReceiver(connectivityReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
				this.connectivityReceiver=connectivityReceiver;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Application.isInternetAvaliable=true;
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
		@NonNull
		final Resources resources=getResources();
		@NonNull
		final AlertDialog.Builder builder=Application.getAlertDialogBuilder(this);
		builder.setTitle(resources.getString(R.string.dialog_title_no_internet));
		builder.setMessage(resources.getString(R.string.dialog_message_no_internet));
		builder.setPositiveButton(resources.getString(R.string.dialog_button_ok),new ErrorLinksAlertDialogOnClickListener());
		builder.show();
	}

	void unregisterNetworkStateChecker(@NonNull final ConnectivityManager connectivityManager)
	{
		try
		{
			if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API)
			{
				@Nullable
				final NetworkCallback networkCallback=this.networkCallback;
				if(networkCallback!=null)
				{
					connectivityManager.unregisterNetworkCallback(networkCallback);
				}
			}
			else
			{
				@Nullable
				final ConnectivityReceiver connectivityReceiver=this.connectivityReceiver;
				if(connectivityReceiver!=null)
				{
					unregisterReceiver(connectivityReceiver);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
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