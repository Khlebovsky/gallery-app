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

	void handleSendText(@NonNull Intent intent)
	{
		@Nullable
		final String imageUrl=intent.getStringExtra(Intent.EXTRA_TEXT);
		@Nullable
		final ImageView imageView_=imageView;
		if(imageView_!=null)
		{
			if(imageUrl!=null)
			{
				url=imageUrl;
				setTitle(imageUrl);
				if(Application.saveImageActivity!=null)
				{
					@Nullable
					final SaveImageActivity saveImageActivity=Application.saveImageActivity.get();
					if(saveImageActivity!=null)
					{
						@Nullable
						final Resources resources=saveImageActivity.getResources();
						if(resources!=null)
						{
							final int size=resources.getDimensionPixelSize(R.dimen.preloaderSize);
							imageView_.setLayoutParams(new LinearLayout.LayoutParams(size,size));
						}
					}
					imageView_.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					imageView_.setImageResource(R.drawable.progress);
					new ShowSavedImageThread().start();
				}
			}
			else
			{
				showSaveImageAlertDialog("Invalid link");
				imageView_.setImageResource(R.drawable.ic_error);
			}
		}
	}

	void initObjects()
	{
		Application.saveImageActivity=new WeakReference<>(SaveImageActivity.this);
		if(Application.cacheDir==null)
		{
			DiskUtils.initCacheDirs(this);
		}
		imageView=findViewById(R.id.share_image_view);
		applyButton=findViewById(R.id.apply_button);
		@Nullable
		final ImageView imageView_=imageView;
		if(imageView_!=null)
		{
			@Nullable
			final Resources resources=Application.saveImageActivity.get().getResources();
			if(resources!=null)
			{
				final int size=resources.getDimensionPixelSize(R.dimen.preloaderSize);
				imageView_.setLayoutParams(new LinearLayout.LayoutParams(size,size));
			}
			imageView_.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			imageView_.setImageResource(R.drawable.progress);
		}
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
		}
		else
		{
			showSaveImageAlertDialog("The image is not transferred or this link format is not supported");
		}
		setOnClickListeners();
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
		final ImageButton applyButton_=applyButton;
		if(applyButton_!=null&&url!=null)
		{
			applyButton_.setOnClickListener(new View.OnClickListener()
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
		if(Application.saveImageActivity!=null)
		{
			@Nullable
			final SaveImageActivity saveImageActivity=Application.saveImageActivity.get();
			if(saveImageActivity!=null)
			{
				saveImageActivity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						@NonNull
						final AlertDialog.Builder builder=new AlertDialog.Builder(saveImageActivity,R.style.AlertDialogTheme);
						builder.setCancelable(false);
						builder.setTitle("Ошибка");
						builder.setMessage("Ошибка: \n"+error);
						builder.setPositiveButton("ОК",new AlertDialogOnClickListener());
						builder.show();
					}
				});
			}
		}
	}

	static void showWarningAlertDialog()
	{
		if(Application.saveImageActivity!=null)
		{
			@Nullable
			final SaveImageActivity saveImageActivity=Application.saveImageActivity.get();
			if(saveImageActivity!=null)
			{
				saveImageActivity.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						@NonNull
						final AlertDialog.Builder builder=new AlertDialog.Builder(saveImageActivity,R.style.AlertDialogTheme);
						builder.setCancelable(true);
						builder.setMessage("Картинка уже есть в списке");
						builder.setPositiveButton("ОК",new WarningAlertDialogOnClickListener());
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
			if(Application.saveImageActivity!=null)
			{
				@Nullable
				final SaveImageActivity saveImageActivity=Application.saveImageActivity.get();
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
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(Application.linksFile));
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
			final ImageView imageView_=imageView;
			@NonNull
			final Context context=SaveImageActivity.this;
			if(imageView_!=null)
			{
				if(isImageInGallery)
				{
					@Nullable
					final ImageButton applyButton_=applyButton;
					if(applyButton_!=null)
					{
						applyButton_.setClickable(false);
						applyButton_.setEnabled(false);
						if(Application.saveImageActivity!=null)
						{
							@Nullable
							final Resources resources=Application.saveImageActivity.get().getResources();
							if(resources!=null)
							{
								applyButton_.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark));
							}
						}
					}
					showWarningAlertDialog();
					ImagesDownloader.getImageFromDiskToSaveScreen(url,imageView_,context);
				}
				else if(url!=null)
				{
					ImagesDownloader.downloadImageToSaveScreen(url,imageView_,context);
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