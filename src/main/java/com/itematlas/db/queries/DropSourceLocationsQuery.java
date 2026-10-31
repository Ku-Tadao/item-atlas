package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasSourceLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DropSourceLocationsQuery extends ModeledQuery<AtlasSourceLocation>
{

	private final int itemId;

	@Override
	public List<AtlasSourceLocation> run(AtlasData data)
	{
		Set<String> sources = new HashSet<>();
		for (AtlasData.Row drop : data.table("DROP_SOURCES"))
		{
			if (drop.integer("ITEM_ID") == itemId)
			{
				sources.add(drop.str("SOURCE"));
			}
		}

		List<AtlasSourceLocation> res = new ArrayList<>();
		for (AtlasData.Row location : data.table("SOURCE_LOCATIONS"))
		{
			if (sources.contains(location.str("SOURCE")))
			{
				String requiredItem = location.str("REQUIRED_ITEM");
				res.add(new AtlasSourceLocation(
					location.str("SOURCE"),
					location.str("LOCATION"),
					location.bool("IS_MEMBERS"),
					location.str("TILES"),
					requiredItem,
					requiredItem == null ? 0 : requiredItemId(data, requiredItem),
					location.str("REQUIREMENTS")
				));
			}
		}
		return res;
	}

	// the wiki can link the singular of a plural item: "house key" is the item "House keys"
	private static int requiredItemId(AtlasData data, String name)
	{
		for (AtlasData.Row item : data.table("ITEMS"))
		{
			String itemName = item.str("NAME");
			if (itemName.equals(name) || itemName.equals(name + "s"))
			{
				return item.integer("ID");
			}
		}
		return 0;
	}
}
