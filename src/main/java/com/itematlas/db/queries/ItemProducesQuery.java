package com.itematlas.db.queries;

import com.itematlas.types.AtlasProductionMaterial;
import com.itematlas.types.AtlasProductionRecipe;
import com.itematlas.types.AtlasProductionSkill;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ItemProducesQuery extends ModeledQuery<AtlasProductionRecipe>
{

	private final int itemID;
	private final String itemName;
	private final int offset;
	private final int limit;

	@Override
	public String getSql()
	{
		//language=SQL
		return "SELECT PR.*, PS.*, PM.*, PR.ID AS PR_ID, PS.ID AS PS_ID, PM.ID AS PM_ID " +
			"FROM PRODUCTION_RECIPES PR " +
			"LEFT JOIN PRODUCTION_SKILLS PS ON PR.ID = PS.RECIPE_ID " +
			"LEFT JOIN PRODUCTION_MATERIALS PM ON PR.ID = PM.RECIPE_ID " +
			// page by whole recipes: a row limit on the join cut the last recipe's materials off
			"WHERE PR.ID IN (SELECT ID FROM PRODUCTION_RECIPES " +
			"WHERE ID IN (SELECT RECIPE_ID FROM PRODUCTION_MATERIALS WHERE ITEM_ID = ?) " +
			// TOOLS is a comma separated list of wiki names
			"OR (', ' || LOWER(TOOLS) || ', ') LIKE ? " +
			"ORDER BY " + order("PRODUCTION_RECIPES") + " LIMIT ? OFFSET ?) " +
			// convertRow reads one recipe's rows in a row, so ID breaks the last tie
			"ORDER BY " + order("PR");
	}

	// lowest skill level first (a recipe's highest skill counts), then by output name
	private static String order(String table)
	{
		return "(SELECT MAX(CAST(LEVEL AS INTEGER)) FROM PRODUCTION_SKILLS WHERE RECIPE_ID = " + table + ".ID), "
			+ table + ".OUTPUT_ITEM_NAME, " + table + ".ID";
	}

	@Override
	public void setParams(PreparedStatement ps) throws SQLException
	{
		ps.setInt(1, itemID);
		ps.setString(2, "%, " + itemName.toLowerCase() + ", %");
		ps.setInt(3, limit);
		ps.setInt(4, offset);
	}

	@Override
	public AtlasProductionRecipe convertRow(ResultSet rs) throws SQLException
	{
		int id = rs.getInt("PR_ID");
		AtlasProductionRecipe res = new AtlasProductionRecipe(
			rs.getString("TICKS"),
			new ArrayList<>(),
			rs.getString("FACILITIES"),
			rs.getString("TOOLS"),
			new ArrayList<>(),
			rs.getBoolean("IS_MEMBERS"),
			rs.getString("OUTPUT_ITEM_NAME"),
			rs.getString("OUTPUT_ITEM_VERSION"),
			rs.getInt("OUTPUT_ITEM_ID"),
			rs.getString("OUTPUT_QUANTITY"),
			rs.getString("OUTPUT_QUANTITY_NOTE"),
			rs.getString("OUTPUT_SUBTEXT")
		);

		do
		{
			if (rs.getInt("PM_ID") != 0)
			{
				AtlasProductionMaterial material = new AtlasProductionMaterial(
					rs.getString("ITEM_NAME"),
					rs.getString("ITEM_VERSION"),
					rs.getInt("ITEM_ID"),
					rs.getString("QUANTITY")
				);

				if (!res.getMaterials().contains(material)) {
					res.getMaterials().add(material);
				}
			}

			if (rs.getInt("PS_ID") != 0)
			{
				AtlasProductionSkill skill = new AtlasProductionSkill(
					rs.getString("NAME"),
					rs.getString("LEVEL"),
					rs.getString("EXPERIENCE"),
					rs.getBoolean("IS_BOOSTABLE")
				);

				if (!res.getSkills().contains(skill)) {
					res.getSkills().add(skill);
				}
			}
		} while (rs.next() && rs.getInt("PR_ID") == id);

		return res;
	}
}
