package com.itematlas.db;

import com.itematlas.db.queries.DropSourceLocationsQuery;
import com.itematlas.db.queries.ItemByNameQuery;
import com.itematlas.db.queries.ItemProducesQuery;
import com.itematlas.types.AtlasProductionRecipe;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AtlasDataTest
{

	private static final String JSON = ("{"
		+ "'ITEM_GROUPS': {'columns': ['ID', 'NAME', 'SEARCH_NAME'], 'rows': [[1, 'Group', 'group']]},"
		+ "'ITEMS': {'columns': ['ID', 'NAME', 'EXAMINE_TEXT', 'GROUP_ID', 'VERSION', 'URL', 'IS_MEMBERS', 'IS_TRADEABLE', 'IS_DEFAULT_VERSION', 'SEARCH_NAME'], 'rows': ["
		+ "  [1, 'House keys', '', 1, null, '', 0, 0, 0, 'house keys'],"
		+ "  [2, 'Bar', '', 1, 'Old', '', 0, 0, 0, 'bar'],"
		+ "  [3, 'bar', '', 1, null, '', 0, 0, 1, 'bar']]},"
		+ "'DROP_SOURCES': {'columns': ['ID', 'ITEM_ID', 'SOURCE'], 'rows': [[1, 2, 'Goblin']]},"
		+ "'SOURCE_LOCATIONS': {'columns': ['ID', 'SOURCE', 'LOCATION', 'IS_MEMBERS', 'TILES', 'REQUIRED_ITEM', 'REQUIREMENTS'], 'rows': ["
		+ "  [1, 'Goblin', 'Lumbridge', 0, '', 'House key', null]]},"
		+ "'PRODUCTION_RECIPES': {'columns': ['ID', 'TICKS', 'FACILITIES', 'TOOLS', 'IS_MEMBERS', 'OUTPUT_ITEM_NAME', 'OUTPUT_ITEM_VERSION', 'OUTPUT_ITEM_ID', 'OUTPUT_QUANTITY', 'OUTPUT_QUANTITY_NOTE', 'OUTPUT_SUBTEXT'], 'rows': ["
		+ "  [1, null, null, 'Hammer, Chisel', 0, 'Zed', null, 9, '1', null, null],"
		+ "  [2, null, null, null, 0, 'Bed', null, 9, '1', null, null],"
		+ "  [3, null, null, 'Hammers', 0, 'Nope', null, 9, '1', null, null],"
		+ "  [4, null, null, null, 0, 'Axe', null, 9, '1', null, null],"
		+ "  [5, null, null, 'chisel', 0, 'Cup', null, 9, '1', null, null]]},"
		+ "'PRODUCTION_MATERIALS': {'columns': ['ID', 'RECIPE_ID', 'ITEM_NAME', 'ITEM_VERSION', 'ITEM_ID', 'QUANTITY'], 'rows': ["
		+ "  [1, 2, 'Hammer', null, 7, '1'], [2, 4, 'Hammer', null, 7, '1'], [3, 4, 'Hammer', null, 7, '1']]},"
		+ "'PRODUCTION_SKILLS': {'columns': ['ID', 'RECIPE_ID', 'NAME', 'LEVEL', 'EXPERIENCE', 'IS_BOOSTABLE'], 'rows': ["
		+ "  [1, 1, 'Smithing', '5 (boostable)', '1', 1], [2, 2, 'Crafting', '50', '1', 0], [3, 2, 'Smithing', '10', '1', 0],"
		+ "  [4, 4, 'Crafting', 'n/a', '1', 0]]}"
		+ "}").replace('\'', '"');

	@Test
	public void queriesMatchTheOldSql() throws Exception
	{
		AtlasData data = AtlasData.read(new StringReader(JSON));

		// case-insensitive, default version first
		assertEquals(3, new ItemByNameQuery("BAR").run(data).get(0).getItemID());

		// "House key" links the plural item
		assertEquals(1, new DropSourceLocationsQuery(2).run(data).get(0).getRequiredItemId());

		// used as a material or listed as a tool; lowest highest level first (unparseable is 0), duplicates merged
		List<AtlasProductionRecipe> uses = new ItemProducesQuery(7, "hammer", 0, 10).run(data);
		assertEquals(List.of("Axe", "Zed", "Bed"), uses.stream().map(AtlasProductionRecipe::getOutputItemName).collect(Collectors.toList()));
		assertEquals(1, uses.get(0).getMaterials().size());
		assertEquals(2, uses.get(2).getSkills().size());
		assertEquals(List.of("Zed"), new ItemProducesQuery(7, "hammer", 1, 1).run(data).stream()
			.map(AtlasProductionRecipe::getOutputItemName).collect(Collectors.toList()));
	}

}
