package com.example.gallery;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;

import static com.example.gallery.FullImage.photoView;

public class ImageCustomView extends LinearLayout
{
	public ImageCustomView(Context context,@Nullable AttributeSet attrs)
	{
		super(context,attrs);
	}

	public ImageCustomView(Context context,@Nullable AttributeSet attrs,int defStyleAttr)
	{
		super(context,attrs,defStyleAttr);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent motionEvent)
	{
		if(photoView.getScale() >= FullImage.MIN_DRAG_ZOOM&&photoView.getScale()<=FullImage.MAX_DRAG_ZOOM)
		{
			final float currentLocationY=Math.abs(photoView.getY());
			switch(motionEvent.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					FullImage.downLength=motionEvent.getY();
					FullImage.offsetDY=photoView.getY()-motionEvent.getRawY();
					break;
				case MotionEvent.ACTION_UP:
					if(currentLocationY >= FullImage.imageShiftThreshold)
					{
						FullImage.closeActivity();
						break;
					}
					FullImage.isTouch=false;
					if(!photoView.isZoomable())
					{
						photoView.setZoomable(true);
					}
					photoView.animate().y(0).setDuration(200).start();
					break;
				case MotionEvent.ACTION_MOVE:
					final float currentDownLength=Math.abs(FullImage.downLength-motionEvent.getY());
					if(currentDownLength >= FullImage.touchSlop&&!FullImage.isTouch)
					{
						FullImage.isTouch=true;
					}
					if(FullImage.isTouch)
					{
						if(photoView.isZoomable())
						{
							photoView.setZoomable(false);
						}
						photoView.animate().y(motionEvent.getRawY()+FullImage.offsetDY).setDuration(0).start();
						if(currentLocationY >= FullImage.imageShiftThreshold)
						{
							final float transparencyCoefficient=(float)(1-Math.sqrt(Math.abs(currentLocationY/1000)));
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