package com.itematlas.db;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class H2DbFetcher
{

	private static final String dbFileName = "data.json.gz";
	private static final String remoteUrl = "https://raw.githubusercontent.com/Ku-Tadao/runelite-wiki-scraper/wiki-data/output/" + dbFileName;

	private final OkHttpClient httpClient;

	// calls back with the data file once it matches the remote checksum, or with the old file when offline
	public void fetch(Filepath dir, Consumer<Filepath> callback)
	{
		Filepath dbFile = dir.join(dbFileName);
		httpClient.newCall(get(remoteUrl + ".sha256")).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to download remote checksum for data file", e);
				if (dbFile.exists())
				{
					callback.accept(dbFile);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				String checksum;
				try (response)
				{
					checksum = response.body().string().trim();
				}

				if (dbFile.exists() && checksum.equals(sha256(dbFile)))
				{
					callback.accept(dbFile);
				}
				else
				{
					log.debug("Missing data file or checksum mismatch. Downloading it");
					download(dbFile, callback);
				}
			}
		});
	}

	private void download(Filepath dbFile, Consumer<Filepath> callback)
	{
		httpClient.newCall(get(remoteUrl)).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to download remote data file", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (response)
				{
					if (!response.isSuccessful())
					{
						log.error("Failed to download remote data file: HTTP {}", response.code());
						return;
					}
					dbFile.write(response.body().bytes());
				}
				callback.accept(dbFile);
			}
		});
	}

	private static Request get(String url)
	{
		return new Request.Builder().get().url(url).build();
	}

	private static String sha256(Filepath file) throws IOException
	{
		try (InputStream is = file.openInputStream())
		{
			return Hashing.sha256().hashBytes(is.readAllBytes()).toString();
		}
	}

}
