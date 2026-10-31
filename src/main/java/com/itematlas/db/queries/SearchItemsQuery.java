package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasInfoItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SearchItemsQuery extends ModeledQuery<AtlasInfoItem>
{

	private final String searchTerms;

	@Override
	public List<AtlasInfoItem> run(AtlasData data)
	{
		String term = searchTerms.toLowerCase();
		List<AtlasInfoItem> res = new ArrayList<>();
		for (AtlasData.Row item : data.table("ITEMS"))
		{
			if (item.str("SEARCH_NAME").toLowerCase().contains(term))
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
