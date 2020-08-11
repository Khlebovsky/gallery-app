package com.example.gallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ConnectivityReceiver extends BroadcastReceiver
{
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="ConnectivityReceiver";

	@Override
	public void onReceive(Context context,Intent intent)
	{
		@NonNull
		final ConnectivityManager connectivityManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		@Nullable
		final NetworkInfo wifiInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		@NonNull
		final boolean wifiConnected=wifiInfo!=null&&wifiInfo.getState()==NetworkInfo.State.CONNECTED;
		@Nullable
		final NetworkInfo mobileInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		@NonNull
		final boolean mobileConnected=(mobileInfo!=null?mobileInfo.getState():null)==NetworkInfo.State.CONNECTED;
		if(wifiConnected||mobileConnected)
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
