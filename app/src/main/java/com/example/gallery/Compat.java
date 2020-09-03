package com.example.gallery;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;

final class Compat
{
	private static final int SIXTY_FPS_INTERVAL=1000/60;

	private Compat()
	{
	}

	public static void postOnAnimation(View view,Runnable runnable)
	{
		if(VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN)
		{
			postOnAnimationJellyBean(view,runnable);
		}
		else
		{
			view.postDelayed(runnable,SIXTY_FPS_INTERVAL);
		}
	}

	private static void postOnAnimationJellyBean(View view,Runnable runnable)
	{
		view.postOnAnimation(runnable);
	}
}
