package com.itematlas.ui;

import com.itematlas.db.H2DataProvider;
import com.itematlas.db.queries.ItemByIDQuery;
import com.itematlas.db.queries.ItemByNameQuery;
import com.itematlas.db.queries.ItemGroupQuery;
import com.itematlas.types.AtlasInfoItem;
import com.itematlas.types.AtlasProductionMaterial;
import com.itematlas.types.AtlasProductionRecipe;
import com.itematlas.types.AtlasProductionSkill;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.LINE_END;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class AtlasRecipePanel extends JPanel
{

	private static final Map<String, BufferedImage> facilityImages = Stream.of(
		"Anvil",
		"Apothecary",
		"Banner easel",
		"Barbarian anvil",
		"Big compost bin",
		"Blast furnace",
		"Brewery",
		"Clay oven",
		"Compost bin",
		"Cooking range",
		"Cooking range (2018 Easter event)",
		"Crafting table 1",
		"Crafting table 2",
		"Crafting table 3",
		"Crafting table 4",
		"Dairy churn",
		"Dairy cow",
		"Demon lectern",
		"Eagle lectern",
		"Eodan",
		"Fancy Clothes Store",
		"Farming patch",
		"Furnace",
		"Loom",
		"Mahogany demon lectern",
		"Mahogany eagle lectern",
		"Metal press",
		"Oak lectern",
		"Pluming stand",
		"Pottery wheel",
		"Sandpit",
		"Sawmill",
		"Sbott",
		"Shield easel",
		"Singing bowl",
		"Spinning wheel",
		"Tannery",
		"Taxidermist",
		"Teak demon lectern",
		"Teak eagle lectern",
		"Thakkrad Sigmundson",
		"Water",
		"Whetstone",
		"Windmill",
		"Woodcutting stump",
		"Workbench"
	).collect(Collectors.toMap(
		Function.identity(),
		name ->
		{
			try
			{
				return ImageUtil.loadImageResource(AtlasRecipePanel.class, "recipe_facility_icons/" + name + ".png");
			}
			catch (Exception e)
			{
				return new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
			}
		}
	));

	private static final Insets NO_INSETS = new Insets(0, 0, 0, 0);
	private static final int BANK_FILLER_ID = 20594;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final AtlasPanel mainPanel;
	private final H2DataProvider dataProvider;

	public AtlasRecipePanel(AtlasProductionRecipe recipe, ItemManager itemManager, ClientThread clientThread, AtlasPanel mainPanel, AtlasInfoItem atlasInfoItem, H2DataProvider dataProvider)
	{
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.mainPanel = mainPanel;
		this.dataProvider = dataProvider;
		// client would be used for images

		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, 800));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setLayout(new GridBagLayout());
		setBorder(new EmptyBorder(7, 7, 6, 7));
		int row = 0;

		// on the output's own page the Output block is a given, so only an amount other than one shows, up top
		boolean ownOutput = recipe.getOutputItemID() == atlasInfoItem.getItemID();
		JLabel quantityLabel = new JLabel((ownOutput ? "Makes x" : "x") + recipe.getOutputQuantity());
		if (recipe.getOutputQuantityNote() != null)
		{
			quantityLabel.setText("<html><body style=\"border-bottom: 1px dotted #ffffff\">" + quantityLabel.getText() + "*");
			String tooltipText = recipe.getOutputQuantityNote().replaceAll("[\\[\\]]|<[^>]*>", "");
			quantityLabel.setToolTipText(String.format("<html><p width=\"%d\">%s</p></html>", 200, tooltipText));
		}
		if (ownOutput && (!"1".equals(recipe.getOutputQuantity()) || recipe.getOutputQuantityNote() != null))
		{
			add(quantityLabel, new GridBagConstraints(0, row++, 4, 1, 0.0, 0.0, LINE_START, NONE, new Insets(0, 0, 2, 0), 4, 4));
		}

		JPanel skillsPanel = new JPanel(new GridBagLayout());
		skillsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		int skillsRow = 0;

		for (AtlasProductionSkill skill : recipe.getSkills())
		{
			String iconPath = "/skill_icons/" + skill.getName().toLowerCase() + ".png";

			// the icons live in the client jar, which a sideloaded plugin's own class can't see
			JLabel skillIcon = new JLabel();
			try
			{
				skillIcon.setIcon(new ImageIcon(ImageUtil.loadImageResource(SkillIconManager.class, iconPath)));
			}
			catch (IllegalArgumentException e)
			{
				log.debug("No skill icon for {}", skill.getName());
			}
			skillsPanel.add(skillIcon, new GridBagConstraints(0, skillsRow, 1, 1, 0.0, 0.0, CENTER, NONE, NO_INSETS, 4, 4));
			skillsPanel.add(new JLabel(skill.getName()), new GridBagConstraints(1, skillsRow, 1, 1, 1.0, 0.0, LINE_START, NONE, NO_INSETS, 4, 4));
			skillsPanel.add(new JLabel("<html><body style=\"text-align:right\">Lv" + skill.getLevel() + "</body></html>"), new GridBagConstraints(2, skillsRow, 1, 1, 0.2, 0.0, LINE_END, NONE, NO_INSETS, 4, 4));
			skillsPanel.add(new JLabel(skill.getExperience() + "xp"), new GridBagConstraints(3, skillsRow++, 1, 1, 0.0, 0.0, LINE_END, NONE, NO_INSETS, 4, 4));
		}

		add(skillsPanel, new GridBagConstraints(0, row++, 4, 1, 0.0, 0.0, LINE_START, BOTH, NO_INSETS, 4, 4));

		if (recipe.getFacilities() != null || recipe.getTools() != null)
		{
			// "Requires: Hammer · Anvil", each with its icon where there is one
			JPanel needs = AtlasSourceRouteUtil.flowPanel(165);
			needs.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			needs.add(needsLabel("Requires:"));
			add(needs, new GridBagConstraints(0, row++, 4, 1, 1.0, 0.0, LINE_START, BOTH, new Insets(2, 0, 2, 0), 0, 0));

			if (recipe.getFacilities() != null)
			{
				// several facilities are alternatives, e.g. "Blood Altar, Blood Altar (Kourend)"
				for (String facility : recipe.getFacilities().split(", "))
				{
					JLabel facilityLabel = addNeed(needs, facility);
					facilityLabel.setToolTipText("Facility: " + facility);
					if (facilityImages.containsKey(facility))
					{
						facilityLabel.setIcon(new ImageIcon(ImageUtil.resizeImage(facilityImages.get(facility), 18, 16, true)));
					}
				}
			}

			if (recipe.getTools() != null)
			{
				for (String tool : recipe.getTools().split(", "))
				{
					String itemName = tool.replace('_', ' ').replaceAll(" \\(.*\\)$", "");
					JLabel toolLabel = addNeed(needs, itemName);
					toolLabel.setToolTipText("Tool: " + tool);
					dataProvider.executeSingle(new ItemByNameQuery(itemName), toolItem ->
					{
						if (toolItem != null)
						{
							clientThread.invokeLater(() -> AtlasSourceRouteUtil.smallItemIcon(toolLabel, itemManager.getImage(itemManager.canonicalize(toolItem.getItemID()))));
							SwingUtilities.invokeLater(() -> addMouseAdapter(toolLabel, toolItem.getItemID(), atlasInfoItem.getItemID()));
						}
					});
				}
			}
		}

		add(new JLabel("Materials"), new GridBagConstraints(0, row++, 4, 1, 1.0, 0.0, CENTER, NONE, NO_INSETS, 4, 4));

		for (AtlasProductionMaterial material : recipe.getMaterials())
		{
			JPanel materialRow = new JPanel(new GridBagLayout());
			materialRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			JLabel materialLabel = new JLabel();
			setItemDetails(materialLabel, material.getItemID(), material.getName());

			materialRow.add(materialLabel, new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0, LINE_START, BOTH, NO_INSETS, 4, 4));

			materialRow.add(new JLabel("x" + material.getQuantity()), new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(0, 8, 0, 0), 4, 4));

			addMouseAdapter(materialLabel, material.getItemID(), atlasInfoItem.getItemID());

			add(materialRow, new GridBagConstraints(0, row++, 4, 1, 0.0, 0.0, CENTER, BOTH, NO_INSETS, 4, 4));
		}

		if (!ownOutput)
		{
			add(new JLabel("Output"), new GridBagConstraints(0, row++, 4, 1, 1.0, 0.0, CENTER, NONE, NO_INSETS, 4, 4));
			JLabel outputLabel = new JLabel();
			setItemDetails(outputLabel, recipe.getOutputItemID(), recipe.getOutputItemName());
			add(outputLabel, new GridBagConstraints(0, row, 3, 1, 1.0, 0.0, LINE_START, BOTH, NO_INSETS, 4, 4));
			addMouseAdapter(outputLabel, recipe.getOutputItemID(), atlasInfoItem.getItemID());
			add(quantityLabel, new GridBagConstraints(3, row, 1, 1, 0.0, 0.0, LINE_END, NONE, new Insets(0, 4, 0, 0), 4, 4));
		}
	}

	/**
	 * One line per recipe (output, skills, xp; materials on hover) that opens into the full card on click.
	 */
	static JPanel collapsed(AtlasProductionRecipe recipe, ItemManager itemManager, ClientThread clientThread, AtlasPanel mainPanel, AtlasInfoItem atlasInfoItem, H2DataProvider dataProvider)
	{
		JPanel panel = new JPanel(new BorderLayout());
		String skills = recipe.getSkills().stream()
			.map(skill -> skill.getName() + " " + skill.getLevel() + " · " + skill.getExperience() + "xp")
			.collect(Collectors.joining(", "));
		String name = AtlasSourceRouteUtil.html(String.valueOf(recipe.getOutputItemName()));
		JLabel summary = new JLabel("<html><div style='width:120px'>" + name
			+ (skills.isEmpty() ? "" : "<br><font color='#a5a5a5'>" + AtlasSourceRouteUtil.html(skills) + "</font>") + "</div></html>");
		summary.setOpaque(true);
		summary.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summary.setBorder(new EmptyBorder(4, 7, 4, 7));
		summary.setIconTextGap(6);
		summary.setCursor(new Cursor(Cursor.HAND_CURSOR));
		summary.setToolTipText("<html>Materials:<br>" + recipe.getMaterials().stream()
			.map(material -> AtlasSourceRouteUtil.html(material.getQuantity() + " x " + material.getName()))
			.collect(Collectors.joining("<br>")) + "</html>");
		clientThread.invokeLater(() ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemManager.canonicalize(recipe.getOutputItemID()));
			SwingUtilities.invokeLater(() -> image.addTo(summary));
		});
		panel.add(summary, BorderLayout.NORTH);

		summary.addMouseListener(new MouseAdapter()
		{
			private AtlasRecipePanel card;

			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!SwingUtilities.isLeftMouseButton(e))
				{
					return;
				}
				// built on first open: a page of full cards is slow and tall
				if (card == null)
				{
					card = new AtlasRecipePanel(recipe, itemManager, clientThread, mainPanel, atlasInfoItem, dataProvider);
					panel.add(card, BorderLayout.CENTER);
				}
				else
				{
					card.setVisible(!card.isVisible());
				}
				summary.setBackground(card.isVisible() ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
				panel.revalidate();
				panel.repaint();
			}
		});
		return panel;
	}

	private static JLabel needsLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JLabel addNeed(JPanel needs, String need)
	{
		if (needs.getComponentCount() > 1)
		{
			needs.add(needsLabel("·"));
		}
		JLabel label = needsLabel(need);
		needs.add(label);
		return label;
	}

	private void setItemDetails(JLabel label, int itemId, String itemName)
	{
		clientThread.invokeLater(() ->
		{
			AsyncBufferedImage itemImage = this.itemManager.getImage(itemManager.canonicalize(itemId));
			ItemComposition itemComposition = this.itemManager.getItemComposition(itemManager.canonicalize(itemId));
				SwingUtilities.invokeLater(() -> {
					label.setIcon(new ImageIcon(itemImage));
					if (itemId != BANK_FILLER_ID)
					{
						label.setText(AtlasSourceRouteUtil.wrap(itemComposition.getName(), 100));
						label.setToolTipText(itemComposition.getName());
					} else {
						label.setText(AtlasSourceRouteUtil.wrap(itemName, 100));
						label.setToolTipText(itemName);
					}

				});
		});
	}

	private void displayItem(AtlasInfoItem itemInfo)
	{
		this.dataProvider.executeMany(new ItemGroupQuery(itemInfo.getGroup()), infoItems ->
			clientThread.invokeLater(() ->
				{
				List<AtlasItem> atlasItems = infoItems.stream().map((groupItem) -> {
					AsyncBufferedImage itemImage = this.itemManager.getImage(itemManager.canonicalize(groupItem.getItemID()));
					return new AtlasItem(itemImage, groupItem);
				}).collect(Collectors.toList());

				AtlasItemGroup itemGroup = new AtlasItemGroup(itemInfo.getGroup(), itemInfo.getVersion(), atlasItems);
				SwingUtilities.invokeLater(() -> mainPanel.displayItem(itemGroup));
			})
		);

	}

	private void addMouseAdapter(JLabel materialLabel, int itemID, int currentItemID)
	{
		if (currentItemID == itemID || itemID == BANK_FILLER_ID) return;

		materialLabel.addMouseListener(new MouseAdapter()
		{
			private Cursor cursor;

			@Override
			public void mouseClicked(MouseEvent e)
			{
				dataProvider.executeSingle(new ItemByIDQuery(itemID), itemInfo ->
					SwingUtilities.invokeLater(() ->
						displayItem(itemInfo)));
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				cursor = getCursor();
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (cursor != null)
				{
					setCursor(cursor);
				} else {
					setCursor(new Cursor(Cursor.getDefaultCursor().getType()));
				}
			}
		});
	}
}
