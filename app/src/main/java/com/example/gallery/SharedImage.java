package com.example.gallery;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.ortiz.touchview.TouchImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class SharedImage extends AppCompatActivity
{
	public static boolean isFullScreen;
	public static TouchImageView touchImageView;
	static Context context;
	@NonNull
	private static final String TAG="ShareImage";
	private static final int FULLSCREENAPI=19;
	private static final int CUTOUTAPI=28;
	@SuppressWarnings("FieldCanBeLocal")
	private static String url;

	// TODO доделать логику добавления картинки
	void handleSendText(Intent intent)
	{
		@Nullable
		final String imageUrl=intent.getStringExtra(Intent.EXTRA_TEXT);
		if(imageUrl!=null)
		{
			url=imageUrl;
			setTitle(url);
			Log.d(TAG,imageUrl);
			ImageDownloader.downloadImageFromSharing(imageUrl,touchImageView,context);
		}
	}

	@RequiresApi(api=Build.VERSION_CODES.KITKAT)
	void hideSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
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
		setContentView(R.layout.activity_share_image);
		touchImageView=findViewById(R.id.share_image_view);
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
		final int size=getResources().getDimensionPixelSize(R.dimen.preloaderSize);
		touchImageView.setMaxZoom(1);
		touchImageView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
		touchImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		touchImageView.setImageResource(R.drawable.progress);
		context=SharedImage.this;
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
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@RequiresApi(api=Build.VERSION_CODES.KITKAT)
	void showSystemUI()
	{
		@NonNull
		final View decorView=getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}
}