package com.itematlas.ui;

import com.itematlas.types.AtlasInfoItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.client.util.AsyncBufferedImage;

@Data
@AllArgsConstructor
class AtlasItem
{
	private AsyncBufferedImage icon;

	private final AtlasInfoItem infoItem;
}
