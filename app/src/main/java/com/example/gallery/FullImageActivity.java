package com.example.gallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
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
import java.io.*;
import java.lang.ref.WeakReference;
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

public class FullImageActivity extends AppCompatActivity
{
	@Nullable
	NotificationManagerCompat notificationManagerCompat;
	boolean isFullScreen;
	int num;
	@Nullable
	String url;
	private static final float MEDIUM_ZOOM=3;
	private static final float MAX_ZOOM=10;
	private static final int CUTOUT_API=28;
	private static final int NOTIFICATION_CHANNEL_API=26;
	private static final int FULL_SCREEN_API=19;
	private static final int STATUS_BAR_COLOR_API=21;
	@NonNull
	private static final String LOADED_IMAGE_TAG="loaded";
	private static final int NOTIFY_ID=101;
	@NonNull
	private static final String SHARE_INTENT_TYPE="text/plain";
	private static final int INTENT_CHOOSER_API=23;
	private static final int REQUEST_CODE=100;

	public static void closeActivity()
	{
		@Nullable
		final WeakReference<FullImageActivity> fullImageWeakReference_=Application.fullImageActivity;
		@Nullable
		final WeakReference<PhotoView> photoViewWeakReference_=Application.photoView;
		if(fullImageWeakReference_!=null&&photoViewWeakReference_!=null)
		{
			@Nullable
			final PhotoView photoView=photoViewWeakReference_.get();
			@Nullable
			final FullImageActivity fullImageActivity=fullImageWeakReference_.get();
			if(photoView!=null&&fullImageActivity!=null)
			{
				photoView.animate().alpha(0).setDuration(50).start();
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						fullImageActivity.finish();
					}
				},50);
			}
		}
	}

	void createNotificationChannel()
	{
		@Nullable
		final WeakReference<FullImageActivity> fullImageWeakReference_=Application.fullImageActivity;
		if(fullImageWeakReference_!=null)
		{
			@Nullable
			final Resources resources=fullImageWeakReference_.get().getResources();
			if(Build.VERSION.SDK_INT >= NOTIFICATION_CHANNEL_API&&resources!=null)
			{


				@NonNull
				final CharSequence name=resources.getString(R.string.channel_name);
				@NonNull
				final String description=resources.getString(R.string.channel_description);
				final int importance=NotificationManager.IMPORTANCE_DEFAULT;
				@NonNull
				final NotificationChannel channel=new NotificationChannel(resources.getString(R.string.channel_name),name,importance);
				channel.setDescription(description);
				@NonNull
				final NotificationManager notificationManager=getSystemService(NotificationManager.class);
				notificationManager.createNotificationChannel(channel);
			}
		}
	}

	PendingIntent getActionBackPendingIntent()
	{
		@NonNull
		final Intent actionBackIntent=new Intent(FullImageActivity.this,FullImageActivity.class);
		actionBackIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		@NonNull
		final PendingIntent actionBackPendingIntent=PendingIntent.getActivity(FullImageActivity.this,REQUEST_CODE,actionBackIntent,PendingIntent.FLAG_CANCEL_CURRENT);
		return actionBackPendingIntent;
	}

	NotificationCompat.Action getActionOpen()
	{
		@NonNull
		final Intent actionOpenIntent=new Intent(Intent.ACTION_VIEW);
		actionOpenIntent.setData(Uri.parse(url));
		@NonNull
		final PendingIntent actionOpenPendingIntent=PendingIntent.getActivity(FullImageActivity.this,0,actionOpenIntent,PendingIntent.FLAG_CANCEL_CURRENT);
		@NonNull
		final NotificationCompat.Action actionOpen=new NotificationCompat.Action(R.drawable.ic_notification_open,"Перейти",actionOpenPendingIntent);
		return actionOpen;
	}

	NotificationCompat.Action getActionShare()
	{
		@NonNull
		final PendingIntent actionSharePendingIntent=PendingIntent.getActivity(FullImageActivity.this,0,getShareImageIntent(),PendingIntent.FLAG_CANCEL_CURRENT);
		@NonNull
		final NotificationCompat.Action actionShare=new NotificationCompat.Action(R.drawable.ic_notification_share,"Поделиться",actionSharePendingIntent);
		return actionShare;
	}

	File getImagePath(@Nullable final File dir)
	{
		@Nullable
		final String url_=url;
		if(dir!=null&&url_!=null)
		{
			@NonNull
			final String fileName=ImagesDownloader.urlToHashMD5(url_);
			@NonNull
			final File path=new File(dir,fileName);
			return path;
		}
		return null;
	}

	Bitmap getNotificationLargeIcon()
	{
		@Nullable
		final Bitmap bitmap=BitmapFactory.decodeFile(String.valueOf(getImagePath(Application.imagePreviewsDir)));
		return bitmap;
	}

	Intent getShareImageIntent()
	{
		@NonNull
		final Intent resultIntent;
		if(Build.VERSION.SDK_INT >= INTENT_CHOOSER_API)
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

	void hideDefaultNotification()
	{
		@Nullable
		final NotificationManagerCompat notificationManagerCompat_=notificationManagerCompat;
		if(notificationManagerCompat_!=null)
		{
			notificationManagerCompat_.cancelAll();
		}
	}

	void hideSystemUI()
	{
		if(Build.VERSION.SDK_INT >= FULL_SCREEN_API)
		{
			@NonNull
			final View decorView=getWindow().getDecorView();
			decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
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
		if(Build.VERSION.SDK_INT >= FULL_SCREEN_API)
		{
			@NonNull
			final View decorView=getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
			if(Build.VERSION.SDK_INT >= STATUS_BAR_COLOR_API)
			{
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				@Nullable
				final WeakReference<FullImageActivity> fullImageWeakReference_=Application.fullImageActivity;
				if(fullImageWeakReference_!=null)
				{
					@Nullable
					final Resources resources=fullImageWeakReference_.get().getResources();
					if(resources!=null)
					{
						getWindow().setStatusBarColor(resources.getColor(R.color.transparentStatusBarColor));
					}
				}
			}
		}
	}

	void initObjects()
	{
		Application.fullImageActivity=new WeakReference<>(FullImageActivity.this);
		@NonNull
		final PhotoView photoView=findViewById(R.id.photo_view);
		Application.photoView=new WeakReference<>(photoView);
		if(Build.VERSION.SDK_INT >= FULL_SCREEN_API)
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
	}

	void initSettings()
	{
		initFlags();
		if(Build.VERSION.SDK_INT >= CUTOUT_API)
		{
			initDisplayCutout();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image);
		initSettings();
		setHomeButton();
		initObjects();
		ImageCustomView.initStatic();
		@NonNull
		final Intent intent=getIntent();
		//noinspection ConstantConditions
		url=intent.getExtras().getString("URL");
		setTitle(url);
		showDefaultNotification();
		num=intent.getExtras().getInt("Num");
		if(url!=null)
		{
			showFullImage(getImagePath(Application.imagesBytesDir));
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
				@Nullable
				final WeakReference<FullImageActivity> fullImageWeakReference_=Application.fullImageActivity;
				if(fullImageWeakReference_!=null)
				{
					@Nullable
					final Resources resources=fullImageWeakReference_.get().getResources();
					if(resources!=null)
					{
						@NonNull
						final String smallText="Открыта картинка";
						@NonNull
						final String bigText="Открыта картинка: \n"+url;
						@NonNull
						final NotificationCompat.Builder builder=new NotificationCompat.Builder(FullImageActivity.this,resources.getString(R.string.channel_name));
						builder.setSmallIcon(R.drawable.ic_notification_icon);
						builder.setContentTitle(resources.getString(R.string.app_name));
						builder.setContentText(smallText);
						builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
						builder.setColor(resources.getColor(R.color.colorPrimary));
						builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
						builder.setLargeIcon(getNotificationLargeIcon());
						builder.setContentIntent(getActionBackPendingIntent());
						builder.setAutoCancel(false);
						builder.setShowWhen(false);
						builder.addAction(getActionOpen());
						builder.addAction(getActionShare());
						@NonNull
						final Notification notification=builder.build();
						createNotificationChannel();
						notificationManagerCompat=NotificationManagerCompat.from(getApplicationContext());
						notificationManagerCompat.notify(NOTIFY_ID,notification);
					}
				}
			}
		}.start();
	}

	void showDeleteImageAlertDialog()
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(FullImageActivity.this,R.style.AlertDialogTheme);
		builder.setTitle("Удалить картинку");
		builder.setMessage("Вы действительно хотите удалить эту картинку?");
		builder.setPositiveButton("Удалить",new DeleteImageDialogOnClickListener());
		builder.setNegativeButton("Отмена",new DeleteImageDialogOnClickListener());
		builder.setCancelable(true);
		builder.show();
	}

	void showErrorAlertDialog(@NonNull final String error)
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(FullImageActivity.this,R.style.AlertDialogTheme);
		builder.setTitle("Ошибка");
		builder.setMessage("Ошибка: \n"+error);
		builder.setPositiveButton("ОК",new ErrorDialogOnClickListener());
		builder.setCancelable(false);
		builder.show();
	}

	void showFullImage(@Nullable final File path)
	{
		@Nullable
		final WeakReference<PhotoView> photoViewWeakReference_=Application.photoView;
		if(photoViewWeakReference_!=null)
		{
			@Nullable
			final PhotoView photoView=photoViewWeakReference_.get();
			if(path!=null&&photoView!=null)
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
	}

	void showSystemUI()
	{
		if(Build.VERSION.SDK_INT >= FULL_SCREEN_API)
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
				final int originalBitmapWidth=options.outWidth;
				@NonNull
				final int originalBitmapHeight=options.outHeight;
				final int reductionRatio=ImagesDownloader.getReductionRadio(originalBitmapWidth,originalBitmapHeight);
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
					@NonNull
					final PhotoView photoView_=photoView;
					photoView_.setTag(LOADED_IMAGE_TAG);
					photoView_.setMaximumScale(MAX_ZOOM);
					photoView_.setMediumScale(MEDIUM_ZOOM);
					photoView_.setScaleType(ImageView.ScaleType.FIT_CENTER);
					photoView_.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
					photoView_.setImageBitmap(bitmap);
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
			timer.schedule(showLoading,Application.ACTION_DELAY_TIME);
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
						@NonNull
						final PhotoView photoView_=photoView;
						if(!LOADED_IMAGE_TAG.equals(photoView_.getTag()))
						{
							@Nullable
							final WeakReference<FullImageActivity> fullImageWeakReference_=Application.fullImageActivity;
							if(fullImageWeakReference_!=null)
							{
								@Nullable
								final Resources resources=fullImageWeakReference_.get().getResources();
								if(resources!=null)
								{
									final int size=resources.getDimensionPixelSize(R.dimen.preloaderSize);
									photoView_.setLayoutParams(new LinearLayout.LayoutParams(size,size));
								}
							}
							photoView_.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
							photoView_.setImageResource(R.drawable.progress);
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
