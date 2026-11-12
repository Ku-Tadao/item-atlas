package com.itematlas.ui;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AtlasSourceRouteUtilTest
{
	@Test
	public void scrollViewStaysAsWideAsItsPane()
	{
		JPanel view = AtlasSourceRouteUtil.scrollView();
		view.add(Box.createHorizontalStrut(500), BorderLayout.NORTH);
		JScrollPane pane = new JScrollPane(view);
		pane.setSize(200, 100);
		pane.doLayout();
		pane.getViewport().doLayout();
		assertEquals(pane.getViewport().getWidth(), view.getWidth());
	}

	@Test
	public void filledSectionKeepsItsHeightInAStack()
	{
		AtlasSection section = new AtlasSection("Shops", "", new java.util.HashMap<>(), true);
		section.fill(" (1)", java.util.List.of(new javax.swing.JLabel("row")));
		JPanel stack = new JPanel();
		stack.setLayout(new javax.swing.BoxLayout(stack, javax.swing.BoxLayout.Y_AXIS));
		stack.add(section);
		stack.setSize(220, 500);
		stack.doLayout();
		assertEquals(true, section.getHeight() > 0);
	}

	@Test
	public void parsesValidWorldPoints()
	{
		assertEquals(new WorldPoint(0, 16383, 0), AtlasSourceRouteUtil.parseWorldPoint("0,16383", "0"));
		assertEquals(new WorldPoint(3200, 3201, 3), AtlasSourceRouteUtil.parseWorldPoint("3200,3201", "3"));
		assertEquals(new WorldPoint(1380, 2932, 0), AtlasSourceRouteUtil.parseWorldPoint("1380, 2932", " 0 "));
	}

	@Test
	public void rejectsInvalidWorldPoints()
	{
		String[][] invalid = {
			{null, "0"},
			{"1,2", null},
			{"", "0"},
			{"1", "0"},
			{"1,2,3", "0"},
			{"-1,2", "0"},
			{"1 2,3", "0"},
			{"a,2", "0"},
			{"16384,2", "0"},
			{"999999999999,2", "0"},
			{"1,2", "-1"},
			{"1,2", "4"},
			{"1,2", "a"}
		};

		for (String[] point : invalid)
		{
			assertNull(AtlasSourceRouteUtil.parseWorldPoint(point[0], point[1]));
		}
	}
}
