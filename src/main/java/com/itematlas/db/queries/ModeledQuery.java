package com.itematlas.db.queries;

import com.itematlas.db.AtlasData;
import java.util.List;

public abstract class ModeledQuery<T>
{

	public abstract List<T> run(AtlasData data);

}
