package com.example.gallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectivityReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context,Intent intent)
	{
		if(MainActivity.isInternet())
		{
			MainActivity.onAvailableConnection();
		}
		else
		{
			MainActivity.onLostConnection();
		}
	}
}
