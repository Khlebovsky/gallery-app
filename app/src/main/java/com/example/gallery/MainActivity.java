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
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.muddzdev.styleabletoast.StyleableToast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.HashMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
	@NonNull
	public static final GalleryHandler GALLERY_HANDLER=new GalleryHandler();
	@NonNull
	public static final ToastHandler TOAST_HANDLER=new ToastHandler();
	int width;
	int height;
	int imageWidth;
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
	private static final int CHECK_NETWORK_STATUS_DELAY_TIME=100;
	@NonNull
	private static final Handler handler=new Handler();

	@RequiresApi(Application.NIGHT_MODE_API)
	void changeTheme()
	{
		final int currentTheme=AppCompatDelegate.getDefaultNightMode();
		if(currentTheme==AppCompatDelegate.MODE_NIGHT_YES)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
			SharedPreferences.putBoolean(getApplicationContext(),SHARED_PREFERENCES_NIGHT_MODE_KEY,false);
		}
		else
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
			SharedPreferences.putBoolean(getApplicationContext(),SHARED_PREFERENCES_NIGHT_MODE_KEY,true);
		}
		recreate();
	}

	void checkNetworkStatus()
	{
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if(isInternetConnected())
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
		},CHECK_NETWORK_STATUS_DELAY_TIME);
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
			if(string!=null&&!string.isEmpty())
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
				if(TEMP!=null)
				{
					Application.URLS_ERROR_LIST.putAll(TEMP);
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
		return getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT?(width/imageWidth):(height/imageWidth);
	}

	int getQuantityItemsOnScreen()
	{
		final float imageWidth=this.imageWidth;
		final int numInWidth;
		final int numInHeight;
		if(getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT)
		{
			numInWidth=(int)(width/imageWidth);
			numInHeight=(int)(height/imageWidth)+2;
		}
		else
		{
			numInWidth=(int)(height/imageWidth);
			numInHeight=(int)(width/imageWidth)+1;
		}
		return numInWidth*numInHeight;
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
		Application.isInternetAvaliable=isInternetConnected();
		initScreenSize();
	}

	@SuppressWarnings("SuspiciousNameCombination")
	void initScreenSize()
	{
		@NonNull
		final Display display=getWindowManager().getDefaultDisplay();
		@NonNull
		final Point size=new Point();
		display.getSize(size);
		final int screenWidth=size.x;
		final int screenHeight=size.y;
		if(screenWidth>screenHeight)
		{
			width=screenHeight;
			height=screenWidth;
		}
		else
		{
			width=screenWidth;
			height=screenHeight;
		}
	}

	boolean isInternetConnected()
	{
		try
		{
			@NonNull
			final ConnectivityManager connectivityManager=Application.getConnectivityManager(getApplicationContext());
			@Nullable
			final NetworkInfo wifiInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if(wifiInfo!=null&&wifiInfo.getState()==NetworkInfo.State.CONNECTED)
			{
				return true;
			}
			@Nullable
			final NetworkInfo mobileInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			return (mobileInfo!=null?mobileInfo.getState():null)==NetworkInfo.State.CONNECTED;
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
		@Nullable
		final GridView gridView=this.gridView;
		if(gridView!=null)
		{
			@Nullable
			final Parcelable gridViewState=gridView.onSaveInstanceState();
			final int position=gridView.getFirstVisiblePosition();
			gridView.setNumColumns(getGridViewColumnsNum());
			if(gridViewState!=null)
			{
				gridView.onRestoreInstanceState(gridViewState);
			}
			else
			{
				gridView.setSelection(position);
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
		registerNetworkStateChecker();
		getErrorListFromSharedPrefs();
		ConnectionSettings.initGooglePlayServices(this);
		DiskUtils.updateUrlsList(this);
		DiskUtils.optimizeDisk(this);
		ImagesDownloader.init(this);
		LruMemoryCache.initMemoryCache(getQuantityItemsOnScreen());
		ClientServer.init(this);
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
		unregisterNetworkStateChecker();
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
			@Nullable
			final String status=Application.getUrlStatus(url);
			if(status!=null)
			{
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
					if(error!=null)
					{
						builder.setMessage(resources.getString(R.string.dialog_message_error,error));
					}
					else
					{
						builder.setMessage(resources.getString(R.string.dialog_message_unknown_error));
					}
					builder.setTitle(resources.getString(R.string.dialog_title_error));
					builder.setNegativeButton(resources.getString(R.string.dialog_button_delete),new ErrorLinksAlertDialogOnClickListener(position));
					builder.setNeutralButton(resources.getString(R.string.dialog_button_reload),new ErrorLinksAlertDialogOnClickListener(position));
				}
				builder.setPositiveButton(resources.getString(R.string.dialog_button_ok),new ErrorLinksAlertDialogOnClickListener());
				builder.show();
			}
			else
			{
				@NonNull
				final Intent intent=new Intent(this,FullImageActivity.class);
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
				handler.postDelayed(new Runnable()
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
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		registerNetworkStateChecker();
	}

	void registerNetworkStateChecker()
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
				Application.getConnectivityManager(getApplicationContext()).registerDefaultNetworkCallback(networkCallback);
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

	void unregisterNetworkStateChecker()
	{
		try
		{
			if(Build.VERSION.SDK_INT >= NETWORK_CALLBACK_API)
			{
				@Nullable
				final NetworkCallback networkCallback=this.networkCallback;
				if(networkCallback!=null)
				{
					Application.getConnectivityManager(getApplicationContext()).unregisterNetworkCallback(networkCallback);
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
		@Nullable
		final ImagesAdapter imagesAdapter=this.imagesAdapter;
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

	static class GalleryHandler extends Handler
	{
		GalleryHandler()
		{
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(@NonNull Message msg)
		{
			@Nullable
			final WeakReference<MainActivity> mainActivityWeakReference=Application.mainActivity;
			if(mainActivityWeakReference!=null)
			{
				@Nullable
				final MainActivity mainActivity=mainActivityWeakReference.get();
				if(mainActivity!=null)
				{
					mainActivity.updateAdapter();
				}
			}
		}
	}

	@RequiresApi(NETWORK_CALLBACK_API)
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

	static class ToastHandler extends Handler
	{
		ToastHandler()
		{
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(@NonNull Message msg)
		{
			super.handleMessage(msg);
			@Nullable
			final String message=(String)msg.obj;
			if(message!=null&&!message.isEmpty())
			{
				@Nullable
				final WeakReference<MainActivity> mainActivityWeakReference=Application.mainActivity;
				if(mainActivityWeakReference!=null)
				{
					@Nullable
					final MainActivity mainActivity=mainActivityWeakReference.get();
					if(mainActivity!=null)
					{
						StyleableToast.makeText(mainActivity,message,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
					}
				}
			}
		}
	}
}