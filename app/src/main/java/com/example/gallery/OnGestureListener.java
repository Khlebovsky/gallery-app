package com.example.gallery;

interface OnGestureListener
{
	void onDrag(final float dx,final float dy);

	@SuppressWarnings({"unused","RedundantSuppression"})
	void onFling(final float startX,final float startY,final float velocityX,final float velocityY);

	void onScale(final float scaleFactor,final float focusX,final float focusY);
}