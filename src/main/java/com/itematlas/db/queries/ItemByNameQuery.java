package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasInfoItem;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemByNameQuery extends ModeledQuery<AtlasInfoItem>
{

	private final String name;

	@Override
	public List<AtlasInfoItem> run(AtlasData data)
	{
		// the default version first, then the lowest ID
		AtlasData.Row best = null;
		for (AtlasData.Row item : data.table("ITEMS"))
		{
			if (item.str("NAME").equalsIgnoreCase(name)
				&& (best == null || item.bool("IS_DEFAULT_VERSION") && !best.bool("IS_DEFAULT_VERSION")))
			{
				best = item;
			}
		}
		return best == null ? Collections.emptyList() : List.of(ItemByIDQuery.toItem(data, best));
	}
}
