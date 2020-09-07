package com.example.gallery;

import android.view.View;
import androidx.annotation.Nullable;

public interface OnViewTapListener
{
	/**
	 * A callback to receive where the user taps on a ImageView. You will receive a callback if
	 * the user taps anywhere on the view, tapping on 'whitespace' will not be ignored.
	 * @param view - View the user tapped.
	 * @param x    - where the user tapped from the left of the View.
	 * @param y    - where the user tapped from the top of the View.
	 */
	void onViewTap(@Nullable final View view,final float x,final float y);
}
