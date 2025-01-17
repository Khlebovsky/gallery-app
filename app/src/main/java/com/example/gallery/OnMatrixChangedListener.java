package com.example.gallery;

import android.graphics.RectF;
import androidx.annotation.NonNull;

/**
 * Interface definition for a callback to be invoked when the internal Matrix has changed for
 * this View.
 */
public interface OnMatrixChangedListener
{
	/**
	 * Callback for when the Matrix displaying the Drawable has changed. This could be because
	 * the View's bounds have changed, or the user has zoomed.
	 * @param rect - Rectangle displaying the Drawable's new bounds.
	 */
	void onMatrixChanged(@NonNull final RectF rect);
}
