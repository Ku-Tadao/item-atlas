package com.itematlas.db;

import com.google.common.base.Stopwatch;
import com.itematlas.db.queries.ModeledQuery;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class H2DataProvider
{

	private static final ExecutorService DB_THREAD = Executors.newSingleThreadExecutor();

	private final H2DbFetcher dbFetcher;

	private volatile AtlasData data;

	public void init(Filepath dir)
	{
		dbFetcher.fetch(dir, file ->
			DB_THREAD.submit(() ->
			{
				Stopwatch sw = Stopwatch.createStarted();
				try (Reader in = new InputStreamReader(new GZIPInputStream(file.openInputStream()), StandardCharsets.UTF_8))
				{
					data = AtlasData.read(in);
					log.debug("Loaded {} in {}ms", file, sw.elapsed(TimeUnit.MILLISECONDS));
				}
				catch (Exception e)
				{
					log.error("Failed to load the data file", e);
				}
			}));
	}

	public <T> void executeSingle(ModeledQuery<T> query, Consumer<T> callback)
	{
		executeMany(query, res ->
			callback.accept(res.isEmpty() ? null : res.get(0)));
	}

	public <T> void executeMany(ModeledQuery<T> query, Consumer<List<T>> callback)
	{
		AtlasData data = this.data;
		if (data == null)
		{
			callback.accept(Collections.emptyList());
			return;
		}

		DB_THREAD.submit(() ->
		{
			Stopwatch sw = Stopwatch.createStarted();
			List<T> res;
			try
			{
				res = query.run(data);
			}
			catch (Exception e)
			{
				log.warn("Query failed in executeMany", e);
				res = Collections.emptyList();
			}
			log.debug("[{}μs] Query {}", sw.elapsed(TimeUnit.MICROSECONDS), query.getClass().getSimpleName());
			callback.accept(res);
		});
	}

}
