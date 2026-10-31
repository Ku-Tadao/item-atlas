package com.itematlas.db;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * The wiki tables from data.json.gz, held in memory.
 * The file is {"TABLE": {"columns": [...], "rows": [[...], ...]}}, rows in ID order.
 */
@RequiredArgsConstructor
public class AtlasData
{

	private final Map<String, List<Row>> tables;

	public List<Row> table(String name)
	{
		return tables.getOrDefault(name, Collections.emptyList());
	}

	// rows are in ID order
	public Row byId(String table, int id)
	{
		List<Row> rows = table(table);
		int lo = 0;
		int hi = rows.size() - 1;
		while (lo <= hi)
		{
			int mid = (lo + hi) >>> 1;
			int midId = rows.get(mid).integer("ID");
			if (midId < id)
			{
				lo = mid + 1;
			}
			else if (midId > id)
			{
				hi = mid - 1;
			}
			else
			{
				return rows.get(mid);
			}
		}
		return null;
	}

	public static AtlasData read(Reader in) throws IOException
	{
		Map<String, List<Row>> tables = new HashMap<>();
		// the same sources, rarities and locations repeat thousands of times
		Map<String, String> strings = new HashMap<>();
		JsonReader json = new JsonReader(in);
		json.beginObject();
		while (json.hasNext())
		{
			String table = json.nextName();
			Map<String, Integer> columns = new HashMap<>();
			List<Row> rows = new ArrayList<>();
			json.beginObject();
			while (json.hasNext())
			{
				boolean isColumns = json.nextName().equals("columns");
				json.beginArray();
				while (json.hasNext())
				{
					if (isColumns)
					{
						columns.put(json.nextString(), columns.size());
						continue;
					}

					List<Object> values = new ArrayList<>(columns.size());
					json.beginArray();
					while (json.hasNext())
					{
						values.add(readValue(json, strings));
					}
					json.endArray();
					rows.add(new Row(columns, values.toArray()));
				}
				json.endArray();
			}
			json.endObject();
			tables.put(table, rows);
		}
		json.endObject();
		return new AtlasData(tables);
	}

	private static Object readValue(JsonReader json, Map<String, String> strings) throws IOException
	{
		JsonToken token = json.peek();
		if (token == JsonToken.NULL)
		{
			json.nextNull();
			return null;
		}
		if (token == JsonToken.NUMBER)
		{
			double d = json.nextDouble();
			return d == (int) d ? (Object) (int) d : (Object) d;
		}
		String s = json.nextString();
		String seen = strings.putIfAbsent(s, s);
		return seen == null ? s : seen;
	}

	@RequiredArgsConstructor
	public static class Row
	{
		private final Map<String, Integer> columns;
		private final Object[] values;

		private Object get(String column)
		{
			return values[columns.get(column)];
		}

		public String str(String column)
		{
			Object v = get(column);
			return v == null ? null : v.toString();
		}

		public int integer(String column)
		{
			Object v = get(column);
			return v instanceof Number ? ((Number) v).intValue() : 0;
		}

		public boolean bool(String column)
		{
			return integer(column) != 0;
		}
	}

}
