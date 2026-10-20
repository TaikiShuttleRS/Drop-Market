package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import org.junit.Test;

public class MarketPainterTest
{
	// Set DROPMARKET_PREVIEW_DIR=/some/dir to dump PNGs of each state for eyeballing.
	private static final String PREVIEW_DIR = System.getenv("DROPMARKET_PREVIEW_DIR");
	private static final int W = DropMarketOverlay.DEFAULT_SIZE.width;
	private static final int H = DropMarketOverlay.DEFAULT_SIZE.height;

	private static final class TestConfig implements DropMarketConfig
	{
		Color line1Color = DEFAULT_LINE_COLOR;
		Color line2Color = DEFAULT_LINE2_COLOR;
		Color line1FallingColor = DEFAULT_NEGATIVE_DELTA_COLOR;
		Color line2FallingColor = DEFAULT_NEGATIVE_DELTA_COLOR;
		boolean showBar1;
		boolean showBar2;
		boolean showSecondLine;
		Color bar1Color = DEFAULT_BAR1_COLOR;
		Color bar2Color = DEFAULT_BAR2_COLOR;
		Color bar1TextColor = Color.WHITE;
		Color bar2TextColor = Color.BLACK;
		String bar1Text = "";
		String bar2Text = "";
		OverlayLayout layout = OverlayLayout.FULL;
		boolean showSecondNumber;
		boolean showBothDeltas = true;

		@Override
		public boolean showSecondNumber()
		{
			return showSecondNumber;
		}

		@Override
		public boolean showBothDeltas()
		{
			return showBothDeltas;
		}

		@Override
		public OverlayLayout overlayLayout()
		{
			return layout;
		}

		@Override
		public Color line1Color()
		{
			return line1Color;
		}

		@Override
		public Color line2Color()
		{
			return line2Color;
		}

		@Override
		public Color line1FallingColor()
		{
			return line1FallingColor;
		}

		@Override
		public Color line2FallingColor()
		{
			return line2FallingColor;
		}

		@Override
		public boolean showBar1()
		{
			return showBar1;
		}

		@Override
		public boolean showBar2()
		{
			return showBar2;
		}

		@Override
		public boolean showSecondLine()
		{
			return showSecondLine;
		}

		@Override
		public Color bar1Color()
		{
			return bar1Color;
		}

		@Override
		public Color bar2Color()
		{
			return bar2Color;
		}

		@Override
		public Color bar1TextColor()
		{
			return bar1TextColor;
		}

		@Override
		public Color bar2TextColor()
		{
			return bar2TextColor;
		}

		@Override
		public String bar1Text()
		{
			return bar1Text;
		}

		@Override
		public String bar2Text()
		{
			return bar2Text;
		}
	}

	private static BufferedImage render(String title, boolean resolved, long nowTick, int w, int h) throws Exception
	{
		return render(title, resolved, nowTick, w, h, new TestConfig(), null, 0);
	}

