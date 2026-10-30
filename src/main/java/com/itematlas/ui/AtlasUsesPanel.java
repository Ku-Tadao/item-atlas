package com.itematlas.ui;

import com.itematlas.ItemAtlasConfig;
import com.itematlas.UseSectionType;
import com.itematlas.config.DefaultOpenUses;
import com.itematlas.db.H2DataProvider;
import com.itematlas.db.queries.ItemCurrencyQuery;
import com.itematlas.db.queries.ItemProducesQuery;
import com.itematlas.types.AtlasProductionRecipe;
import com.itematlas.types.AtlasShop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

class AtlasUsesPanel extends JPanel
{
	// open state of the sections the player toggled, for this session
	private static final Map<String, Boolean> openSections = new HashMap<>();

	private static final int RECIPE_PAGE = 50;

	private final AtlasItem atlasItem;
	private final H2DataProvider dataProvider;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final Client client;
	private final EventBus eventBus;
	private final PluginManager pluginManager;
	private final String useName;
	private final AtlasPanel mainPanel;
	private final ItemAtlasConfig config;

	AtlasUsesPanel(AtlasItem atlasItem, ItemManager itemManager, H2DataProvider dataProvider, ClientThread clientThread, AtlasPanel mainPanel, ItemAtlasConfig config, Client client, EventBus eventBus, PluginManager pluginManager)
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

		this.useName = atlasItem.getInfoItem().getName().length() > atlasItem.getInfoItem().getGroup().length()
			? atlasItem.getInfoItem().getName()
			: atlasItem.getInfoItem().getGroup();

		setLayout(new BorderLayout());

		// the headers show straight away, and each section fills in as its query returns
		AtlasSection recipesSection = section(UseSectionType.RECIPES);
		AtlasSection shopsSection = section(UseSectionType.SHOPS);

		GridBagConstraints containerGbc = new GridBagConstraints(
			0,
			0,
			1,
			1,
			1.0,
			1.0,
			GridBagConstraints.LINE_START,
			GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0),
			0,
			0
		);
		JPanel container = new JPanel(new GridBagLayout());
		container.add(recipesSection, containerGbc);
		containerGbc.gridy++;
		container.add(shopsSection, containerGbc);

		JPanel wrapper = new JPanel(new BorderLayout());

		wrapper.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
		wrapper.add(container, BorderLayout.NORTH);
		// sits under the sections that have come in so far
		JPanel loading = AtlasSourceRouteUtil.noDataPanel("Loading uses…");
		wrapper.add(loading, BorderLayout.CENTER);

		JScrollPane scrollWrapper = new JScrollPane(wrapper);
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 5, 0, 0));
		scrollWrapper.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 1000));
		scrollWrapper.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(scrollWrapper, BorderLayout.CENTER);

		dataProvider.executeMany(recipePage(0), recipes ->
		{
			SwingUtilities.invokeLater(() -> fillRecipes(recipesSection, recipes));
			dataProvider.executeMany(new ItemCurrencyQuery(useName), shops ->
				SwingUtilities.invokeLater(() ->
				{
					wrapper.remove(loading);
					wrapper.revalidate();
					shopsSection.fill(
						" (" + Math.min(shops.stream().mapToInt(shop -> shop.getItems().size()).sum(), AtlasPanel.MAX_ENTRIES) + ")",
						shops.isEmpty() ? List.of() : List.of(new AtlasShopsPanel(shops, atlasItem, itemManager, clientThread, true, mainPanel, client, eventBus, pluginManager, config.showMembersStar())));
					if (recipes.isEmpty() && shops.isEmpty())
					{
						remove(scrollWrapper);
						add(AtlasSourceRouteUtil.noDataPanel("The wiki lists no uses for this item."), BorderLayout.NORTH);
						revalidate();
						repaint();
					}
				}));
		});
	}

	private AtlasSection section(UseSectionType useSectionType)
	{
		return new AtlasSection(useSectionType.sectionName, useSectionType.sectionDesc, openSections,
			config.defaultOpenUses().contains(DefaultOpenUses.of(useSectionType)));
	}

	private void fillRecipes(AtlasSection recipesSection, List<AtlasProductionRecipe> recipes)
	{
		if (recipes.isEmpty())
		{
			recipesSection.fill("", List.of());
			return;
		}

		JPanel recipeList = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		addRecipes(recipeList, recipes, 0);
		// one page is loaded, so more than a page reads "50+"
		recipesSection.fill(recipes.size() > RECIPE_PAGE ? " (" + RECIPE_PAGE + "+)" : " (" + recipes.size() + ")", List.of(recipeList));
	}

	// one more than a page, to know whether there's a next one
	private ItemProducesQuery recipePage(int offset)
	{
		return new ItemProducesQuery(atlasItem.getInfoItem().getItemID(), atlasItem.getInfoItem().getName(), offset, RECIPE_PAGE + 1);
	}

	private void addRecipes(JPanel contents, List<AtlasProductionRecipe> page, int offset)
	{
		page.stream().limit(RECIPE_PAGE).forEach(recipe ->
			contents.add(AtlasRecipePanel.collapsed(recipe, itemManager, clientThread, mainPanel, atlasItem.getInfoItem(), dataProvider)));

		if (page.size() > RECIPE_PAGE)
		{
			int next = offset + RECIPE_PAGE;
			JButton loadMore = new JButton("Load more (" + next + " shown)");
			loadMore.addActionListener(e ->
			{
				loadMore.setEnabled(false);
				loadMore.setText("Loading...");
				dataProvider.executeMany(recipePage(next), more ->
					SwingUtilities.invokeLater(() ->
					{
						contents.remove(loadMore);
						addRecipes(contents, more, next);
						contents.revalidate();
						contents.repaint();
					}));
			});
			contents.add(loadMore);
		}
	}
}
