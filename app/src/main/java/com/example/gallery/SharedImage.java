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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SharedImage extends AppCompatActivity
{
	public static ImageView imageView;
	static Context context;
	@SuppressWarnings("FieldCanBeLocal")
	static String url;
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="SharedImage";

	void handleSendText(Intent intent)
	{
		@NonNull
		final AlertDialog.Builder builder=new AlertDialog.Builder(SharedImage.this,R.style.AlertDialogStyle);
		builder.setCancelable(false);
		builder.setTitle("Ошибка");
		builder.setPositiveButton("OK",new AlertDialogOnClickListener());
		@Nullable
		final String imageUrl=intent.getStringExtra(Intent.EXTRA_TEXT);
		if(imageUrl!=null)
		{
			url=imageUrl;
			setTitle(url);
			ImageDownloader.downloadImageFromSharing(imageUrl,imageView,context,builder);
		}
		else
		{
			builder.setMessage("Ошибка: \nНевалидная ссылка");
			builder.show();
			imageView.setImageResource(R.drawable.ic_error);
		}
	}

	void initStatic()
	{
		MainActivity.initCacheDirs(getBaseContext());
		imageView=findViewById(R.id.share_image_view);
		@NonNull
		final ImageButton cancelButton=(ImageButton)findViewById(R.id.cancel_button);
		@NonNull
		final ImageButton applyButton=(ImageButton)findViewById(R.id.apply_button);
		final int size=getResources().getDimensionPixelSize(R.dimen.preloaderSize);
		imageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		imageView.setImageResource(R.drawable.progress);
		context=SharedImage.this;
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
					finish();
				}
			}
		});
	}

	// TODO исправить обработку входящих ссылок
	// TODO исправить цвет кнопок до API21
	// TODO исправить обрезание картинки навигацинными кнопками
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		MainActivity.sharedPreferences=getSharedPreferences("Gallery",MODE_PRIVATE);
		MainActivity.initTheme();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share_image);
		initStatic();
		@NonNull
		final Intent intent=getIntent();
		@Nullable
		final String action=intent.getAction();
		@Nullable
		final String type=intent.getType();
		if(Intent.ACTION_SEND.equals(action)&&"text/plain".equals(type))
		{
			handleSendText(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		@NonNull
		final MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.share_image_menu,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if(item.getItemId()==R.id.home)
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	class AlertDialogOnClickListener implements DialogInterface.OnClickListener
	{
		@Override
		public void onClick(DialogInterface dialogInterface,int i)
		{
			finish();
		}
	}
}