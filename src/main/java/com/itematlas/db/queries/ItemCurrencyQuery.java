package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasShop;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemCurrencyQuery extends ModeledQuery<AtlasShop>
{

	private final String name;

	@Override
	public List<AtlasShop> run(AtlasData data)
	{
		return ItemSoldAtQuery.shopsSelling(data, shopItem -> name.equals(shopItem.str("CURRENCY")));
	}
}
