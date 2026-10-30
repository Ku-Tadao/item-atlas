package com.itematlas.ui;

import com.itematlas.types.AtlasDropSource;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class AtlasDropsPanelTest
{
	@Test
	public void displayName()
	{
		assertEquals("Bronze dragon (Standard)", AtlasDropsPanel.displayName("Bronze dragon#Standard"));
		assertEquals("Muddy chest", AtlasDropsPanel.displayName("Muddy chest"));
	}

	@Test
	public void levelText()
	{
		assertEquals("Lvl 50", AtlasDropsPanel.levelText("50"));
		assertEquals("Lvl 13–76", AtlasDropsPanel.levelText("24,76,13"));
		assertEquals("", AtlasDropsPanel.levelText(""));
		assertEquals("N/A", AtlasDropsPanel.levelText("N/A"));
	}

	@Test
	public void mergeBySourceSortsByBestRate()
	{
		List<List<AtlasDropSource>> merged = AtlasDropsPanel.mergeBySource(List.of(
			drop("Goblin", 14, "1/512"),
			drop("Unknown", 1, "???"),
			drop("Goblin", 1, "1/128"),
			drop("Chest", 1, "Always")));
		assertEquals(List.of("Chest", "Goblin", "Unknown"),
			merged.stream().map(drops -> drops.get(0).getSource()).collect(Collectors.toList()));
		// quantities in order within a source
		assertEquals(1, merged.get(1).get(0).getQuantityLow());
		assertEquals(14, merged.get(1).get(1).getQuantityLow());
	}

	private static AtlasDropSource drop(String source, int quantity, String rarity)
	{
		return new AtlasDropSource(source, quantity, quantity, rarity, "", "combat");
	}
}
