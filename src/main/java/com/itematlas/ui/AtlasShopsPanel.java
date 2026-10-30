package com.itematlas.ui;

import com.itematlas.types.AtlasShop;
import com.itematlas.types.AtlasShopItem;
import static com.itematlas.ui.AtlasPanel.MAX_ENTRIES;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.LINE_END;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;

@Slf4j
public class AtlasShopsPanel extends JPanel
{

	private static final Insets NO_INSETS = new Insets(0, 0, 0, 0);

	public AtlasShopsPanel(List<AtlasShop> shops, AtlasItem atlasItem, ItemManager itemManager, ClientThread clientThread, boolean isCurrency, AtlasPanel mainPanel, Client client, EventBus eventBus, PluginManager pluginManager)
	{

		String useName = atlasItem.getInfoItem().getName().length() > atlasItem.getInfoItem().getGroup().length()
			? atlasItem.getInfoItem().getName()
			: atlasItem.getInfoItem().getGroup();

		setMaximumSize(new Dimension(220, 80));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//		setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));

		int row = 0;
		int entries = 0;

		shopLoop:
		for (AtlasShop shop : shops)
		{
			List<AtlasShopItem> shopItems;
			shopItems = shop.getItems();

			for (AtlasShopItem shopItem : shopItems)
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
				shopName.setMaximumSize(new Dimension(0, 20));
				shopName.setPreferredSize(new Dimension(0, 20));
				shopName.setText(shop.getName());
				shopName.setToolTipText(shop.getName());
				JLabel location = new JLabel();

				if (shop.isMembers())
				{
					shopName.setForeground(new Color(209, 174, 59));
				}

				shopPanel.add(shopName, new GridBagConstraints(0, row, 1, 1, 1.0, 0.0, LINE_START, BOTH, new Insets(0, 4, 0, 0), 4, 4));
				int finalRow = row;
				clientThread.invokeLater(() ->
				{
					String sellPrice;
					if (shop.getSellMultiplier() != null)
					{
						if (isCurrency) {
							sellPrice = String.valueOf(itemManager.getItemComposition(
								itemManager.canonicalize(
									shopItem.getItemID()))
								.getPrice() * (Integer.parseInt(shop.getSellMultiplier()) / 1000));
						}
						else
						{
							sellPrice = String.valueOf(itemManager.getItemComposition(
								itemManager.canonicalize(
									atlasItem.getInfoItem().getItemID()))
								.getPrice() * (Integer.parseInt(shop.getSellMultiplier())) / 1000);
						}
					}
					else
					{
						sellPrice = shopItem.getSellPrice();
					}

					if (shopItem.getCurrency().equals("Coins"))
					{
						sellPrice += "gp";
					}
					else
					{
						sellPrice += " " + shopItem.getCurrency();
					}
					String finalSellPrice = sellPrice;
					SwingUtilities.invokeLater(() ->
						shopPanel.add(new JLabel(finalSellPrice), new GridBagConstraints(1, finalRow, 1, 1, 0.1, 0.0, LINE_END, NONE, NO_INSETS, 4, 4)));

					WorldPoint playerWP;

					if (client.getGameState() == GameState.LOGGED_IN) {
						playerWP = client.getLocalPlayer().getWorldLocation();
					}
					else {
						playerWP = null;
					}

					// the route plugin picks the nearest of the shop's map points
					Set<WorldPoint> shopTiles = new HashSet<>(AtlasSourceRouteUtil.tilesOrCoords(shop.getTiles(), shop.getCoords(), shop.getPlane()));
					AtlasSourceRouteUtil.Router router = AtlasSourceRouteUtil.findActiveRouter(pluginManager);

					JPopupMenu menu = new JPopupMenu("Menu");

					if (!shopTiles.isEmpty() && playerWP != null && router != null) {

						JMenuItem shortestPathMenu = new JMenuItem(new AbstractAction(router.pluginName)
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								Map<String, Object> data = new HashMap<>();
								data.put("start", playerWP);
								data.put("target", shopTiles);
								router.route(clientThread, eventBus, data);
							}
						});

						menu.add(shortestPathMenu);
					}

					var menuHasEntries = menu.getSubElements().length > 0;

					if (menuHasEntries)
					{
						shopPanel.addMouseListener(new MouseAdapter()
						{
							@Override
							public void mouseClicked(MouseEvent e)
							{
								if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1)
								{
									menu.show(shopPanel, e.getX(), e.getY());
								}
							}
						});

						shopName.addMouseListener(new MouseAdapter()
						{
							@Override
							public void mouseClicked(MouseEvent e)
							{
								if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1)
								{
									menu.show(shopName, e.getX(), e.getY());
								}
							}
						});

						location.addMouseListener(new MouseAdapter()
						{
							@Override
							public void mouseClicked(MouseEvent e)
							{
								if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1)
								{
									menu.show(location, e.getX(), e.getY());
								}
							}
						});
					}
				});
				row++;

				if (isCurrency)
				{
					shopPanel.add(new JLabel(shopItem.getName()), new GridBagConstraints(0, row, 2, 1, 0.0, 0.0, LINE_END, NONE, NO_INSETS, 4, 4));
					row++;
				}

				location.setMaximumSize(new Dimension(0, 20));
				location.setPreferredSize(new Dimension(0, 20));
				location.setText(shop.getLocation());
				location.setToolTipText(shop.getLocation());
				shopPanel.add(location, new GridBagConstraints(0, row, 1, 1, 1.0, 0.0, LINE_START, BOTH, new Insets(0, 4, 0, 0), 4, 4));
				shopPanel.add(new JLabel("x" + shopItem.getStock()), new GridBagConstraints(1, row++, 1, 1, 0.1, 0.0, LINE_END, NONE, NO_INSETS, 4, 4));

				if (AtlasSourceRouteUtil.tilesOrCoords(shop.getTiles(), shop.getCoords(), shop.getPlane()).isEmpty())
				{
					shopPanel.add(AtlasSourceRouteUtil.noLocationLabel(), new GridBagConstraints(0, row++, 2, 1, 1.0, 0.0, LINE_START, NONE, new Insets(0, 4, 0, 0), 4, 4));
				}

				container.add(shopPanel);
				add(container);

				entries++;

//				JSeparator actionSeparator = new JSeparator(JSeparator.HORIZONTAL);
//				actionSeparator.setBackground(ColorScheme.LIGHT_GRAY_COLOR);
//				actionSeparator.setPreferredSize(new Dimension(1, 2));
////				add(actionSeparator, new GridBagConstraints(0, row++, 4, 1, 1.0, 0.0, CENTER, HORIZONTAL, NO_INSETS, 0, 0));
//				add(actionSeparator);

			}
		}
	}
}
