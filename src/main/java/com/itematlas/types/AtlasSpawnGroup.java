package com.itematlas.types;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AtlasSpawnGroup
{
	private final String group;
	private final List<AtlasSpawnItem> spawns;
}
