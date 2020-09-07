package com.example.gallery;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Does a whole lot of gesture detecting.
 */
class CustomGestureDetector
{
	@Nullable
	final OnGestureListener mListener;
	private static final int INVALID_POINTER_ID=-1;
	@Nullable
	private final ScaleGestureDetector mDetector;
	private float mTouchSlop;
	private float mMinimumVelocity;
	private int mActivePointerId=INVALID_POINTER_ID;
	private int mActivePointerIndex;
	@Nullable
	private VelocityTracker mVelocityTracker;
	private boolean mIsDragging;
	private float mLastTouchX;
	private float mLastTouchY;

	CustomGestureDetector(@Nullable final Context context,@Nullable final OnGestureListener listener)
	{
		@Nullable
		final ViewConfiguration configuration=ViewConfiguration.get(context);
		if(configuration!=null)
		{
			mMinimumVelocity=configuration.getScaledMinimumFlingVelocity();
			mTouchSlop=configuration.getScaledTouchSlop();
		}
		mListener=listener;
		@NonNull
		final ScaleGestureDetector.OnScaleGestureListener mScaleListener=new ScaleGestureDetector.OnScaleGestureListener()
		{
			@Override
			public boolean onScale(ScaleGestureDetector detector)
			{
				final float scaleFactor=detector.getScaleFactor();
				if(Float.isNaN(scaleFactor)||Float.isInfinite(scaleFactor))
				{
					return false;
				}
				if(scaleFactor >= 0&&mListener!=null)
				{
					mListener.onScale(scaleFactor,detector.getFocusX(),detector.getFocusY());
				}
				return true;
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector)
			{
				return true;
			}

			@Override
			public void onScaleEnd(ScaleGestureDetector detector)
			{
				// NO-OP
			}
		};
		mDetector=new ScaleGestureDetector(context,mScaleListener);
	}

	private float getActiveX(@NonNull final MotionEvent ev)
	{
		try
		{
			return ev.getX(mActivePointerIndex);
		}
		catch(Exception e)
		{
			return ev.getX();
		}
	}

	private float getActiveY(@NonNull final MotionEvent ev)
	{
		try
		{
			return ev.getY(mActivePointerIndex);
		}
		catch(Exception e)
		{
			return ev.getY();
		}
	}

	public boolean isDragging()
	{
		return mIsDragging;
	}

	public boolean isScaling()
	{
		return mDetector!=null&&mDetector.isInProgress();
	}

	public boolean onTouchEvent(@NonNull final MotionEvent ev)
	{
		try
		{
			if(mDetector!=null)
			{
				mDetector.onTouchEvent(ev);
			}
			return processTouchEvent(ev);
		}
		catch(IllegalArgumentException e)
		{
			// Fix for support lib bug, happening when onDestroy is called
			return true;
		}
	}

	@SuppressWarnings("SameReturnValue")
	private boolean processTouchEvent(@NonNull final MotionEvent ev)
	{
		final int action=ev.getAction();
		switch(action&MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_DOWN:
				mActivePointerId=ev.getPointerId(0);
				mVelocityTracker=VelocityTracker.obtain();
				if(null!=mVelocityTracker)
				{
					mVelocityTracker.addMovement(ev);
				}
				mLastTouchX=getActiveX(ev);
				mLastTouchY=getActiveY(ev);
				mIsDragging=false;
				break;
			case MotionEvent.ACTION_MOVE:
				final float x=getActiveX(ev);
				final float y=getActiveY(ev);
				final float dx=x-mLastTouchX;
				final float dy=y-mLastTouchY;
				if(!mIsDragging)
				{
					// Use Pythagoras to see if drag length is larger than
					// touch slop
					mIsDragging=Math.sqrt((dx*dx)+(dy*dy)) >= mTouchSlop;
				}
				if(mIsDragging)
				{
					if(mListener!=null)
					{
						mListener.onDrag(dx,dy);
					}
					mLastTouchX=x;
					mLastTouchY=y;
					if(null!=mVelocityTracker)
					{
						mVelocityTracker.addMovement(ev);
					}
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				mActivePointerId=INVALID_POINTER_ID;
				// Recycle Velocity Tracker
				if(null!=mVelocityTracker)
				{
					mVelocityTracker.recycle();
					mVelocityTracker=null;
				}
				break;
			case MotionEvent.ACTION_UP:
				mActivePointerId=INVALID_POINTER_ID;
				if(mIsDragging)
				{
					if(null!=mVelocityTracker)
					{
						mLastTouchX=getActiveX(ev);
						mLastTouchY=getActiveY(ev);
						// Compute velocity within the last 1000ms
						mVelocityTracker.addMovement(ev);
						mVelocityTracker.computeCurrentVelocity(1000);
						final float vX=mVelocityTracker.getXVelocity();
						final float vY=mVelocityTracker.getYVelocity();
						// If the velocity is greater than minVelocity, call
						// listener
						if(Math.max(Math.abs(vX),Math.abs(vY)) >= mMinimumVelocity)
						{
							if(mListener!=null)
							{
								mListener.onFling(mLastTouchX,mLastTouchY,-vX,-vY);
							}
						}
					}
				}
				// Recycle Velocity Tracker
				if(null!=mVelocityTracker)
				{
					mVelocityTracker.recycle();
					mVelocityTracker=null;
				}
				break;
			case MotionEvent.ACTION_POINTER_UP:
				final int pointerIndex=Util.getPointerIndex(ev.getAction());
				final int pointerId=ev.getPointerId(pointerIndex);
				if(pointerId==mActivePointerId)
				{
					// This was our active pointer going up. Choose a new
					// active pointer and adjust accordingly.
					final int newPointerIndex=pointerIndex==0?1:0;
					mActivePointerId=ev.getPointerId(newPointerIndex);
					mLastTouchX=ev.getX(newPointerIndex);
					mLastTouchY=ev.getY(newPointerIndex);
				}
				break;
		}
		mActivePointerIndex=ev.findPointerIndex(mActivePointerId==INVALID_POINTER_ID?0:mActivePointerId);
		return true;
	}
}
