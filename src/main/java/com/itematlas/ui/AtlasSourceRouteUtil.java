package com.itematlas.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Constants;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class AtlasSourceRouteUtil
{
	private AtlasSourceRouteUtil()
	{
	}

	/**
	 * Marks a source the wiki gives no map location for, so routing skips it.
	 */
	static JLabel noLocationLabel()
	{
		JLabel label = new JLabel("No location data");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		label.setToolTipText("The wiki has no map location for this, so routing skips it.");
		return label;
	}

	/**
	 * A "+ summary" line that shows or hides the given parts when clicked. They start hidden, like loot tracker boxes.
	 */
	static JLabel collapseHeader(String summary, JComponent... parts)
	{
		JLabel header = new JLabel("+ " + summary);
		header.setFont(FontManager.getRunescapeSmallFont());
		header.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.setCursor(new Cursor(Cursor.HAND_CURSOR));
		for (JComponent part : parts)
		{
			part.setVisible(false);
		}
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (!SwingUtilities.isLeftMouseButton(e))
				{
					return;
				}
				boolean show = !parts[0].isVisible();
				for (JComponent part : parts)
				{
					part.setVisible(show);
				}
				header.setText((show ? "- " : "+ ") + summary);
				header.getParent().revalidate();
				header.getParent().repaint();
			}
		});
		return header;
	}

	/**
	 * Shown in place of a tab whose item has nothing listed on the wiki.
	 */
	static JPanel noDataPanel(String text)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		JLabel label = new JLabel("<html><div style='text-align:center'>" + text + "</div></html>", SwingConstants.CENTER);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		panel.add(label, BorderLayout.NORTH);
		return panel;
	}

	/**
	 * Plugins that accept "path" PluginMessages. GPS is checked first and gets its own namespace,
	 * because it also listens on Shortest Path's: sending there with both enabled routes twice.
	 */
	@RequiredArgsConstructor
	enum Router
	{
		GPS("GPS", "gps"),
		SHORTEST_PATH("Shortest Path", "shortestpath");

		final String pluginName;
		final String namespace;

		void route(EventBus eventBus, Map<String, Object> data)
		{
			Map<String, Object> message = new HashMap<>(data);
			// GPS shows this in its route header; Shortest Path ignores it
			message.put("source", "Item Atlas");
			eventBus.post(new PluginMessage(namespace, "path", message));
		}
	}

	@Nullable
	static Router findActiveRouter(PluginManager pluginManager)
	{
		for (Router router : Router.values())
		{
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if (router.pluginName.equals(plugin.getName()) && pluginManager.isPluginActive(plugin))
				{
					return router;
				}
			}
		}
		return null;
	}

	@Nullable
	static WorldPoint parseWorldPoint(@Nullable String coords, @Nullable String plane)
	{
		if (coords == null || plane == null)
		{
			return null;
		}

		String[] parts = coords.split(",", -1);
		if (parts.length != 2)
		{
			return null;
		}

		return toWorldPoint(parts[0], parts[1], plane);
	}

	/**
	 * Parses a TILES column ("x,y,plane" separated by ';'), skipping invalid tiles.
	 */
	static List<WorldPoint> parseTiles(@Nullable String tiles)
	{
		List<WorldPoint> points = new ArrayList<>();
		if (tiles == null || tiles.isEmpty())
		{
			return points;
		}

		for (String tile : tiles.split(";"))
		{
			String[] parts = tile.split(",", -1);
			WorldPoint point = parts.length == 3 ? toWorldPoint(parts[0], parts[1], parts[2]) : null;
			if (point != null)
			{
				points.add(point);
			}
		}
		return points;
	}

	/**
	 * Every listed tile, or the single coordinate for data without tiles.
	 */
	static List<WorldPoint> tilesOrCoords(@Nullable String tileList, @Nullable String coords, @Nullable String plane)
	{
		List<WorldPoint> tiles = parseTiles(tileList);
		if (tiles.isEmpty())
		{
			WorldPoint point = parseWorldPoint(coords, plane);
			if (point != null)
			{
				tiles.add(point);
			}
		}
		return tiles;
	}

	@Nullable
	private static WorldPoint toWorldPoint(String xs, String ys, String zs)
	{
		try
		{
			int x = Integer.parseInt(xs.trim());
			int y = Integer.parseInt(ys.trim());
			int z = Integer.parseInt(zs.trim());

			// Jagex packed coordinates use 14 bits for x/y, and RuneLite defines four planes.
			return x >= 0 && x <= 0x3FFF && y >= 0 && y <= 0x3FFF && z >= 0 && z < Constants.MAX_Z
				? new WorldPoint(x, y, z)
				: null;
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}
}
