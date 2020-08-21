package com.example.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageAdapter extends BaseAdapter
{
	@NonNull
	public static final ArrayList<String> URLS=new ArrayList<>();
	@NonNull
	final Context context;
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="ImageAdapter";
	@NonNull
	private static final String LOADING="progress";
	@NonNull
	private static final String ERROR="error";
	@NonNull
	private static final String LOADING_ERROR="progress error";

	public ImageAdapter(@NonNull Context context)
	{
		this.context=context;
		URLS.clear();
		try
		{
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
			String url;
			while((url=bufferedReader.readLine())!=null)
			{
				URLS.add(url);
				MainActivity.LINKS_STATUS.put(url,"progress");
			}
			bufferedReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public int getCount()
	{
		return URLS.size();
	}

	@Override
	@NonNull
	public Object getItem(int i)
	{
		return URLS.get(i);
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
			final int margin=context.getResources().getDimensionPixelSize(R.dimen.gridViewElementMargin);
			final int height=context.getResources().getDimensionPixelSize(R.dimen.imageWidth);
			imageView=new ImageView(viewGroup.getContext());
			imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height));
			imageView.setPadding(margin,margin,margin,margin);
		}
		else
		{
			imageView=(ImageView)view;
		}
		@NonNull
		final String url=URLS.get(i);
		if(ImageDownloader.NO_INTERNET_LINKS.contains(url))
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
				MainActivity.LINKS_STATUS.put(url,ERROR);
			}
		}
		else if(!url.equals(imageView.getTag()))
		{
			@Nullable
			final Bitmap bitmap=ImageDownloader.getImageBitmap(url);
			if(bitmap!=null)
			{
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				imageView.setImageBitmap(bitmap);
				imageView.setTag(url);
				MainActivity.LINKS_STATUS.remove(url);
			}
			else if(!LOADING.equals(imageView.getTag()))
			{
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setImageResource(R.drawable.ic_progress);
				imageView.setTag(LOADING);
				MainActivity.LINKS_STATUS.put(url,LOADING);
			}
		}
		return imageView;
	}
}