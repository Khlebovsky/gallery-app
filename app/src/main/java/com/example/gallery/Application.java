package com.example.gallery;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

public final class Application
{
	// TODO управлять из одного места
	@NonNull
	public static final HashMap<String,String> URLS_LINKS_STATUS=new HashMap<>();
	@NonNull
	public static final HashMap<String,String> URLS_ERROR_LIST=new HashMap<>();
	public static final int ACTION_DELAY_TIME=500;
	@NonNull
	public static final HashMap<String,String> URLS_FILE_NAMES=new HashMap<>();
	@NonNull
	public static final ArrayList<String> NO_INTERNET_LINKS=new ArrayList<>();
	public static final int MAX_BITMAP_SIZE=4096;
	@NonNull
	public static final ArrayList<String> URLS_LIST=new ArrayList<>();
	public static final int NIGHT_MODE_API=17;
	@Nullable
	public static File cacheDir;
	@Nullable
	public static File imagePreviewsDir;
	@Nullable
	public static File imagesBytesDir;
	@Nullable
	public static File textfilesDir;
	@Nullable
	public static File linksFile;
	public static boolean isInternetAvaliable;
	public static boolean hasThemeInit;
	@Nullable
	public static WeakReference<MainActivity> mainActivity;
	@Nullable
	public static WeakReference<FullImageActivity> fullImageActivity;
	@Nullable
	public static WeakReference<PhotoView> photoView;
	public static int DOWNLOADING_REPEAT_NUM=3;
	@Nullable
	public static WeakReference<SaveImageActivity> saveImageActivity;

	private Application()
	{
	}

	static void initTheme(@NonNull final Context context)
	{
		if(!Application.hasThemeInit&&Build.VERSION.SDK_INT >= NIGHT_MODE_API)
		{
			try
			{
				final boolean isNightMode=SharedPreferences.getBoolean(context,"isNightMode",false);
				final int theme=isNightMode?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO;
				AppCompatDelegate.setDefaultNightMode(theme);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			Application.hasThemeInit=true;
		}
	}
}
