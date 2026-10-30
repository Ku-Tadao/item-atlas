package com.itematlas.db.queries;

import com.itematlas.types.AtlasSourceLocation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DropSourceLocationsQuery extends ModeledQuery<AtlasSourceLocation>
{

	private final int itemId;

	@Override
	public String getSql()
	{
		//language=SQL	
		return "SELECT * FROM SOURCE_LOCATIONS " +
			"WHERE SOURCE IN (SELECT SOURCE FROM DROP_SOURCES WHERE ITEM_ID = ?)";
	}

	@Override
	public void setParams(PreparedStatement ps) throws SQLException
	{
		ps.setInt(1, itemId);
	}

	@Override
	public AtlasSourceLocation convertRow(ResultSet rs) throws SQLException
	{
		try
		{
			return new AtlasSourceLocation(
				rs.getString("SOURCE"),
				rs.getString("LOCATION"),
				rs.getBoolean("IS_MEMBERS"),
				rs.getString("TILES")
			);
		}
		finally
		{
			rs.next();
		}
	}
}
