package com.example.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImagesAdapter extends BaseAdapter
{
	@NonNull
	public static final ArrayList<String> URLS_LIST=new ArrayList<>();
	@NonNull
	public static final GalleryHandler GALLERY_HANDLER=new GalleryHandler();
	@NonNull
	private static final String LOADING="progress";
	@NonNull
	private static final String ERROR="error";
	@NonNull
	private static final String LOADING_ERROR="progress error";
	@Nullable
	private static Resources resources;

	public ImagesAdapter()
	{
		DiskUtils.updateUrlsList();
	}

	public static void callNotifyDataSetChanged()
	{
		@NonNull
		final Message message=GALLERY_HANDLER.obtainMessage();
		GALLERY_HANDLER.sendMessage(message);
	}

	@Override
	public int getCount()
	{
		return URLS_LIST.size();
	}

	@Override
	@NonNull
	public Object getItem(int i)
	{
		return URLS_LIST.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return i;
	}

	@Override
	@NonNull
	public View getView(final int i,View view,ViewGroup viewGroup)
	{
		@NonNull
		final ImageView imageView;
		if(view==null)
		{
			imageView=new ImageView(viewGroup.getContext());
			if(resources!=null)
			{
				final int margin=resources.getDimensionPixelSize(R.dimen.gridViewElementMargin);
				final int height=resources.getDimensionPixelSize(R.dimen.imageWidth);
				imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height));
				imageView.setPadding(margin,margin,margin,margin);
			}
		}
		else
		{
			imageView=(ImageView)view;
		}
		@NonNull
		final String url=URLS_LIST.get(i);
		if(ImagesDownloader.NO_INTERNET_LINKS.contains(url))
		{
			if(!LOADING_ERROR.equals(imageView.getTag()))
			{
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setImageResource(R.drawable.ic_no_internet);
				imageView.setTag(LOADING_ERROR);
			}
		}
		else if(MainActivity.ERROR_LIST.containsKey(url))
		{
			if(!ERROR.equals(imageView.getTag()))
			{
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setImageResource(R.drawable.ic_error);
				imageView.setTag(ERROR);
				MainActivity.URLS_LINKS_STATUS.put(url,ERROR);
			}
		}
		else if(!url.equals(imageView.getTag()))
		{
			@Nullable
			final Bitmap bitmap=ImagesDownloader.getImageBitmap(url);
			if(bitmap!=null)
			{
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				imageView.setImageBitmap(bitmap);
				imageView.setTag(url);
				MainActivity.URLS_LINKS_STATUS.remove(url);
			}
			else if(!LOADING.equals(imageView.getTag()))
			{
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setImageResource(R.drawable.ic_progress);
				imageView.setTag(LOADING);
				MainActivity.URLS_LINKS_STATUS.put(url,LOADING);
			}
		}
		return imageView;
	}

	public static void initResources(@Nullable final Context context)
	{
		if(context!=null)
		{
			resources=context.getResources();
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
			if(MainActivity.imagesAdapter!=null)
			{
				MainActivity.imagesAdapter.notifyDataSetChanged();
			}
		}
	}
}