package com.example.gallery;

import android.content.DialogInterface;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class FullImage extends AppCompatActivity
{
	public static boolean isFullScreen;
	@Nullable
	DisplayCutout displayCutout;
	@NonNull
	private static final String TAG="FullImageScreen";
	private static final int FULLSCREENAPI=19;
	private static final int CUTOUTAPI=28;
	@SuppressWarnings("FieldCanBeLocal")
	@Nullable
	private static Toolbar toolbar;
	@Nullable
	private static ActionBar actionBar;

	@RequiresApi(api=Build.VERSION_CODES.KITKAT)
	void hideSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		if(Build.VERSION.SDK_INT >= CUTOUTAPI&&actionBar!=null)
		{
			actionBar.hide();
		}
	}

	@RequiresApi(api=Build.VERSION_CODES.P)
	void initDisplayCutout()
	{
		@NonNull
		final WindowManager.LayoutParams cutoutAttributes=getWindow().getAttributes();
		cutoutAttributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
	}

	@RequiresApi(api=Build.VERSION_CODES.KITKAT)
	void initFlags()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		getWindow()
			.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
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
	public void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
		{
			displayCutout=getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
			if(displayCutout!=null)
			{
				Log.d(TAG,"Margin top "+displayCutout.getSafeInsetTop());
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if(Build.VERSION.SDK_INT >= FULLSCREENAPI)
		{
			initFlags();
		}
		if(Build.VERSION.SDK_INT >= CUTOUTAPI)
		{
			initDisplayCutout();
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image);
		if(Build.VERSION.SDK_INT >= CUTOUTAPI)
		{
			toolbar=(Toolbar)findViewById(R.id.toolbar);
			setSupportActionBar(toolbar);
		}
		@NonNull
		final Intent intent=getIntent();
		//noinspection ConstantConditions
		@NonNull
		final String image=intent.getExtras().getString("image");
		if(image!=null)
		{
			@NonNull
			final File path=new File(MainActivity.bytes,image);
			try
			{
				@NonNull
				final GestureDetector gestureDetector=initGestureDetector();
				@Nullable
				String title=null;
				if(ImageDownloader.FILE_NAMES.containsValue(image))
				{
					//noinspection rawtypes
					for(final Map.Entry entry : ImageDownloader.FILE_NAMES.entrySet())
					{
						if(image.equals(entry.getValue()))
						{
							title=(String)entry.getKey();
						}
					}
				}
				if(Build.VERSION.SDK_INT >= CUTOUTAPI)
				{
					actionBar=getSupportActionBar();
					if(actionBar!=null)
					{
						actionBar.setTitle(title);
					}
				}
				else
				{
					setTitle(title);
				}
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
				if(Build.VERSION.SDK_INT >= FULLSCREENAPI)
				{
					touchImageView.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
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
					});
				}
				@NonNull
				final BitmapWorkerTask task=new BitmapWorkerTask(touchImageView,String.valueOf(path));
				task.execute(1);
			}
			catch(Exception e)
			{
				showErrorAlertDialog();
				e.printStackTrace();
				Log.d(TAG,String.valueOf(e));
			}
		}
		else
		{
			showErrorAlertDialog();
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

	public void showErrorAlertDialog()
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(FullImage.this,R.style.AlertDialogStyle);
		builder.setTitle("Ошибка");
		builder.setMessage("Ошибка: \nПроизошла непредвиденная ошибка. Попробуйте ещё раз");
		builder.setPositiveButton("OK",new ErrorDialogOnClickListener());
		builder.setCancelable(false);
		builder.show();
	}

	@RequiresApi(api=Build.VERSION_CODES.KITKAT)
	void showSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		if(Build.VERSION.SDK_INT >= CUTOUTAPI&&actionBar!=null)
		{
			actionBar.show();
		}
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
				Log.d(TAG,"bitmap!=null");
				@NonNull
				final TouchImageView touchImageView=touchImageViewWeakReference.get();
				if(touchImageView!=null)
				{
					Log.d(TAG,"touchImageView!=null");
					touchImageView.setImageBitmap(bitmap);
					touchImageView.setDoubleTapScale(3);
					touchImageView.setMaxZoom(10);
					touchImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					touchImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
				}
				else
				{
					Log.d(TAG,"touchImageView==null");
					showErrorAlertDialog();
				}
			}
			else
			{
				Log.d(TAG,"bitmap==null");
				showErrorAlertDialog();
			}
		}

		@Override
		protected void onPreExecute()
		{
			Log.d(TAG,"Декодирование начато "+image);
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

	class ErrorDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			dialog.dismiss();
			finish();
		}
	}
}
