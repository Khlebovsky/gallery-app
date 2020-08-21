package com.example.gallery;

import android.view.MotionEvent;

public final class SwipeDetector
{
	private static final int SWIPE_MIN_DISTANCE=120;
	private static final int SWIPE_THRESHOLD_VELOCITY=200;
	private final int swipe_distance;
	private final int swipe_velocity;

	@SuppressWarnings("unused")
	public SwipeDetector(int distance,int velocity)
	{
		swipe_distance=distance;
		swipe_velocity=velocity;
	}

	public SwipeDetector()
	{
		swipe_distance=SWIPE_MIN_DISTANCE;
		swipe_velocity=SWIPE_THRESHOLD_VELOCITY;
	}

	private boolean isSwipe(float coordinateA,float coordinateB,float velocity)
	{
		return isSwipeDistance(coordinateA,coordinateB)&&isSwipeSpeed(velocity);
	}

	private boolean isSwipeDistance(float coordinateA,float coordinateB)
	{
		return (coordinateA-coordinateB)>swipe_distance;
	}

	public boolean isSwipeDown(MotionEvent e1,MotionEvent e2,float velocityY)
	{
		return isSwipe(e2.getY(),e1.getY(),velocityY);
	}

	public boolean isSwipeLeft(MotionEvent e1,MotionEvent e2,float velocityX)
	{
		return isSwipe(e1.getX(),e2.getX(),velocityX);
	}

	public boolean isSwipeRight(MotionEvent e1,MotionEvent e2,float velocityX)
	{
		return isSwipe(e2.getX(),e1.getX(),velocityX);
	}

	private boolean isSwipeSpeed(float velocity)
	{
		return Math.abs(velocity)>swipe_velocity;
	}

	public boolean isSwipeUp(MotionEvent e1,MotionEvent e2,float velocityY)
	{
		return isSwipe(e1.getY(),e2.getY(),velocityY);
	}
}