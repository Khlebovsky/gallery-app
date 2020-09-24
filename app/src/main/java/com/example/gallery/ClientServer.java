package com.example.gallery;

import android.content.Context;
import android.os.Message;
import java.io.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.*;

public final class ClientServer
{
	@Nullable
	static Context context;
	@NonNull
	private static final String SCRIPT_URL="https://khlebovsky.ru/linkseditor.php";
	@NonNull
	private static final String SCRIPT_LOGIN="testinggalleryapprequest";
	@NonNull
	private static final String SCRIPT_PASSWORD="6e=Cmf&pUk7Lp{M@Gdq+";
	@NonNull
	private static final String DELETE_TASK="Delete";
	@NonNull
	private static final String ADD_TASK="Add";

	private ClientServer()
	{
	}

	public static void addImage(@NonNull final String url)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@NonNull
					final OkHttpClient client=ConnectionSettings.getOkHttpClient();
					@NonNull
					final RequestBody requestBody=new FormBody.Builder().add("Login",SCRIPT_LOGIN).add("Password",SCRIPT_PASSWORD).add("Task",ADD_TASK).add("Add",url).build();
					@NonNull
					final Call call=client.newCall(new Request.Builder().url(SCRIPT_URL).post(requestBody).build());
					call.enqueue(new AddImageRequestCallback(url));
					call.execute();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void deleteImage(final int numToDelete)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@NonNull
					final OkHttpClient client=ConnectionSettings.getOkHttpClient();
					@NonNull
					final RequestBody requestBody=new FormBody.Builder().add("Login",SCRIPT_LOGIN).add("Password",SCRIPT_PASSWORD).add("Task",DELETE_TASK).add("Delete",String.valueOf(numToDelete))
						.add("Link",Application.URLS_LIST.get(numToDelete)).build();
					@NonNull
					final Call call=client.newCall(new Request.Builder().url(SCRIPT_URL).post(requestBody).build());
					call.enqueue(new DeleteImageRequestCallback(numToDelete));
					call.execute();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void init(@NonNull final Context context)
	{
		ClientServer.context=context;
	}

	private static final class AddImageRequestCallback implements Callback
	{
		@NonNull
		private final String url;

		AddImageRequestCallback(@NonNull final String url)
		{
			this.url=url;
		}

		@Override
		public void onFailure(@NonNull Call call,@NonNull IOException e)
		{
			if(context!=null)
			{
				@NonNull
				final Message message=MainActivity.TOAST_HANDLER.obtainMessage(0,context.getString(R.string.image_add_error));
				MainActivity.TOAST_HANDLER.sendMessage(message);
			}
		}

		@Override
		public void onResponse(@NonNull Call call,@NonNull Response response)
		{
			@NonNull
			final String url=this.url;
			Application.URLS_LIST.add(url);
			@Nullable
			final Context context=ClientServer.context;
			if(context!=null)
			{
				DiskUtils.addStringToLinksfile(url,context);
				@NonNull
				final String stringMessage=context.getString(R.string.image_add_success,url);
				@NonNull
				final Message message=MainActivity.TOAST_HANDLER.obtainMessage(0,stringMessage);
				MainActivity.TOAST_HANDLER.sendMessage(message);
			}
			ImagesAdapter.callNotifyDataSetChanged();
		}
	}

	private static final class DeleteImageRequestCallback implements Callback
	{
		private final int numToDelete;

		DeleteImageRequestCallback(final int numToDelete)
		{
			this.numToDelete=numToDelete;
		}

		@Override
		public void onFailure(@NonNull Call call,@NonNull IOException e)
		{
			if(context!=null)
			{
				@NonNull
				final Message message=MainActivity.TOAST_HANDLER.obtainMessage(0,context.getString(R.string.image_delete_error));
				MainActivity.TOAST_HANDLER.sendMessage(message);
			}
		}

		@Override
		public void onResponse(@NonNull Call call,@NonNull Response response)
		{
			final int numToDelete=this.numToDelete;
			@NonNull
			final String urlToDelete=Application.URLS_LIST.get(numToDelete);
			@NonNull
			final String fileName=ImagesDownloader.urlToHashMD5(urlToDelete);
			Application.URLS_FILE_NAMES.remove(urlToDelete);
			Application.removeUrlFromUrlsStatusList(urlToDelete);
			Application.URLS_LIST.remove(numToDelete);
			@Nullable
			final Context context=ClientServer.context;
			if(context!=null)
			{
				DiskUtils.removeStringFromLinksfile(numToDelete,context);
				DiskUtils.deleteImageFromDisk(fileName,context);
				@NonNull
				final String stringMessage=context.getString(R.string.image_delete_success,urlToDelete);
				@NonNull
				final Message message=MainActivity.TOAST_HANDLER.obtainMessage(0,stringMessage);
				MainActivity.TOAST_HANDLER.sendMessage(message);
			}
			ImagesAdapter.callNotifyDataSetChanged();
		}
	}
}
