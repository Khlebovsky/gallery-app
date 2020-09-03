package com.example.gallery;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		@NonNull
		final Intent intent=new Intent(LauncherActivity.this,MainActivity.class);
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