package com.example.gallery;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;

final class Compat
{
	private Compat()
	{
	}

	public static void postOnAnimation(View view,Runnable runnable)
	{
		postOnAnimationJellyBean(view,runnable);
	}

	private static void postOnAnimationJellyBean(View view,Runnable runnable)
	{
		view.postOnAnimation(runnable);
	}
}
