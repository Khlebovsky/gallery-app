package com.example.gallery;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SaveImageActivity extends AppCompatActivity
{
	@Nullable
	String url;
	@Nullable
	ImageButton applyButton;
	@Nullable
	ImageView imageView;
	@NonNull
	private static final String MIMETYPE_TEXT="text/plain";

	void handleSendText(@NonNull final Intent intent)
	{
		@Nullable
		final String imageUrl=intent.getStringExtra(Intent.EXTRA_TEXT);
		@Nullable
		final ImageView imageView=this.imageView;
		if(imageView!=null)
		{
			if(imageUrl!=null)
			{
				url=imageUrl;
				setTitle(imageUrl);
				@NonNull
				final Resources resources=getResources();
				final int size=resources.getDimensionPixelSize(R.dimen.preloaderSize);
				imageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setImageResource(R.drawable.progress);
				new ShowSavedImageThread().start();
			}
			else
			{
				showSaveImageAlertDialog("Invalid link");
				imageView.setImageResource(R.drawable.ic_error);
			}
		}
	}

	void initObjects()
	{
		Application.saveImageActivity=new WeakReference<>(this);
		applyButton=findViewById(R.id.apply_button);
		@NonNull
		final ImageView imageView=findViewById(R.id.share_image_view);
		final int size=getResources().getDimensionPixelSize(R.dimen.preloaderSize);
		imageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		imageView.setImageResource(R.drawable.progress);
		this.imageView=imageView;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Application.initTheme(getApplicationContext());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_save_image);
		ConnectionSettings.initGooglePlayServices(this);
		setHomeButton();
		initObjects();
		@NonNull
		final Intent intent=getIntent();
		@Nullable
		final String action=intent.getAction();
		@Nullable
		final String type=intent.getType();
		if(Intent.ACTION_SEND.equals(action)&&MIMETYPE_TEXT.equals(type))
		{
			handleSendText(intent);
			setOnClickListeners();
		}
		else
		{
			showSaveImageAlertDialog("The image is not transferred or this link format is not supported");
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if(item.getItemId()==android.R.id.home)
		{
			final Intent intent=new Intent(SaveImageActivity.this,MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setPackage(getPackageName());
			try
			{
				startActivity(intent);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			ImagesAdapter.callNotifyDataSetChanged();
			finish();
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

	void setOnClickListeners()
	{
		@NonNull
		final ImageButton cancelButton=findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				finish();
			}
		});
		@Nullable
		final ImageButton applyButton=this.applyButton;
		@Nullable
		final String url=this.url;
		if(applyButton!=null&&url!=null)
		{
			applyButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					ClientServer.addImage(url);
					@NonNull
					final Intent intent=new Intent(SaveImageActivity.this,MainActivity.class);
					intent.setPackage(getPackageName());
					try
					{
						startActivity(intent);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					ImagesAdapter.callNotifyDataSetChanged();
					finish();
				}
			});
		}
	}

	public static void showSaveImageAlertDialog(@NonNull final String error)
	{
		@Nullable
		final WeakReference<SaveImageActivity> saveImageActivityWeakReference=Application.saveImageActivity;
		if(saveImageActivityWeakReference!=null)
		{
			@Nullable
			final SaveImageActivity saveImageActivity=saveImageActivityWeakReference.get();
			if(saveImageActivity!=null)
			{
				saveImageActivity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						@NonNull
						final Resources resources=saveImageActivity.getResources();
						@NonNull
						final AlertDialog.Builder builder=Application.getAlertDialogBuilder(saveImageActivity);
						builder.setCancelable(false);
						builder.setTitle(resources.getString(R.string.dialog_title_error));
						builder.setMessage(resources.getString(R.string.dialog_message_error,error));
						builder.setPositiveButton(resources.getString(R.string.dialog_button_ok),new AlertDialogOnClickListener());
						builder.show();
					}
				});
			}
		}
	}

	static void showWarningAlertDialog()
	{
		@Nullable
		final WeakReference<SaveImageActivity> saveImageActivityWeakReference=Application.saveImageActivity;
		if(saveImageActivityWeakReference!=null)
		{
			@Nullable
			final SaveImageActivity saveImageActivity=saveImageActivityWeakReference.get();
			if(saveImageActivity!=null)
			{
				saveImageActivity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						@NonNull
						final Resources resources=saveImageActivity.getResources();
						@NonNull
						final AlertDialog.Builder builder=Application.getAlertDialogBuilder(saveImageActivity);
						builder.setCancelable(true);
						builder.setMessage(resources.getString(R.string.dialog_message_image_is_in_gallery));
						builder.setPositiveButton(resources.getString(R.string.dialog_button_ok),new WarningAlertDialogOnClickListener());
						builder.show();
					}
				});
			}
		}
	}

	static class AlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialogInterface,int i)
		{
			@Nullable
			final WeakReference<SaveImageActivity> saveImageActivityWeakReference=Application.saveImageActivity;
			if(saveImageActivityWeakReference!=null)
			{
				@Nullable
				final SaveImageActivity saveImageActivity=saveImageActivityWeakReference.get();
				if(saveImageActivity!=null)
				{
					saveImageActivity.finish();
				}
			}
		}
	}

	class ShowSavedImageThread extends Thread
	{
		@Override
		public void run()
		{
			boolean isImageInGallery=false;
			try
			{
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(DiskUtils.getLinksFile(DiskUtils.getCacheDir(SaveImageActivity.this))));
				@Nullable
				String string;
				while((string=bufferedReader.readLine())!=null)
				{
					if(string.equals(url))
					{
						isImageInGallery=true;
					}
				}
				bufferedReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			@Nullable
			final ImageView imageView=SaveImageActivity.this.imageView;
			@Nullable
			final String url=SaveImageActivity.this.url;
			if(imageView!=null&&url!=null)
			{
				if(isImageInGallery)
				{
					@Nullable
					final ImageButton applyButton=SaveImageActivity.this.applyButton;
					if(applyButton!=null)
					{
						applyButton.setClickable(false);
						applyButton.setEnabled(false);
						@Nullable
						final WeakReference<SaveImageActivity> saveImageActivityWeakReference=Application.saveImageActivity;
						if(saveImageActivityWeakReference!=null)
						{
							@Nullable
							final Resources resources=saveImageActivityWeakReference.get().getResources();
							if(resources!=null)
							{
								applyButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark));
							}
						}
					}
					showWarningAlertDialog();
					ImagesDownloader.getImageFromDiskToSaveScreen(url,imageView,getApplicationContext());
				}
				else
				{
					ImagesDownloader.downloadImageToSaveScreen(url,imageView);
				}
			}
		}
	}

	static class WarningAlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialogInterface,int i)
		{
			dialogInterface.dismiss();
		}
	}
}