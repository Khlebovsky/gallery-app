package com.example.gallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.ortiz.touchview.TouchImageView;
import java.io.File;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FullImage extends AppCompatActivity
{
	public static boolean isFullScreen;
	@NonNull
	private static final String TAG="FullImageScreen";

	@RequiresApi(api=Build.VERSION_CODES.KITKAT)
	void hideSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	private GestureDetector initGestureDetector()
	{
		//noinspection deprecation
		return new GestureDetector(new GestureDetector.SimpleOnGestureListener()
		{
			private SwipeDetector swipeDetector=new SwipeDetector();

			@Override
			public boolean onFling(MotionEvent e1,MotionEvent e2,float velocityX,float velocityY)
			{
				try
				{
					if(swipeDetector.isSwipeDown(e1,e2,velocityY))
					{
						return false;
					}
					else if(swipeDetector.isSwipeUp(e1,e2,velocityY))
					{
						finish();
					}
					else if(swipeDetector.isSwipeLeft(e1,e2,velocityX))
					{
						return false;
					}
					else if(swipeDetector.isSwipeRight(e1,e2,velocityX))
					{
						return false;
					}
				}
				catch(Exception ignored)
				{
				}
				return false;
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image);
		@NonNull
		final GestureDetector gestureDetector=initGestureDetector();
		@NonNull
		final Intent intent=getIntent();
		//noinspection ConstantConditions
		@NonNull
		final String image=intent.getExtras().getString("image");
		@Nullable
		String title=null;
		if(ImageDownloader.FILE_NAMES.containsValue(image))
		{
			//noinspection rawtypes
			for(final Map.Entry entry : ImageDownloader.FILE_NAMES.entrySet())
			{
				if(image!=null&&image.equals(entry.getValue()))
				{
					title=(String)entry.getKey();
				}
			}
		}
		setTitle(title);
		@NonNull
		final TouchImageView touchImageView=findViewById(R.id.touch_image_view);
		//noinspection AnonymousInnerClassMayBeStatic
		touchImageView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view,MotionEvent motionEvent)
			{
				if(touchImageView.getCurrentZoom()==1)
				{
					return gestureDetector.onTouchEvent(motionEvent);
				}
				return false;
			}
		});
		touchImageView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if(Build.VERSION.SDK_INT >= 19)
				{
					if(isFullScreen)
					{
						showSystemUI();
						isFullScreen=false;
					}
					else
					{
						hideSystemUI();
						isFullScreen=true;
					}
				}
			}
		});
		@NonNull
		final String path=MainActivity.bytes+File.separator+image;
		try
		{
			@NonNull
			final BitmapWorkerTask task=new BitmapWorkerTask(touchImageView,path);
			task.execute(1);
		}
		catch(Exception e)
		{
			@NonNull
			final AlertDialog.Builder builder=new AlertDialog.Builder(FullImage.this,R.style.AlertDialogStyle);
			builder.setTitle("Ошибка");
			builder.setMessage("Ошибка: \nПроизошла непредвиденная ошибка");
			builder.setPositiveButton("OK",new MainActivity.MyOnClickListener());
			builder.show();
			e.printStackTrace();
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		@NonNull
		final MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.secondmenu,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId()==R.id.home)
		{
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void showSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}

	class BitmapWorkerTask extends AsyncTask<Integer,Void,Bitmap>
	{
		@NonNull
		final WeakReference<TouchImageView> touchImageViewWeakReference;
		@NonNull
		Timer timer=new Timer();
		@NonNull
		private String image;

		BitmapWorkerTask(TouchImageView touchImageView,@NonNull String path)
		{
			touchImageViewWeakReference=new WeakReference<>(touchImageView);
			image=path;
		}

		@Override
		protected Bitmap doInBackground(Integer... params)
		{
			return BitmapFactory.decodeFile(image);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			timer.cancel();
			if(bitmap!=null)
			{
				@NonNull
				final TouchImageView touchImageView=touchImageViewWeakReference.get();
				if(touchImageView!=null)
				{
					touchImageView.setDoubleTapScale(3);
					touchImageView.setMaxZoom(10);
					touchImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
					touchImageView.setImageBitmap(bitmap);
				}
			}
		}

		@Override
		protected void onPreExecute()
		{
			@NonNull
			final ShowLoading showLoading=new ShowLoading();
			timer.schedule(showLoading,500);
		}

		class ShowLoading extends TimerTask
		{
			@Override
			public void run()
			{
				@NonNull
				final TouchImageView touchImageView=touchImageViewWeakReference.get();
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						final int size=getResources().getDimensionPixelSize(R.dimen.preloaderSize);
						touchImageView.setMaxZoom(1);
						touchImageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
						touchImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						touchImageView.setImageResource(R.drawable.progress);
					}
				});
			}
		}
	}
}
