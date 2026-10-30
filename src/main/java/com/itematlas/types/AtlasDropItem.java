package com.itematlas.types;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AtlasDropItem
{
	private final String name;

	@Nullable
	private final String version;

	private final List<AtlasDropSource> dropSources;
}
