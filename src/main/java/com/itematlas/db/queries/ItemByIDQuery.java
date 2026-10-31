package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasInfoItem;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemByIDQuery extends ModeledQuery<AtlasInfoItem>
{

	private final int itemID;

	@Override
	public List<AtlasInfoItem> run(AtlasData data)
	{
		AtlasData.Row item = data.byId("ITEMS", itemID);
		return item == null ? Collections.emptyList() : List.of(toItem(data, item));
	}

	static AtlasInfoItem toItem(AtlasData data, AtlasData.Row item)
	{
		AtlasData.Row group = data.byId("ITEM_GROUPS", item.integer("GROUP_ID"));
		return new AtlasInfoItem(
			item.integer("ID"),
			item.str("NAME"),
			item.str("EXAMINE_TEXT"),
			group == null ? null : group.str("NAME"),
			item.str("VERSION"),
			item.str("URL"),
			item.bool("IS_MEMBERS"),
			item.bool("IS_TRADEABLE"),
			item.bool("IS_DEFAULT_VERSION")
		);
	}
}
