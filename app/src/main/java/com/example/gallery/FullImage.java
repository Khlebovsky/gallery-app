package com.example.gallery;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.github.chrisbanes.photoview.PhotoView;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
	public static final float MAX_DRAG_ZOOM=1.1f;
	public static final float MEDIUM_ZOOM=3;
	public static final float MIN_DRAG_ZOOM=1;
	public static final float MAX_ZOOM=10;
	public static Activity fullImage;
	static boolean isFullScreen;
	static int num;
	static PhotoView photoView;
	static String url;
	static NotificationManagerCompat notificationManagerCompat;
	static float offsetDY;
	static float downLength;
	static int touchSlop;
	static boolean isTouch;
	static int imageShiftThreshold;
	private static final int CUTOUTAPI=28;
	private static final int NOTIFICATIONCHANNELAPI=26;
	private static final int FULLSCREENAPI=19;
	private static final int STATUSBARCOLORAPI=21;
	@NonNull
	private static final String LOADED_IMAGE_TAG="loaded";
	private static final int NOTIFY_ID=101;
	@NonNull
	private static final String SHARE_INTENT_TYPE="text/plain";
	private static final int INTENTCHOOSERAPI=23;

	public static void closeActivity()
	{
		photoView.animate().alpha(0).setDuration(50).start();
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				fullImage.finish();
			}
		},50);
	}

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

	PendingIntent getActionBackPendingIntent()
	{
		final int requestCode=100;
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

	Intent getShareImageIntent()
	{
		@NonNull
		final Intent resultIntent;
		if(Build.VERSION.SDK_INT >= INTENTCHOOSERAPI)
		{
			@NonNull
			final Intent shareIntent=new Intent(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_TEXT,url);
			shareIntent.setType(SHARE_INTENT_TYPE);
			@NonNull
			final List<ResolveInfo> resInfos=getPackageManager().queryIntentActivities(shareIntent,0);
			@NonNull
			final ArrayList<Intent> shareIntentsList=new ArrayList<>();
			@NonNull
			final String appPackageName=getPackageName();
			for(final ResolveInfo resInfo : resInfos)
			{
				@NonNull
				final String packageName=resInfo.activityInfo.packageName;
				if(!packageName.equals(appPackageName))
				{
					@NonNull
					final Intent intent=new Intent(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_TEXT,url);
					intent.setType(SHARE_INTENT_TYPE);
					intent.setPackage(packageName);
					shareIntentsList.add(intent);
				}
			}
			resultIntent=Intent.createChooser(shareIntentsList.remove(0),"Выберите приложение");
			resultIntent.putExtra(Intent.EXTRA_ALTERNATE_INTENTS,shareIntentsList.toArray(new Parcelable[shareIntentsList.size()]));
		}
		else
		{
			resultIntent=new Intent(Intent.ACTION_SEND);
			resultIntent.putExtra(Intent.EXTRA_TEXT,url);
			resultIntent.setType(SHARE_INTENT_TYPE);
		}
		return resultIntent;
	}

	public static void hideDefaultNotification()
	{
		notificationManagerCompat.cancelAll();
	}

	void hideSystemUI()
	{
		if(Build.VERSION.SDK_INT >= FULLSCREENAPI)
		{
			@NonNull
			final View decorView=getWindow().getDecorView();
			decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
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
		if(Build.VERSION.SDK_INT >= FULLSCREENAPI)
		{
			@NonNull
			final View decorView=getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
			if(Build.VERSION.SDK_INT >= STATUSBARCOLORAPI)
			{
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				getWindow().setStatusBarColor(MainActivity.resources.getColor(R.color.transparentStatusBarColor));
			}
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
		if(MainActivity.resources==null)
		{
			MainActivity.resources=getResources();
		}
		fullImage=FullImage.this;
		photoView=findViewById(R.id.photo_view);
		if(Build.VERSION.SDK_INT >= FULLSCREENAPI)
		{
			photoView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
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
		final ViewConfiguration viewConfiguration=ViewConfiguration.get(photoView.getContext());
		touchSlop=viewConfiguration.getScaledTouchSlop();
		imageShiftThreshold=MainActivity.resources.getDimensionPixelSize(R.dimen.imageShiftThreshold);
	}

	void loadImage(@Nullable final File path)
	{
		if(path!=null)
		{
			@NonNull
			final BitmapWorkerTask task=new BitmapWorkerTask(photoView,String.valueOf(path));
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
				// builder.addAction(getActionBack());
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
		if(Build.VERSION.SDK_INT >= FULLSCREENAPI)
		{
			@NonNull
			final View decorView=getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	class BitmapWorkerTask extends AsyncTask<Integer,Void,Bitmap>
	{
		@NonNull
		final Timer timer=new Timer();
		@NonNull
		final PhotoView photoView;
		@NonNull
		private final String path;

		BitmapWorkerTask(@NonNull final PhotoView photoView,@NonNull final String path)
		{
			this.photoView=photoView;
			this.path=path;
		}

		@Override
		protected Bitmap doInBackground(Integer... params)
		{
			@Nullable
			final Bitmap bitmap;
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
				return bitmap;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				showErrorAlertDialog("Decoding in background error");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			if(bitmap!=null)
			{
				try
				{
					timer.cancel();
					photoView.setTag(LOADED_IMAGE_TAG);
					photoView.setMaximumScale(MAX_ZOOM);
					photoView.setMediumScale(MEDIUM_ZOOM);
					photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					photoView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
					photoView.setImageBitmap(bitmap);
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
			timer.schedule(showLoading,MainActivity.ACTION_DELAY_TIME);
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
						if(!LOADED_IMAGE_TAG.equals(photoView.getTag()))
						{
							final int size=MainActivity.resources.getDimensionPixelSize(R.dimen.preloaderSize);
							photoView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
							photoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
							photoView.setImageResource(R.drawable.progress);
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
}
