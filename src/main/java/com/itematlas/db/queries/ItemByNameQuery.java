package com.itematlas.db.queries;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Finds an item by its wiki name, preferring the default version.
 */
public class ItemByNameQuery extends ItemByIDQuery
{

	private final String name;

	public ItemByNameQuery(String name)
	{
		super(0);
		this.name = name;
	}

	@Override
	public String getSql()
	{
		//language=SQL
		return "SELECT I.*, IG.NAME AS GROUP_NAME FROM ITEMS I " +
			"LEFT JOIN ITEM_GROUPS IG on IG.ID = I.GROUP_ID " +
			"WHERE LOWER(I.NAME) = LOWER(?) " +
			"ORDER BY I.IS_DEFAULT_VERSION DESC, I.ID " +
			"LIMIT 1";
	}

	@Override
	public void setParams(PreparedStatement ps) throws SQLException
	{
		ps.setString(1, name);
	}
}
