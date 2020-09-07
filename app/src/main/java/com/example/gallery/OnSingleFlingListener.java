package com.example.gallery;

import android.view.MotionEvent;
import androidx.annotation.NonNull;

/**
 * A callback to be invoked when the ImageView is flung with a single
 * touch
 */
public interface OnSingleFlingListener
{
	/**
	 * A callback to receive where the user flings on a ImageView. You will receive a callback if
	 * the user flings anywhere on the view.
	 * @param e1        MotionEvent the user first touch.
	 * @param e2        MotionEvent the user last touch.
	 * @param velocityX distance of user's horizontal fling.
	 * @param velocityY distance of user's vertical fling.
	 */
	boolean onFling(@NonNull final MotionEvent e1,@NonNull final MotionEvent e2,final float velocityX,final float velocityY);
}
