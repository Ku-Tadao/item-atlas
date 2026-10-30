package com.itematlas.ui;

import com.google.common.base.Strings;
import com.itematlas.ItemAtlasConfig;
import com.itematlas.ItemOwnership;
import com.itematlas.ItemAtlasPlugin;
import com.itematlas.db.H2DataProvider;
import com.itematlas.db.queries.ItemByIDQuery;
import com.itematlas.db.queries.ItemGroupQuery;
import com.itematlas.types.AtlasInfoItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class AtlasPanel extends PluginPanel
{
	private final IconTextField searchBar = new IconTextField();
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final H2DataProvider dataProvider;
	private final ItemAtlasConfig config;
	private final Client client;
	private final EventBus eventBus;
	private final PluginManager pluginManager;
	private final ItemOwnership itemOwnership;

	public final static int MAX_ENTRIES = 100;

	private AtlasItemPanel itemPanel;
//	private AtlasItemPanel prevItemPanel;

	private JPanel currentPanel;

	@Getter
	private final AtlasSearchResultsPanel searchResultsPanel;
	private boolean showUses = false;

	@Inject
	private AtlasPanel(
		ItemAtlasPlugin plugin,
		ClientThread clientThread,
		ItemManager itemManager,
		H2DataProvider dataProvider,
		ScheduledExecutorService executor,
		ItemAtlasConfig config,
		Client client,
		EventBus eventBus,
		PluginManager pluginManager,
		ItemOwnership itemOwnership
	)
	{
		super(false);
		this.searchResultsPanel = new AtlasSearchResultsPanel(clientThread, itemManager, dataProvider, this);
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.dataProvider = dataProvider;
		this.config = config;
		this.client = client;
		this.eventBus = eventBus;
		this.pluginManager = pluginManager;
		this.itemOwnership = itemOwnership;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 5, 5, 5));

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(100, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addActionListener(e -> executor.execute(this::itemSearch));
		searchBar.addClearListener(this::updateSearch);

		add(searchBar, BorderLayout.NORTH);
		add(searchResultsPanel);
		currentPanel = searchResultsPanel;
	}

	private void updateSearch()
	{
		searchResultsPanel.updateSearch(searchBar);

//		if (prevItemPanel != null)
//		{
//			log.info("previous item exists");
//			SwingUtilities.invokeLater(() ->
//			{
//				itemPanel = new AtlasItemPanel(prevItemPanel.item, itemManager, atlasData, clientThread, this);
//				remove(searchResultsPanel);
//				add(itemPanel, BorderLayout.CENTER);
//				this.updateUI();
//				prevItemPanel = null;
//			});
//
//		}
	}

	private void itemSearch()
	{
		if (Strings.isNullOrEmpty(searchBar.getText()))
		{
			updateSearch();
			return;
		}

		if (itemPanel != null)
		{
			remove(itemPanel);
			SwingUtilities.invokeLater(() ->
			{
//				prevItemPanel = new AtlasItemPanel(itemPanel.item, itemManager, atlasData, clientThread, this);
				this.updateUI();
				itemPanel = null;
			});
		}
		currentPanel = searchResultsPanel;
		add(searchResultsPanel);
		searchResultsPanel.itemSearch(searchBar);
		searchBar.setEditable(true);
	}

	private void itemSearchFirst()
	{
		if (Strings.isNullOrEmpty(searchBar.getText()))
		{
			updateSearch();
			return;
		}

		if (itemPanel != null)
		{
			remove(itemPanel);
			SwingUtilities.invokeLater(() ->
			{
//				prevItemPanel = new AtlasItemPanel(itemPanel.item, itemManager, atlasData, clientThread, this);
				this.updateUI();
				itemPanel = null;
			});
		}
		currentPanel = searchResultsPanel;
		add(searchResultsPanel);
		searchResultsPanel.itemSearchFirst(searchBar);
		searchBar.setEditable(true);
	}

	void displayItem(AtlasItemGroup itemGroup) {
		displayItem(itemGroup, false);
	}

	void displayItem(AtlasItemGroup itemGroup, boolean showUses)
	{
		itemPanel = new AtlasItemPanel(itemGroup, itemManager, dataProvider, clientThread, this, config, client, eventBus, pluginManager, itemOwnership, showUses);
		remove(currentPanel);
		currentPanel = itemPanel;
		add(itemPanel, BorderLayout.CENTER);
		updateUI();
	}

	/**
	 * Rebuilds the open item on the same tab, for config changes that affect how it's drawn.
	 */
	public void redisplayItem()
	{
		if (itemPanel != null && currentPanel == itemPanel)
		{
			displayItemById(itemPanel.selectedItem.getInfoItem().getItemID(), itemPanel.showingUses);
		}
	}

	public void displayItemById(int rawItemId) {
		displayItemById(rawItemId, false);
	}

	public void displayItemById(int rawItemId, boolean showUses)
	{
		clientThread.invokeLater(() ->
		{
			int itemId = itemManager.canonicalize(rawItemId);
			this.showUses = showUses;
			dataProvider.executeSingle(new ItemByIDQuery(itemId), this::getNerItem);
		});
	}

	public void searchItemName(String itemName) {
		SwingUtilities.invokeLater(() -> {
			searchBar.setText(itemName);
			itemSearch();
		});
	}

	public void searchItemNameAndDisplay(String itemName) {
		SwingUtilities.invokeLater(() -> {
			searchBar.setText(itemName);
			itemSearchFirst();

		});
	}

	void getNerItem(AtlasInfoItem itemInfo)
	{
		this.dataProvider.executeMany(new ItemGroupQuery(itemInfo.getGroup()), infoItems ->
			clientThread.invokeLater(() ->
				{
				List<AtlasItem> atlasItems = infoItems.stream().map((groupItem) -> {
					AsyncBufferedImage itemImage = this.itemManager.getImage(itemManager.canonicalize(groupItem.getItemID()));
					return new AtlasItem(itemImage, groupItem);
				}).collect(Collectors.toList());

				AtlasItemGroup itemGroup = new AtlasItemGroup(itemInfo.getGroup(), itemInfo.getVersion(), atlasItems);
				SwingUtilities.invokeLater(() -> displayItem(itemGroup, showUses));
			})
		);
	}

//	public AtlasInfoItem getItemByNameAndVersion(String itemName, String version)
//	{
//		List<AtlasInfoItem> matchedItems = dataProvider.executeMany(new WideSearchItemsQuery(itemName));
//
//		return matchedItems.stream()
//			.min(compareNameAndGroup(itemName, version))
//			.orElse(new AtlasInfoItem(0, "", "", "", "", "", false, false));
//	}
//
//	private static Comparator<AtlasInfoItem> compareNameAndGroup(String itemName, String version)
//	{
//		return Comparator.comparing((AtlasInfoItem item) -> new LevenshteinDistance().apply(item.getName(), itemName))
//			.thenComparing(item -> new LevenshteinDistance().apply(item.getGroup(), itemName))
//			.thenComparing(item -> new LevenshteinDistance().apply(item.getVersion() != null ? item.getVersion() : "", version != null ? version : ""));
//
//	}
}
