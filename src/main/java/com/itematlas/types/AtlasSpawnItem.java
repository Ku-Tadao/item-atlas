package com.itematlas.types;

import javax.annotation.Nullable;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AtlasSpawnItem
{
	private final String group;
	private final String name;

	@Nullable
	private final String version;

	private final String coords;

	// "x,y,plane" tiles separated by ';'
	@Nullable
	private final String tiles;
	private final String location;
	private final boolean isMembers;
	private final String plane;
	private final String mapID;
}
