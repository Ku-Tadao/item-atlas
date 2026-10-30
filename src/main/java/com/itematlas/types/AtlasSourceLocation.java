package com.itematlas.types;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AtlasSourceLocation
{
	private final String source;
	private final String location;
	private final boolean isMembers;

	// "x,y,plane" tiles separated by ';'
	private final String tiles;

	// the key a chest needs, or null; its item ID is 0 when the name matches no item
	private final String requiredItem;
	private final int requiredItemId;
}
