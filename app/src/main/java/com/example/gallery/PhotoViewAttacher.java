package com.example.gallery;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.OverScroller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The component of {@link PhotoView} which does the work allowing for zooming, scaling, panning, etc.
 * It is made public in case you need to subclass something other than AppCompatImageView and still
 * gain the functionality that {@link PhotoView} offers
 */
@SuppressWarnings({"unused","RedundantSuppression"})
public class PhotoViewAttacher implements View.OnTouchListener, View.OnLayoutChangeListener
{
	static final float DEFAULT_MIN_SCALE=1.0f;
	static final int SINGLE_TOUCH=1;
	@NonNull
	final Matrix mSuppMatrix=new Matrix();
	@Nullable
	ImageView mImageView;
	@NonNull
	Interpolator mInterpolator=new AccelerateDecelerateInterpolator();
	int mZoomDuration=DEFAULT_ZOOM_DURATION;
	float mMaxScale=DEFAULT_MAX_SCALE;
	boolean mAllowParentInterceptOnEdge=true;
	boolean mBlockParentIntercept;
	@Nullable
	CustomGestureDetector mScaleDragDetector;
	@Nullable
	OnPhotoTapListener mPhotoTapListener;
	@Nullable
	OnOutsidePhotoTapListener mOutsidePhotoTapListener;
	@Nullable
	OnViewTapListener mViewTapListener;
	@Nullable
	View.OnClickListener mOnClickListener;
	@Nullable
	OnLongClickListener mLongClickListener;
	@Nullable
	OnScaleChangedListener mScaleChangeListener;
	@Nullable
	OnSingleFlingListener mSingleFlingListener;
	@Nullable
	OnViewDragListener mOnViewDragListener;
	@Nullable
	FlingRunnable mCurrentFlingRunnable;
	int mHorizontalScrollEdge=HORIZONTAL_EDGE_BOTH;
	int mVerticalScrollEdge=VERTICAL_EDGE_BOTH;
	private static final int HORIZONTAL_EDGE_NONE=-1;
	private static final int HORIZONTAL_EDGE_LEFT=0;
	private static final int HORIZONTAL_EDGE_RIGHT=1;
	private static final int HORIZONTAL_EDGE_BOTH=2;
	private static final int VERTICAL_EDGE_NONE=-1;
	private static final int VERTICAL_EDGE_TOP=0;
	private static final int VERTICAL_EDGE_BOTTOM=1;
	private static final int VERTICAL_EDGE_BOTH=2;
	private static final float DEFAULT_MAX_SCALE=3.0f;
	private static final float DEFAULT_MID_SCALE=1.75f;
	private static final int DEFAULT_ZOOM_DURATION=200;
	// These are set so we don't keep allocating them on the heap
	private final Matrix mBaseMatrix=new Matrix();
	private final Matrix mDrawMatrix=new Matrix();
	private final RectF mDisplayRect=new RectF();
	private final float[] mMatrixValues=new float[9];
	private float mMinScale=DEFAULT_MIN_SCALE;
	private float mMidScale=DEFAULT_MID_SCALE;
	// Gesture Detectors
	@Nullable
	private GestureDetector mGestureDetector;
	// Listeners
	@Nullable
	private OnMatrixChangedListener mMatrixChangeListener;
	private float mBaseRotation;
	private boolean mZoomEnabled=true;
	@NonNull
	private ScaleType mScaleType=ScaleType.FIT_CENTER;
	@NonNull
	final OnGestureListener onGestureListener=new OnGestureListener()
	{
		@Override
		public void onDrag(float dx,float dy)
		{
			if(mScaleDragDetector.isScaling())
			{
				return; // Do not drag if we are already scaling
			}
			if(mOnViewDragListener!=null)
			{
				mOnViewDragListener.onDrag(dx,dy);
			}
			mSuppMatrix.postTranslate(dx,dy);
			checkAndDisplayMatrix();

			/*
			 * Here we decide whether to let the ImageView's parent to start taking
			 * over the touch event.
			 *
			 * First we check whether this function is enabled. We never want the
			 * parent to take over if we're scaling. We then check the edge we're
			 * on, and the direction of the scroll (i.e. if we're pulling against
			 * the edge, aka 'overscrolling', let the parent take over).
			 */
			@Nullable
			final ViewParent parent=mImageView!=null?mImageView.getParent():null;
			if(mAllowParentInterceptOnEdge&&!mScaleDragDetector.isScaling()&&!mBlockParentIntercept)
			{
				if(mHorizontalScrollEdge==HORIZONTAL_EDGE_BOTH||(mHorizontalScrollEdge==HORIZONTAL_EDGE_LEFT&&dx >= 1f)||(mHorizontalScrollEdge==HORIZONTAL_EDGE_RIGHT&&dx<=-1f)||(mVerticalScrollEdge==VERTICAL_EDGE_TOP&&dy >= 1f)||
					(mVerticalScrollEdge==VERTICAL_EDGE_BOTTOM&&dy<=-1f))
				{
					if(parent!=null)
					{
						parent.requestDisallowInterceptTouchEvent(false);
					}
				}
			}
			else
			{
				if(parent!=null)
				{
					parent.requestDisallowInterceptTouchEvent(true);
				}
			}
		}

		@Override
		public void onFling(float startX,float startY,float velocityX,float velocityY)
		{
			if(mImageView!=null)
			{
				mCurrentFlingRunnable=new FlingRunnable(mImageView.getContext());
				mCurrentFlingRunnable.fling(getImageViewWidth(mImageView),getImageViewHeight(mImageView),(int)velocityX,(int)velocityY);
				mImageView.post(mCurrentFlingRunnable);
			}
		}

		@Override
		public void onScale(float scaleFactor,float focusX,float focusY)
		{
			if(getScale()<mMaxScale||scaleFactor<1f)
			{
				if(mScaleChangeListener!=null)
				{
					mScaleChangeListener.onScaleChange(scaleFactor,focusX,focusY);
				}
				mSuppMatrix.postScale(scaleFactor,scaleFactor,focusX,focusY);
				checkAndDisplayMatrix();
			}
		}
	};

