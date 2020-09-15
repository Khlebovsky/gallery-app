package com.example.gallery;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import com.muddzdev.styleabletoast.StyleableToast;
import java.io.*;
import java.lang.ref.WeakReference;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.*;

public final class ClientServer
{
	@NonNull
	static final RequestHandler REQUEST_HANDLER=new RequestHandler();
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
	@NonNull
	private static final String ERROR="error";

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
			@NonNull
			final Message message=REQUEST_HANDLER.obtainMessage(0,ERROR);
			REQUEST_HANDLER.sendMessage(message);
		}

		@Override
		public void onResponse(@NonNull Call call,@NonNull Response response)
		{
			if(context!=null)
			{
				DiskUtils.addStringToLinksfile(url,context);
			}
			Application.URLS_LIST.add(url);
			@NonNull
			final Message message=REQUEST_HANDLER.obtainMessage(0,1,1,url);
			REQUEST_HANDLER.sendMessage(message);
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
			@NonNull
			final Message message=REQUEST_HANDLER.obtainMessage(0,ERROR);
			REQUEST_HANDLER.sendMessage(message);
		}

		@Override
		public void onResponse(@NonNull Call call,@NonNull Response response)
		{
			@NonNull
			final String urlToDelete=Application.URLS_LIST.get(numToDelete);
			@NonNull
			final String fileName=ImagesDownloader.urlToHashMD5(urlToDelete);
			if(context!=null)
			{
				DiskUtils.removeStringFromLinksfile(numToDelete,context);
				DiskUtils.deleteImageFromDisk(fileName,context);
			}
			Application.URLS_FILE_NAMES.remove(urlToDelete);
			Application.removeUrlFromUrlsStatusList(urlToDelete);
			Application.URLS_LIST.remove(numToDelete);
			@NonNull
			final Message message=REQUEST_HANDLER.obtainMessage(0,2,2,urlToDelete);
			REQUEST_HANDLER.sendMessage(message);
			ImagesAdapter.callNotifyDataSetChanged();
		}
	}

	static class RequestHandler extends Handler
	{
		RequestHandler()
		{
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(@NonNull Message msg)
		{
			super.handleMessage(msg);
			@Nullable
			final String result=(String)msg.obj;
			@Nullable
			final WeakReference<MainActivity> mainActivityWeakReference=Application.mainActivity;
			if(mainActivityWeakReference!=null)
			{
				@Nullable
				final MainActivity mainActivity=mainActivityWeakReference.get();
				if(mainActivity!=null)
				{
					if(result!=null)
					{
						if(ERROR.equals(result))
						{
							StyleableToast.makeText(mainActivity,"Произошла ошибка. Попробуйте ещё раз",Toast.LENGTH_SHORT,R.style.ToastStyle).show();
						}
						else
						{
							final int status=msg.arg1;
							switch(status)
							{
								case 1:
									StyleableToast.makeText(mainActivity,"Картинка добавлена: \n"+result,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
									break;
								case 2:
									StyleableToast.makeText(mainActivity,"Картинка удалена: \n"+result,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
									break;
							}
						}
					}
				}
			}
		}
	}
}