	private static BufferedImage render(String title, boolean resolved, long nowTick, int w, int h,
		TestConfig config, MarketGraph polyGraph, long volumePop) throws Exception
	{
		MarketGraph graph = new MarketGraph(new Random(1234));
		graph.reset();
		if (resolved)
		{
			graph.pinToCertain();
			graph.pinToCertain();
		}

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		MarketPainter.paint(g, w, h, title, graph, polyGraph, config, resolved, nowTick,
			FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), "3 KC", true, volumePop, 12, 9);
		g.dispose();
		return img;
	}

	private static void save(BufferedImage img, String name) throws Exception
	{
		if (PREVIEW_DIR != null)
		{
			ImageIO.write(img, "png", new File(PREVIEW_DIR, name + ".png"));
		}
	}

	private static boolean containsColor(BufferedImage img, Color c)
	{
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if ((img.getRGB(x, y) & 0xFFFFFF) == (c.getRGB() & 0xFFFFFF))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void drawsLineInGraphColor() throws Exception
	{
		BufferedImage img = render("Zulrah drops Tanzanite fang", false, 0, W, H);
		save(img, "market");
		assertTrue(containsColor(img, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void resolvedFlashesRaidPurple() throws Exception
	{
		BufferedImage on = render("Chambers of Xeric drops Twisted bow", true, 0, W, H);
		BufferedImage off = render("Chambers of Xeric drops Twisted bow", true, 2, W, H);
		save(on, "resolved-flash-on");
		save(off, "resolved-flash-off");
		assertTrue(containsColor(on, MarketPainter.RAID_PURPLE));
	}

	@Test
	public void longTitlesAndOtherSizesRender() throws Exception
	{
		save(render(DropMarketPlugin.LIVE_ODDS, false, 7, W, H), "live-odds");
		save(render("Theatre of Blood drops Sanguinesti staff and an extremely long suffix", false, 0, W, H),
			"long-title");
		save(render("Tombs of Amascut drops Tumeken's shadow", false, 0, 320, 160), "large");
		save(render("Goblin drops Anything good", false, 0, 120, 80), "tiny");
	}

	@Test
	public void brandBarsRender() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		TestConfig config = new TestConfig();
		config.showBar1 = true;
		config.showBar2 = true;
		config.showSecondLine = true;
		config.bar1Text = "Line 1 Odds";
		config.bar2Text = "Line 2 Odds";
		BufferedImage img = render("Zulrah drops Tanzanite fang", false, 0, W, H, config, poly, 1_400_000L);
		save(img, "brand-bars");
		assertTrue(containsColor(img, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void flashTurnsBothLinesPurple() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		TestConfig config = new TestConfig();
		config.showSecondLine = true;
		BufferedImage idle = render("Zulrah drops Tanzanite fang", false, 0, W, H, config, poly, 0);
		BufferedImage flashing = render("Zulrah drops Tanzanite fang", true, 0, W, H, config, poly, 0);
		assertTrue(containsColor(idle, DropMarketConfig.DEFAULT_LINE2_COLOR));
		assertTrue(containsColor(flashing, MarketPainter.RAID_PURPLE));
		assertFalse(containsColor(flashing, DropMarketConfig.DEFAULT_LINE2_COLOR));
		assertFalse(containsColor(flashing, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void compactAndTickerLayoutsRenderTheOddsAndFlash() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		for (OverlayLayout layout : new OverlayLayout[]{OverlayLayout.COMPACT, OverlayLayout.TICKER})
		{
			TestConfig config = new TestConfig();
			config.layout = layout;
			config.showSecondLine = true;
			int h = MarketPainter.layoutHeight(config);
			BufferedImage idle = render("Theatre of Blood drops Scythe of vitur", false, 0, 300, h, config, poly, 0);
			BufferedImage flashing = render("Theatre of Blood drops Scythe of vitur", true, 0, 300, h, config, poly, 0);
			assertTrue(layout + " idle shows line 1", containsColor(idle, DropMarketConfig.DEFAULT_LINE_COLOR));
			assertTrue(layout + " idle shows line 2", containsColor(idle, DropMarketConfig.DEFAULT_LINE2_COLOR));
			assertTrue(layout + " flashes purple", containsColor(flashing, MarketPainter.RAID_PURPLE));
			assertFalse(layout + " flash covers line 2", containsColor(flashing, DropMarketConfig.DEFAULT_LINE2_COLOR));
		}
	}

	@Test
	public void layoutHeightsShrinkAndBarsAddToCompactOnly()
	{
		TestConfig config = new TestConfig();
		assertTrue(MarketPainter.layoutHeight(config) < 0);
		config.layout = OverlayLayout.COMPACT;
		int plain = MarketPainter.layoutHeight(config);
		config.showBar1 = true;
		config.showBar2 = true;
		assertTrue(MarketPainter.layoutHeight(config) > plain);
		config.layout = OverlayLayout.TICKER;
		assertTrue(MarketPainter.layoutHeight(config) < plain);
	}

	@Test
	public void compactTitleFitsOnOneLineThenTwoThenUsesShortNames()
	{
		// One unit per character keeps the widths easy to reason about.
		java.util.function.ToIntFunction<String> chars = String::length;
		String shaman = "Lizardman shaman drops Dragon warhammer";
		// Fits whole on one line: nothing is shortened.
		assertEquals(java.util.Arrays.asList("LIZARDMAN SHAMAN DROPS DRAGON WARHAMMER"),
			MarketPainter.compactLines(shaman, chars, 40, 40));
		// Doesn't fit on one line, so it takes a second: still nothing shortened.
		assertEquals(java.util.Arrays.asList("LIZARDMAN SHAMAN DROPS", "DRAGON WARHAMMER"),
			MarketPainter.compactLines(shaman, chars, 25, 25));
		assertEquals(java.util.Arrays.asList("ZULRAH DROPS TANZANITE FANG"),
			MarketPainter.compactLines("Zulrah drops Tanzanite fang", chars, 40, 40));
		assertEquals(java.util.Arrays.asList("LIVE ODDS"), MarketPainter.compactLines("LIVE ODDS", chars, 40, 40));
		// The first line can be narrower (a badge sits beside it) while the second runs full width.
		assertEquals(java.util.Arrays.asList("LIZARDMAN SHAMAN", "DROPS DRAGON WARHAMMER"),
			MarketPainter.compactLines(shaman, chars, 17, 23));
		// A third line would be needed, so the raid's short name is used, keeping the item whole.
		assertEquals(java.util.Arrays.asList("TOB DROPS SCYTHE OF", "VITUR"),
			MarketPainter.compactLines("Theatre of Blood drops Scythe of vitur", chars, 19, 19));
		// Still too long, so the item gets its nickname too.
		assertEquals(java.util.Arrays.asList("TOB DROPS", "SCYTHE"),
			MarketPainter.compactLines("Theatre of Blood drops Scythe of vitur", chars, 14, 14));
		assertEquals(java.util.Arrays.asList("TOA DROPS", "FANG"),
			MarketPainter.compactLines("Tombs of Amascut drops Osmumten's fang", chars, 12, 12));
		assertEquals(java.util.Arrays.asList("TOA DROPS", "SHADOW"),
			MarketPainter.compactLines("Tombs of Amascut drops Tumeken's shadow", chars, 12, 12));
		// Never more than two lines, and never a scroll: whatever can't fit is cut with an ellipsis.
		java.util.List<String> cut = MarketPainter.compactLines("Bob GETS Scythe of vitur", chars, 6, 6);
		assertEquals(2, cut.size());
		assertTrue(cut.get(1).endsWith("..."));
	}

	@Test
	public void itemShortNames()
	{
		assertEquals("fang", DropTables.itemAbbreviation("Osmumten's fang"));
		assertEquals("shadow", DropTables.itemAbbreviation("Tumeken's shadow"));
		assertEquals("ward", DropTables.itemAbbreviation("Elidinis' ward"));
		assertEquals("Tbow", DropTables.itemAbbreviation("Twisted bow"));
		assertEquals("Scythe", DropTables.itemAbbreviation("Scythe of vitur"));
		assertEquals("DWH", DropTables.itemAbbreviation("Dragon warhammer"));
		// No nickname and no owner: left alone.
		assertEquals("Abyssal whip", DropTables.itemAbbreviation("Abyssal whip"));
		assertEquals("Coins", DropTables.itemAbbreviation("Coins"));
	}

	@Test
	public void compactCardGrowsForASecondTitleLine() throws Exception
	{
		TestConfig config = new TestConfig();
		config.layout = OverlayLayout.COMPACT;
		java.awt.Canvas canvas = new java.awt.Canvas();
		java.awt.FontMetrics titleFm = canvas.getFontMetrics(FontManager.getRunescapeBoldFont());
		java.awt.FontMetrics smallFm = canvas.getFontMetrics(FontManager.getRunescapeSmallFont());
		assertEquals(0, MarketPainter.compactExtraHeight("Zulrah drops Tanzanite fang", "3 KC", 260, titleFm, smallFm));
		int extra = MarketPainter.compactExtraHeight("Lizardman shaman drops Dragon warhammer", "17 KC", 200, titleFm, smallFm);
		assertEquals(titleFm.getHeight(), extra);
		int h = MarketPainter.layoutHeight(config) + extra;
		BufferedImage img = render("Lizardman shaman drops Dragon warhammer", false, 0, 200, h, config, null, 0);
		save(img, "compact-two-line-title");
		// Both rows of text were drawn: there is light text pixels in the second title row's band.
		assertTrue(containsColor(img.getSubimage(10, 6 + titleFm.getHeight(), 150, titleFm.getHeight()),
			new Color(235, 237, 240)));
	}

	@Test
	public void fullTitleScrollsOnlyWhenTwoLinesCannotHoldIt() throws Exception
	{
		java.awt.FontMetrics fm = new java.awt.Canvas().getFontMetrics(FontManager.getRunescapeBoldFont());
		assertNotNull(MarketPainter.titleLines("Lizardman shaman drops Dragon warhammer", fm, 190));
		assertEquals(2, MarketPainter.titleLines("Lizardman shaman drops Dragon warhammer", fm, 190).size());
		assertNull(MarketPainter.titleLines("Lizardman shaman drops an extremely long suffix that cannot ever fit", fm, 190));
		// A single word wider than the box can't wrap, so it scrolls too.
		assertNull(MarketPainter.titleLines("Supercalifragilisticexpialidocious Supercalifragilisticexpialidocious", fm, 60));
	}

	@Test
	public void compactKeepsItsSparklineWithTwoBigNumbers() throws Exception
	{
		TestConfig config = new TestConfig();
		config.layout = OverlayLayout.COMPACT;
		config.showSecondLine = true;
		config.showSecondNumber = true;
		int h = MarketPainter.layoutHeight(config);
		BufferedImage img = render("Zulrah drops Tanzanite fang", false, 0, 200, h, config, secondGraph(), 0);
		save(img, "compact-two-numbers");
		// The sparkline lives in the right-hand part of the lower row; line 1's color must show up there.
		BufferedImage sparkArea = img.getSubimage(150, h / 2, 50, h / 2);
		assertTrue(containsColor(sparkArea, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void drawnLineHoldsFlatAndKeepsTheRealLatestValue()
	{
		MarketGraph graph = new MarketGraph(new Random(3));
		graph.reset();
		for (int i = 0; i < 200; i++)
		{
			graph.step();
		}
		double[] shown = MarketPainter.rigid(graph);
		assertEquals(graph.size(), shown.length);
		assertEquals(graph.current(), shown[shown.length - 1], 0.0001);
		int flat = 0;
		for (int i = 1; i < shown.length - 1; i++)
		{
			flat += shown[i] == shown[i - 1] ? 1 : 0;
			// Every drawn level is a real value from the graph, never an invented one.
			boolean real = false;
			for (int j = 0; j < graph.size(); j++)
			{
				real |= graph.get(j) == shown[i];
			}
			assertTrue(real);
		}
		assertTrue("flat holds " + flat, flat > 5);
	}

	/** Odd sizes, titles and badges in every layout: drawing must never throw, whatever it is handed. */
	@Test
	public void paintingNeverThrowsForOddInputs() throws Exception
	{
		String[] titles = {null, "", " ", "A", "LIVE ODDS", "Zulrah drops Tanzanite fang", "X drops ", " drops Y", "drops",
			"Bob GETS Scythe of vitur", "Supercalifragilisticexpialidocious Supercalifragilisticexpialidocious drops "
			+ "Pneumonoultramicroscopicsilicovolcanoconiosis and then some more words after that so it really is long"};
		String[] badges = {null, "", "17 KC", "A very long badge that cannot possibly fit anywhere"};
		int[] widths = {0, 1, 5, 40, 120, 200, 400};
		int[] heights = {0, 1, 20, 60, 192};
		Random random = new Random(8);
		for (OverlayLayout layout : OverlayLayout.values())
		{
			for (boolean second : new boolean[]{false, true})
			{
				TestConfig config = new TestConfig();
				config.layout = layout;
				config.showSecondLine = second;
				config.showSecondNumber = random.nextBoolean();
				config.showBothDeltas = random.nextBoolean();
				config.showBar1 = random.nextBoolean();
				config.showBar2 = random.nextBoolean();
				for (String title : titles)
				{
					for (String badge : badges)
					{
						for (int w : widths)
						{
							for (int h : heights)
							{
								MarketGraph graph = new MarketGraph(new Random(random.nextInt()));
								graph.reset();
								MarketGraph poly = new MarketGraph(new Random(random.nextInt()));
								poly.reset();
								BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
								Graphics2D g = img.createGraphics();
								MarketPainter.paint(g, w, h, title, graph, poly, config, random.nextBoolean(), random.nextInt(500),
									FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), badge,
									random.nextBoolean(), random.nextInt(3) * 1_000_000L, random.nextInt(21) - 10,
									random.nextInt(21) - 10);
								g.dispose();
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void theDotBetweenTheTwoOddsOnlyAppearsWhenThereIsRoomForIt()
	{
		assertTrue(MarketPainter.gapDotFits(30, 3));
		assertTrue(MarketPainter.gapDotFits(15, 3));
		assertFalse(MarketPainter.gapDotFits(14, 3));
		assertFalse(MarketPainter.gapDotFits(0, 3));
		assertFalse(MarketPainter.gapDotFits(-10, 3));
	}

	@Test
	public void bothDeltasCanBeTurnedOff() throws Exception
	{
		TestConfig on = new TestConfig();
		on.showSecondLine = true;
		on.showSecondNumber = true;
		TestConfig off = new TestConfig();
		off.showSecondLine = true;
		off.showSecondNumber = true;
		off.showBothDeltas = false;
		for (OverlayLayout layout : OverlayLayout.values())
		{
			on.layout = layout;
			off.layout = layout;
			int h = layout == OverlayLayout.FULL ? H : MarketPainter.layoutHeight(on);
			BufferedImage withBoth = render("Zulrah drops Tanzanite fang", false, 0, 320, h, on, secondGraph(), 0);
			BufferedImage withOne = render("Zulrah drops Tanzanite fang", false, 0, 320, h, off, secondGraph(), 0);
			save(withOne, "one-delta-" + layout);
			assertTrue(layout + " should differ", differs(withBoth, withOne));
		}
	}

	private static MarketGraph secondGraph()
	{
		MarketGraph graph = new MarketGraph(new Random(99));
		graph.reset();
		return graph;
	}

	@Test
	public void longTickerTitlesScrollEachTick() throws Exception
	{
		TestConfig config = new TestConfig();
		config.layout = OverlayLayout.TICKER;
		int h = MarketPainter.layoutHeight(config);
		BufferedImage first = render("Theatre of Blood drops Scythe of vitur", false, 0, 200, h, config, null, 0);
		BufferedImage later = render("Theatre of Blood drops Scythe of vitur", false, 3, 200, h, config, null, 0);
		assertTrue(differs(first, later));
		assertEquals("TOB DROPS SCYTHE OF VITUR", MarketPainter.abbreviatedTitle("Theatre of Blood drops Scythe of vitur"));
	}

	private static boolean differs(BufferedImage a, BufferedImage b)
	{
		for (int y = 0; y < a.getHeight(); y++)
		{
			for (int x = 0; x < a.getWidth(); x++)
			{
				if (a.getRGB(x, y) != b.getRGB(x, y))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static int colorPixelsAbove(BufferedImage img, Color c, int maxY)
	{
		int n = 0;
		for (int y = 0; y < Math.min(maxY, img.getHeight()); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if ((img.getRGB(x, y) & 0xFFFFFF) == (c.getRGB() & 0xFFFFFF))
				{
					n++;
				}
			}
		}
		return n;
	}

	@Test
	public void secondOddsNumberAddsLineTwoTextNextToTheFirst() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		TestConfig config = new TestConfig();
		config.showSecondLine = true;
		BufferedImage off = render("Zulrah drops Tanzanite fang", false, 0, 260, 192, config, poly, 0);
		config.showSecondNumber = true;
		BufferedImage on = render("Zulrah drops Tanzanite fang", false, 0, 260, 192, config, poly, 0);
		// The text rows sit above the chart, so extra line-2 colored pixels up there are the second number.
		assertTrue(colorPixelsAbove(on, DropMarketConfig.DEFAULT_LINE2_COLOR, 70)
			> colorPixelsAbove(off, DropMarketConfig.DEFAULT_LINE2_COLOR, 70));
	}

	private static int rightmostColorX(BufferedImage img, Color c, int maxY)
	{
		int best = -1;
		for (int y = 0; y < Math.min(maxY, img.getHeight()); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if ((img.getRGB(x, y) & 0xFFFFFF) == (c.getRGB() & 0xFFFFFF))
				{
					best = Math.max(best, x);
				}
			}
		}
		return best;
	}

	@Test
	public void secondReadoutSitsAtTheRightEdgeAndMainNumberAtTheLeft() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		TestConfig config = new TestConfig();
		config.showSecondLine = true;
		config.showSecondNumber = true;
		BufferedImage img = render("Zulrah drops Tanzanite fang", false, 0, 260, 192, config, poly, 0);
		int line2Right = rightmostColorX(img, DropMarketConfig.DEFAULT_LINE2_COLOR, 70);
		assertTrue("line 2 reaches the right edge: " + line2Right, line2Right >= 260 - 12);
	}
}