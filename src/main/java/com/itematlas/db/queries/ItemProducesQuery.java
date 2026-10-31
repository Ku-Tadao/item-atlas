package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasProductionRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemProducesQuery extends ModeledQuery<AtlasProductionRecipe>
{

	private static final Pattern LEADING_INT = Pattern.compile("^\\s*([+-]?\\d+)");

	private final int itemID;
	private final String itemName;
	private final int offset;
	private final int limit;

	@Override
	public List<AtlasProductionRecipe> run(AtlasData data)
	{
		Set<Integer> usedIn = new HashSet<>();
		for (AtlasData.Row m : data.table("PRODUCTION_MATERIALS"))
		{
			if (m.integer("ITEM_ID") == itemID)
			{
				usedIn.add(m.integer("RECIPE_ID"));
			}
		}

		// TOOLS is a comma separated list of wiki names
		String tool = ", " + itemName.toLowerCase() + ", ";
		Map<AtlasData.Row, Long> level = new HashMap<>();
		List<AtlasData.Row> recipes = new ArrayList<>();
		for (AtlasData.Row recipe : data.table("PRODUCTION_RECIPES"))
		{
			String tools = recipe.str("TOOLS");
			if (usedIn.contains(recipe.integer("ID"))
				|| tools != null && (", " + tools.toLowerCase() + ", ").contains(tool))
			{
				recipes.add(recipe);
				level.put(recipe, highestLevel(data, recipe.integer("ID")));
			}
		}

		// lowest skill level first (a recipe's highest skill counts), then by output name
		recipes.sort(Comparator.<AtlasData.Row, Long>comparing(level::get, Comparator.nullsFirst(Comparator.<Long>naturalOrder()))
			.thenComparing(r -> r.str("OUTPUT_ITEM_NAME"), Comparator.nullsFirst(Comparator.<String>naturalOrder()))
			.thenComparingInt(r -> r.integer("ID")));

		List<AtlasProductionRecipe> res = new ArrayList<>();
		int end = Math.min(offset + limit, recipes.size());
		for (int i = offset; i < end; i++)
		{
			res.add(ItemProducedByQuery.toRecipe(data, recipes.get(i)));
		}
		return res;
	}

	// like SQL's MAX(CAST(LEVEL AS INTEGER)): leading digits count, others are 0, no skills is null
	private static Long highestLevel(AtlasData data, int recipeId)
	{
		Long max = null;
		for (AtlasData.Row skill : ItemProducedByQuery.skills(data, recipeId))
		{
			String text = skill.str("LEVEL");
			if (text == null)
			{
				continue;
			}
			Matcher m = LEADING_INT.matcher(text);
			long level = m.find() ? Long.parseLong(m.group(1)) : 0;
			max = max == null ? level : Math.max(max, level);
		}
		return max;
	}
}
