package com.itematlas.types;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AtlasProductionRecipe
{
	private final String ticks;
	private final List<AtlasProductionMaterial> materials;

	@Nullable
	private final String facilities;

	@Nullable
	private final String tools;

	private final List<AtlasProductionSkill> skills;

	@SerializedName("members")
	private final boolean isMembers;

	private final String outputItemName;

	@Nullable
	private final String outputItemVersion;

	private final int outputItemID;
	private final String outputQuantity;
	private final String outputQuantityNote;
	private final String outputSubtext;

}
