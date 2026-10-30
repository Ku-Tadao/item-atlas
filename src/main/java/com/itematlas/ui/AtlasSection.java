package com.itematlas.ui;

import com.itematlas.ItemAtlasPlugin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * A collapsible Sources or Uses section. Its header shows straight away; the count and rows fill in once the
 * query behind it returns.
 */
class AtlasSection extends JPanel
{
	private static final ImageIcon SECTION_EXPAND_ICON;
	private static final ImageIcon SECTION_EXPAND_ICON_HOVER;
	private static final ImageIcon SECTION_RETRACT_ICON;
	private static final ImageIcon SECTION_RETRACT_ICON_HOVER;

	static
	{
		BufferedImage sectionRetractIcon = ImageUtil.loadImageResource(ItemAtlasPlugin.class, "ui/arrow_right.png");
		sectionRetractIcon = ImageUtil.luminanceOffset(sectionRetractIcon, -121);
		SECTION_EXPAND_ICON = new ImageIcon(sectionRetractIcon);
		SECTION_EXPAND_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionRetractIcon, -100));
		final BufferedImage sectionExpandIcon = ImageUtil.rotateImage(sectionRetractIcon, Math.PI / 2);
		SECTION_RETRACT_ICON = new ImageIcon(sectionExpandIcon);
		SECTION_RETRACT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionExpandIcon, -100));
	}

	private final String title;
	private final JLabel sectionName;
	private final JPanel sectionContents;

	/**
	 * @param openSections the open state of the sections the player toggled, so a section stays that way for the next item
	 */
	AtlasSection(String title, String description, Map<String, Boolean> openSections, boolean defaultOpen)
	{
		this.title = title;
		// hidden until it has rows, so sections appear one by one as their data comes in
		setVisible(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setMinimumSize(new Dimension(210, 0));
		setMaximumSize(new Dimension(220, 0));

		final JPanel sectionHeader = new JPanel();
		sectionHeader.setLayout(new BorderLayout());
		sectionHeader.setMinimumSize(new Dimension(220, 0));
		// For whatever reason, the header extends out by a single pixel when closed. Adding a single pixel of
		// border on the right only affects the width when closed, fixing the issue.
		sectionHeader.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, 3, 1)));
		add(sectionHeader);

		final JButton sectionToggle = new JButton(SECTION_EXPAND_ICON);
		sectionToggle.setRolloverIcon(SECTION_EXPAND_ICON_HOVER);
		sectionToggle.setPreferredSize(new Dimension(18, 0));
		sectionToggle.setBorder(new EmptyBorder(0, 0, 0, 5));
		sectionToggle.setToolTipText("Expand");
		SwingUtil.removeButtonDecorations(sectionToggle);
		sectionHeader.add(sectionToggle, BorderLayout.WEST);

		sectionName = new JLabel(title);
		sectionName.setForeground(ColorScheme.BRAND_ORANGE);
		sectionName.setFont(FontManager.getRunescapeBoldFont());
		sectionName.setToolTipText(description);
		sectionHeader.add(sectionName, BorderLayout.CENTER);

		sectionContents = new JPanel();
		sectionContents.setLayout(new DynamicGridLayout(0, 1, 0, 10));
		sectionContents.setMinimumSize(new Dimension(220, 0));
		sectionContents.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(PluginPanel.BORDER_OFFSET, 0, PluginPanel.BORDER_OFFSET, 0)));
		sectionContents.setVisible(false);
		add(sectionContents);

		final MouseAdapter sectionAdapter = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				openSections.put(title, toggleSection(sectionToggle));
			}
		};
		sectionToggle.addActionListener(actionEvent -> openSections.put(title, toggleSection(sectionToggle)));
		sectionName.addMouseListener(sectionAdapter);
		sectionHeader.addMouseListener(sectionAdapter);

		if (openSections.getOrDefault(title, defaultOpen))
		{
			toggleSection(sectionToggle);
		}
	}

	/**
	 * Shows the section with its count and rows; one without rows stays hidden. Call on the EDT.
	 */
	void fill(String count, List<? extends Component> rows)
	{
		if (rows.isEmpty())
		{
			return;
		}
		sectionName.setText(title + count);
		rows.forEach(sectionContents::add);
		setVisible(true);
		revalidate();
		repaint();
	}

	private boolean toggleSection(JButton button)
	{
		boolean newState = !sectionContents.isVisible();
		sectionContents.setVisible(newState);
		button.setIcon(newState ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
		button.setRolloverIcon(newState ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
		button.setToolTipText(newState ? "Retract" : "Expand");
		SwingUtilities.invokeLater(sectionContents::revalidate);
		return newState;
	}
}
