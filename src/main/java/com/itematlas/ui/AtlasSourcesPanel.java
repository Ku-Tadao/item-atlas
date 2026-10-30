package com.itematlas.ui;

import com.itematlas.ItemAtlasConfig;
import com.itematlas.ItemOwnership;
import com.itematlas.LocationRequirements;
import com.itematlas.RarityParser;
import com.itematlas.SourceSectionType;
import com.itematlas.config.DefaultOpenSources;
import com.itematlas.db.H2DataProvider;
import com.itematlas.db.queries.DropSourceLocationsQuery;
import com.itematlas.db.queries.ItemDropSourcesQuery;
import com.itematlas.db.queries.ItemProducedByQuery;
import com.itematlas.db.queries.ItemSoldAtQuery;
import com.itematlas.db.queries.ItemSpawnQuery;
import com.itematlas.types.AtlasDropItem;
import com.itematlas.types.AtlasDropSource;
import com.itematlas.types.AtlasProductionRecipe;
import com.itematlas.types.AtlasShop;
import com.itematlas.types.AtlasSourceLocation;
import com.itematlas.types.AtlasSpawnItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
class AtlasSourcesPanel extends JPanel
{
	// open state of the sections the player toggled, for this session
	private static final Map<String, Boolean> openSections = new HashMap<>();

	private final AtlasItem atlasItem;
	private final H2DataProvider dataProvider;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final Client client;
	private final EventBus eventBus;
	private final PluginManager pluginManager;
	private final AtlasPanel mainPanel;
	private final ItemAtlasConfig config;
	private final ItemOwnership itemOwnership;

