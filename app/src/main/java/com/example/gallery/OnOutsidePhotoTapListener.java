package com.example.gallery;

import android.widget.ImageView;
import androidx.annotation.Nullable;

/**
 * Callback when the user tapped outside of the photo
 */
public interface OnOutsidePhotoTapListener
{
	/**
	 * The outside of the photo has been tapped
	 */
	void onOutsidePhotoTap(@Nullable final ImageView imageView);
}
