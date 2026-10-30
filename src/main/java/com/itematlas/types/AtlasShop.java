package com.itematlas.types;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AtlasShop
{
	private final String name;
	private final String sellMultiplier;
	private final String location;
	private final boolean isMembers;
	private final String coords;
	private final String plane;
	private final String mapID;
	private final List<AtlasShopItem> items;
}
