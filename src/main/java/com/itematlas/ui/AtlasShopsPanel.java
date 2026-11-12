package com.itematlas.ui;

import com.itematlas.types.AtlasShop;
import com.itematlas.types.AtlasShopItem;
import static com.itematlas.ui.AtlasPanel.MAX_ENTRIES;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.LINE_END;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

@Slf4j
public class AtlasShopsPanel extends JPanel
{

	private static final Insets NO_INSETS = new Insets(0, 0, 0, 0);

	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final Client client;
	private final EventBus eventBus;
	private final PluginManager pluginManager;
	private final AtlasPanel mainPanel;

	/**
	 * @param isCurrency true on the Uses tab, where the shops list what the item buys instead of where it's sold
	 */
	public AtlasShopsPanel(List<AtlasShop> shops, AtlasItem atlasItem, ItemManager itemManager, ClientThread clientThread, boolean isCurrency, AtlasPanel mainPanel, Client client, EventBus eventBus, PluginManager pluginManager, boolean showMembers)
	{
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.client = client;
		this.eventBus = eventBus;
		this.pluginManager = pluginManager;
		this.mainPanel = mainPanel;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		if (isCurrency)
		{
			int entries = 0;
			for (AtlasShop shop : shops)
			{
				if (entries >= MAX_ENTRIES)
				{
					break;
				}
				List<AtlasShopItem> items = shop.getItems().subList(0, Math.min(shop.getItems().size(), MAX_ENTRIES - entries));
				entries += items.size();
				add(currencyCard(shop, items, showMembers));
			}
			return;
		}

		int row = 0;
		int entries = 0;

		shopLoop:
		for (AtlasShop shop : shops)
		{
			for (AtlasShopItem shopItem : shop.getItems())
			{
				if (entries >= MAX_ENTRIES)
				{
					break shopLoop;
				}
				JPanel container = new JPanel(new BorderLayout());
				container.setBorder(new EmptyBorder(0, 0, 7, 0));
				JPanel shopPanel = new JPanel(new GridBagLayout());
				shopPanel.setBorder(new EmptyBorder(7, 7, 6, 7));
				shopPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

				JLabel shopName = new JLabel();
				shopName.setText(AtlasSourceRouteUtil.wrap(shop.getName(), showMembers && shop.isMembers() ? 105 : 125));
				JLabel location = new JLabel();

				if (showMembers && shop.isMembers())
				{
					AtlasSourceRouteUtil.markMembers(shopName);
				}

				shopPanel.add(shopName, new GridBagConstraints(0, row, 1, 1, 1.0, 0.0, LINE_START, BOTH, new Insets(0, 4, 0, 0), 4, 4));
				int finalRow = row;
				clientThread.invokeLater(() ->
				{
					String sellPrice = shop.getSellMultiplier() != null
						? String.valueOf(itemManager.getItemComposition(
							itemManager.canonicalize(
								atlasItem.getInfoItem().getItemID()))
							.getPrice() * (Integer.parseInt(shop.getSellMultiplier())) / 1000)
						: shopItem.getSellPrice();
					String finalSellPrice = sellPrice + (shopItem.getCurrency().equals("Coins") ? "gp" : " " + shopItem.getCurrency());
					SwingUtilities.invokeLater(() ->
						shopPanel.add(new JLabel(finalSellPrice), new GridBagConstraints(1, finalRow, 1, 1, 0.1, 0.0, LINE_END, NONE, NO_INSETS, 4, 4)));

					addRouteMenu(shop, shopPanel, shopName, location);
				});
				row++;

				location.setText(AtlasSourceRouteUtil.wrap(shop.getLocation(), 125));
				location.setFont(FontManager.getRunescapeSmallFont());
				location.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				JLabel stock = new JLabel("Stock: " + shopItem.getStock());
				stock.setFont(FontManager.getRunescapeSmallFont());
				stock.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				shopPanel.add(location, new GridBagConstraints(0, row, 1, 1, 1.0, 0.0, LINE_START, BOTH, new Insets(0, 4, 0, 0), 4, 4));
				shopPanel.add(stock, new GridBagConstraints(1, row++, 1, 1, 0.1, 0.0, LINE_END, NONE, NO_INSETS, 4, 4));

				if (AtlasSourceRouteUtil.tilesOrCoords(shop.getTiles(), shop.getCoords(), shop.getPlane()).isEmpty())
				{
					shopPanel.add(AtlasSourceRouteUtil.noLocationLabel(), new GridBagConstraints(0, row++, 2, 1, 1.0, 0.0, LINE_START, NONE, new Insets(0, 4, 0, 0), 4, 4));
				}

				container.add(shopPanel);
				add(container);

				entries++;
			}
		}
	}

