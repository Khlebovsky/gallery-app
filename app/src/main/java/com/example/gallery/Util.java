package com.example.gallery;

import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.annotation.Nullable;

final class Util
{
	private Util()
	{
	}

	static void checkZoomLevels(final float minZoom,final float midZoom,final float maxZoom)
	{
		if(minZoom >= midZoom)
		{
			throw new IllegalArgumentException("Minimum zoom has to be less than Medium zoom. Call setMinimumZoom() with a more appropriate value");
		}
		else if(midZoom >= maxZoom)
		{
			throw new IllegalArgumentException("Medium zoom has to be less than Maximum zoom. Call setMaximumZoom() with a more appropriate value");
		}
	}

	static int getPointerIndex(final int action)
	{
		return (action&MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	}

	static boolean hasDrawable(@Nullable final ImageView imageView)
	{
		return (imageView!=null?imageView.getDrawable():null)!=null;
	}

	static boolean isSupportedScaleType(@Nullable final ImageView.ScaleType scaleType)
	{
		if(scaleType==null)
		{
			return false;
		}
		if(scaleType==ImageView.ScaleType.MATRIX)
		{
			throw new IllegalStateException("Matrix scale type is not supported");
		}
		return true;
	}
}
