package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasDropSource;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemDropSourcesQuery extends ModeledQuery<AtlasDropSource>
{

	private final int itemId;

	@Override
	public List<AtlasDropSource> run(AtlasData data)
	{
		List<AtlasDropSource> res = new ArrayList<>();
		for (AtlasData.Row drop : data.table("DROP_SOURCES"))
		{
			if (drop.integer("ITEM_ID") == itemId)
			{
				res.add(new AtlasDropSource(
					drop.str("SOURCE"),
					drop.integer("QUANTITY_LOW"),
					drop.integer("QUANTITY_HIGH"),
					drop.str("RARITY"),
					drop.str("DROP_LEVEL"),
					drop.str("DROP_TYPE")
				));
			}
		}
		return res;
	}
}
