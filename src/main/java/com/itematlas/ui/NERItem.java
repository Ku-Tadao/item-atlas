package com.itematlas.ui;

import com.itematlas.types.NERInfoItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.client.util.AsyncBufferedImage;

@Data
@AllArgsConstructor
class NERItem
{
	private AsyncBufferedImage icon;

	private final NERInfoItem infoItem;
}
