package com.example.gallery;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class PreloaderActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		@NonNull
		final Intent intent=new Intent(PreloaderActivity.this,MainActivity.class);
		intent.setPackage(getPackageName());
		try
		{
			startActivity(intent);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finish();
	}
}