	public PhotoViewAttacher(@Nullable final ImageView imageView)
	{
		if(imageView!=null)
		{
			mImageView=imageView;
			imageView.setOnTouchListener(this);
			imageView.addOnLayoutChangeListener(this);
			if(imageView.isInEditMode())
			{
				return;
			}
			mBaseRotation=0.0f;
			// Create Gesture Detectors...
			mScaleDragDetector=new CustomGestureDetector(imageView.getContext(),onGestureListener);
			mGestureDetector=new GestureDetector(imageView.getContext(),new GestureDetector.SimpleOnGestureListener()
			{
				@Override
				public boolean onFling(MotionEvent e1,MotionEvent e2,float velocityX,float velocityY)
				{
					if(mSingleFlingListener!=null)
					{
						if(getScale()>DEFAULT_MIN_SCALE)
						{
							return false;
						}
						if(e1.getPointerCount()>SINGLE_TOUCH||e2.getPointerCount()>SINGLE_TOUCH)
						{
							return false;
						}
						return mSingleFlingListener.onFling(e1,e2,velocityX,velocityY);
					}
					return false;
				}

				// forward long click listener
				@Override
				public void onLongPress(MotionEvent e)
				{
					if(mLongClickListener!=null)
					{
						mLongClickListener.onLongClick(mImageView);
					}
				}
			});
			mGestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener()
			{
				@Override
				public boolean onDoubleTap(MotionEvent ev)
				{
					try
					{
						final float scale=getScale();
						final float x=ev.getX();
						final float y=ev.getY();
						if(scale<getMediumScale())
						{
							setScale(getMediumScale(),x,y,true);
						}
						else if(scale >= getMediumScale()&&scale<getMaximumScale())
						{
							setScale(getMaximumScale(),x,y,true);
						}
						else
						{
							setScale(getMinimumScale(),x,y,true);
						}
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						// Can sometimes happen when getX() and getY() is called
					}
					return true;
				}

				@Override
				public boolean onDoubleTapEvent(MotionEvent e)
				{
					// Wait for the confirmed onDoubleTap() instead
					return false;
				}

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e)
				{
					if(mOnClickListener!=null)
					{
						mOnClickListener.onClick(mImageView);
					}
					final RectF displayRect=getDisplayRect();
					final float x=e.getX();
					final float y=e.getY();
					if(mViewTapListener!=null)
					{
						mViewTapListener.onViewTap(mImageView,x,y);
					}
					if(displayRect!=null)
					{
						// Check to see if the user tapped on the photo
						if(displayRect.contains(x,y))
						{
							final float xResult=(x-displayRect.left)/displayRect.width();
							final float yResult=(y-displayRect.top)/displayRect.height();
							if(mPhotoTapListener!=null)
							{
								mPhotoTapListener.onPhotoTap(mImageView,xResult,yResult);
							}
							return true;
						}
						else
						{
							if(mOutsidePhotoTapListener!=null)
							{
								mOutsidePhotoTapListener.onOutsidePhotoTap(mImageView);
							}
						}
					}
					return false;
				}
			});
		}
	}

	private void cancelFling()
	{
		if(mCurrentFlingRunnable!=null)
		{
			mCurrentFlingRunnable.cancelFling();
			mCurrentFlingRunnable=null;
		}
	}

	/**
	 * Helper method that simply checks the Matrix, and then displays the result
	 */
	void checkAndDisplayMatrix()
	{
		if(checkMatrixBounds())
		{
			setImageViewMatrix(getDrawMatrix());
		}
	}

	private boolean checkMatrixBounds()
	{
		final RectF rect=getDisplayRect(getDrawMatrix());
		if(rect==null)
		{
			return false;
		}
		final float height=rect.height();
		final float width=rect.width();
		float deltaX=0;
		float deltaY=0;
		int viewHeight=0;
		if(mImageView!=null)
		{
			viewHeight=getImageViewHeight(mImageView);
		}
		if(height<=viewHeight)
		{
			switch(mScaleType)
			{
				case FIT_START:
					deltaY=-rect.top;
					break;
				case FIT_END:
					deltaY=viewHeight-height-rect.top;
					break;
				default:
					deltaY=(viewHeight-height)/2-rect.top;
					break;
			}
			mVerticalScrollEdge=VERTICAL_EDGE_BOTH;
		}
		else if(rect.top>0)
		{
			mVerticalScrollEdge=VERTICAL_EDGE_TOP;
			deltaY=-rect.top;
		}
		else if(rect.bottom<viewHeight)
		{
			mVerticalScrollEdge=VERTICAL_EDGE_BOTTOM;
			deltaY=viewHeight-rect.bottom;
		}
		else
		{
			mVerticalScrollEdge=VERTICAL_EDGE_NONE;
		}
		int viewWidth=0;
		if(mImageView!=null)
		{
			viewWidth=getImageViewWidth(mImageView);
		}
		if(width<=viewWidth)
		{
			switch(mScaleType)
			{
				case FIT_START:
					deltaX=-rect.left;
					break;
				case FIT_END:
					deltaX=viewWidth-width-rect.left;
					break;
				default:
					deltaX=(viewWidth-width)/2-rect.left;
					break;
			}
			mHorizontalScrollEdge=HORIZONTAL_EDGE_BOTH;
		}
		else if(rect.left>0)
		{
			mHorizontalScrollEdge=HORIZONTAL_EDGE_LEFT;
			deltaX=-rect.left;
		}
		else if(rect.right<viewWidth)
		{
			deltaX=viewWidth-rect.right;
			mHorizontalScrollEdge=HORIZONTAL_EDGE_RIGHT;
		}
		else
		{
			mHorizontalScrollEdge=HORIZONTAL_EDGE_NONE;
		}
		// Finally actually translate the matrix
		mSuppMatrix.postTranslate(deltaX,deltaY);
		return true;
	}

	/**
	 * Get the display matrix
	 * @param matrix target matrix to copy to
	 */
	public void getDisplayMatrix(@NonNull final Matrix matrix)
	{
		matrix.set(getDrawMatrix());
	}

	public RectF getDisplayRect()
	{
		checkMatrixBounds();
		return getDisplayRect(getDrawMatrix());
	}

	/**
	 * Helper method that maps the supplied Matrix to the current Drawable
	 * @param matrix - Matrix to map Drawable against
	 * @return RectF - Displayed Rectangle
	 */
	private RectF getDisplayRect(@NonNull final Matrix matrix)
	{
		Drawable d=null;
		if(mImageView!=null)
		{
			d=mImageView.getDrawable();
		}
		if(d!=null)
		{
			mDisplayRect.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
			matrix.mapRect(mDisplayRect);
			return mDisplayRect;
		}
		return null;
	}

	private Matrix getDrawMatrix()
	{
		mDrawMatrix.set(mBaseMatrix);
		mDrawMatrix.postConcat(mSuppMatrix);
		return mDrawMatrix;
	}

	public Matrix getImageMatrix()
	{
		return mDrawMatrix;
	}

	static int getImageViewHeight(@Nullable final ImageView imageView)
	{
		return (imageView!=null?imageView.getHeight():0)-(imageView!=null?imageView.getPaddingTop():0)-(imageView!=null?imageView.getPaddingBottom():0);
	}

	static int getImageViewWidth(@Nullable final ImageView imageView)
	{
		return (imageView!=null?imageView.getWidth():0)-(imageView!=null?imageView.getPaddingLeft():0)-(imageView!=null?imageView.getPaddingRight():0);
	}

	public float getMaximumScale()
	{
		return mMaxScale;
	}

	public float getMediumScale()
	{
		return mMidScale;
	}

	public float getMinimumScale()
	{
		return mMinScale;
	}

	public float getScale()
	{
		return (float)Math.sqrt((float)Math.pow(getValue(mSuppMatrix,Matrix.MSCALE_X),2)+(float)Math.pow(getValue(mSuppMatrix,Matrix.MSKEW_Y),2));
	}

	public ScaleType getScaleType()
	{
		return mScaleType;
	}

	/**
	 * Get the current support matrix
	 */
	public void getSuppMatrix(@NonNull final Matrix matrix)
	{
		matrix.set(mSuppMatrix);
	}

	/**
	 * Helper method that 'unpacks' a Matrix and returns the required value
	 * @param matrix     Matrix to unpack
	 * @param whichValue Which value from Matrix.M* to return
	 * @return returned value
	 */
	private float getValue(@NonNull final Matrix matrix,final int whichValue)
	{
		matrix.getValues(mMatrixValues);
		return mMatrixValues[whichValue];
	}

	@Deprecated
	public boolean isZoomEnabled()
	{
		return mZoomEnabled;
	}

	public boolean isZoomable()
	{
		return mZoomEnabled;
	}

	@Override
	public void onLayoutChange(final View v,final int left,final int top,final int right,final int bottom,final int oldLeft,final int oldTop,final int oldRight,final int oldBottom)
	{
		// Update our base matrix, as the bounds have changed
		if(left!=oldLeft||top!=oldTop||right!=oldRight||bottom!=oldBottom)
		{
			if(mImageView!=null)
			{
				updateBaseMatrix(mImageView.getDrawable());
			}
		}
	}

	@Override
	public boolean onTouch(final View v,final MotionEvent ev)
	{
		boolean handled=false;
		if(mZoomEnabled&&Util.hasDrawable((ImageView)v))
		{
			switch(ev.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					final ViewParent parent=v.getParent();
					// First, disable the Parent from intercepting the touch
					// event
					if(parent!=null)
					{
						parent.requestDisallowInterceptTouchEvent(true);
					}
					// If we're flinging, and the user presses down, cancel
					// fling
					cancelFling();
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					// If the user has zoomed less than min scale, zoom back
					// to min scale
					if(getScale()<mMinScale)
					{
						final RectF rect=getDisplayRect();
						if(rect!=null)
						{
							v.post(new AnimatedZoomRunnable(getScale(),mMinScale,rect.centerX(),rect.centerY()));
							handled=true;
						}
					}
					else if(getScale()>mMaxScale)
					{
						final RectF rect=getDisplayRect();
						if(rect!=null)
						{
							v.post(new AnimatedZoomRunnable(getScale(),mMaxScale,rect.centerX(),rect.centerY()));
							handled=true;
						}
					}
					break;
			}
			// Try the Scale/Drag detector
			if(mScaleDragDetector!=null)
			{
				final boolean wasScaling=mScaleDragDetector.isScaling();
				final boolean wasDragging=mScaleDragDetector.isDragging();
				handled=mScaleDragDetector.onTouchEvent(ev);
				final boolean didntScale=!wasScaling&&!mScaleDragDetector.isScaling();
				final boolean didntDrag=!wasDragging&&!mScaleDragDetector.isDragging();
				mBlockParentIntercept=didntScale&&didntDrag;
			}
			// Check to see if the user double tapped
			if(mGestureDetector!=null&&mGestureDetector.onTouchEvent(ev))
			{
				handled=true;
			}
		}
		return handled;
	}

	/**
	 * Resets the Matrix back to FIT_CENTER, and then displays its contents
	 */
	private void resetMatrix()
	{
		mSuppMatrix.reset();
		setRotationBy(mBaseRotation);
		setImageViewMatrix(getDrawMatrix());
		checkMatrixBounds();
	}

	public void setAllowParentInterceptOnEdge(final boolean allow)
	{
		mAllowParentInterceptOnEdge=allow;
	}

	public void setBaseRotation(final float degrees)
	{
		mBaseRotation=degrees%360;
		update();
		setRotationBy(mBaseRotation);
		checkAndDisplayMatrix();
	}

	public boolean setDisplayMatrix(@NonNull final Matrix finalMatrix)
	{
		if(mImageView!=null&&mImageView.getDrawable()==null)
		{
			return false;
		}
		mSuppMatrix.set(finalMatrix);
		checkAndDisplayMatrix();
		return true;
	}

	private void setImageViewMatrix(@NonNull final Matrix matrix)
	{
		if(mImageView!=null)
		{
			mImageView.setImageMatrix(matrix);
		}
		// Call MatrixChangedListener if needed
		if(mMatrixChangeListener!=null)
		{
			final RectF displayRect=getDisplayRect(matrix);
			if(displayRect!=null)
			{
				mMatrixChangeListener.onMatrixChanged(displayRect);
			}
		}
	}

	public void setMaximumScale(final float maximumScale)
	{
		Util.checkZoomLevels(mMinScale,mMidScale,maximumScale);
		mMaxScale=maximumScale;
	}

	public void setMediumScale(final float mediumScale)
	{
		Util.checkZoomLevels(mMinScale,mediumScale,mMaxScale);
		mMidScale=mediumScale;
	}

	public void setMinimumScale(final float minimumScale)
	{
		Util.checkZoomLevels(minimumScale,mMidScale,mMaxScale);
		mMinScale=minimumScale;
	}

	public void setOnClickListener(@NonNull final View.OnClickListener listener)
	{
		mOnClickListener=listener;
	}

	public void setOnDoubleTapListener(@NonNull final GestureDetector.OnDoubleTapListener newOnDoubleTapListener)
	{
		if(mGestureDetector!=null)
		{
			mGestureDetector.setOnDoubleTapListener(newOnDoubleTapListener);
		}
	}

	public void setOnLongClickListener(@NonNull final OnLongClickListener listener)
	{
		mLongClickListener=listener;
	}

	public void setOnMatrixChangeListener(@NonNull final OnMatrixChangedListener listener)
	{
		mMatrixChangeListener=listener;
	}

	public void setOnOutsidePhotoTapListener(@NonNull final OnOutsidePhotoTapListener mOutsidePhotoTapListener)
	{
		this.mOutsidePhotoTapListener=mOutsidePhotoTapListener;
	}

	public void setOnPhotoTapListener(@NonNull final OnPhotoTapListener listener)
	{
		mPhotoTapListener=listener;
	}

	public void setOnScaleChangeListener(@NonNull final OnScaleChangedListener onScaleChangeListener)
	{
		mScaleChangeListener=onScaleChangeListener;
	}

	public void setOnSingleFlingListener(@NonNull final OnSingleFlingListener onSingleFlingListener)
	{
		mSingleFlingListener=onSingleFlingListener;
	}

	public void setOnViewDragListener(@NonNull final OnViewDragListener listener)
	{
		mOnViewDragListener=listener;
	}

	public void setOnViewTapListener(@NonNull final OnViewTapListener listener)
	{
		mViewTapListener=listener;
	}

	public void setRotationBy(final float degrees)
	{
		mSuppMatrix.postRotate(degrees%360);
		checkAndDisplayMatrix();
	}

	public void setRotationTo(final float degrees)
	{
		mSuppMatrix.setRotate(degrees%360);
		checkAndDisplayMatrix();
	}

	public void setScale(final float scale)
	{
		setScale(scale,false);
	}

	public void setScale(final float scale,final boolean animate)
	{
		//noinspection IntegerDivisionInFloatingPointContext
		setScale(scale,(mImageView!=null?mImageView.getRight():0)/2,(mImageView!=null?mImageView.getBottom():0)/2,animate);
	}

	public void setScale(final float scale,final float focalX,final float focalY,final boolean animate)
	{
		// Check to see if the scale is within bounds
		if(scale<mMinScale||scale>mMaxScale)
		{
			throw new IllegalArgumentException("Scale must be within the range of minScale and maxScale");
		}
		if(animate&&mImageView!=null)
		{
			mImageView.post(new AnimatedZoomRunnable(getScale(),scale,focalX,focalY));
		}
		else
		{
			mSuppMatrix.setScale(scale,scale,focalX,focalY);
			checkAndDisplayMatrix();
		}
	}

	public void setScaleLevels(final float minimumScale,final float mediumScale,final float maximumScale)
	{
		Util.checkZoomLevels(minimumScale,mediumScale,maximumScale);
		mMinScale=minimumScale;
		mMidScale=mediumScale;
		mMaxScale=maximumScale;
	}

	public void setScaleType(@NonNull final ScaleType scaleType)
	{
		if(Util.isSupportedScaleType(scaleType)&&scaleType!=mScaleType)
		{
			mScaleType=scaleType;
			update();
		}
	}

	/**
	 * Set the zoom interpolator
	 * @param interpolator the zoom interpolator
	 */
	public void setZoomInterpolator(@NonNull final Interpolator interpolator)
	{
		mInterpolator=interpolator;
	}

	public void setZoomTransitionDuration(final int milliseconds)
	{
		mZoomDuration=milliseconds;
	}

	public void setZoomable(final boolean zoomable)
	{
		mZoomEnabled=zoomable;
		update();
	}

	public void update()
	{
		if(mZoomEnabled&&mImageView!=null)
		{
			// Update the base matrix using the current drawable
			updateBaseMatrix(mImageView.getDrawable());
		}
		else
		{
			// Reset the Matrix...
			resetMatrix();
		}
	}

	/**
	 * Calculate Matrix for FIT_CENTER
	 * @param drawable - Drawable being displayed
	 */
	private void updateBaseMatrix(@Nullable final Drawable drawable)
	{
		if(drawable==null)
		{
			return;
		}
		final float viewWidth=getImageViewWidth(mImageView);
		final float viewHeight=getImageViewHeight(mImageView);
		final int drawableWidth=drawable.getIntrinsicWidth();
		final int drawableHeight=drawable.getIntrinsicHeight();
		mBaseMatrix.reset();
		final float widthScale=viewWidth/drawableWidth;
		final float heightScale=viewHeight/drawableHeight;
		if(mScaleType==ScaleType.CENTER)
		{
			mBaseMatrix.postTranslate((viewWidth-drawableWidth)/2F,(viewHeight-drawableHeight)/2F);
		}
		else if(mScaleType==ScaleType.CENTER_CROP)
		{
			final float scale=Math.max(widthScale,heightScale);
			mBaseMatrix.postScale(scale,scale);
			mBaseMatrix.postTranslate((viewWidth-drawableWidth*scale)/2F,(viewHeight-drawableHeight*scale)/2F);
		}
		else if(mScaleType==ScaleType.CENTER_INSIDE)
		{
			final float scale=Math.min(1.0f,Math.min(widthScale,heightScale));
			mBaseMatrix.postScale(scale,scale);
			mBaseMatrix.postTranslate((viewWidth-drawableWidth*scale)/2F,(viewHeight-drawableHeight*scale)/2F);
		}
		else
		{
			RectF mTempSrc=new RectF(0,0,drawableWidth,drawableHeight);
			final RectF mTempDst=new RectF(0,0,viewWidth,viewHeight);
			if((int)mBaseRotation%180!=0)
			{
				//noinspection SuspiciousNameCombination
				mTempSrc=new RectF(0,0,drawableHeight,drawableWidth);
			}
			switch(mScaleType)
			{
				case FIT_CENTER:
					mBaseMatrix.setRectToRect(mTempSrc,mTempDst,ScaleToFit.CENTER);
					break;
				case FIT_START:
					mBaseMatrix.setRectToRect(mTempSrc,mTempDst,ScaleToFit.START);
					break;
				case FIT_END:
					mBaseMatrix.setRectToRect(mTempSrc,mTempDst,ScaleToFit.END);
					break;
				case FIT_XY:
					mBaseMatrix.setRectToRect(mTempSrc,mTempDst,ScaleToFit.FILL);
					break;
				default:
					break;
			}
		}
		resetMatrix();
	}

	private final class AnimatedZoomRunnable implements Runnable
	{
		private final float mFocalX;
		private final float mFocalY;
		private final long mStartTime;
		private final float mZoomStart;
		private final float mZoomEnd;

		AnimatedZoomRunnable(final float currentZoom,final float targetZoom,final float focalX,final float focalY)
		{
			mFocalX=focalX;
			mFocalY=focalY;
			mStartTime=System.currentTimeMillis();
			mZoomStart=currentZoom;
			mZoomEnd=targetZoom;
		}

		private float interpolate()
		{
			float t=1f*(System.currentTimeMillis()-mStartTime)/mZoomDuration;
			t=Math.min(1f,t);
			t=mInterpolator.getInterpolation(t);
			return t;
		}

		@Override
		public void run()
		{
			final float t=interpolate();
			final float scale=mZoomStart+t*(mZoomEnd-mZoomStart);
			final float deltaScale=scale/getScale();
			onGestureListener.onScale(deltaScale,mFocalX,mFocalY);
			// We haven't hit our target scale yet, so post ourselves again
			if(t<1f)
			{
				Compat.postOnAnimation(mImageView,this);
			}
		}
	}

	private final class FlingRunnable implements Runnable
	{
		private final OverScroller mScroller;
		private int mCurrentX;
		private int mCurrentY;

		FlingRunnable(@NonNull final Context context)
		{
			mScroller=new OverScroller(context);
		}

		public void cancelFling()
		{
			mScroller.forceFinished(true);
		}

		public void fling(final int viewWidth,final int viewHeight,final int velocityX,final int velocityY)
		{
			final RectF rect=getDisplayRect();
			if(rect==null)
			{
				return;
			}
			final int startX=Math.round(-rect.left);
			final int minX;
			final int maxX;
			final int minY;
			final int maxY;
			if(viewWidth<rect.width())
			{
				minX=0;
				maxX=Math.round(rect.width()-viewWidth);
			}
			else
			{
				minX=maxX=startX;
			}
			final int startY=Math.round(-rect.top);
			if(viewHeight<rect.height())
			{
				minY=0;
				maxY=Math.round(rect.height()-viewHeight);
			}
			else
			{
				minY=maxY=startY;
			}
			mCurrentX=startX;
			mCurrentY=startY;
			// If we actually can move, fling the scroller
			if(startX!=maxX||startY!=maxY)
			{
				mScroller.fling(startX,startY,velocityX,velocityY,minX,maxX,minY,maxY,0,0);
			}
		}

		@Override
		public void run()
		{
			if(mScroller.isFinished())
			{
				return; // remaining post that should not be handled
			}
			if(mScroller.computeScrollOffset())
			{
				final int newX=mScroller.getCurrX();
				final int newY=mScroller.getCurrY();
				mSuppMatrix.postTranslate(mCurrentX-newX,mCurrentY-newY);
				checkAndDisplayMatrix();
				mCurrentX=newX;
				mCurrentY=newY;
				// Post On animation
				Compat.postOnAnimation(mImageView,this);
			}
		}
	}
}
