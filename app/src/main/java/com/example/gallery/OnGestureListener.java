package com.example.gallery;

interface OnGestureListener
{
	void onDrag(float dx,float dy);

	@SuppressWarnings({"unused","RedundantSuppression"})
	void onFling(float startX,float startY,float velocityX,float velocityY);

	void onScale(float scaleFactor,float focusX,float focusY);
}