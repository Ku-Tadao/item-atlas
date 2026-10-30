package com.itematlas;

import com.itematlas.types.AtlasSourceLocation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Where the player keeps an item, for sources that need one (a chest's key). The bank and group storage
 * are only sent while open, so their contents are saved to the RuneScape profile and survive restarts.
 * Everything here runs on the client thread.
 */
@Singleton
public class ItemOwnership
{
	private static final String BANK_KEY = "bankItems";
	private static final String GROUP_STORAGE_KEY = "groupStorageItems";

	@RequiredArgsConstructor
	public enum Status
	{
		CARRIED("you have it"),
		BANK("in bank"),
		GROUP_STORAGE("in group storage"),
		MISSING("not owned"),
		UNKNOWN("bank not checked yet");

		public final String text;
	}

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	/**
	 * Saves the bank and group storage; returns whether the change can alter a {@link #status}.
	 */
	boolean onContainerChanged(int containerId, ItemContainer container)
	{
		if (containerId == InventoryID.INV || containerId == InventoryID.WORN)
		{
			return true;
		}
		String key = containerId == InventoryID.BANK ? BANK_KEY : containerId == InventoryID.INV_GROUP_TEMP ? GROUP_STORAGE_KEY : null;
		if (key == null)
		{
			return false;
		}
		// placeholders have quantity 0 and would canonicalize to the real item
		String ids = Arrays.stream(container.getItems())
			.filter(item -> item.getId() > 0 && item.getQuantity() > 0)
			.map(item -> String.valueOf(itemManager.canonicalize(item.getId())))
			.distinct()
			.collect(Collectors.joining(","));
		configManager.setRSProfileConfiguration(ItemAtlasConfig.GROUP, key, ids);
		return true;
	}

	public Status status(int itemId)
	{
		if (carries(InventoryID.INV, itemId) || carries(InventoryID.WORN, itemId))
		{
			return Status.CARRIED;
		}
		String bank = configManager.getRSProfileConfiguration(ItemAtlasConfig.GROUP, BANK_KEY);
		if (stored(bank, itemId))
		{
			return Status.BANK;
		}
		if (stored(configManager.getRSProfileConfiguration(ItemAtlasConfig.GROUP, GROUP_STORAGE_KEY), itemId))
		{
			return Status.GROUP_STORAGE;
		}
		return bank == null ? Status.UNKNOWN : Status.MISSING;
	}

	/**
	 * Whether a location can be looted: true unless it needs an item the player is known not to own,
	 * or a quest or skill level they don't have.
	 */
	public boolean canLoot(AtlasSourceLocation location)
	{
		return (location.getRequiredItemId() <= 0 || status(location.getRequiredItemId()) != Status.MISSING)
			&& unmetRequirements(location).isEmpty();
	}

	public List<String> unmetRequirements(AtlasSourceLocation location)
	{
		return LocationRequirements.unmet(client, location.getRequirements());
	}

	/**
	 * The route plugin's header label for these locations; when any needs an item, it names it
	 * and says so in chat too, since Shortest Path shows no label.
	 */
	public String routeLabel(Collection<AtlasSourceLocation> locations, boolean otherTargets)
	{
		String needs = routeNeeds(locations, otherTargets);
		if (needs.isEmpty())
		{
			return "Item Atlas";
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Item Atlas: this route " + needs + ".", null);
		return "Item Atlas · " + needs;
	}

	/**
	 * "may need Muddy key (in bank)", or empty when none of these locations needs an item.
	 * {@code otherTargets}: the route also goes to targets that are not these locations (shops, ground spawns).
	 */
	public String routeNeeds(Collection<AtlasSourceLocation> locations, boolean otherTargets)
	{
		Map<String, Status> needs = new LinkedHashMap<>();
		for (AtlasSourceLocation location : locations)
		{
			if (location.getRequiredItem() != null)
			{
				needs.put(location.getRequiredItem(), location.getRequiredItemId() > 0 ? status(location.getRequiredItemId()) : Status.UNKNOWN);
			}
		}
		if (needs.isEmpty())
		{
			return "";
		}

		String note = needs.entrySet().stream()
			.map(need -> need.getKey() + " (" + need.getValue().text + ")")
			.collect(Collectors.joining(", "));
		// several targets: the route plugin picks the nearest, which may not be the one with the lock
		String verb = !otherTargets && locations.stream().allMatch(location -> location.getRequiredItem() != null) ? "needs " : "may need ";
		return verb + note;
	}

	private boolean carries(int containerId, int itemId)
	{
		ItemContainer container = client.getItemContainer(containerId);
		return container != null && Arrays.stream(container.getItems())
			.mapToInt(Item::getId)
			.anyMatch(id -> id > 0 && itemManager.canonicalize(id) == itemId);
	}

	private static boolean stored(String ids, int itemId)
	{
		return ids != null && !ids.isEmpty() && Arrays.asList(ids.split(",")).contains(String.valueOf(itemId));
	}
}
