package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasSpawnItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemSpawnQuery extends ModeledQuery<AtlasSpawnItem>
{

	private final int itemId;

	@Override
	public List<AtlasSpawnItem> run(AtlasData data)
	{
		List<AtlasSpawnItem> res = new ArrayList<>();
		for (AtlasData.Row spawn : data.table("SPAWN_ITEMS"))
		{
			AtlasData.Row group = data.byId("ITEM_GROUPS", spawn.integer("GROUP_ID"));
			if (spawn.integer("ITEM_ID") == itemId && group != null)
			{
				res.add(new AtlasSpawnItem(
					group.str("NAME"),
					spawn.str("NAME"),
					spawn.str("VERSION"),
					spawn.str("COORDS"),
					spawn.str("TILES"),
					spawn.str("LOCATION"),
					spawn.bool("IS_MEMBERS"),
					spawn.str("PLANE"),
					spawn.str("MAP_ID")
				));
			}
		}
		return res;
	}
}
