package com.itematlas.ui;

import com.itematlas.ItemAtlasConfig;
import com.itematlas.ItemOwnership;
import com.itematlas.LocationRequirements;
import com.itematlas.RarityParser;
import com.itematlas.types.AtlasDropItem;
import com.itematlas.types.AtlasDropSource;
import com.itematlas.types.AtlasSourceLocation;
import static com.itematlas.ui.AtlasPanel.MAX_ENTRIES;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class AtlasDropsPanel extends JPanel
{
	
	private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.##%");

	// "Needs" labels to refresh when a key moves; weak, since every search rebuilds the panel
	private static final Map<JLabel, Integer> needsLabels = Collections.synchronizedMap(new WeakHashMap<>());

	private static final String[] dropTypes = {
		"agility",
		"combat",
		"farming",
		"firemaking",
		"fishing",
		"hunter",
		"mining",
		"reward",
		"smithing",
		"woodcutting",
		"thieving",
		"sailing",
		"construction",
		""
	};
	private static final Map<String, BufferedImage> dropTypeImages = Stream.of(
		dropTypes
	).collect(Collectors.toMap(
		Function.identity(),
		name ->
		{
			try
			{
				if (name.equals(""))
				{
					return ImageUtil.loadImageResource(AtlasDropsPanel.class, "drop_source_types/combat.png");
				}
				else
				{
					return ImageUtil.loadImageResource(AtlasDropsPanel.class, "drop_source_types/" + name + ".png");
				}
			}
			catch (Exception e)
			{
				return new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
			}
		}
	));

	public AtlasDropsPanel(AtlasDropItem dropItem, Map<String, List<AtlasSourceLocation>> locationsBySource, ClientThread clientThread, ItemManager itemManager, EventBus eventBus, PluginManager pluginManager, ItemAtlasConfig config, ItemOwnership itemOwnership, AtlasPanel mainPanel)
	{
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// best rate first: "Always" is 1, and rates the parser can't read sort last
		mergeBySource(dropItem.getDropSources()).stream()
			.limit(MAX_ENTRIES)
			.forEachOrdered(drops ->
			{
				AtlasDropSource dropSource = drops.get(0);
				JPanel container = new JPanel(new BorderLayout());
				container.setBorder(new EmptyBorder(0, 0, 7, 0));
				JPanel dropPanel = new JPanel(new GridBagLayout());
				dropPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				dropPanel.setBorder(new EmptyBorder(7, 7, 6, 7));

				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridy = 0;
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.LINE_START;
				gbc.gridwidth = 3;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.weightx = 1.0;
				gbc.insets = new Insets(0, 4, 0, 4);
				JLabel dropName = new JLabel(AtlasSourceRouteUtil.wrap(displayName(dropSource.getSource()), 140));
				dropPanel.add(dropName, gbc);

				gbc.gridx = 0;
				gbc.gridy++;
				gbc.gridwidth = 1;
				gbc.weightx = 0.0;
				gbc.fill = GridBagConstraints.NONE;
				dropPanel.add(new JLabel(drops.stream().map(AtlasDropsPanel::buildQuantityString).distinct().collect(Collectors.joining(" / "))), gbc);

				String level = levelText(dropSource.getDropLevel());
				JLabel dropType = new JLabel((level + " " + dropSource.getDropType()).trim(), SwingConstants.CENTER);
				ImageIcon dropTypeIcon = new ImageIcon(new BufferedImage(1, 20, BufferedImage.TYPE_INT_ARGB));
				if (Arrays.asList(dropTypes).contains(dropSource.getDropType())) {
					dropTypeIcon = new ImageIcon(dropTypeImages.get(dropSource.getDropType()));
					dropType.setText(level);
				}
				dropType.setIcon(dropTypeIcon);
				if (dropSource.getDropLevel().contains(","))
				{
					dropType.setToolTipText("Levels " + dropSource.getDropLevel().replace(",", ", "));
				}
				dropType.setPreferredSize(new Dimension(20, 20));
				gbc.gridx = 1;
				gbc.weightx = 1.0;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.fill = GridBagConstraints.BOTH;
				dropPanel.add(dropType, gbc);
				
				AtlasDropSource best = drops.stream().max(Comparator.comparingDouble(RarityParser::calculateRarity)).get();
				JLabel rarityLabel = new JLabel(best.getRarity());
				if (drops.stream().map(AtlasDropSource::getRarity).distinct().count() > 1)
				{
					// merged quantities with their own rates: show the best, list them all on hover
					addTooltip(rarityLabel, "<html>" + drops.stream()
						.map(drop -> buildQuantityString(drop) + ": " + drop.getRarity())
						.distinct()
						.collect(Collectors.joining("<br>")) + "</html>");
				}
				else if (best.getRarity().contains("/"))
					addTooltip(rarityLabel, PERCENT_FORMAT.format(RarityParser.calculateRarity(best)));
				gbc.gridx = 2;
				gbc.anchor = GridBagConstraints.LINE_END;
				gbc.fill = GridBagConstraints.NONE;
				gbc.weightx = 0.0;
				dropPanel.add(rarityLabel, gbc);

				gbc.gridx = 0;
				gbc.gridwidth = 3;
				gbc.anchor = GridBagConstraints.LINE_START;
				List<AtlasSourceLocation> locations = locationsBySource.getOrDefault(dropSource.getSource(), List.of());
				if (locations.isEmpty())
				{
					gbc.gridy++;
					dropPanel.add(AtlasSourceRouteUtil.noLocationLabel(), gbc);
				}
				else
				{
					String requiredItem = locations.get(0).getRequiredItem();
					if (requiredItem != null)
					{
						gbc.gridy++;
						dropPanel.add(needsLabel(requiredItem, locations.get(0).getRequiredItemId(), clientThread, itemManager, itemOwnership, mainPanel), gbc);
					}

					JPanel locationList = createLocationList(locations, clientThread, eventBus, pluginManager, itemOwnership, config.showMembersStar());
					int count = locationList.getComponentCount();
					gbc.fill = GridBagConstraints.HORIZONTAL;
					// a single location shows as is; only a list is worth collapsing
					if (count > 1)
					{
						gbc.gridy++;
						dropPanel.add(AtlasSourceRouteUtil.collapseHeader(count + " locations", locationList), gbc);
					}
					gbc.gridy++;
					dropPanel.add(locationList, gbc);
				}

				gbc.gridx = 0;
				gbc.gridy++;
				gbc.gridwidth = 3;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.insets = new Insets(4, 0, 4, 0);

				boolean bmePluginEnabled = false;
				for (Plugin p : pluginManager.getPlugins())
				{
					if (p.getClass().getName().equals("com.bettermonsterexamine.BetterMonsterExaminePlugin"))
					{
						bmePluginEnabled = pluginManager.isPluginActive(p);
						break;
					}
				}

				if (config.bmeNPCLookup() && bmePluginEnabled && dropSource.getDropType().equals("combat")) {
					JPopupMenu menu = new JPopupMenu("Menu");

					JMenuItem betterMonsterExamineMenu = new JMenuItem(new AbstractAction("Better Monster Examine")
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							Map<String, Object> data = new HashMap<>();
							// Remove (eg) "#MVP" or "(Tarn's Lair)" from NPC names
							String name = dropSource.getSource()
								.split("#")[0]
								.split(" \\(")[0];
							int level = Integer.parseInt(dropSource.getDropLevel().split(",")[0]);
							data.put("name", name);
							data.put("level", level);
							data.put("tab", "drops");
							eventBus.post(new PluginMessage("bettermonsterexamine", "displayMonster", data));
						}
					});
					menu.add(betterMonsterExamineMenu);

					dropPanel.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseClicked(MouseEvent e)
						{
							if (e.getClickCount() == 1)
							{
								menu.show(dropPanel, e.getX(), e.getY());
							}
						}
					});

					dropType.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseClicked(MouseEvent e)
						{
							if (e.getClickCount() == 1)
							{
								menu.show(dropType, e.getX(), e.getY());
							}
						}
					});

					rarityLabel.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseClicked(MouseEvent e)
						{
							if (e.getClickCount() == 1)
							{
								menu.show(rarityLabel, e.getX(), e.getY());
							}
						}
					});
				}

				container.add(dropPanel);
				add(container);
			});
		
	}

	/**
	 * "Needs: Muddy key (in bank)"; the status is read on the client thread, so it fills in after.
	 * Clicking it looks the key up, for where to get one.
	 */
	private static JLabel needsLabel(String requiredItem, int requiredItemId, ClientThread clientThread, ItemManager itemManager, ItemOwnership itemOwnership, AtlasPanel mainPanel)
	{
		JLabel label = new JLabel(AtlasSourceRouteUtil.wrap("Needs: " + requiredItem, 120));
		label.setName(requiredItem);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setToolTipText("Click to look up " + requiredItem);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					mainPanel.searchItemNameAndDisplay(requiredItem);
				}
			}
		});
		if (requiredItemId > 0)
		{
			needsLabels.put(label, requiredItemId);
			clientThread.invokeLater(() ->
			{
				AtlasSourceRouteUtil.smallItemIcon(label, itemManager.getImage(requiredItemId));
				showStatus(label, requiredItemId, itemOwnership);
			});
		}
		return label;
	}

	/**
	 * Re-reads every shown key status; call on the client thread when a key may have moved.
	 */
	public static void refreshNeeds(ItemOwnership itemOwnership)
	{
		Map<JLabel, Integer> labels;
		synchronized (needsLabels)
		{
			labels = new HashMap<>(needsLabels);
		}
		labels.forEach((label, itemId) -> showStatus(label, itemId, itemOwnership));
	}

	private static void showStatus(JLabel label, int itemId, ItemOwnership itemOwnership)
	{
		ItemOwnership.Status status = itemOwnership.status(itemId);
		boolean missing = status == ItemOwnership.Status.MISSING;
		boolean unchecked = status == ItemOwnership.Status.UNKNOWN || status == ItemOwnership.Status.GROUP_UNKNOWN;
		SwingUtilities.invokeLater(() ->
		{
			label.setText(AtlasSourceRouteUtil.wrap("Needs: " + label.getName() + " (" + status.text + ")", 120));
			label.setForeground(missing ? ColorScheme.PROGRESS_ERROR_COLOR
				: unchecked ? ColorScheme.PROGRESS_INPROGRESS_COLOR
				: ColorScheme.PROGRESS_COMPLETE_COLOR);
			label.setToolTipText((missing ? "Routing skips this until you have one. " : "") + "Click to look up " + label.getName());
		});
	}

	/**
	 * One row per named location of a mob; right-click a row to route to its nearest tile.
	 */
	private static JPanel createLocationList(List<AtlasSourceLocation> locations, ClientThread clientThread, EventBus eventBus, PluginManager pluginManager, ItemOwnership itemOwnership, boolean showMembers)
	{
		// the wiki can list one location on several lines, so merge rows by name
		Map<String, Set<WorldPoint>> tilesByName = new LinkedHashMap<>();
		Map<String, Boolean> membersByName = new HashMap<>();
		Map<String, AtlasSourceLocation> rowByName = new HashMap<>();
		for (AtlasSourceLocation location : locations)
		{
			String name = location.getLocation() == null || location.getLocation().isBlank() ? "Unnamed location" : location.getLocation();
			tilesByName.computeIfAbsent(name, k -> new HashSet<>()).addAll(AtlasSourceRouteUtil.parseTiles(location.getTiles()));
			membersByName.merge(name, location.isMembers(), Boolean::logicalAnd);
			rowByName.putIfAbsent(name, location);
		}

		JPanel list = new JPanel(new GridLayout(0, 1));
		list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tilesByName.forEach((name, tiles) ->
		{
			JLabel row = new JLabel(name);
			row.setFont(FontManager.getRunescapeSmallFont());
			row.setBorder(new EmptyBorder(1, 8, 1, 0));
			if (showMembers && membersByName.get(name))
			{
				AtlasSourceRouteUtil.markMembers(row);
			}
			String tooltip = tiles.isEmpty() ? name + " (no map tiles)" : name + " (right-click to path here)";
			AtlasSourceLocation location = rowByName.get(name);
			row.setToolTipText(rowTooltip(tooltip, location, List.of()));
			if (location.getRequirements() != null)
			{
				clientThread.invokeLater(() ->
				{
					List<String> unmet = itemOwnership.unmetRequirements(location);
					if (!unmet.isEmpty())
					{
						SwingUtilities.invokeLater(() ->
						{
							row.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
							row.setToolTipText(rowTooltip(tooltip, location, unmet));
						});
					}
				});
			}
			row.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (!SwingUtilities.isRightMouseButton(e) || tiles.isEmpty())
					{
						return;
					}
					AtlasSourceRouteUtil.Router router = AtlasSourceRouteUtil.findActiveRouter(pluginManager);
					JPopupMenu menu = new JPopupMenu();
					JMenuItem item = new JMenuItem(new AbstractAction(router == null ? "Enable Shortest Path or GPS to route" : "Path with " + router.pluginName)
					{
						@Override
						public void actionPerformed(ActionEvent event)
						{
							clientThread.invoke(() -> router.route(clientThread, eventBus, Map.of("target", tiles,
								"source", itemOwnership.routeLabel(List.of(rowByName.get(name)), false))));
						}
					});
					item.setEnabled(router != null);
					menu.add(item);
					menu.show(row, e.getX(), e.getY());
				}
			});
			list.add(row);
		});
		return list;
	}

	private static String rowTooltip(String tooltip, AtlasSourceLocation location, List<String> unmet)
	{
		List<String> notes = LocationRequirements.notes(location.getRequirements());
		if (notes.isEmpty())
		{
			return tooltip;
		}
		StringBuilder html = new StringBuilder("<html>").append(AtlasSourceRouteUtil.html(tooltip));
		notes.forEach(note -> html.append("<br>").append(AtlasSourceRouteUtil.html(note)));
		if (!unmet.isEmpty())
		{
			html.append("<br>Not met: ").append(AtlasSourceRouteUtil.html(String.join(", ", unmet))).append(", so routing skips it");
		}
		return html.append("</html>").toString();
	}

	/**
	 * One list per source, the wiki giving each quantity of a drop its own line; best rate first.
	 */
	static List<List<AtlasDropSource>> mergeBySource(List<AtlasDropSource> dropSources)
	{
		return dropSources.stream()
			.sorted(Comparator.comparingInt(AtlasDropSource::getQuantityLow))
			.collect(Collectors.groupingBy(AtlasDropSource::getSource, LinkedHashMap::new, Collectors.toList()))
			.values().stream()
			.sorted(Comparator.comparingDouble((List<AtlasDropSource> drops) ->
				drops.stream().mapToDouble(RarityParser::calculateRarity).max().orElse(0)).reversed())
			.collect(Collectors.toList());
	}

	/**
	 * "Bronze dragon#Standard" reads "Bronze dragon (Standard)".
	 */
	static String displayName(String source)
	{
		int hash = source.indexOf('#');
		return hash < 0 ? source : source.substring(0, hash) + " (" + source.substring(hash + 1) + ")";
	}

	/**
	 * "50" reads "Lvl 50", and a list like "13,24,76" reads "Lvl 13–76".
	 */
	static String levelText(String levels)
	{
		if (levels.isBlank())
		{
			return "";
		}
		try
		{
			IntSummaryStatistics stats = Arrays.stream(levels.split(","))
				.mapToInt(level -> Integer.parseInt(level.trim()))
				.summaryStatistics();
			return stats.getMin() == stats.getMax() ? "Lvl " + stats.getMin() : "Lvl " + stats.getMin() + "–" + stats.getMax();
		}
		catch (NumberFormatException e)
		{
			return levels;
		}
	}

	private static String buildQuantityString(AtlasDropSource dropSource)
	{
		if (dropSource.getQuantityLow() == dropSource.getQuantityHigh())
		{
			return "x" + dropSource.getQuantityLow();
		}

		return "x" + dropSource.getQuantityLow() + "-" + dropSource.getQuantityHigh();
	}

	private void addTooltip(JLabel toUnderline, String tooltip)
	{
		// add underline to text
		Font f = toUnderline.getFont();
		Map<TextAttribute, Object> attributes = new HashMap<>(f.getAttributes());
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
		toUnderline.setFont(f.deriveFont(attributes));
		toUnderline.setText(toUnderline.getText());

		// create hover tooltip
		toUnderline.setToolTipText(tooltip);
	}

}
