package com.example.gallery;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import static android.content.Context.CONNECTIVITY_SERVICE;

public final class Application
{
	@NonNull
	public static final HashMap<String,String> URLS_ERROR_LIST=new HashMap<>();
	public static final int ACTION_DELAY_TIME=500;
	@NonNull
	public static final HashMap<String,String> URLS_FILE_NAMES=new HashMap<>();
	public static final int MAX_BITMAP_SIZE=4096;
	@NonNull
	public static final ArrayList<String> URLS_LIST=new ArrayList<>();
	public static final int NIGHT_MODE_API=17;
	@NonNull
	public static final String PROGRESS_TAG="progress";
	@NonNull
	public static final String NO_INTERNET_TAG="no internet";
	public static boolean isInternetAvaliable;
	public static boolean hasThemeInit;
	@Nullable
	public static WeakReference<MainActivity> mainActivity;
	@Nullable
	public static WeakReference<FullImageActivity> fullImageActivity;
	public static int DOWNLOADING_REPEAT_NUM=3;
	@Nullable
	public static WeakReference<SaveImageActivity> saveImageActivity;
	@NonNull
	private static final String SHARED_PREFS_NIGHT_MODE_KEY="isNightMode";
	@NonNull
	private static final HashMap<String,String> URLS_STATUS_LIST=new HashMap<>();
	@Nullable
	private static ConnectivityManager connectivityManager;

	private Application()
	{
	}

	public static void addUrlInUrlsStatusList(@NonNull final String url,@NonNull final String status)
	{
		URLS_STATUS_LIST.put(url,status);
	}

	public static AlertDialog.Builder getAlertDialogBuilder(@NonNull final Context context)
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(context,R.style.AlertDialogTheme);
		return builder;
	}

	public static ConnectivityManager getConnectivityManager(@NonNull final Context context)
	{
		@Nullable
		ConnectivityManager connectivityManager=Application.connectivityManager;
		if(connectivityManager!=null)
		{
			return connectivityManager;
		}
		connectivityManager=(ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
		Application.connectivityManager=connectivityManager;
		return connectivityManager;
	}

	public static String getUrlStatus(@NonNull final String url)
	{
		return URLS_STATUS_LIST.get(url);
	}

	public static void initTheme(@NonNull final Context context)
	{
		if(!hasThemeInit&&Build.VERSION.SDK_INT >= NIGHT_MODE_API)
		{
			try
			{
				final boolean isNightMode=SharedPreferences.getBoolean(context,SHARED_PREFS_NIGHT_MODE_KEY,false);
				final int theme=isNightMode?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO;
				AppCompatDelegate.setDefaultNightMode(theme);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			hasThemeInit=true;
		}
	}

	public static void removeNoInternetUrlsFromUrlsStatusList()
	{
		for(final String url : URLS_LIST)
		{
			if(NO_INTERNET_TAG.equals(getUrlStatus(url)))
			{
				removeUrlFromUrlsStatusList(url);
			}
		}
	}

	public static void removeUrlFromUrlsStatusList(@NonNull final String url)
	{
		URLS_STATUS_LIST.remove(url);
	}
}
