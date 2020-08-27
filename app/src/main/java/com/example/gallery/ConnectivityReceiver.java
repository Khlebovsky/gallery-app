package com.example.gallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

public class ConnectivityReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context,Intent intent)
	{
		if(MainActivity.isInternet())
		{
			MainActivity.isConnected=true;
			ImageDownloader.NO_INTERNET_LINKS.clear();
			ImageDownloader.callNotifyDataSetChanged();
		}
		else
		{
			MainActivity.isConnected=false;
		}
	}
}