	// one card per shop: its name and location once, then a line per item the currency buys
	private JPanel currencyCard(AtlasShop shop, List<AtlasShopItem> items, boolean showMembers)
	{
		JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
		card.setBorder(new EmptyBorder(7, 7, 6, 7));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel shopName = new JLabel(AtlasSourceRouteUtil.wrap(shop.getName(), showMembers && shop.isMembers() ? 170 : 190));
		if (showMembers && shop.isMembers())
		{
			AtlasSourceRouteUtil.markMembers(shopName);
		}
		card.add(shopName);

		JLabel location = new JLabel(AtlasSourceRouteUtil.wrap(shop.getLocation(), 190));
		location.setFont(FontManager.getRunescapeSmallFont());
		location.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		location.setBorder(new EmptyBorder(0, 0, 3, 0));
		card.add(location);

		List<JComponent> menuTargets = new ArrayList<>(List.of(card, shopName, location));
		for (AtlasShopItem shopItem : items)
		{
			JPanel line = new JPanel(new BorderLayout(6, 0));
			line.setOpaque(false);
			JLabel name = new JLabel(AtlasSourceRouteUtil.wrap(shopItem.getName(), 115));
			JLabel price = new JLabel();
			price.setVerticalAlignment(SwingConstants.TOP);
			String stock = "Stock: " + shopItem.getStock();
			name.setToolTipText("Click to look up " + shopItem.getName());
			price.setToolTipText(stock);
			name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			name.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e))
					{
						mainPanel.displayItemById(shopItem.getItemID());
					}
				}
			});
			line.add(name, BorderLayout.CENTER);
			line.add(price, BorderLayout.EAST);
			card.add(line);
			menuTargets.add(name);
			menuTargets.add(price);

			clientThread.invokeLater(() ->
			{
				int itemId = itemManager.canonicalize(shopItem.getItemID());
				AtlasSourceRouteUtil.smallItemIcon(name, itemManager.getImage(itemId));
				String amount = shop.getSellMultiplier() != null
					? String.valueOf(itemManager.getItemComposition(itemId).getPrice() * (Integer.parseInt(shop.getSellMultiplier()) / 1000))
					: shopItem.getSellPrice();
				// the currency is the item on show, so only coins need a unit
				String text = amount + (shopItem.getCurrency().equals("Coins") ? "gp" : "");
				SwingUtilities.invokeLater(() ->
				{
					price.setText(text);
					price.setToolTipText(amount + " " + shopItem.getCurrency() + ", " + stock.toLowerCase());
				});
			});
		}

		if (AtlasSourceRouteUtil.tilesOrCoords(shop.getTiles(), shop.getCoords(), shop.getPlane()).isEmpty())
		{
			card.add(AtlasSourceRouteUtil.noLocationLabel());
		}
		clientThread.invokeLater(() -> addRouteMenu(shop, menuTargets.toArray(new JComponent[0])));

		JPanel container = new JPanel(new BorderLayout());
		container.setBorder(new EmptyBorder(0, 0, 7, 0));
		container.add(card);
		return container;
	}

	private void addRouteMenu(AtlasShop shop, JComponent... targets)
	{
		AtlasSourceRouteUtil.addRouteMenu(client, clientThread, eventBus, pluginManager,
			new HashSet<>(AtlasSourceRouteUtil.tilesOrCoords(shop.getTiles(), shop.getCoords(), shop.getPlane())), targets);
	}
}
