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

	public static android.content.SharedPreferences getSharedPreferences(@NonNull final Context context)
	{
		@Nullable
		android.content.SharedPreferences sharedPreferences=SharedPreferences.sharedPreferences;
		if(sharedPreferences!=null)
		{
			return sharedPreferences;
		}
		sharedPreferences=context.getSharedPreferences(SHARED_PREFERENCES_NAME,SHARED_PREFERENCES_MODE);
		SharedPreferences.sharedPreferences=sharedPreferences;
		return sharedPreferences;
	}

	public static String getString(@NonNull final Context context,@NonNull final String key,@Nullable final String defaultValue)
	{
		return getSharedPreferences(context).getString(key,defaultValue);
	}

	public static void putBoolean(@NonNull final Context context,@NonNull final String key,final boolean value)
	{
		getSharedPreferences(context).edit().putBoolean(key,value).apply();
	}

	public static void putString(@NonNull final Context context,@NonNull final String key,@NonNull final String value)
	{
		getSharedPreferences(context).edit().putString(key,value).apply();
	}
}
