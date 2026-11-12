package com.itematlas.ui;

import com.itematlas.types.AtlasSpawnItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class AtlasSpawnPanel extends JPanel
{
	
	private static final BufferedImage F2P_ICON = ImageUtil.loadImageResource(AtlasSpawnPanel.class, "free_to_play.png");
	
	public AtlasSpawnPanel(AtlasSpawnItem spawn, ClientThread clientThread, Client client, EventBus eventBus, PluginManager pluginManager, boolean showMembers)
	{
		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, 200));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(7, 7, 6, 7));
		setLayout(new BorderLayout());

		JLabel membersLabel = new JLabel();
		if (showMembers)
		{
			membersLabel.setIcon(spawn.isMembers() ? AtlasSourceRouteUtil.MEMBERS_ICON : new ImageIcon(F2P_ICON));
			membersLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
		}
		add(membersLabel, BorderLayout.WEST);
		JLabel location = new JLabel(AtlasSourceRouteUtil.wrap(spawn.getLocation(), 130));
		add(location, BorderLayout.CENTER);

		if (AtlasSourceRouteUtil.tilesOrCoords(spawn.getTiles(), spawn.getCoords(), spawn.getPlane()).isEmpty())
		{
			add(AtlasSourceRouteUtil.noLocationLabel(), BorderLayout.SOUTH);
		}

		clientThread.invokeLater(() -> AtlasSourceRouteUtil.addRouteMenu(client, clientThread, eventBus, pluginManager,
			new HashSet<>(AtlasSourceRouteUtil.tilesOrCoords(spawn.getTiles(), spawn.getCoords(), spawn.getPlane())), this, location, membersLabel));
	}
	
}
