package com.example.gallery;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LruMemoryCache
{
	@Nullable
	public static LruCache<String,Bitmap> memoryCache;
	private static final int RESIZE_MEMORY_CACHE_API=21;

	private LruMemoryCache()
	{
	}

	public static void addBitmapToMemoryCache(@NonNull final String key,@NonNull final Bitmap bitmap)
	{
		if(getBitmapFromMemoryCache(key)==null&&memoryCache!=null)
		{
			memoryCache.put(key,bitmap);
		}
	}

	public static Bitmap getBitmapFromMemoryCache(@NonNull String key)
	{
		return memoryCache!=null?memoryCache.get(key):null;
	}

	static void initMemoryCache(final int getQuantityItemsOnScreen)
	{
		if(memoryCache==null)
		{
			memoryCache=new LruCache<>(getQuantityItemsOnScreen);
		}
	}

	static void resizeMemoryCache(final int getQuantityItemsOnScreen)
	{
		if(memoryCache!=null&&Build.VERSION.SDK_INT >= RESIZE_MEMORY_CACHE_API)
		{
			memoryCache.resize(getQuantityItemsOnScreen);
		}
	}
}
