package com.example.gallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

public class ConnectivityReceiver extends BroadcastReceiver
{
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="ConnectivityReceiver";

	@Override
	public void onReceive(Context context,Intent intent)
	{
		if(MainActivity.isInternet())
		{
			MainActivity.isConnected=true;
			ImageDownloader.NO_INTERNET_LINKS.clear();
			if(MainActivity.imageAdapter!=null)
			{
				//noinspection AnonymousInnerClassMayBeStatic
				((MainActivity)context).runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						MainActivity.imageAdapter.notifyDataSetChanged();
					}
				});
			}
		}
		else
		{
			MainActivity.isConnected=false;
		}
	}
}
