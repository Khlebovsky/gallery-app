package com.example.gallery;

import android.content.Context;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DiskUtils
{
	private DiskUtils()
	{
	}

	public static void addStringToLinksfile(@NonNull final String url)
	{
		@Nullable
		final File linksFile_=Application.linksFile;
		if(linksFile_!=null)
		{
			@NonNull
			final ArrayList<String> LinksFileString=new ArrayList<>();
			try
			{
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(linksFile_));
				String string;
				while((string=bufferedReader.readLine())!=null)
				{
					LinksFileString.add(string);
				}
				LinksFileString.add(url);
				bufferedReader.close();
				@NonNull
				final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(linksFile_));
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
			ImagesAdapter.callNotifyDataSetChanged();
		}
	}

	static void deleteImageFromDisk(@Nullable final String fileName)
	{
		if(fileName!=null)
		{
			try
			{
				int num=0;
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(Application.linksFile));
				String string;
				while((string=bufferedReader.readLine())!=null)
				{
					if(fileName.equals(ImagesDownloader.urlToHashMD5(string)))
					{
						num++;
					}
				}
				bufferedReader.close();
				if(num>1)
				{
					@NonNull
					final File fullImage=new File(Application.imagesBytesDir,fileName);
					if(fullImage.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						fullImage.delete();
					}
					@NonNull
					final File preview=new File(Application.imagePreviewsDir,fileName);
					if(preview.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						preview.delete();
					}
					Application.URLS_FILE_NAMES.remove(fileName);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void initCacheDirs(@Nullable final Context context)
	{
		if(context!=null)
		{
			try
			{
				if(Environment.isExternalStorageEmulated()||!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||(Application.cacheDir=context.getExternalCacheDir())==null||!Application.cacheDir.canWrite())
				{
					Application.cacheDir=context.getCacheDir();
				}
			}
			catch(Throwable e)
			{
				Application.cacheDir=context.getCacheDir();
			}
			// TODO разбить на методы
			@NonNull
			final File cacheDir_=Application.cacheDir;
			Application.imagePreviewsDir=new File(cacheDir_,"previews");
			Application.imagesBytesDir=new File(cacheDir_,"bytes");
			Application.textfilesDir=new File(cacheDir_,"textfiles");
			Application.linksFile=new File(Application.textfilesDir,"links.txt");
			try
			{
				if(!cacheDir_.exists())
				{
					//noinspection ResultOfMethodCallIgnored
					cacheDir_.mkdirs();
				}
				@NonNull
				final File imagePreviewsDir_=Application.imagePreviewsDir;
				if(imagePreviewsDir_.exists())
				{
					//noinspection ResultOfMethodCallIgnored
					imagePreviewsDir_.mkdirs();
				}
				@NonNull
				final File imageBytesDir_=Application.imagesBytesDir;
				if(!imageBytesDir_.exists())
				{
					//noinspection ResultOfMethodCallIgnored
					imageBytesDir_.mkdirs();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			try
			{
				@NonNull
				final File linksFile_=Application.linksFile;
				if(!linksFile_.exists())
				{
					@NonNull
					final File textfilesdir=new File(cacheDir_,"textfiles");
					if(!textfilesdir.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						textfilesdir.mkdirs();
					}
					if(!linksFile_.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						linksFile_.createNewFile();
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	static void optimizeDisk()
	{
		// TODO определение папки
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					@NonNull
					final ArrayList<String> diskFiles=new ArrayList<>();
					for(final String string : Application.URLS_LIST)
					{
						diskFiles.add(ImagesDownloader.urlToHashMD5(string));
					}
					@Nullable
					final File imagesBytesDir_=Application.imagesBytesDir;
					if(imagesBytesDir_!=null&&imagesBytesDir_.exists())
					{
						@NonNull
						final String parentDir=imagesBytesDir_+"/";
						@Nullable
						final File[] files=imagesBytesDir_.listFiles();
						if(files!=null&&files.length!=0)
						{
							for(final File file : files)
							{
								@NonNull
								final String fileName=file.toString().substring(parentDir.length());
								if(!diskFiles.contains(fileName))
								{
									deleteImageFromDisk(fileName);
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void removeStringFromLinksfile(final int numToDelete)
	{
		@Nullable
		final File linksFile_=Application.linksFile;
		if(linksFile_!=null)
		{
			@NonNull
			final ArrayList<String> LinksFileString=new ArrayList<>();
			try
			{
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(linksFile_));
				String string;
				while((string=bufferedReader.readLine())!=null)
				{
					if(!string.equals(Application.URLS_LIST.get(numToDelete)))
					{
						LinksFileString.add(string);
					}
				}
				bufferedReader.close();
				@NonNull
				final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(linksFile_));
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
			ImagesAdapter.callNotifyDataSetChanged();
		}
	}

	public static void updateUrlsList()
	{
		try
		{
			Application.URLS_LIST.clear();
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(Application.linksFile));
			@Nullable
			String url;
			while((url=bufferedReader.readLine())!=null)
			{
				Application.URLS_LIST.add(url);
				Application.URLS_LINKS_STATUS.put(url,"progress");
			}
			bufferedReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
