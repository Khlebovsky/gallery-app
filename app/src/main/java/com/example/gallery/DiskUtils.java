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
	@NonNull
	private static final String PREVIEWS="previews";
	@NonNull
	private static final String BYTES="bytes";
	@NonNull
	private static final String LINKS_FILE="links.txt";
	@Nullable
	private static File cacheDir;
	@Nullable
	private static File imagePreviewsDir;
	@Nullable
	private static File imagesBytesDir;
	@Nullable
	private static File linksFile;

	private DiskUtils()
	{
	}

	public static void addStringToLinksfile(@NonNull final String url,@NonNull final Context context)
	{
		@NonNull
		final File linksFile=getLinksFile(getCacheDir(context));
		@NonNull
		final ArrayList<String> LinksFileString=new ArrayList<>();
		try
		{
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(linksFile));
			String string;
			while((string=bufferedReader.readLine())!=null)
			{
				LinksFileString.add(string);
			}
			LinksFileString.add(url);
			bufferedReader.close();
			@NonNull
			final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(linksFile));
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

	static void createFile(@NonNull final File file)
	{
		try
		{
			if(!file.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				file.createNewFile();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	static void deleteImageFromDisk(@Nullable final String fileName,@NonNull final Context context)
	{
		if(fileName!=null)
		{
			try
			{
				int num=0;
				@NonNull
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(getLinksFile(getCacheDir(context))));
				@Nullable
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
					final File fullImage=new File(getImagesBytesDir(getCacheDir(context)),fileName);
					if(fullImage.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						fullImage.delete();
					}
					@NonNull
					final File preview=new File(getImagePreviewsDir(getCacheDir(context)),fileName);
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

	public static File getCacheDir(@NonNull final Context context)
	{
		@Nullable
		File cacheDir=DiskUtils.cacheDir;
		if(cacheDir!=null)
		{
			makeDir(cacheDir);
			return cacheDir;
		}
		try
		{
			if(Environment.isExternalStorageEmulated()||!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||(cacheDir=context.getExternalCacheDir())==null||!cacheDir.canWrite())
			{
				cacheDir=context.getCacheDir();
			}
		}
		catch(Throwable e)
		{
			cacheDir=context.getCacheDir();
		}
		makeDir(cacheDir);
		DiskUtils.cacheDir=cacheDir;
		return cacheDir;
	}

	public static File getImagePath(@Nullable final String url,@NonNull final File dir)
	{
		if(url!=null)
		{
			@NonNull
			final String fileName=ImagesDownloader.urlToHashMD5(url);
			@NonNull
			final File path=new File(dir,fileName);
			return path;
		}
		return null;
	}

	public static File getImagePreviewsDir(@NonNull final File cacheDir)
	{
		@Nullable
		File imagePreviewsDir=DiskUtils.imagePreviewsDir;
		if(imagePreviewsDir!=null)
		{
			makeDir(imagePreviewsDir);
			return imagePreviewsDir;
		}
		imagePreviewsDir=new File(cacheDir,PREVIEWS);
		DiskUtils.imagePreviewsDir=imagePreviewsDir;
		makeDir(imagePreviewsDir);
		return imagePreviewsDir;
	}

	public static File getImagesBytesDir(@NonNull final File cacheDir)
	{
		@Nullable
		File imageBytesDir=DiskUtils.imagesBytesDir;
		if(imageBytesDir!=null)
		{
			makeDir(imageBytesDir);
			return imageBytesDir;
		}
		imageBytesDir=new File(cacheDir,BYTES);
		DiskUtils.imagesBytesDir=imageBytesDir;
		makeDir(imageBytesDir);
		return imageBytesDir;
	}

	public static File getLinksFile(@NonNull final File cacheDir)
	{
		@Nullable
		File linksFile=DiskUtils.linksFile;
		if(linksFile!=null)
		{
			createFile(linksFile);
			return linksFile;
		}
		linksFile=new File(cacheDir,LINKS_FILE);
		DiskUtils.linksFile=linksFile;
		createFile(linksFile);
		return linksFile;
	}

	static void makeDir(@NonNull final File dir)
	{
		if(!dir.exists())
		{
			//noinspection ResultOfMethodCallIgnored
			dir.mkdirs();
		}
	}

	static void optimizeDisk(@NonNull final Context context)
	{
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
					@NonNull
					final File imagesBytesDir=getImagesBytesDir(getCacheDir(context));
					if(imagesBytesDir.exists())
					{
						@NonNull
						final String parentDir=imagesBytesDir+"/";
						@Nullable
						final File[] files=imagesBytesDir.listFiles();
						if(files!=null&&files.length!=0)
						{
							for(final File file : files)
							{
								@NonNull
								final String fileName=file.toString().substring(parentDir.length());
								if(!diskFiles.contains(fileName))
								{
									deleteImageFromDisk(fileName,context);
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

	public static void removeStringFromLinksfile(final int numToDelete,@NonNull final Context context)
	{
		@Nullable
		final File linksFile=getLinksFile(getCacheDir(context));
		@NonNull
		final ArrayList<String> LinksFileString=new ArrayList<>();
		try
		{
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(linksFile));
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
			final BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(linksFile));
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

	public static void updateUrlsList(@NonNull final Context context)
	{
		try
		{
			Application.URLS_LIST.clear();
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(getLinksFile(getCacheDir(context))));
			@Nullable
			String url;
			while((url=bufferedReader.readLine())!=null)
			{
				Application.URLS_LIST.add(url);
				Application.addUrlInUrlsStatusList(url,Application.PROGRESS);
			}
			bufferedReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}