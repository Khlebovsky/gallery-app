package com.example.gallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.github.chrisbanes.photoview.PhotoView;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FullImage extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_image);
		@NonNull
		final Intent intent=getIntent();
		//noinspection ConstantConditions
		@NonNull
		final String image=intent.getExtras().getString("image");
		@NonNull
		final PhotoView photoView=findViewById(R.id.photo_view);
		photoView.setMaximumScale(5);
		@NonNull
		final String path=MainActivity.bytes+File.separator+image;
		try
		{
			@NonNull
			final BitmapWorkerTask task=new BitmapWorkerTask(photoView,path);
			task.execute(1);
		}
		catch(Exception e)
		{
			@NonNull
			final AlertDialog.Builder builder=new AlertDialog.Builder(FullImage.this,R.style.AlertDialogStyle);
			builder.setTitle("Ошибка");
			builder.setMessage("Ошибка: \nПроизошла непредвиденная ошибка");
			builder.setPositiveButton("OK",new MainActivity.MyOnClickListener());
			builder.show();
			e.printStackTrace();
			finish();
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

	class BitmapWorkerTask extends AsyncTask<Integer,Void,Bitmap>
	{
		@NonNull
		final WeakReference<PhotoView> photoViewReference;
		@NonNull
		Timer timer=new Timer();
		@NonNull
		private String image;

		BitmapWorkerTask(PhotoView photoView,@NonNull String path)
		{
			photoViewReference=new WeakReference<>(photoView);
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
				@NonNull
				final PhotoView photoView=photoViewReference.get();
				if(photoView!=null)
				{
					photoView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
					photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					photoView.setImageBitmap(bitmap);
					photoView.setZoomable(true);
				}
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
				@NonNull
				final PhotoView photoView=photoViewReference.get();
				photoView.setZoomable(false);
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						final int size=getResources().getDimensionPixelSize(R.dimen.preloaderSize);
						photoView.setLayoutParams(new LinearLayout.LayoutParams(size,size));
						photoView.setZoomable(false);
						photoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						photoView.setScale(1);
						photoView.setImageResource(R.drawable.progress);
					}
				});
			}
		}
	}
}
