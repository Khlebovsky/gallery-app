package com.example.gallery;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * A zoomable ImageView. See {@link PhotoViewAttacher} for most of the details on how the zooming
 * is accomplished
 */
@SuppressWarnings({"unused","RedundantSuppression"})
public class PhotoView extends AppCompatImageView
{
	@Nullable
	private PhotoViewAttacher attacher;
	@Nullable
	private ScaleType pendingScaleType;

	public PhotoView(@NonNull final Context context)
	{
		this(context,null);
	}

	public PhotoView(@NonNull final Context context,@Nullable final AttributeSet attr)
	{
		this(context,attr,0);
	}

	public PhotoView(@NonNull final Context context,@Nullable final AttributeSet attr,final int defStyle)
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
		if(attacher!=null)
		{
			return attacher;
		}
		return null;
	}

	public void getDisplayMatrix(Matrix matrix)
	{
		if(attacher!=null)
		{
			attacher.getDisplayMatrix(matrix);
		}
	}

	public RectF getDisplayRect()
	{
		if(attacher!=null)
		{
			return attacher.getDisplayRect();
		}
		return null;
	}

	@Override
	public Matrix getImageMatrix()
	{
		if(attacher!=null)
		{
			return attacher.getImageMatrix();
		}
		return null;
	}

	public float getMaximumScale()
	{
		if(attacher!=null)
		{
			return attacher.getMaximumScale();
		}
		return 0;
	}

	public float getMediumScale()
	{
		if(attacher!=null)
		{
			return attacher.getMediumScale();
		}
		return 0;
	}

	public float getMinimumScale()
	{
		if(attacher!=null)
		{
			return attacher.getMinimumScale();
		}
		return 0;
	}

	public float getScale()
	{
		if(attacher!=null)
		{
			return attacher.getScale();
		}
		return 0;
	}

	@Override
	public ScaleType getScaleType()
	{
		if(attacher!=null)
		{
			return attacher.getScaleType();
		}
		return null;
	}

	public void getSuppMatrix(Matrix matrix)
	{
		if(attacher!=null)
		{
			attacher.getSuppMatrix(matrix);
		}
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
		return attacher!=null&&attacher.isZoomable();
	}

	public void setAllowParentInterceptOnEdge(boolean allow)
	{
		if(attacher!=null)
		{
			attacher.setAllowParentInterceptOnEdge(allow);
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean setDisplayMatrix(Matrix finalRectangle)
	{
		return attacher!=null&&attacher.setDisplayMatrix(finalRectangle);
	}

	@Override
	protected boolean setFrame(int l,int t,int r,int b)
	{
		final boolean changed=super.setFrame(l,t,r,b);
		if(changed&&attacher!=null)
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
		if(attacher!=null)
		{
			attacher.setMaximumScale(maximumScale);
		}
	}

	public void setMediumScale(float mediumScale)
	{
		if(attacher!=null)
		{
			attacher.setMediumScale(mediumScale);
		}
	}

	public void setMinimumScale(float minimumScale)
	{
		if(attacher!=null)
		{
			attacher.setMinimumScale(minimumScale);
		}
	}

	@Override
	public void setOnClickListener(OnClickListener l)
	{
		if(attacher!=null)
		{
			attacher.setOnClickListener(l);
		}
	}

	public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener onDoubleTapListener)
	{
		if(attacher!=null)
		{
			attacher.setOnDoubleTapListener(onDoubleTapListener);
		}
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l)
	{
		if(attacher!=null)
		{
			attacher.setOnLongClickListener(l);
		}
	}

	public void setOnMatrixChangeListener(OnMatrixChangedListener listener)
	{
		if(attacher!=null)
		{
			attacher.setOnMatrixChangeListener(listener);
		}
	}

	public void setOnOutsidePhotoTapListener(OnOutsidePhotoTapListener listener)
	{
		if(attacher!=null)
		{
			attacher.setOnOutsidePhotoTapListener(listener);
		}
	}

	public void setOnPhotoTapListener(OnPhotoTapListener listener)
	{
		if(attacher!=null)
		{
			attacher.setOnPhotoTapListener(listener);
		}
	}

	public void setOnScaleChangeListener(OnScaleChangedListener onScaleChangedListener)
	{
		if(attacher!=null)
		{
			attacher.setOnScaleChangeListener(onScaleChangedListener);
		}
	}

	public void setOnSingleFlingListener(OnSingleFlingListener onSingleFlingListener)
	{
		if(attacher!=null)
		{
			attacher.setOnSingleFlingListener(onSingleFlingListener);
		}
	}

	public void setOnViewDragListener(OnViewDragListener listener)
	{
		if(attacher!=null)
		{
			attacher.setOnViewDragListener(listener);
		}
	}

	public void setOnViewTapListener(OnViewTapListener listener)
	{
		if(attacher!=null)
		{
			attacher.setOnViewTapListener(listener);
		}
	}

	public void setRotationBy(float rotationDegree)
	{
		if(attacher!=null)
		{
			attacher.setRotationBy(rotationDegree);
		}
	}

	public void setRotationTo(float rotationDegree)
	{
		if(attacher!=null)
		{
			attacher.setRotationTo(rotationDegree);
		}
	}

	public void setScale(float scale)
	{
		if(attacher!=null)
		{
			attacher.setScale(scale);
		}
	}

	public void setScale(float scale,boolean animate)
	{
		if(attacher!=null)
		{
			attacher.setScale(scale,animate);
		}
	}

	public void setScale(float scale,float focalX,float focalY,boolean animate)
	{
		if(attacher!=null)
		{
			attacher.setScale(scale,focalX,focalY,animate);
		}
	}

	public void setScaleLevels(float minimumScale,float mediumScale,float maximumScale)
	{
		if(attacher!=null)
		{
			attacher.setScaleLevels(minimumScale,mediumScale,maximumScale);
		}
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
		return attacher!=null&&attacher.setDisplayMatrix(matrix);
	}

	public void setZoomTransitionDuration(int milliseconds)
	{
		if(attacher!=null)
		{
			attacher.setZoomTransitionDuration(milliseconds);
		}
	}

	public void setZoomable(boolean zoomable)
	{
		if(attacher!=null)
		{
			attacher.setZoomable(zoomable);
		}
	}
}