	AtlasSourcesPanel(AtlasItem atlasItem, ItemManager itemManager, H2DataProvider dataProvider, ClientThread clientThread, AtlasPanel mainPanel, ItemAtlasConfig config, Client client, EventBus eventBus, PluginManager pluginManager, ItemOwnership itemOwnership)
	{
		this.atlasItem = atlasItem;
		this.dataProvider = dataProvider;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.mainPanel = mainPanel;
		this.config = config;
		this.client = client;
		this.eventBus = eventBus;
		this.pluginManager = pluginManager;
		this.itemOwnership = itemOwnership;

		setLayout(new BorderLayout());

		// the headers show straight away, and each section fills in as its query returns
		AtlasSection recipesSection = section(SourceSectionType.RECIPES);
		AtlasSection dropsSection = section(SourceSectionType.DROPS);
		AtlasSection shopsSection = section(SourceSectionType.SHOPS);
		AtlasSection spawnsSection = section(SourceSectionType.SPAWNS);

		// row 0 is kept for the route button, added once every source is in
		GridBagConstraints containerGbc = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		JPanel container = new JPanel(new GridBagLayout());
		for (AtlasSection section : List.of(recipesSection, dropsSection, shopsSection, spawnsSection))
		{
			container.add(section, containerGbc);
			containerGbc.gridy++;
		}

		JPanel wrapper = new JPanel(new BorderLayout());

		wrapper.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
		wrapper.add(container, BorderLayout.NORTH);
		// sits under the sections that have come in so far
		JPanel loading = AtlasSourceRouteUtil.noDataPanel("Loading sources…");
		wrapper.add(loading, BorderLayout.CENTER);

		JScrollPane scrollWrapper = new JScrollPane(wrapper);
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 5, 0, 0));
		scrollWrapper.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 1000));
		scrollWrapper.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(scrollWrapper, BorderLayout.CENTER);

		int itemId = atlasItem.getInfoItem().getItemID();
		dataProvider.executeMany(new ItemProducedByQuery(itemId), recipes ->
		{
			SwingUtilities.invokeLater(() -> recipesSection.fill(" (" + recipes.size() + ")", recipes.stream()
				.map(recipe -> new AtlasRecipePanel(recipe, itemManager, clientThread, mainPanel, atlasItem.getInfoItem(), dataProvider))
				.collect(Collectors.toList())));
			dataProvider.executeMany(new ItemDropSourcesQuery(itemId), dropSources ->
				dataProvider.executeMany(new DropSourceLocationsQuery(itemId), dropLocations ->
				{
					SwingUtilities.invokeLater(() -> fillDrops(dropsSection, dropSources, dropLocations));
					dataProvider.executeMany(new ItemSoldAtQuery(itemId), shops ->
					{
						SwingUtilities.invokeLater(() -> shopsSection.fill(
							" (" + Math.min(shops.stream().mapToInt(shop -> shop.getItems().size()).sum(), AtlasPanel.MAX_ENTRIES) + ")",
							shops.isEmpty() ? List.of() : List.of(new AtlasShopsPanel(shops, atlasItem, itemManager, clientThread, false, mainPanel, client, eventBus, pluginManager, config.showMembersStar()))));
						dataProvider.executeMany(new ItemSpawnQuery(itemId), spawns ->
							SwingUtilities.invokeLater(() ->
							{
								spawnsSection.fill(" (" + spawns.size() + ")", spawns.stream()
									.map(spawnItem -> new AtlasSpawnPanel(spawnItem, clientThread, client, eventBus, pluginManager, config.showMembersStar()))
									.collect(Collectors.toList()));
								wrapper.remove(loading);
								finish(container, scrollWrapper, recipes, dropSources, shops, spawns, dropLocations);
							}));
					});
				}));
		});
	}

	private AtlasSection section(SourceSectionType sectionType)
	{
		return new AtlasSection(sectionType.sectionName, sectionType.sectionDesc, openSections,
			config.defaultOpenSources().contains(DefaultOpenSources.of(sectionType)));
	}

	private void fillDrops(AtlasSection dropsSection, List<AtlasDropSource> dropSources, List<AtlasSourceLocation> dropLocations)
	{
		if (dropSources.isEmpty())
		{
			dropsSection.fill("", List.of());
			return;
		}

		Map<String, List<AtlasSourceLocation>> locationsBySource = dropLocations.stream().collect(Collectors.groupingBy(AtlasSourceLocation::getSource));
		dropsSection.fill(" (" + Math.min(dropSources.stream().map(AtlasDropSource::getSource).distinct().count(), AtlasPanel.MAX_ENTRIES) + ")",
			List.of(new AtlasDropsPanel(new AtlasDropItem(atlasItem.getInfoItem().getName(), atlasItem.getInfoItem().getVersion(), dropSources), locationsBySource, clientThread, itemManager, eventBus, pluginManager, config, itemOwnership, mainPanel)));
	}

	private void finish(JPanel container, JScrollPane scrollWrapper, List<AtlasProductionRecipe> recipes, List<AtlasDropSource> dropSources, List<AtlasShop> shops, List<AtlasSpawnItem> spawns, List<AtlasSourceLocation> dropLocations)
	{
		if (recipes.isEmpty() && dropSources.isEmpty() && shops.isEmpty() && spawns.isEmpty())
		{
			remove(scrollWrapper);
			add(AtlasSourceRouteUtil.noDataPanel("The wiki lists no way to get this item."), BorderLayout.NORTH);
		}
		else if (config.routeShopSources() || config.routeGroundSpawnSources() || config.routeDropSources())
		{
			// a source can drop the item at several rates, e.g. per quantity; its best one counts
			Map<String, Double> dropRarity = dropSources.stream()
				.collect(Collectors.toMap(AtlasDropSource::getSource, RarityParser::calculateRarity, Math::max));
			container.add(createRouteButton(shops, spawns, dropLocations, dropRarity), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 8, 0), 0, 0));
		}
		revalidate();
		repaint();
	}

	private JButton createRouteButton(List<AtlasShop> shops, List<AtlasSpawnItem> spawns, List<AtlasSourceLocation> dropLocations, Map<String, Double> dropRarity)
	{
		JButton routeButton = new JButton("Route to closest source");
		routeButton.setPreferredSize(new Dimension(220, 30));
		routeButton.setEnabled(false);
		routeButton.setToolTipText("Checking route availability...");
		routeButton.addActionListener(event -> updateRouteButton(routeButton, shops, spawns, dropLocations, dropRarity, true));
		updateRouteButton(routeButton, shops, spawns, dropLocations, dropRarity, false);
		return routeButton;
	}

	private void updateRouteButton(JButton routeButton, List<AtlasShop> shops, List<AtlasSpawnItem> spawns, List<AtlasSourceLocation> dropLocations, Map<String, Double> dropRarity, boolean requestRoute)
	{
		if (requestRoute)
		{
			routeButton.setEnabled(false);
		}

		clientThread.invokeLater(() ->
		{
			String tooltip;
			AtlasSourceRouteUtil.Router router = AtlasSourceRouteUtil.findActiveRouter(pluginManager);
			if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
			{
				tooltip = "Log in to route to an item source.";
			}
			else if (router == null)
			{
				tooltip = "Enable the GPS or Shortest Path plugin to use this action.";
			}
			else
			{
				List<AtlasSourceLocation> routedDrops = new ArrayList<>();
				Set<WorldPoint> targets = buildRouteTargets(shops, spawns, dropLocations, dropRarity, routedDrops);
				if (targets.isEmpty())
				{
					SwingUtilities.invokeLater(() ->
					{
						routeButton.setText("No locations to route to");
						routeButton.setEnabled(false);
						routeButton.setToolTipText("The wiki has no map location for any enabled shop, ground spawn or drop source of this item on this world.");
					});
					return;
				}
				else
				{
					boolean otherTargets = !routedDrops.stream()
						.flatMap(location -> AtlasSourceRouteUtil.parseTiles(location.getTiles()).stream())
						.collect(Collectors.toSet())
						.containsAll(targets);
					if (requestRoute)
					{
						router.route(clientThread, eventBus, Map.of("target", targets,
							"source", itemOwnership.routeLabel(routedDrops, otherTargets)));
					}
					tooltip = requestRoute
						? "Route request sent to " + router.pluginName + ". Stock and access are not guaranteed."
						: "Ask " + router.pluginName + " to route to the closest enabled shop, ground spawn or drop source. Stock and access are not guaranteed.";
					tooltip = routeTooltip(tooltip, itemOwnership.routeNeeds(routedDrops, otherTargets), routedDrops);
				}
			}

			setRouteButtonState(routeButton, tooltip);
		});
	}

	private Set<WorldPoint> buildRouteTargets(List<AtlasShop> shops, List<AtlasSpawnItem> spawns, List<AtlasSourceLocation> dropLocations, Map<String, Double> dropRarity, List<AtlasSourceLocation> routedDrops)
	{
		Set<WorldPoint> targets = new HashSet<>();
		boolean membersWorld = client.getWorldType().contains(WorldType.MEMBERS);

		if (config.routeShopSources())
		{
			for (AtlasShop shop : shops)
			{
				if ((!membersWorld && shop.isMembers()) || hasKnownZeroStock(shop))
				{
					continue;
				}

				targets.addAll(AtlasSourceRouteUtil.tilesOrCoords(shop.getTiles(), shop.getCoords(), shop.getPlane()));
			}
		}

		if (config.routeGroundSpawnSources())
		{
			for (AtlasSpawnItem spawn : spawns)
			{
				if (!membersWorld && spawn.isMembers())
				{
					continue;
				}

				targets.addAll(AtlasSourceRouteUtil.tilesOrCoords(spawn.getTiles(), spawn.getCoords(), spawn.getPlane()));
			}
		}

		if (config.routeDropSources())
		{
			List<AtlasSourceLocation> drops = dropLocations.stream()
				.filter(location -> membersWorld || !location.isMembers())
				// a chest whose key the player has nowhere is no source
				.filter(itemOwnership::canLoot)
				.collect(Collectors.toList());
			if (config.routeBestDropRate())
			{
				// ties keep every source at the best rate, e.g. several monsters that all drop it at 1/128
				double best = drops.stream().mapToDouble(location -> dropRarity.getOrDefault(location.getSource(), 0D)).max().orElse(0);
				drops.removeIf(location -> dropRarity.getOrDefault(location.getSource(), 0D) < best);
			}

			routedDrops.addAll(drops);
			for (AtlasSourceLocation location : drops)
			{
				targets.addAll(AtlasSourceRouteUtil.parseTiles(location.getTiles()));
			}
		}

		return targets;
	}

	/**
	 * Adds what the routed drop locations need: a key, and the wiki's access notes (a Slayer task, a diary).
	 */
	private static String routeTooltip(String tooltip, String needs, List<AtlasSourceLocation> routedDrops)
	{
		List<String> notes = routedDrops.stream()
			.flatMap(location -> LocationRequirements.notes(location.getRequirements()).stream())
			.distinct()
			.collect(Collectors.toList());
		if (needs.isEmpty() && notes.isEmpty())
		{
			return tooltip;
		}
		StringBuilder html = new StringBuilder("<html>").append(AtlasSourceRouteUtil.html(tooltip));
		if (!needs.isEmpty())
		{
			html.append("<br>This route ").append(AtlasSourceRouteUtil.html(needs)).append('.');
		}
		notes.forEach(note -> html.append("<br>").append(AtlasSourceRouteUtil.html(note)));
		return html.append("</html>").toString();
	}

	private static boolean hasKnownZeroStock(AtlasShop shop)
	{
		return !shop.getItems().isEmpty() && shop.getItems().stream()
			.allMatch(item -> item.getStock() != null && "0".equals(item.getStock().trim()));
	}


	private static void setRouteButtonState(JButton routeButton, String tooltip)
	{
		SwingUtilities.invokeLater(() ->
		{
			routeButton.setEnabled(true);
			routeButton.setToolTipText(tooltip);
		});
	}
}
