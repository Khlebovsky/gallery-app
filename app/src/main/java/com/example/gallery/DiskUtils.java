package com.example.gallery;

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
		final File linksFile_=MainActivity.linksFile;
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
				final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
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
					final File fullImage=new File(MainActivity.imagesBytesDir,fileName);
					if(fullImage.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						fullImage.delete();
					}
					@NonNull
					final File preview=new File(MainActivity.imagePreviewsDir,fileName);
					if(preview.exists())
					{
						//noinspection ResultOfMethodCallIgnored
						preview.delete();
					}
					ImagesDownloader.URLS_FILE_NAMES.remove(fileName);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void makeDirs()
	{
		@Nullable
		final File cacheDir=MainActivity.cacheDir;
		try
		{
			if(cacheDir!=null&&!cacheDir.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				cacheDir.mkdirs();
			}
			@Nullable
			final File imagePreviewsDir=MainActivity.imagePreviewsDir;
			if(imagePreviewsDir!=null&&!imagePreviewsDir.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				imagePreviewsDir.mkdirs();
			}
			@Nullable
			final File imageBytesDir=MainActivity.imagesBytesDir;
			if(imageBytesDir!=null&&!imageBytesDir.exists())
			{
				//noinspection ResultOfMethodCallIgnored
				imageBytesDir.mkdirs();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			@Nullable
			final File linksFile_=MainActivity.linksFile;
			if(linksFile_!=null&&!linksFile_.exists())
			{
				@NonNull
				final File textfilesdir=new File(cacheDir,"textfiles");
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

	static void optimizeDisk()
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
					for(final String string : ImagesAdapter.URLS_LIST)
					{
						diskFiles.add(ImagesDownloader.urlToHashMD5(string));
					}
					@Nullable
					final File imagesBytesDir_=MainActivity.imagesBytesDir;
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

	public static void removeStringFromLinksfile(int numToDelete)
	{
		@Nullable
		final File linksFile_=MainActivity.linksFile;
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
					if(!string.equals(ImagesAdapter.URLS_LIST.get(numToDelete)))
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
		ImagesAdapter.URLS_LIST.clear();
		try
		{
			@NonNull
			final BufferedReader bufferedReader=new BufferedReader(new FileReader(MainActivity.linksFile));
			String url;
			while((url=bufferedReader.readLine())!=null)
			{
				ImagesAdapter.URLS_LIST.add(url);
				MainActivity.URLS_LINKS_STATUS.put(url,"progress");
			}
			bufferedReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
