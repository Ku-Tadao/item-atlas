package com.itematlas.ui;

import com.itematlas.ItemAtlasConfig;
import com.itematlas.ItemAtlasPlugin;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

class AtlasUsesPanel extends JPanel
{
	private static final ImageIcon SECTION_EXPAND_ICON;
	private static final ImageIcon SECTION_EXPAND_ICON_HOVER;
	private static final ImageIcon SECTION_RETRACT_ICON;
	private static final ImageIcon SECTION_RETRACT_ICON_HOVER;

	static final ImageIcon BACK_ICON;

	private static final int RECIPE_PAGE = 25;
	static final ImageIcon BACK_ICON_HOVER;

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

	static
	{
		final BufferedImage backIcon = ImageUtil.loadImageResource(ItemAtlasPlugin.class, "ui/config_back_icon.png");
		BACK_ICON = new ImageIcon(backIcon);
		BACK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(backIcon, -100));

		BufferedImage sectionRetractIcon = ImageUtil.loadImageResource(ItemAtlasPlugin.class, "ui/arrow_right.png");
		sectionRetractIcon = ImageUtil.luminanceOffset(sectionRetractIcon, -121);
		SECTION_EXPAND_ICON = new ImageIcon(sectionRetractIcon);
		SECTION_EXPAND_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionRetractIcon, -100));
		final BufferedImage sectionExpandIcon = ImageUtil.rotateImage(sectionRetractIcon, Math.PI / 2);
		SECTION_RETRACT_ICON = new ImageIcon(sectionExpandIcon);
		SECTION_RETRACT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionExpandIcon, -100));
	}

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

		dataProvider.executeMany(recipePage(0), recipes ->
			dataProvider.executeMany(new ItemCurrencyQuery(useName), shops ->
				SwingUtilities.invokeLater(() ->
					buildPanel(recipes, shops))));
	}

	// one more than a page, to know whether there's a next one
	private ItemProducesQuery recipePage(int offset)
	{
		return new ItemProducesQuery(atlasItem.getInfoItem().getItemID(), atlasItem.getInfoItem().getName(), offset, RECIPE_PAGE + 1);
	}

	private void addRecipes(JPanel contents, List<AtlasProductionRecipe> page, int offset)
	{
		page.stream().limit(RECIPE_PAGE).forEach(recipe ->
			contents.add(new AtlasRecipePanel(recipe, itemManager, clientThread, mainPanel, atlasItem.getInfoItem(), dataProvider)));

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

	private void buildPanel(List<AtlasProductionRecipe> recipes, List<AtlasShop> shops)
	{
		if (recipes.isEmpty() && shops.isEmpty())
		{
			add(AtlasSourceRouteUtil.noDataPanel("The wiki lists no uses for this item."), BorderLayout.NORTH);
			revalidate();
			return;
		}

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
		JPanel recipeSection = createSection(UseSectionType.RECIPES, recipes, shops);
		JPanel container = new JPanel(new GridBagLayout());
		container.add(recipeSection, containerGbc);
		containerGbc.gridy++;
		JPanel shopsSection = createSection(UseSectionType.SHOPS, recipes, shops);
		container.add(shopsSection, containerGbc);
		containerGbc.gridy++;

		JPanel wrapper = new JPanel(new BorderLayout());

		wrapper.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
		wrapper.add(container, BorderLayout.NORTH);

		JScrollPane scrollWrapper = new JScrollPane(wrapper);
		scrollWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		scrollWrapper.getVerticalScrollBar().setBorder(new EmptyBorder(0, 5, 0, 0));
		scrollWrapper.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 1000));
		scrollWrapper.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(scrollWrapper, BorderLayout.CENTER);
	}

	private JPanel createSection(UseSectionType useSectionType, List<AtlasProductionRecipe> recipes, List<AtlasShop> shops)
	{
		ArrayList<JPanel> sectionItems = new ArrayList<>();
		switch (useSectionType)
		{
			case RECIPES:
				if (!recipes.isEmpty())
				{
					JPanel recipeList = new JPanel(new DynamicGridLayout(0, 1, 0, 10));
					addRecipes(recipeList, recipes, 0);
					sectionItems.add(recipeList);
				}
				break;

			case SHOPS:
				if (!shops.isEmpty())
				{
					sectionItems.add(new AtlasShopsPanel(shops, atlasItem, itemManager, clientThread, true, mainPanel, client, eventBus, pluginManager));
				}
				break;
		}

		if (sectionItems.isEmpty())
		{
			return new JPanel();
		}

		final JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setMinimumSize(new Dimension(210, 0));
		section.setMaximumSize(new Dimension(220, 0));


		final JPanel sectionHeader = new JPanel();
		sectionHeader.setLayout(new BorderLayout());
		sectionHeader.setMinimumSize(new Dimension(220, 0));
		// For whatever reason, the header extends out by a single pixel when closed. Adding a single pixel of
		// border on the right only affects the width when closed, fixing the issue.
		sectionHeader.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, 3, 1)
		));
		section.add(sectionHeader, BorderLayout.NORTH);

		final JButton sectionToggle = new JButton(SECTION_EXPAND_ICON);
		sectionToggle.setRolloverIcon(SECTION_EXPAND_ICON_HOVER);
		sectionToggle.setPreferredSize(new Dimension(18, 0));
		sectionToggle.setBorder(new EmptyBorder(0, 0, 0, 5));
		sectionToggle.setToolTipText("Expand");
		SwingUtil.removeButtonDecorations(sectionToggle);
		sectionHeader.add(sectionToggle, BorderLayout.WEST);

		final JLabel sectionName = new JLabel(useSectionType.sectionName);
		sectionName.setForeground(ColorScheme.BRAND_ORANGE);
		sectionName.setFont(FontManager.getRunescapeBoldFont());
		sectionName.setToolTipText(useSectionType.sectionDesc);
		sectionHeader.add(sectionName, BorderLayout.CENTER);

		final JPanel sectionContents = new JPanel();
		sectionContents.setLayout(new DynamicGridLayout(0, 1, 0, 10));
		sectionContents.setMinimumSize(new Dimension(220, 0));
		sectionContents.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(PluginPanel.BORDER_OFFSET, 0, PluginPanel.BORDER_OFFSET, 0)
		));
		sectionContents.setVisible(false);
		section.add(sectionContents, BorderLayout.SOUTH);

		final MouseAdapter sectionAdapter = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggleSection(sectionToggle, sectionContents);
			}
		};
		sectionToggle.addActionListener(actionEvent -> toggleSection(sectionToggle, sectionContents));
		sectionName.addMouseListener(sectionAdapter);
		sectionHeader.addMouseListener(sectionAdapter);

		sectionItems.forEach(sectionContents::add);

		if (config.defaultOpenUses().contains(DefaultOpenUses.of(useSectionType))) {
			toggleSection(sectionToggle, sectionContents);
		}

		return section;
	}

	private void toggleSection(JButton button, JPanel contents)
	{
		boolean newState = !contents.isVisible();
		contents.setVisible(newState);
		button.setIcon(newState ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
		button.setRolloverIcon(newState ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
		button.setToolTipText(newState ? "Retract" : "Expand");
		SwingUtilities.invokeLater(contents::revalidate);
	}
}
