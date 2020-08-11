package com.example.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageAdapter extends BaseAdapter
{
	@NonNull
	public static final ArrayList<String> URLS=new ArrayList<>();
	@NonNull
	Context mContext;
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="ImageAdapter";
	@NonNull
	private static final String LOADING="progress";
	@NonNull
	private static final String ERROR="error";
	@NonNull
	private static final String LOADING_ERROR="progress error";

	public ImageAdapter(@NonNull Context c)
	{
		mContext=c;
		URLS.clear();
		@NonNull
		final File textfilesdir=new File(MainActivity.cache,"textfiles");
		@NonNull
		final File linksfile=new File(textfilesdir,"links.txt");
		try
		{
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(linksfile));
			String url;
			while((url=bufferedReader.readLine())!=null)
			{
				URLS.add(url);
				MainActivity.LINKS_STATUS.put(url,"progress");
			}
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
			imageView=new ImageView(viewGroup.getContext());
			@NonNull
			final float scale=mContext.getResources().getDisplayMetrics().density;
			@NonNull
			final int margindp=(int)(4*scale);
			@NonNull
			final int height=mContext.getResources().getDimensionPixelSize(R.dimen.imageWidth);
			imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height));
			imageView.setPadding(margindp,margindp,margindp,margindp);
		}
		else
		{
			imageView=(ImageView)view;
		}
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