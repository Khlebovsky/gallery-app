package com.example.gallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.ortiz.touchview.TouchImageView;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class FullImage extends AppCompatActivity
{
	static final int MIN_LOCATION_Y=-350;
	static final int MAX_LOCATION_Y=450;
	static boolean isFullScreen;
	static int num;
	static GestureDetector gestureDetector;
	static TouchImageView touchImageView;
	static String url;
	static NotificationManagerCompat notificationManagerCompat;
	static float dY;
	static long downTime;
	static float downLength;
	private static final int CUTOUTAPI=28;
	private static final int NOTIFICATIONCHANNELAPI=26;
	private static final int IMPROVEDFULLSCREENAPI=19;
	@NonNull
	private static final String LOADED_IMAGE_TAG="loaded";
	private static final int NOTIFY_ID=101;
	@NonNull
	private static final String SHARE_INTENT_TYPE="text/plain";

	void createNotificationChannel()
	{
		if(Build.VERSION.SDK_INT >= NOTIFICATIONCHANNELAPI)
		{
			@NonNull
			final CharSequence name=MainActivity.resources.getString(R.string.channel_name);
			@NonNull
			final String description=MainActivity.resources.getString(R.string.channel_description);
			final int importance=NotificationManager.IMPORTANCE_DEFAULT;
			@NonNull
			final NotificationChannel channel=new NotificationChannel(MainActivity.resources.getString(R.string.channel_name),name,importance);
			channel.setDescription(description);
			@NonNull
			final NotificationManager notificationManager=getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	NotificationCompat.Action getActionBack()
	{
		@NonNull
		final NotificationCompat.Action actionBack=new NotificationCompat.Action(R.drawable.ic_notification_back,"Открыть",getActionBackPendingIntent());
		return actionBack;
	}

	PendingIntent getActionBackPendingIntent()
	{
		final int requestCode=(int)System.currentTimeMillis();
		@NonNull
		final Intent actionBackIntent=new Intent(FullImage.this,FullImage.class);
		actionBackIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		@NonNull
		final PendingIntent actionBackPendingIntent=PendingIntent.getActivity(FullImage.this,requestCode,actionBackIntent,PendingIntent.FLAG_CANCEL_CURRENT);
		return actionBackPendingIntent;
	}

	NotificationCompat.Action getActionOpen()
	{
		@NonNull
		final Intent actionOpenIntent=new Intent(Intent.ACTION_VIEW);
		actionOpenIntent.setData(Uri.parse(url));
		@NonNull
		final PendingIntent actionOpenPendingIntent=PendingIntent.getActivity(FullImage.this,0,actionOpenIntent,PendingIntent.FLAG_CANCEL_CURRENT);
		@NonNull
		final NotificationCompat.Action actionOpen=new NotificationCompat.Action(R.drawable.ic_notification_open,"Перейти",actionOpenPendingIntent);
		return actionOpen;
	}

	NotificationCompat.Action getActionShare()
	{
		@NonNull
		final PendingIntent actionSharePendingIntent=PendingIntent.getActivity(FullImage.this,0,getShareImageIntent(),PendingIntent.FLAG_CANCEL_CURRENT);
		@NonNull
		final NotificationCompat.Action actionShare=new NotificationCompat.Action(R.drawable.ic_notification_share,"Поделиться",actionSharePendingIntent);
		return actionShare;
	}

	static File getImagePath(@Nullable File dir)
	{
		@Nullable
		final String fileName=ImageDownloader.FILE_NAMES.containsKey(url)?ImageDownloader.FILE_NAMES.get(url):null;
		if(fileName!=null&&dir!=null)
		{
			@NonNull
			final File path=new File(dir,fileName);
			return path;
		}
		return null;
	}

	static Bitmap getNotificationLargeIcon()
	{
		@Nullable
		final Bitmap bitmap=BitmapFactory.decodeFile(String.valueOf(getImagePath(MainActivity.previews)));
		return bitmap;
	}

	static Intent getShareImageIntent()
	{
		@NonNull
		final Intent actionShareIntent=new Intent(Intent.ACTION_SEND);
		actionShareIntent.putExtra(Intent.EXTRA_TEXT,url);
		actionShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		actionShareIntent.setType(SHARE_INTENT_TYPE);
		return actionShareIntent;
	}

	void hideAndCloseImage()
	{
		touchImageView.animate().alpha(0).setDuration(50).start();
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				finish();
			}
		},50);
	}

	public static void hideDefaultNotification()
	{
		notificationManagerCompat.cancelAll();
	}

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

	void initSettings()
	{
		initFlags();
		if(Build.VERSION.SDK_INT >= CUTOUTAPI)
		{
			initDisplayCutout();
		}
	}

	void initStatic()
	{
		touchImageView=findViewById(R.id.touch_image_view);
		gestureDetector=new GestureDetector(this,new ImageGestureDetector());
	}

	void loadImage(@Nullable final File path)
	{
		if(path!=null)
		{
			touchImageView.setOnTouchListener(new ImageOnTouchListener());
			@NonNull
			final BitmapWorkerTask task=new BitmapWorkerTask(touchImageView,String.valueOf(path));
			task.execute(1);
		}
		else
		{
			showErrorAlertDialog("The requested picture does not exist");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		initSettings();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image);
		setHomeButton();
		initStatic();
		@NonNull
		final Intent intent=getIntent();
		//noinspection ConstantConditions
		url=intent.getExtras().getString("URL");
		setTitle(url);
		showDefaultNotification();
		num=intent.getExtras().getInt("Num");
		if(url!=null)
		{
			loadImage(getImagePath(MainActivity.bytes));
		}
		else
		{
			showErrorAlertDialog("The requested picture does not exist");
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
	protected void onDestroy()
	{
		super.onDestroy();
		try
		{
			hideDefaultNotification();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				finish();
				return true;
			case R.id.shareImage:
				try
				{
					startActivity(getShareImageIntent());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				return true;
			case R.id.deleteImage:
				showDeleteImageAlertDialog();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void setHomeButton()
	{
		@Nullable
		final ActionBar actionBar=getSupportActionBar();
		if(actionBar!=null)
		{
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	void showDefaultNotification()
	{
		new Thread()
		{
			@Override
			public void run()
			{
				@NonNull
				final String smallText="Открыта картинка";
				@NonNull
				final String bigText="Открыта картинка: \n"+url;
				@NonNull
				final NotificationCompat.Builder builder=new NotificationCompat.Builder(FullImage.this,MainActivity.resources.getString(R.string.channel_name));
				builder.setSmallIcon(R.drawable.ic_notification_icon);
				builder.setContentTitle(MainActivity.resources.getString(R.string.app_name));
				builder.setContentText(smallText);
				builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
				builder.setColor(MainActivity.resources.getColor(R.color.colorPrimary));
				builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
				builder.setLargeIcon(getNotificationLargeIcon());
				builder.setContentIntent(getActionBackPendingIntent());
				builder.setAutoCancel(false);
				builder.setShowWhen(false);
				builder.addAction(getActionBack());
				builder.addAction(getActionOpen());
				builder.addAction(getActionShare());
				@NonNull
				final Notification notification=builder.build();
				createNotificationChannel();
				notificationManagerCompat=NotificationManagerCompat.from(FullImage.this);
				notificationManagerCompat.notify(NOTIFY_ID,notification);
			}
		}.start();
	}

	void showDeleteImageAlertDialog()
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(FullImage.this,R.style.AlertDialogTheme);
		builder.setTitle("Удалить картинку");
		builder.setMessage("Вы действительно хотите удалить эту картинку?");
		builder.setPositiveButton("Удалить",new DeleteImageDialogOnClickListener());
		builder.setNegativeButton("Отмена",new DeleteImageDialogOnClickListener());
		builder.setCancelable(true);
		builder.show();
	}

	void showErrorAlertDialog(@NonNull final String error)
	{
		try
		{
			@NonNull
			final AlertDialog.Builder builder=new AlertDialog.Builder(FullImage.this,R.style.AlertDialogTheme);
			builder.setTitle("Ошибка");
			builder.setMessage("Ошибка: \n"+error);
			builder.setPositiveButton("Ок",new ErrorDialogOnClickListener());
			builder.setCancelable(false);
			builder.show();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			finish();
		}
	}

	void showSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		if(Build.VERSION.SDK_INT >= IMPROVEDFULLSCREENAPI)
		{
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
		else
		{
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	class BitmapWorkerTask extends AsyncTask<Integer,Void,Bitmap>
	{
		@NonNull
		final Timer timer=new Timer();
		@NonNull
		final TouchImageView touchImageView;
		@NonNull
		private final String path;

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
				showErrorAlertDialog("Decoding in background error");
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			if(bitmap!=null)
			{
				try
				{
					timer.cancel();
					touchImageView.setTag(LOADED_IMAGE_TAG);
					touchImageView.setDoubleTapScale(3);
					touchImageView.setMaxZoom(10);
					touchImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					touchImageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
					touchImageView.setImageBitmap(bitmap);
				}
				catch(Exception e)
				{
					showErrorAlertDialog("Picture insertion error");
					e.printStackTrace();
				}
			}
			else
			{
				showErrorAlertDialog("Bitmap decoding error");
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
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if(!LOADED_IMAGE_TAG.equals(touchImageView.getTag()))
						{
							final int size=MainActivity.resources.getDimensionPixelSize(R.dimen.preloaderSize);
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

	class DeleteImageDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			switch(which)
			{
				case DialogInterface.BUTTON_POSITIVE:
					ClientServer.deleteImage(num);
					finish();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					dialog.dismiss();
					break;
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

	class ImageGestureDetector extends GestureDetector.SimpleOnGestureListener
	{
		@NonNull
		static final String TAG="ImageGestureDetector";
		static final int MIN_FLING_LENGTH=600;
		static final int MIN_FLING_SPEED=5;

		@Override
		public boolean onFling(MotionEvent e1,MotionEvent e2,float velocityX,float velocityY)
		{
			if(touchImageView.getCurrentZoom()==1)
			{
				final float flingLength=Math.abs(e2.getY()-e1.getY());
				final float flingTime=e2.getEventTime()-e1.getEventTime();
				final float flingSpeed=flingLength/flingTime;
				if(flingLength >= MIN_FLING_LENGTH&&flingSpeed >= MIN_FLING_SPEED)
				{
					hideAndCloseImage();
				}
			}
			return true;
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
			return true;
		}
	}

	// TODO доделать логику смахиваний, ontouch slop
	class ImageOnTouchListener implements View.OnTouchListener
	{
		@Override
		public boolean onTouch(View view,MotionEvent motionEvent)
		{
			@NonNull
			final String TAG="Touch";
			if(touchImageView.getCurrentZoom()==1)
			{
				final float currentLocationY=touchImageView.getY();
				switch(motionEvent.getAction())
				{
					case MotionEvent.ACTION_DOWN:
						downTime=System.currentTimeMillis();
						downLength=motionEvent.getY();
						dY=touchImageView.getY()-motionEvent.getRawY();
						break;
					case MotionEvent.ACTION_UP:
						if(currentLocationY >= MAX_LOCATION_Y||MIN_LOCATION_Y >= currentLocationY)
						{
							hideAndCloseImage();
						}
						else
						{
							touchImageView.animate().y(0).setDuration(200).start();
						}
						break;
					case MotionEvent.ACTION_MOVE:
						final long actionTime=System.currentTimeMillis()-downTime;
						final float currentDownLength=Math.abs(downLength-motionEvent.getY());
						if(currentDownLength >= 5)
						{
							touchImageView.animate().y(0).setDuration(0).start();
							touchImageView.animate().y(motionEvent.getRawY()+dY).setDuration(0).start();
							if(currentLocationY >= MAX_LOCATION_Y||MIN_LOCATION_Y >= currentLocationY)
							{
								touchImageView.animate().alpha(0.5f).setDuration(0).start();
							}
							else
							{
								touchImageView.animate().alpha(1).setDuration(0).start();
							}
						}
						break;
					default:
						break;
				}
			}
			return gestureDetector.onTouchEvent(motionEvent);
		}
	}
}
