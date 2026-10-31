package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasInfoItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemGroupQuery extends ModeledQuery<AtlasInfoItem>
{

	private final String searchTerms;

	@Override
	public List<AtlasInfoItem> run(AtlasData data)
	{
		List<AtlasInfoItem> res = new ArrayList<>();
		for (AtlasData.Row item : data.table("ITEMS"))
		{
			AtlasData.Row group = data.byId("ITEM_GROUPS", item.integer("GROUP_ID"));
			if (group != null && group.str("SEARCH_NAME").equalsIgnoreCase(searchTerms))
			{
				res.add(ItemByIDQuery.toItem(data, item));
				if (res.size() == 200)
				{
					break;
				}
			}
		}
		return res;
	}
}
