package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import com.itematlas.types.AtlasProductionMaterial;
import com.itematlas.types.AtlasProductionRecipe;
import com.itematlas.types.AtlasProductionSkill;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemProducedByQuery extends ModeledQuery<AtlasProductionRecipe>
{

	private final int itemID;

	@Override
	public List<AtlasProductionRecipe> run(AtlasData data)
	{
		List<AtlasProductionRecipe> res = new ArrayList<>();
		for (AtlasData.Row recipe : data.table("PRODUCTION_RECIPES"))
		{
			if (recipe.integer("OUTPUT_ITEM_ID") == itemID)
			{
				res.add(toRecipe(data, recipe));
				if (res.size() == 100)
				{
					break;
				}
			}
		}
		return res;
	}

	static AtlasProductionRecipe toRecipe(AtlasData data, AtlasData.Row recipe)
	{
		int id = recipe.integer("ID");
		AtlasProductionRecipe res = new AtlasProductionRecipe(
			recipe.str("TICKS"),
			new ArrayList<>(),
			recipe.str("FACILITIES"),
			recipe.str("TOOLS"),
			new ArrayList<>(),
			recipe.bool("IS_MEMBERS"),
			recipe.str("OUTPUT_ITEM_NAME"),
			recipe.str("OUTPUT_ITEM_VERSION"),
			recipe.integer("OUTPUT_ITEM_ID"),
			recipe.str("OUTPUT_QUANTITY"),
			recipe.str("OUTPUT_QUANTITY_NOTE"),
			recipe.str("OUTPUT_SUBTEXT")
		);

		for (AtlasData.Row m : data.table("PRODUCTION_MATERIALS"))
		{
			if (m.integer("RECIPE_ID") != id)
			{
				continue;
			}
			AtlasProductionMaterial material = new AtlasProductionMaterial(
				m.str("ITEM_NAME"),
				m.str("ITEM_VERSION"),
				m.integer("ITEM_ID"),
				m.str("QUANTITY")
			);
			if (!res.getMaterials().contains(material))
			{
				res.getMaterials().add(material);
			}
		}

		for (AtlasData.Row s : skills(data, id))
		{
			AtlasProductionSkill skill = new AtlasProductionSkill(
				s.str("NAME"),
				s.str("LEVEL"),
				s.str("EXPERIENCE"),
				s.bool("IS_BOOSTABLE")
			);
			if (!res.getSkills().contains(skill))
			{
				res.getSkills().add(skill);
			}
		}
		return res;
	}

	static List<AtlasData.Row> skills(AtlasData data, int recipeId)
	{
		List<AtlasData.Row> res = new ArrayList<>();
		for (AtlasData.Row s : data.table("PRODUCTION_SKILLS"))
		{
			if (s.integer("RECIPE_ID") == recipeId)
			{
				res.add(s);
			}
		}
		return res;
	}
}
