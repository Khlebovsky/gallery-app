package com.example.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import java.lang.ref.WeakReference;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageCustomView extends LinearLayout
{
	@Nullable
	public PhotoView photoView;
	@Nullable
	public FullImageActivity fullImageActivity;
	private static final float MAX_DRAG_ZOOM=1.1f;
	private static final float MIN_DRAG_ZOOM=1;
	private static final int CLOSE_ACTIVITY_DELAY=50;
	private static float offsetDY;
	private static float downLength;
	private static boolean isTouch;
	private static int touchSlop;
	private static int imageShiftThreshold;

	public ImageCustomView(Context context,@Nullable AttributeSet attrs)
	{
		super(context,attrs);
	}

	public ImageCustomView(Context context,@Nullable AttributeSet attrs,int defStyleAttr)
	{
		super(context,attrs,defStyleAttr);
	}

	void closeActivity()
	{
		@Nullable
		final PhotoView photoView=this.photoView;
		@Nullable
		final FullImageActivity fullImageActivity=this.fullImageActivity;
		if(photoView!=null&&fullImageActivity!=null)
		{
			photoView.animate().alpha(0).setDuration(50).start();
			//noinspection AnonymousInnerClassMayBeStatic
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					fullImageActivity.finish();
				}
			},CLOSE_ACTIVITY_DELAY);
		}
	}

	public static void initStatic(final int touchSlop,final int imageShiftThreshold)
	{
		ImageCustomView.touchSlop=touchSlop;
		ImageCustomView.imageShiftThreshold=imageShiftThreshold;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent motionEvent)
	{
		@Nullable
		final PhotoView photoView=this.photoView;
		if(photoView!=null&&photoView.getScale() >= MIN_DRAG_ZOOM&&photoView.getScale()<=MAX_DRAG_ZOOM)
		{
			final int imageShiftThreshold=ImageCustomView.imageShiftThreshold;
			final float currentImagePositionY=Math.abs(photoView.getY());
			switch(motionEvent.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					downLength=motionEvent.getY();
					offsetDY=photoView.getY()-motionEvent.getRawY();
					break;
				case MotionEvent.ACTION_UP:
					if(currentImagePositionY >= imageShiftThreshold)
					{
						closeActivity();
						break;
					}
					isTouch=false;
					if(!photoView.isZoomable())
					{
						photoView.setZoomable(true);
					}
					photoView.animate().y(0).setDuration(200).start();
					break;
				case MotionEvent.ACTION_MOVE:
					final float currentDownLength=Math.abs(downLength-motionEvent.getY());
					if(currentDownLength >= touchSlop&&!isTouch)
					{
						isTouch=true;
					}
					if(isTouch)
					{
						if(photoView.isZoomable())
						{
							photoView.setZoomable(false);
						}
						photoView.animate().y(motionEvent.getRawY()+offsetDY).setDuration(0).start();
						if(currentImagePositionY >= imageShiftThreshold)
						{
							final float transparencyCoefficient=(float)(1-Math.sqrt(Math.abs(currentImagePositionY/1000)));
							photoView.animate().alpha(transparencyCoefficient).setDuration(0).start();
						}
						else
						{
							photoView.animate().alpha(1).setDuration(0).start();
						}
					}
					break;
			}
		}
		return false;
	}
}