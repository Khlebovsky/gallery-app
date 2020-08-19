package com.example.gallery;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.ortiz.touchview.TouchImageView;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FullImage extends AppCompatActivity
{
	static boolean isFullScreen;
	static int num;
	static GestureDetector gestureDetector;
	static TouchImageView touchImageView;
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="FullImageScreen";
	private static final int CUTOUTAPI=28;
	private static final int IMPROVEDFULLSCREENAPI=19;

	void hideSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		if(Build.VERSION.SDK_INT >= IMPROVEDFULLSCREENAPI)
		{
			decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
		else
		{
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	@RequiresApi(api=Build.VERSION_CODES.P)
	void initDisplayCutout()
	{
		@NonNull
		final WindowManager.LayoutParams cutoutAttributes=getWindow().getAttributes();
		cutoutAttributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
	}

	void initFlags()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		if(Build.VERSION.SDK_INT >= IMPROVEDFULLSCREENAPI)
		{
			getWindow()
				.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		}
	}

	GestureDetector initGestureDetector()
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
						if(touchImageView.getCurrentZoom()==1)
						{
							finish();
						}
						return true;
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

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e)
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
				return super.onSingleTapConfirmed(e);
			}
		});
	}

	void initSettings()
	{
		initFlags();
		if(Build.VERSION.SDK_INT >= CUTOUTAPI)
		{
			initDisplayCutout();
		}
	}

	void loadImage(final File path)
	{
		touchImageView.setOnTouchListener(new ImageOnTouchListener());
		@NonNull
		final BitmapWorkerTask task=new BitmapWorkerTask(touchImageView,String.valueOf(path));
		task.execute(1);
	}

	// TODO исправить обрезание меню на телефонах с вырезами
	// TODO исправить размер шрифта в заголовке
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		initSettings();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image);
		touchImageView=findViewById(R.id.touch_image_view);
		gestureDetector=initGestureDetector();
		@NonNull
		final Intent intent=getIntent();
		//noinspection ConstantConditions
		@Nullable
		final String url=intent.getExtras().getString("URL");
		num=intent.getExtras().getInt("Num");
		setTitle(url);
		if(url!=null)
		{
			@Nullable
			final String fileName=ImageDownloader.FILE_NAMES.containsKey(url)?ImageDownloader.FILE_NAMES.get(url):null;
			if(fileName!=null)
			{
				try
				{
					@NonNull
					final File path=new File(MainActivity.bytes,fileName);
					loadImage(path);
				}
				catch(Exception e)
				{
					showErrorAlertDialog();
					e.printStackTrace();
				}
			}
			else
			{
				showErrorAlertDialog();
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
		inflater.inflate(R.menu.full_image_menu,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.home:
				finish();
				return true;
			case R.id.deleteImage:
				ClientServer.deleteImage(num);
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

	void showSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		if(Build.VERSION.SDK_INT >= IMPROVEDFULLSCREENAPI)
		{
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
		else
		{
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	class BitmapWorkerTask extends AsyncTask<Integer,Void,Bitmap>
	{
		@NonNull
		Timer timer=new Timer();
		@Nullable
		TouchImageView touchImageView;
		@NonNull
		private String path;

		BitmapWorkerTask(@NonNull TouchImageView touchImageView,@NonNull String path)
		{
			this.touchImageView=touchImageView;
			this.path=path;
		}

		@Override
		protected Bitmap doInBackground(Integer... params)
		{
			@Nullable
			Bitmap bitmap=null;
			try
			{
				@NonNull
				final BitmapFactory.Options options=new BitmapFactory.Options();
				options.inJustDecodeBounds=true;
				BitmapFactory.decodeFile(path,options);
				@NonNull
				int originalBitmapWidth=options.outWidth;
				@NonNull
				int originalBitmapHeight=options.outHeight;
				int reductionRatio=0;
				if(originalBitmapWidth>ImageDownloader.MAX_BITMAP_SIZE||originalBitmapHeight>ImageDownloader.MAX_BITMAP_SIZE)
				{
					reductionRatio=1;
					while(originalBitmapWidth>ImageDownloader.MAX_BITMAP_SIZE||originalBitmapHeight>ImageDownloader.MAX_BITMAP_SIZE)
					{
						reductionRatio<<=1;
						originalBitmapWidth/=2;
						originalBitmapHeight/=2;
					}
				}
				@NonNull
				final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
				bitmapOptions.inSampleSize=reductionRatio;
				bitmap=BitmapFactory.decodeFile(path,bitmapOptions);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				showErrorAlertDialog();
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			timer.cancel();
			if(bitmap!=null)
			{
				if(touchImageView!=null)
				{
					touchImageView.setImageBitmap(bitmap);
					touchImageView.setDoubleTapScale(3);
					touchImageView.setMaxZoom(10);
					touchImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					touchImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
				}
				else
				{
					showErrorAlertDialog();
				}
			}
			else
			{
				showErrorAlertDialog();
			}
		}

		@Override
		protected void onPreExecute()
		{
			if(touchImageView!=null)
			{
				@NonNull
				final ShowLoading showLoading=new ShowLoading();
				timer.schedule(showLoading,500);
			}
			else
			{
				showErrorAlertDialog();
			}
		}

		class ShowLoading extends TimerTask
		{
			@Override
			public void run()
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if(touchImageView!=null)
						{
							final int size=getResources().getDimensionPixelSize(R.dimen.preloaderSize);
							touchImageView.setMaxZoom(1);
							touchImageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
							touchImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
							touchImageView.setImageResource(R.drawable.progress);
						}
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

	static class ImageOnTouchListener implements View.OnTouchListener
	{
		@Override
		public boolean onTouch(View view,MotionEvent motionEvent)
		{
			return gestureDetector.onTouchEvent(motionEvent);
		}
	}
}
