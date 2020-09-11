package com.example.gallery;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImagesAdapter extends BaseAdapter
{
	@NonNull
	static final GalleryHandler GALLERY_HANDLER=new GalleryHandler();
	@NonNull
	final LayoutInflater layoutInflater;
	@NonNull
	private static final String LOADING="progress";
	@NonNull
	private static final String ERROR="error";
	@NonNull
	private static final String LOADING_ERROR="progress error";

	public ImagesAdapter(@NonNull final LayoutInflater layoutInflater)
	{
		this.layoutInflater=layoutInflater;
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
		return Application.URLS_LIST.size();
	}

	@Override
	public String getItem(int i)
	{
		return Application.URLS_LIST.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return i;
	}

	@Override
	public View getView(final int i,View view,ViewGroup viewGroup)
	{
		@Nullable
		final LinearLayout linearLayout;
		@Nullable
		final ImageView imageView;
		@NonNull
		final LayoutInflater layoutInflater_=layoutInflater;
		linearLayout=view==null?(LinearLayout)layoutInflater_.inflate(R.layout.gridview_element,viewGroup,false):(LinearLayout)view;
		if(linearLayout!=null)
		{
			imageView=linearLayout.findViewById(R.id.gridview_image);
			if(imageView!=null)
			{
				@NonNull
				final String url=Application.URLS_LIST.get(i);
				if(Application.NO_INTERNET_LINKS.contains(url))
				{
					if(!LOADING_ERROR.equals(imageView.getTag()))
					{
						imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						imageView.setImageResource(R.drawable.ic_no_internet);
						imageView.setTag(LOADING_ERROR);
					}
				}
				else if(Application.URLS_ERROR_LIST.containsKey(url))
				{
					if(!ERROR.equals(imageView.getTag()))
					{
						imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						imageView.setImageResource(R.drawable.ic_error);
						imageView.setTag(ERROR);
						Application.URLS_LINKS_STATUS.put(url,ERROR);
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
						Application.URLS_LINKS_STATUS.remove(url);
					}
					else if(!LOADING.equals(imageView.getTag()))
					{
						imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						imageView.setImageResource(R.drawable.ic_progress);
						imageView.setTag(LOADING);
						Application.URLS_LINKS_STATUS.put(url,LOADING);
					}
				}
			}
		}
		return linearLayout;
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
			if(Application.mainActivity!=null)
			{
				@Nullable
				final MainActivity mainActivity=Application.mainActivity.get();
				if(mainActivity!=null)
				{
					mainActivity.updateAdapter();
				}
			}
		}
	}
}