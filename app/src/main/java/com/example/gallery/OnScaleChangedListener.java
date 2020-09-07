package com.example.gallery;

/**
 * Interface definition for callback to be invoked when attached ImageView scale changes
 */
public interface OnScaleChangedListener
{
	/**
	 * Callback for when the scale changes
	 * @param scaleFactor the scale factor (less than 1 for zoom out, greater than 1 for zoom in)
	 * @param focusX      focal point X position
	 * @param focusY      focal point Y position
	 */
	void onScaleChange(final float scaleFactor,final float focusX,final float focusY);
}
