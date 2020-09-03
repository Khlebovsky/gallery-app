package com.example.gallery;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * A zoomable ImageView. See {@link PhotoViewAttacher} for most of the details on how the zooming
 * is accomplished
 */
@SuppressWarnings({"unused","RedundantSuppression"})
public class PhotoView extends AppCompatImageView
{
	private PhotoViewAttacher attacher;
	private ScaleType pendingScaleType;

	public PhotoView(Context context)
	{
		this(context,null);
	}

	public PhotoView(Context context,AttributeSet attr)
	{
		this(context,attr,0);
	}

	public PhotoView(Context context,AttributeSet attr,int defStyle)
	{
		super(context,attr,defStyle);
		init();
	}

	/**
	 * Get the current {@link PhotoViewAttacher} for this view. Be wary of holding on to references
	 * to this attacher, as it has a reference to this view, which, if a reference is held in the
	 * wrong place, can cause memory leaks.
	 * @return the attacher.
	 */
	public PhotoViewAttacher getAttacher()
	{
		return attacher;
	}

	public void getDisplayMatrix(Matrix matrix)
	{
		attacher.getDisplayMatrix(matrix);
	}

	public RectF getDisplayRect()
	{
		return attacher.getDisplayRect();
	}

	@Override
	public Matrix getImageMatrix()
	{
		return attacher.getImageMatrix();
	}

	public float getMaximumScale()
	{
		return attacher.getMaximumScale();
	}

	public float getMediumScale()
	{
		return attacher.getMediumScale();
	}

	public float getMinimumScale()
	{
		return attacher.getMinimumScale();
	}

	public float getScale()
	{
		return attacher.getScale();
	}

	@Override
	public ScaleType getScaleType()
	{
		return attacher.getScaleType();
	}

	public void getSuppMatrix(Matrix matrix)
	{
		attacher.getSuppMatrix(matrix);
	}

	private void init()
	{
		attacher=new PhotoViewAttacher(this);
		//We always pose as a Matrix scale type, though we can change to another scale type
		//via the attacher
		super.setScaleType(ScaleType.MATRIX);
		//apply the previously applied scale type
		if(pendingScaleType!=null)
		{
			setScaleType(pendingScaleType);
			pendingScaleType=null;
		}
	}

	public boolean isZoomable()
	{
		return attacher.isZoomable();
	}

	public void setAllowParentInterceptOnEdge(boolean allow)
	{
		attacher.setAllowParentInterceptOnEdge(allow);
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean setDisplayMatrix(Matrix finalRectangle)
	{
		return attacher.setDisplayMatrix(finalRectangle);
	}

	@Override
	protected boolean setFrame(int l,int t,int r,int b)
	{
		final boolean changed=super.setFrame(l,t,r,b);
		if(changed)
		{
			attacher.update();
		}
		return changed;
	}

	@Override
	public void setImageDrawable(Drawable drawable)
	{
		super.setImageDrawable(drawable);
		// setImageBitmap calls through to this method
		if(attacher!=null)
		{
			attacher.update();
		}
	}

	@Override
	public void setImageResource(int resId)
	{
		super.setImageResource(resId);
		if(attacher!=null)
		{
			attacher.update();
		}
	}

	@Override
	public void setImageURI(Uri uri)
	{
		super.setImageURI(uri);
		if(attacher!=null)
		{
			attacher.update();
		}
	}

	public void setMaximumScale(float maximumScale)
	{
		attacher.setMaximumScale(maximumScale);
	}

	public void setMediumScale(float mediumScale)
	{
		attacher.setMediumScale(mediumScale);
	}

	public void setMinimumScale(float minimumScale)
	{
		attacher.setMinimumScale(minimumScale);
	}

	@Override
	public void setOnClickListener(OnClickListener l)
	{
		attacher.setOnClickListener(l);
	}

	public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener onDoubleTapListener)
	{
		attacher.setOnDoubleTapListener(onDoubleTapListener);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l)
	{
		attacher.setOnLongClickListener(l);
	}

	public void setOnMatrixChangeListener(OnMatrixChangedListener listener)
	{
		attacher.setOnMatrixChangeListener(listener);
	}

	public void setOnOutsidePhotoTapListener(OnOutsidePhotoTapListener listener)
	{
		attacher.setOnOutsidePhotoTapListener(listener);
	}

	public void setOnPhotoTapListener(OnPhotoTapListener listener)
	{
		attacher.setOnPhotoTapListener(listener);
	}

	public void setOnScaleChangeListener(OnScaleChangedListener onScaleChangedListener)
	{
		attacher.setOnScaleChangeListener(onScaleChangedListener);
	}

	public void setOnSingleFlingListener(OnSingleFlingListener onSingleFlingListener)
	{
		attacher.setOnSingleFlingListener(onSingleFlingListener);
	}

	public void setOnViewDragListener(OnViewDragListener listener)
	{
		attacher.setOnViewDragListener(listener);
	}

	public void setOnViewTapListener(OnViewTapListener listener)
	{
		attacher.setOnViewTapListener(listener);
	}

	public void setRotationBy(float rotationDegree)
	{
		attacher.setRotationBy(rotationDegree);
	}

	public void setRotationTo(float rotationDegree)
	{
		attacher.setRotationTo(rotationDegree);
	}

	public void setScale(float scale)
	{
		attacher.setScale(scale);
	}

	public void setScale(float scale,boolean animate)
	{
		attacher.setScale(scale,animate);
	}

	public void setScale(float scale,float focalX,float focalY,boolean animate)
	{
		attacher.setScale(scale,focalX,focalY,animate);
	}

	public void setScaleLevels(float minimumScale,float mediumScale,float maximumScale)
	{
		attacher.setScaleLevels(minimumScale,mediumScale,maximumScale);
	}

	@Override
	public void setScaleType(ScaleType scaleType)
	{
		if(attacher==null)
		{
			pendingScaleType=scaleType;
		}
		else
		{
			attacher.setScaleType(scaleType);
		}
	}

	public boolean setSuppMatrix(Matrix matrix)
	{
		return attacher.setDisplayMatrix(matrix);
	}

	public void setZoomTransitionDuration(int milliseconds)
	{
		attacher.setZoomTransitionDuration(milliseconds);
	}

	public void setZoomable(boolean zoomable)
	{
		attacher.setZoomable(zoomable);
	}
}
