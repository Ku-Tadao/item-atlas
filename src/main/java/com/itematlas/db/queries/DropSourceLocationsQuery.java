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
		// the wiki can link the singular of a plural item: "house key" is the item "House keys"
		return "SELECT SL.*, (SELECT MIN(ID) FROM ITEMS WHERE NAME IN (SL.REQUIRED_ITEM, SL.REQUIRED_ITEM || 's')) AS REQUIRED_ITEM_ID " +
			"FROM SOURCE_LOCATIONS SL " +
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
				rs.getString("TILES"),
				rs.getString("REQUIRED_ITEM"),
				rs.getInt("REQUIRED_ITEM_ID"),
				rs.getString("REQUIREMENTS")
			);
		}
		finally
		{
			rs.next();
		}
	}
}
