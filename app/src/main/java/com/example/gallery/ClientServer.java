package com.example.gallery;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import com.muddzdev.styleabletoast.StyleableToast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import okhttp3.*;

public final class ClientServer
{
	@NonNull
	static final RequestHandler REQUEST_HANDLER=new RequestHandler();
	@SuppressWarnings("unused")
	@NonNull
	private static final String TAG="ClientServer";
	@NonNull
	private static final String SCRIPT_URL="https://khlebovsky.ru/linkseditor.php";
	@NonNull
	private static final String SCRIPT_LOGIN="testinggalleryapprequest";
	@NonNull
	private static final String SCRIPT_PASSWORD="6e=Cmf&pUk7Lp{M@Gdq+";
	@NonNull
	private static final String DELETE_TASK="Delete";
	@SuppressWarnings("unused")
	@NonNull
	private static final String ADD_TASK="Add";

	private ClientServer()
	{
	}

	public static void addImage(final String url)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@NonNull
					final OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(ConnectionSettings.getTLSSocketFactory(),ConnectionSettings.getTrustManager()[0]).build();
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

	// TODO протестировать удаление и добавление на всех устройствах
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
					final OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(ConnectionSettings.getTLSSocketFactory(),ConnectionSettings.getTrustManager()[0]).build();
					@NonNull
					final RequestBody requestBody=new FormBody.Builder().add("Login",SCRIPT_LOGIN).add("Password",SCRIPT_PASSWORD).add("Task",DELETE_TASK).add("Delete",String.valueOf(numToDelete))
						.add("Link",ImageAdapter.URLS.get(numToDelete)).build();
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

	private static final class AddImageRequestCallback implements Callback
	{
		private final String url;

		AddImageRequestCallback(String url)
		{
			this.url=url;
		}

		@Override
		public void onFailure(@NonNull Call call,@NonNull IOException e)
		{
			@NonNull
			final Message message=REQUEST_HANDLER.obtainMessage(0,"error");
			REQUEST_HANDLER.sendMessage(message);
		}

		@Override
		public void onResponse(@NonNull Call call,@NonNull Response response)
		{
			if(MainActivity.linksFile!=null)
			{
				@NonNull
				final ArrayList<String> LinksFileString=new ArrayList<>();
				try
				{
					@NonNull
					final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
					String string;
					while((string=bufferedReader.readLine())!=null)
					{
						LinksFileString.add(string);
					}
					LinksFileString.add(url);
					bufferedReader.close();
					@NonNull
					final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(MainActivity.linksFile));
					for(final String str : LinksFileString)
					{
						bufferedWriter.write(str+'\n');
					}
					bufferedWriter.flush();
					bufferedReader.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			@NonNull
			Message message=REQUEST_HANDLER.obtainMessage(0,1,1,url);
			REQUEST_HANDLER.sendMessage(message);
			ImageAdapter.URLS.add(url);
			message=ImageDownloader.GALLERY_HANDLER.obtainMessage();
			ImageDownloader.GALLERY_HANDLER.sendMessage(message);
		}
	}

	private static final class DeleteImageRequestCallback implements Callback
	{
		private final int numToDelete;

		DeleteImageRequestCallback(int numToDelete)
		{
			this.numToDelete=numToDelete;
		}

		@Override
		public void onFailure(@NonNull Call call,@NonNull IOException e)
		{
			@NonNull
			final Message message=REQUEST_HANDLER.obtainMessage(0,"error");
			REQUEST_HANDLER.sendMessage(message);
		}

		@Override
		public void onResponse(@NonNull Call call,@NonNull Response response)
		{
			if(MainActivity.linksFile!=null)
			{
				@NonNull
				final ArrayList<String> LinksFileString=new ArrayList<>();
				try
				{
					@NonNull
					final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
					String string;
					while((string=bufferedReader.readLine())!=null)
					{
						if(!string.equals(ImageAdapter.URLS.get(numToDelete)))
						{
							LinksFileString.add(string);
						}
					}
					bufferedReader.close();
					@NonNull
					final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(MainActivity.linksFile));
					for(final String str : LinksFileString)
					{
						bufferedWriter.write(str+'\n');
					}
					bufferedWriter.flush();
					bufferedReader.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			@NonNull
			Message message=REQUEST_HANDLER.obtainMessage(0,2,2,ImageAdapter.URLS.get(numToDelete));
			REQUEST_HANDLER.sendMessage(message);
			ImageAdapter.URLS.remove(numToDelete);
			message=ImageDownloader.GALLERY_HANDLER.obtainMessage();
			ImageDownloader.GALLERY_HANDLER.sendMessage(message);
		}
	}

	@SuppressLint("HandlerLeak")
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
			@NonNull
			final String result=(String)msg.obj;
			if("error".equals(result))
			{
				StyleableToast.makeText(MainActivity.context,"Произошла ошибка. Попробуйте ещё раз",Toast.LENGTH_SHORT,R.style.ToastStyle).show();
			}
			else
			{
				final int status=msg.arg1;
				switch(status)
				{
					case 1:
						StyleableToast.makeText(MainActivity.context,"Картинка добавлена: \n"+result,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
						break;
					case 2:
						StyleableToast.makeText(MainActivity.context,"Картинка удалена: \n"+result,Toast.LENGTH_SHORT,R.style.ToastStyle).show();
						break;
				}
			}
		}
	}
}
