package com.example.gallery;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SharedImage extends AppCompatActivity
{
	public static ImageView imageView;
	static Context context;
	static String url;
	static SharedImage sharedImage;
	@NonNull
	private static final String MIMETYPE_TEXT="text/plain";
	private static ImageButton applyButton;

	void handleSendText(@NonNull Intent intent)
	{
		@Nullable
		final String imageUrl=intent.getStringExtra(Intent.EXTRA_TEXT);
		if(imageUrl!=null)
		{
			url=imageUrl;
			setTitle(url);
			// TODO упростить логику
			if(ImageAdapter.URLS.contains(url))
			{
				applyButton.setClickable(false);
				applyButton.setEnabled(false);
				applyButton.setBackgroundColor(MainActivity.resources.getColor(R.color.colorPrimaryDark));
				showWarningAlertDialog();
			}
			ImageDownloader.downloadImageFromSharing(url,imageView,context);
		}
		else
		{
			showSharedImageAlertDialog("Invalid link");
			imageView.setImageResource(R.drawable.ic_error);
		}
	}

	void initStatic()
	{
		if(MainActivity.cache==null)
		{
			MainActivity.initCacheDirs(getBaseContext());
		}
		if(MainActivity.resources==null)
		{
			MainActivity.resources=getResources();
		}
		sharedImage=SharedImage.this;
		context=SharedImage.this;
		imageView=findViewById(R.id.share_image_view);
		@NonNull
		final ImageButton cancelButton=findViewById(R.id.cancel_button);
		applyButton=findViewById(R.id.apply_button);
		final int size=MainActivity.resources.getDimensionPixelSize(R.dimen.preloaderSize);
		imageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		imageView.setImageResource(R.drawable.progress);
		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				finish();
			}
		});
		applyButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				ClientServer.addImage(url);
				@NonNull
				final Intent intent=new Intent(SharedImage.this,MainActivity.class);
				intent.setPackage(getPackageName());
				try
				{
					startActivity(intent);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				ImageDownloader.callNotifyDataSetChanged();
				finish();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		MainActivity.sharedPreferences=getSharedPreferences("Gallery",MODE_PRIVATE);
		MainActivity.initTheme();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shared_image);
		setHomeButton();
		initStatic();
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
			showSharedImageAlertDialog("The image is not transferred or this link format is not supported");
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if(item.getItemId()==android.R.id.home)
		{
			final Intent intent=new Intent(SharedImage.this,MainActivity.class);
			intent.setPackage(getPackageName());
			try
			{
				startActivity(intent);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			ImageDownloader.callNotifyDataSetChanged();
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

	public static void showSharedImageAlertDialog(@NonNull final String error)
	{
		sharedImage.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				@NonNull
				final AlertDialog.Builder builder=new AlertDialog.Builder(sharedImage,R.style.AlertDialogTheme);
				builder.setCancelable(false);
				builder.setTitle("Ошибка");
				builder.setMessage("Ошибка: \n"+error);
				builder.setPositiveButton("Ок",new AlertDialogOnClickListener());
				builder.show();
			}
		});
	}

	static void showWarningAlertDialog()
	{
		sharedImage.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				@NonNull
				final AlertDialog.Builder builder=new AlertDialog.Builder(sharedImage,R.style.AlertDialogTheme);
				builder.setCancelable(true);
				builder.setTitle("Предупреждение");
				builder.setMessage("Предупреждение: \nКартинка уже есть в списке");
				builder.setPositiveButton("Ок",new WarningAlertDialogOnClickListener());
				builder.show();
			}
		});
	}

	static class AlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialogInterface,int i)
		{
			sharedImage.finish();
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