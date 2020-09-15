package com.example.gallery;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LruMemoryCache
{
	private static final int RESIZE_MEMORY_CACHE_API=21;
	@Nullable
	private static LruCache<String,Bitmap> memoryCache;

	private LruMemoryCache()
	{
	}

	public static void addBitmapToMemoryCache(@NonNull final String key,@NonNull final Bitmap bitmap)
	{
		@Nullable
		final LruCache<String,Bitmap> memoryCache=LruMemoryCache.memoryCache;
		if(getBitmapFromMemoryCache(key)==null&&memoryCache!=null)
		{
			memoryCache.put(key,bitmap);
		}
	}

	public static Bitmap getBitmapFromMemoryCache(@NonNull String key)
	{
		@Nullable
		final LruCache<String,Bitmap> memoryCache=LruMemoryCache.memoryCache;
		if(memoryCache!=null)
		{
			return memoryCache.get(key);
		}
		return null;
	}

	static void initMemoryCache(final int getQuantityItemsOnScreen)
	{
		if(memoryCache==null)
		{
			memoryCache=new LruCache<>(getQuantityItemsOnScreen);
		}
	}

	public static void removeBitmapFromMemoryCache(@NonNull final String key)
	{
		@Nullable
		final LruCache<String,Bitmap> memoryCache=LruMemoryCache.memoryCache;
		if(getBitmapFromMemoryCache(key)==null&&memoryCache!=null)
		{
			memoryCache.remove(key);
		}
	}

	static void resizeMemoryCache(final int getQuantityItemsOnScreen)
	{
		@Nullable
		final LruCache<String,Bitmap> memoryCache=LruMemoryCache.memoryCache;
		if(memoryCache!=null&&Build.VERSION.SDK_INT >= RESIZE_MEMORY_CACHE_API)
		{
			memoryCache.resize(getQuantityItemsOnScreen);
		}
	}
}
