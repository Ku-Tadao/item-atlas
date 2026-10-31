package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasShop;
import com.itematlas.types.AtlasShopItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemSoldAtQuery extends ModeledQuery<AtlasShop>
{

	private final int itemID;

	@Override
	public List<AtlasShop> run(AtlasData data)
	{
		return shopsSelling(data, shopItem -> shopItem.integer("ITEM_ID") == itemID);
	}

	// each shop holds only the matching items, not its whole stock
	static List<AtlasShop> shopsSelling(AtlasData data, Predicate<AtlasData.Row> matches)
	{
		Map<Integer, AtlasShop> shops = new LinkedHashMap<>();
		for (AtlasData.Row shopItem : data.table("SHOP_ITEMS"))
		{
			AtlasData.Row shop = data.byId("SHOPS", shopItem.integer("SHOP_ID"));
			if (shop == null || !matches.test(shopItem))
			{
				continue;
			}

			AtlasShop res = shops.computeIfAbsent(shop.integer("ID"), id -> new AtlasShop(
				shop.str("NAME"),
				shop.str("SELL_MULTIPLIER"),
				shop.str("LOCATION"),
				shop.bool("IS_MEMBERS"),
				shop.str("COORDS"),
				shop.str("TILES"),
				shop.str("PLANE"),
				shop.str("MAP_ID"),
				new ArrayList<>()
			));

			AtlasShopItem item = new AtlasShopItem(
				shopItem.str("ITEM_NAME"),
				shopItem.str("ITEM_VERSION"),
				shopItem.integer("ITEM_ID"),
				shopItem.str("CURRENCY"),
				shopItem.str("STOCK"),
				shopItem.str("BUY_PRICE"),
				shopItem.str("SELL_PRICE")
			);
			if (!res.getItems().contains(item))
			{
				res.getItems().add(item);
			}
		}
		return new ArrayList<>(shops.values());
	}
}
