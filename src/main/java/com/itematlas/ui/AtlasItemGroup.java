package com.itematlas.ui;

import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AtlasItemGroup
{
	private final String group;

	@Nullable
	private String selectedVersion;

	private final List<AtlasItem> items;
}
