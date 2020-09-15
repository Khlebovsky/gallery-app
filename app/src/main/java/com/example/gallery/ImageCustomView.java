package com.example.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import java.lang.ref.WeakReference;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageCustomView extends LinearLayout
{
	private static final float MAX_DRAG_ZOOM=1.1f;
	private static final float MIN_DRAG_ZOOM=1;
	private static float offsetDY;
	private static float downLength;
	private static int touchSlop;
	private static boolean isTouch;
	private static int imageShiftThreshold;

	public ImageCustomView(Context context,@Nullable AttributeSet attrs)
	{
		super(context,attrs);
	}

	public ImageCustomView(Context context,@Nullable AttributeSet attrs,int defStyleAttr)
	{
		super(context,attrs,defStyleAttr);
	}

	public static void initStatic()
	{
		@Nullable
		final WeakReference<PhotoView> photoViewWeakReference=Application.photoView;
		if(photoViewWeakReference!=null)
		{
			@Nullable
			final PhotoView photoView=photoViewWeakReference.get();
			if(photoView!=null)
			{
				@NonNull
				final ViewConfiguration viewConfiguration=ViewConfiguration.get(photoView.getContext());
				touchSlop=viewConfiguration.getScaledTouchSlop();
			}
		}
		@Nullable
		final WeakReference<FullImageActivity> fullImageWeakReference=Application.fullImageActivity;
		if(fullImageWeakReference!=null)
		{
			@Nullable
			final Resources resources=fullImageWeakReference.get().getResources();
			if(resources!=null)
			{
				imageShiftThreshold=resources.getDimensionPixelSize(R.dimen.imageShiftThreshold);
			}
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent motionEvent)
	{
		@Nullable
		final WeakReference<PhotoView> photoViewWeakReference=Application.photoView;
		if(photoViewWeakReference!=null)
		{
			@Nullable
			final PhotoView photoView=photoViewWeakReference.get();
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
							FullImageActivity.closeActivity();
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
		}
		return false;
	}
}