package com.example.gallery;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.example.gallery.FullImage.photoView;

public class ImageCustomView extends LinearLayout
{
	public static final float MAX_DRAG_ZOOM=1.1f;
	public static final float MIN_DRAG_ZOOM=1;
	static float offsetDY;
	static float downLength;
	static int touchSlop;
	static boolean isTouch;
	static int imageShiftThreshold;
	@Nullable
	private static Resources resources;

	public ImageCustomView(Context context,@Nullable AttributeSet attrs)
	{
		super(context,attrs);
	}

	public ImageCustomView(Context context,@Nullable AttributeSet attrs,int defStyleAttr)
	{
		super(context,attrs,defStyleAttr);
	}

	public static void initStatic(@Nullable final Context context)
	{
		if(context!=null)
		{
			resources=context.getResources();
		}
		@Nullable
		final PhotoView photoView_=photoView;
		if(photoView_!=null)
		{
			@NonNull
			final ViewConfiguration viewConfiguration=ViewConfiguration.get(photoView_.getContext());
			touchSlop=viewConfiguration.getScaledTouchSlop();
		}
		@Nullable
		final Resources resources_=resources;
		if(resources_!=null)
		{
			imageShiftThreshold=resources_.getDimensionPixelSize(R.dimen.imageShiftThreshold);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent motionEvent)
	{
		@Nullable
		final PhotoView photoView_=photoView;
		if(photoView_!=null&&photoView_.getScale() >= MIN_DRAG_ZOOM&&photoView_.getScale()<=MAX_DRAG_ZOOM)
		{
			final int imageShiftThreshold_=imageShiftThreshold;
			final float currentImagePositionY=Math.abs(photoView_.getY());
			switch(motionEvent.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					downLength=motionEvent.getY();
					offsetDY=photoView_.getY()-motionEvent.getRawY();
					break;
				case MotionEvent.ACTION_UP:
					if(currentImagePositionY >= imageShiftThreshold_)
					{
						FullImage.closeActivity();
						break;
					}
					isTouch=false;
					if(!photoView_.isZoomable())
					{
						photoView_.setZoomable(true);
					}
					photoView_.animate().y(0).setDuration(200).start();
					break;
				case MotionEvent.ACTION_MOVE:
					final float currentDownLength=Math.abs(downLength-motionEvent.getY());
					if(currentDownLength >= touchSlop&&!isTouch)
					{
						isTouch=true;
					}
					if(isTouch)
					{
						if(photoView_.isZoomable())
						{
							photoView_.setZoomable(false);
						}
						photoView_.animate().y(motionEvent.getRawY()+offsetDY).setDuration(0).start();
						if(currentImagePositionY >= imageShiftThreshold_)
						{
							final float transparencyCoefficient=(float)(1-Math.sqrt(Math.abs(currentImagePositionY/1000)));
							photoView_.animate().alpha(transparencyCoefficient).setDuration(0).start();
						}
						else
						{
							photoView_.animate().alpha(1).setDuration(0).start();
						}
					}
					break;
			}
		}
		return false;
	}
}