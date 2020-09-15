package com.example.gallery;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SharedPreferences
{
	@Nullable
	static android.content.SharedPreferences sharedPreferences;
	@NonNull
	private static final String SHARED_PREFERENCES_NAME="Gallery";
	private static final int SHARED_PREFERENCES_MODE=Context.MODE_PRIVATE;

	private SharedPreferences()
	{
	}

	public static boolean getBoolean(@NonNull final Context context,@NonNull final String key,final boolean defaultValue)
	{
		return getSharedPreferences(context).getBoolean(key,defaultValue);
	}

	public static int getInt(@NonNull final Context context,@NonNull final String key,final int defaultValue)
	{
		return getSharedPreferences(context).getInt(key,defaultValue);
	}

	public static android.content.SharedPreferences getSharedPreferences(@NonNull final Context context)
	{
		@Nullable
		android.content.SharedPreferences sharedPreferences_=sharedPreferences;
		if(sharedPreferences_!=null)
		{
			return sharedPreferences_;
		}
		sharedPreferences_=context.getSharedPreferences(SHARED_PREFERENCES_NAME,SHARED_PREFERENCES_MODE);
		sharedPreferences=sharedPreferences_;
		return sharedPreferences_;
	}

	public static String getString(@NonNull final Context context,@NonNull final String key,@Nullable final String defaultValue)
	{
		return getSharedPreferences(context).getString(key,defaultValue);
	}

	public static void putBoolean(@NonNull final Context context,@NonNull final String key,final boolean value)
	{
		getSharedPreferences(context).edit().putBoolean(key,value).apply();
	}

	public static void putInt(@NonNull final Context context,@NonNull final String key,final int value)
	{
		getSharedPreferences(context).edit().putInt(key,value).apply();
	}

	public static void putString(@NonNull final Context context,@NonNull final String key,@NonNull final String value)
	{
		getSharedPreferences(context).edit().putString(key,value).apply();
	}
}
