package com.dropmarket;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

final class MarketPainter
{
	static final Color RAID_PURPLE = new Color(0xEF20FF);

	private static final Color BACKGROUND = new Color(12, 14, 18, 200);
	private static final Color BORDER = new Color(255, 255, 255, 30);
	private static final Color GRID = new Color(255, 255, 255, 45);
	private static final Color MUTED = new Color(150, 156, 166);
	private static final Color TITLE = new Color(235, 237, 240);
	private static final Color LIVE_RED = new Color(0xFF3B5C);
	private static final Color EYE = new Color(20, 20, 24);
	private static final Font BAR_TEXT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
	private static final Stroke GRID_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
		10f, new float[]{2f, 3f}, 0f);
	// Mitered joins keep the corners of the line crisp, like a real market chart.
	private static final Stroke LINE_STROKE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 3f);
	private static final int PAD = 6;
	// All animation here steps once per game tick (0.6s) rather than wall-clock time, so it stays in lockstep
	// with everything else OSRS does on the tick.
	private static final int FLASH_PERIOD_TICKS = 2;
	private static final int DOT_PERIOD_TICKS = 1;
	private static final int PULSE_PERIOD_TICKS = 3;
	private static final int BRAND_BAR_H = 14;
	private static final int COMPACT_BODY_H = 56;
	private static final int TICKER_H = 20;
	// Spacing in the odds row: big number to its change, between the two numbers, and between two changes.
	private static final int CHANGE_GAP = 7;
	private static final int GROUP_GAP = 14;
	// The same gaps, tightened for compact's two smaller numbers so the sparkline keeps its room.
	private static final int TIGHT_CHANGE_GAP = 5;
	private static final int TIGHT_GROUP_GAP = 10;
	private static final int PAIR_GAP = 10;
	private static final int SCROLL_GAP = 28;
	// Empty room kept on each side of the dash between the two odds.
	private static final int GAP_DASH_MARGIN = 6;
	// The drawn line only moves once the odds have changed by this many points, so it holds flat and then turns sharply.
	private static final double RIGID_STEP = 1.4;
	private static final int SCROLL_PX_PER_TICK = 8;

	private MarketPainter()
	{
	}

	/** The configured layout, or Full if the setting somehow has none. */
	static OverlayLayout layout(DropMarketConfig config)
	{
		OverlayLayout layout = config.overlayLayout();
		return layout == null ? OverlayLayout.FULL : layout;
	}

	/** The configured colour, or the default if the setting somehow has none. */
	private static Color color(Color configured, Color fallback)
	{
		return configured == null ? fallback : configured;
	}

	static void paint(Graphics2D graphics, int w, int h, String title, MarketGraph graph, MarketGraph polyGraph,
		DropMarketConfig config, boolean resolved, long nowTick, Font titleFont, Font smallFont, String badge,
		boolean badgeSkull, long volumePop, int delta1, int delta2)
	{
		title = title == null ? "" : title;
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			boolean flashOn = resolved && config.flashOnWin() && (nowTick / FLASH_PERIOD_TICKS) % 2 == 0;
			Color lineColor = color(config.line1Color(), DropMarketConfig.DEFAULT_LINE_COLOR);
			Color accent = flashOn ? RAID_PURPLE : lineColor;
			MarketGraph secondLine = config.showSecondLine() ? polyGraph : null;
			Color accent2 = flashOn ? RAID_PURPLE : color(config.line2Color(), DropMarketConfig.DEFAULT_LINE2_COLOR);

			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			RoundRectangle2D panel = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 8, 8);
			g.setColor(BACKGROUND);
			g.fill(panel);
			if (flashOn)
			{
				g.setColor(new Color(RAID_PURPLE.getRed(), RAID_PURPLE.getGreen(), RAID_PURPLE.getBlue(), 45));
				g.fill(panel);
			}

			OverlayLayout layout = layout(config);
			boolean bars = layout != OverlayLayout.TICKER;
			int y = 0;
			if (bars && config.showBar1())
			{
				g.setColor(color(config.bar1Color(), DropMarketConfig.DEFAULT_BAR1_COLOR));
				g.fillRect(0, y, w, BRAND_BAR_H);
				if (config.bar1Text() != null && !config.bar1Text().isEmpty())
				{
					drawBarText(g, config.bar1Text(), w, y, BRAND_BAR_H, color(config.bar1TextColor(), Color.BLACK));
				}
				y += BRAND_BAR_H;
			}
			if (bars && config.showBar2())
			{
				g.setColor(color(config.bar2Color(), DropMarketConfig.DEFAULT_BAR2_COLOR));
				g.fillRect(0, y, w, BRAND_BAR_H);
				if (config.bar2Text() != null && !config.bar2Text().isEmpty())
				{
					drawBarText(g, config.bar2Text(), w, y, BRAND_BAR_H, color(config.bar2TextColor(), Color.WHITE));
				}
				y += BRAND_BAR_H;
			}

			g.setColor(flashOn ? RAID_PURPLE : BORDER);
			g.setStroke(new BasicStroke(flashOn ? 2f : 1f));
			g.draw(panel);

			if (layout == OverlayLayout.COMPACT)
			{
				paintCompact(g, w, y, title, graph, secondLine, accent, accent2, config, nowTick, titleFont, smallFont,
					badge, delta1, delta2);
				return;
			}
			if (layout == OverlayLayout.TICKER)
			{
				paintTicker(g, w, h, title, graph, secondLine, accent, accent2, config, nowTick, titleFont, smallFont,
					delta1, delta2);
				return;
			}

			Font bigFont = titleFont.deriveFont(titleFont.getSize2D() * 1.25f);
			FontMetrics titleFm = g.getFontMetrics(titleFont);
			FontMetrics smallFm = g.getFontMetrics(smallFont);
			FontMetrics bigFm = g.getFontMetrics(bigFont);

			y += PAD;
			int liveBaseline = y + smallFm.getAscent();
			int lx = PAD;
			boolean dotOn = (nowTick / DOT_PERIOD_TICKS) % 2 == 0;
			int dotR = 3;
			g.setFont(smallFont.deriveFont(Font.BOLD));
			FontMetrics liveFm = g.getFontMetrics();
			double dotCenterY = liveBaseline - liveFm.getAscent() / 2.0 - 1;
			g.setColor(dotOn ? LIVE_RED : withAlpha(LIVE_RED, 80));
			g.fill(new Ellipse2D.Double(lx, dotCenterY - dotR, dotR * 2, dotR * 2));
			lx += dotR * 2 + 4;
			text(g, "LIVE", lx, liveBaseline, LIVE_RED);
			lx += liveFm.stringWidth("LIVE") + 8;

			int skullSize = Math.max(6, smallFm.getAscent() - 2);
			if (badge != null && !badge.isEmpty())
			{
				g.setFont(smallFont);
				lx += 2;
				text(g, "·", lx, liveBaseline, MUTED);
				lx += smallFm.stringWidth("·") + 6;
				if (badgeSkull)
				{
					drawSkull(g, lx, (int) Math.round(dotCenterY + skullSize / 2.0), skullSize, MUTED);
					lx += skullSize + 3;
				}
				text(g, badge, lx, liveBaseline, MUTED);
			}
			y += Math.max(smallFm.getHeight(), skullSize + 2) + 4;

			List<String> lines = titleLines(title, titleFm, w - PAD * 2);
			g.setFont(titleFont);
			if (lines == null)
			{
				// Too long even for two lines with the wiki short name: one line that scrolls like the ticker.
				y += titleFm.getAscent();
				scrollText(g, abbreviatedTitle(title), titleFm, PAD, w - PAD, y, nowTick);
				y += titleFm.getDescent();
			}
			else
			{
				for (String line : lines)
				{
					y += titleFm.getAscent();
					text(g, line, PAD, y, TITLE);
					y += titleFm.getDescent();
				}
			}

			y += 3 + bigFm.getAscent();
			int pct = (int) Math.round(graph.current());
			String pctText = pct + "%";
			String pct2 = secondLine == null ? null : Math.round(secondLine.current()) + "%";
			// The changes sit 1px higher than the numbers' baseline in the full card. If both big numbers and their
			// changes won't fit across at the big size (a win's 100% with large changes), they shrink to the
			// ticker's size and tighten up rather than line 2's number being dropped.
			Font oddsFont = bigFont;
			boolean tight = false;
			if (pct2 != null && config.showSecondNumber()
				&& PAD + twoNumberWidth(g, bigFont, smallFont, pctText, pct2, delta1, delta2, config.showBothDeltas(), true,
				false) > w - PAD)
			{
				oddsFont = titleFont;
				tight = true;
			}
			drawOdds(g, PAD, y, w - PAD, true, tight, 1, oddsFont, smallFont, accent, accent2, pctText, pct2, delta1, delta2,
				config);
			y += bigFm.getDescent();
			g.setFont(smallFont);

			int footerH = smallFm.getAscent() + 2;
			int labelW = smallFm.stringWidth("100%") + 4;
			int chartLeft = PAD;
			int chartRight = w - PAD - labelW;
			int chartTop = y + 4 + smallFm.getAscent() / 2;
			int chartBottom = h - PAD - footerH - smallFm.getAscent() / 2;
			if (chartBottom - chartTop >= 10 && chartRight - chartLeft >= 20)
			{
				drawChart(g, graph, secondLine, accent, accent2, chartLeft, chartTop, chartRight,
					chartBottom, smallFm, nowTick);
			}

			int footY = h - PAD;
			g.setFont(smallFont);
			String volume = formatVolume(graph.volume());
			text(g, volume, PAD, footY, MUTED);
			int footRight = w - PAD;
			if (resolved)
			{
				String badgeText = "RESOLVED: YES";
				footRight -= smallFm.stringWidth(badgeText);
				text(g, badgeText, footRight, footY, flashOn ? RAID_PURPLE : TITLE);
			}
			if (volumePop > 0)
			{
				String pop = "+ " + formatPop(volumePop);
				int px = PAD + smallFm.stringWidth(volume) + 6;
				if (px + smallFm.stringWidth(pop) <= footRight - 4)
				{
					text(g, pop, px, footY, accent);
				}
			}
		}
		finally
		{
			g.dispose();
		}
	}

	/** Height of the overlay for a layout; Full keeps whatever size the player resized it to (returns -1). */
	static int layoutHeight(DropMarketConfig config)
	{
		switch (layout(config))
		{
			case COMPACT:
				return (config.showBar1() ? BRAND_BAR_H : 0) + (config.showBar2() ? BRAND_BAR_H : 0) + COMPACT_BODY_H;
			case TICKER:
				return TICKER_H;
			default:
				return -1;
		}
	}

	/** Two rows: live dot + title (+ badge), then the odds with their changes and a sparkline. */
	private static void paintCompact(Graphics2D g, int w, int top, String title, MarketGraph graph,
		MarketGraph secondLine, Color accent, Color accent2, DropMarketConfig config, long nowTick, Font titleFont,
		Font smallFont, String badge, int delta1, int delta2)
	{
		FontMetrics titleFm = g.getFontMetrics(titleFont);
		FontMetrics smallFm = g.getFontMetrics(smallFont);
		Font bigFont = titleFont.deriveFont(titleFont.getSize2D() * 1.25f);
		FontMetrics bigFm = g.getFontMetrics(bigFont);

		int y = top + PAD;
		int baseline = y + titleFm.getAscent();
		int x = PAD;
		int dotR = 3;
		boolean dotOn = (nowTick / DOT_PERIOD_TICKS) % 2 == 0;
		g.setColor(dotOn ? LIVE_RED : withAlpha(LIVE_RED, 80));
		g.fill(new Ellipse2D.Double(x, baseline - titleFm.getAscent() / 2.0 - dotR - 1, dotR * 2, dotR * 2));
		x += dotR * 2 + 5;

		if (badge != null && !badge.isEmpty())
		{
			g.setFont(smallFont);
			text(g, badge, w - PAD - smallFm.stringWidth(badge), baseline, MUTED);
		}
		// The title takes a second line if it has to, so the card is taller for long titles (see compactExtraHeight).
		List<String> titleRows = compactTitleLines(title, badge, w, titleFm, smallFm);
		g.setFont(titleFont);
		for (int row = 0; row < titleRows.size(); row++)
		{
			text(g, titleRows.get(row), x, baseline + row * titleFm.getHeight(), TITLE);
		}
		y += titleFm.getHeight() * titleRows.size() + 2;

		int pctBaseline = y + bigFm.getAscent();
		String pctText = Math.round(graph.current()) + "%";
		String pct2 = secondLine == null ? null : Math.round(secondLine.current()) + "%";
		// With two big numbers they shrink to the ticker's size so the sparkline still has room to show.
		Font oddsFont = pct2 != null && config.showSecondNumber() ? titleFont : bigFont;
		g.setFont(oddsFont);
		// The numbers sit 3px lower than the row they were laid out on; the sparkline keeps its place.
		int dx = drawOdds(g, PAD, pctBaseline + 3, w - PAD - 24, false, oddsFont == titleFont, 0, oddsFont, smallFont,
			accent, accent2, pctText, pct2, delta1, delta2, config);

		int sparkLeft = dx + 12;
		int sparkRight = w - PAD - 3;
		if (sparkRight - sparkLeft >= 24)
		{
			int sparkTop = y + 2;
			int sparkBottom = pctBaseline + bigFm.getDescent() - 2;
			if (secondLine != null)
			{
				drawLine(g, secondLine, accent2, sparkLeft, sparkTop, sparkRight, sparkBottom, nowTick);
			}
			drawLine(g, graph, accent, sparkLeft, sparkTop, sparkRight, sparkBottom, nowTick);
		}
	}

	/** One line: live dot, title, odds, change(s) and a small sparkline, like a stock ticker. */
	private static void paintTicker(Graphics2D g, int w, int h, String title, MarketGraph graph,
		MarketGraph secondLine, Color accent, Color accent2, DropMarketConfig config, long nowTick, Font titleFont,
		Font smallFont, int delta1, int delta2)
	{
		FontMetrics titleFm = g.getFontMetrics(titleFont);
		int baseline = (h + titleFm.getAscent() - titleFm.getDescent()) / 2;

		int dotR = 3;
		boolean dotOn = (nowTick / DOT_PERIOD_TICKS) % 2 == 0;
		g.setColor(dotOn ? LIVE_RED : withAlpha(LIVE_RED, 80));
		g.fill(new Ellipse2D.Double(PAD, h / 2.0 - dotR, dotR * 2, dotR * 2));
		int titleLeft = PAD + dotR * 2 + 5;

		int right = w - PAD;
		int sparkW = w >= 260 ? 44 : 0;
		// Only wide strips have room for the second line's change next to the odds.
		boolean secondDelta = secondLine != null && w >= 300 && config.showBothDeltas();
		if (sparkW > 0)
		{
			int sparkLeft = right - sparkW;
			if (secondLine != null)
			{
				drawLine(g, secondLine, accent2, sparkLeft, 4, right - 3, h - 4, nowTick);
			}
			drawLine(g, graph, accent, sparkLeft, 4, right - 3, h - 4, nowTick);
			right = sparkLeft - 8;
		}

		boolean secondNumber = secondLine != null && config.showSecondNumber() && w >= 300;
		String pct1 = Math.round(graph.current()) + "%";
		String pct2 = secondLine == null ? "" : Math.round(secondLine.current()) + "%";
		int d1W = deltaWidth(g, delta1, smallFont);
		int d2W = secondDelta ? deltaWidth(g, delta2, smallFont) : 0;
		int groupGap = 10;
		int group1W = titleFm.stringWidth(pct1) + 6 + d1W;
		int group2W = secondNumber ? titleFm.stringWidth(pct2) + (secondDelta ? 6 + d2W : 0) : secondDelta ? d2W : 0;
		int startX = right - (group1W + (group2W > 0 ? groupGap + group2W : 0));
		int x = startX;
		g.setFont(titleFont);
		text(g, pct1, x, baseline, accent);
		x += titleFm.stringWidth(pct1) + 6;
		x += drawDelta(g, x, baseline, delta1, accent, color(config.line1FallingColor(), DropMarketConfig.DEFAULT_NEGATIVE_DELTA_COLOR), smallFont);
		if (group2W > 0)
		{
			x += groupGap;
			if (secondNumber)
			{
				g.setFont(titleFont);
				text(g, pct2, x, baseline, accent2);
				x += titleFm.stringWidth(pct2) + 6;
			}
			if (secondDelta)
			{
				drawDelta(g, x, baseline, delta2, accent2, color(config.line2FallingColor(), DropMarketConfig.DEFAULT_NEGATIVE_DELTA_COLOR), smallFont);
			}
		}
		Font tickerTitleFont = smallFont.deriveFont(Font.BOLD);
		g.setFont(tickerTitleFont);
		FontMetrics tickerFm = g.getFontMetrics(tickerTitleFont);
		int titleRight = startX - 8;
		String shortTitle = abbreviatedTitle(title);
		if (tickerFm.stringWidth(shortTitle) <= titleRight - titleLeft)
		{
			text(g, shortTitle, titleLeft, baseline, TITLE);
		}
		else
		{
			scrollText(g, shortTitle, tickerFm, titleLeft, titleRight, baseline, nowTick);
		}
	}

	/** The room the Compact title has: from after the live dot to before the badge. */
	static List<String> compactTitleLines(String title, String badge, int w, FontMetrics titleFm, FontMetrics smallFm)
	{
		int left = PAD + 3 * 2 + 5;
		int right = w - PAD;
		int firstRight = right;
		if (badge != null && !badge.isEmpty())
		{
			firstRight -= smallFm.stringWidth(badge) + 6;
		}
		// The badge only sits on the first row, so the second row can run the full width.
		return compactLines(title, titleFm::stringWidth, firstRight - left, right - left);
	}

	/** How much taller than a one-line title the Compact card must be for this title. */
	static int compactExtraHeight(String title, String badge, int w, FontMetrics titleFm, FontMetrics smallFm)
	{
		return (compactTitleLines(title, badge, w, titleFm, smallFm).size() - 1) * titleFm.getHeight();
	}

	/**
	 * The Compact title as one or two lines. The whole title goes on one line if it fits, else on two. Only if it would
	 * need a third line are the wiki short names used: the source first (Theatre of Blood becomes ToB), then the item
	 * too (Osmumten's fang becomes fang). If even that doesn't fit, it is cut with an ellipsis rather than scrolled.
	 */
	static List<String> compactLines(String title, ToIntFunction<String> width, int firstMax, int restMax)
	{
		title = title == null ? "" : title;
		String upper = title.toUpperCase(Locale.ROOT);
		for (String candidate : new String[]{upper, shortTitle(title, false), shortTitle(title, true)})
		{
			List<String> lines = wrap(candidate, width, firstMax, restMax, Integer.MAX_VALUE);
			boolean fits = lines.size() <= 2;
			for (int i = 0; i < lines.size(); i++)
			{
				fits &= width.applyAsInt(lines.get(i)) <= (i == 0 ? firstMax : restMax);
			}
			if (fits)
			{
				return lines;
			}
		}
		return wrap(shortTitle(title, true), width, firstMax, restMax, 2);
	}

	/** The title with the source's wiki short name and, if asked, the item's short name. Upper-cased. */
	static String shortTitle(String title, boolean shortItem)
	{
		title = title == null ? "" : title;
		String upper = title.toUpperCase(Locale.ROOT);
		String verb = upper.contains(" DROPS ") ? " DROPS " : upper.contains(" GETS ") ? " GETS " : null;
		if (verb == null)
		{
			return upper;
		}
		int at = upper.indexOf(verb);
		String item = title.substring(at + verb.length());
		return (DropTables.abbreviation(title.substring(0, at)) + verb.toLowerCase(Locale.ROOT)
			+ (shortItem ? DropTables.itemAbbreviation(item) : item)).toUpperCase(Locale.ROOT);
	}

	/**
	 * The title as at most two lines for the Full card: the whole title if it wraps into two, else with the wiki short
	 * name. Returns null when even that doesn't fit, and the caller scrolls it.
	 */
	static List<String> titleLines(String title, FontMetrics fm, int maxWidth)
	{
		title = title == null ? "" : title;
		for (String candidate : new String[]{title.toUpperCase(Locale.ROOT), abbreviatedTitle(title)})
		{
			List<String> lines = wrap(candidate, fm, maxWidth, Integer.MAX_VALUE);
			boolean fits = lines.size() <= 2;
			for (String line : lines)
			{
				fits &= fm.stringWidth(line) <= maxWidth;
			}
			if (fits)
			{
				return lines;
			}
		}
		return null;
	}

	/** Source swapped for its wiki short name (ToB, CoX...), everything else untouched, upper-cased. */
	static String abbreviatedTitle(String title)
	{
		title = title == null ? "" : title;
		String upper = title.toUpperCase(Locale.ROOT);
		String verb = upper.contains(" DROPS ") ? " DROPS " : upper.contains(" GETS ") ? " GETS " : null;
		if (verb == null)
		{
			return upper;
		}
		int at = upper.indexOf(verb);
		return DropTables.abbreviation(title.substring(0, at)).toUpperCase(Locale.ROOT) + upper.substring(at);
	}

	/** Scrolls text right to left inside [left, right], looping like a stock ticker; one step per game tick. */
	private static void scrollText(Graphics2D g, String s, FontMetrics fm, int left, int right, int baseline,
		long nowTick)
	{
		int cycle = fm.stringWidth(s) + SCROLL_GAP;
		int offset = (int) ((nowTick * SCROLL_PX_PER_TICK) % cycle);
		java.awt.Shape oldClip = g.getClip();
		g.clipRect(left, 0, right - left, baseline + fm.getDescent() + 4);
		text(g, s, left - offset, baseline, TITLE);
		text(g, s, left - offset + cycle, baseline, TITLE);
		g.setClip(oldClip);
	}

	private static int deltaWidth(Graphics2D g, int delta, Font smallFont)
	{
		FontMetrics fm = g.getFontMetrics(deltaFont(smallFont));
		return (delta != 0 ? 11 : 0) + fm.stringWidth(Math.abs(delta) + "%");
	}

	/**
	 * Draws line 1's big number and change on the left, then line 2. With the second number on, line 2's number and
	 * change sit at the right edge of the box (Full) or follow line 1's (Compact, which keeps the right for its
	 * sparkline). Without the second number, line 2's change just follows line 1's. Returns where the row ended.
	 */
	private static int drawOdds(Graphics2D g, int startX, int baseline, int limit, boolean full, boolean tight,
		int deltaLift, Font bigFont, Font smallFont, Color accent, Color accent2, String pct1, String pct2, int delta1,
		int delta2, DropMarketConfig config)
	{
		final int changeGap = tight ? TIGHT_CHANGE_GAP : CHANGE_GAP;
		final int groupGap = tight ? TIGHT_GROUP_GAP : GROUP_GAP;
		FontMetrics bigFm = g.getFontMetrics(bigFont);
		int deltaBase = baseline - deltaLift;
		boolean delta2On = pct2 != null && config.showBothDeltas();
		int pct1W = bigFm.stringWidth(pct1);
		int d1W = deltaWidth(g, delta1, smallFont);
		int d2W = delta2On ? deltaWidth(g, delta2, smallFont) : 0;
		int pct2W = pct2 == null ? 0 : bigFm.stringWidth(pct2);
		int tail2 = delta2On ? changeGap + d2W : 0;
		boolean number = pct2 != null && config.showSecondNumber();
		if (number)
		{
			number = startX + twoNumberWidth(g, bigFont, smallFont, pct1, pct2, delta1, delta2, delta2On, full, tight) <= limit;
		}

		if (number)
		{
			int x = startX;
			g.setFont(bigFont);
			text(g, pct1, x, baseline, accent);
			x += pct1W + changeGap;
			x += drawDelta(g, x, deltaBase, delta1, accent, color(config.line1FallingColor(), DropMarketConfig.DEFAULT_NEGATIVE_DELTA_COLOR), smallFont);
			int firstGroupEnd = x;
			// Line 2's readout hugs the right edge in Full; Compact keeps that side for its sparkline.
			x = full ? limit - (pct2W + tail2) : x + groupGap;
			if (full)
			{
				drawGapDash(g, firstGroupEnd, x, baseline, bigFm, smallFont);
			}
			g.setFont(bigFont);
			text(g, pct2, x, baseline, accent2);
			x += pct2W;
			if (!delta2On)
			{
				return x;
			}
			x += changeGap;
			return x + drawDelta(g, x, deltaBase, delta2, accent2, color(config.line2FallingColor(), DropMarketConfig.DEFAULT_NEGATIVE_DELTA_COLOR), smallFont);
		}

		int x = startX;
		g.setFont(bigFont);
		text(g, pct1, x, baseline, accent);
		x += pct1W + changeGap;
		x += drawDelta(g, x, deltaBase, delta1, accent, color(config.line1FallingColor(), DropMarketConfig.DEFAULT_NEGATIVE_DELTA_COLOR), smallFont);
		if (!delta2On)
		{
			return x;
		}
		// No second number: line 2's change simply follows line 1's.
		x += PAIR_GAP;
		return x + drawDelta(g, x, deltaBase, delta2, accent2, color(config.line2FallingColor(), DropMarketConfig.DEFAULT_NEGATIVE_DELTA_COLOR), smallFont);
	}

	/** How wide the row is with both big numbers and their changes. */
	private static int twoNumberWidth(Graphics2D g, Font bigFont, Font smallFont, String pct1, String pct2, int delta1,
		int delta2, boolean delta2On, boolean full, boolean tight)
	{
		int changeGap = tight ? TIGHT_CHANGE_GAP : CHANGE_GAP;
		int groupGap = tight ? TIGHT_GROUP_GAP : GROUP_GAP;
		FontMetrics fm = g.getFontMetrics(bigFont);
		int tail2 = delta2On ? changeGap + deltaWidth(g, delta2, smallFont) : 0;
		return fm.stringWidth(pct1) + changeGap + deltaWidth(g, delta1, smallFont) + groupGap + fm.stringWidth(pct2) + tail2;
	}

	/** True when there is room for the small dash between the two odds: some empty space on both sides of it. */
	static boolean gapDashFits(int gap, int dashWidth)
	{
		return gap >= dashWidth + 2 * GAP_DASH_MARGIN;
	}

	/** A small dash centred in the empty space between the two odds. */
	private static void drawGapDash(Graphics2D g, int left, int right, int baseline, FontMetrics bigFm, Font smallFont)
	{
		FontMetrics smallFm = g.getFontMetrics(smallFont);
		int dashWidth = smallFm.stringWidth("-");
		int gap = right - left;
		if (!gapDashFits(gap, dashWidth))
		{
			return;
		}
		g.setFont(smallFont);
		// The dash sits mid-height of the big numbers, not on their baseline.
		int y = baseline - bigFm.getAscent() / 2 + smallFm.getAscent() / 3 + 4;
		text(g, "-", left + (gap - dashWidth) / 2, y, MUTED);
	}

	/** Always renders (the delta should never disappear); the arrow is skipped only for an exact 0. */
	private static int drawDelta(Graphics2D g, int x, int y, int delta, Color positiveColor, Color negativeColor,
		Font smallFont)
	{
		Color color = delta < 0 ? negativeColor : positiveColor;
		String deltaText = Math.abs(delta) + "%";
		Font deltaFont = deltaFont(smallFont);
		FontMetrics deltaFm = g.getFontMetrics(deltaFont);
		int triW = 7;
		int mid = y - deltaFm.getAscent() / 2 - 1;
		if (delta < 0)
		{
			// Drop the down-triangle back down 1px relative to the up-triangle's nudge.
			mid += 1;
		}
		int textX = x;
		if (delta != 0)
		{
			Polygon tri = delta > 0
				? new Polygon(new int[]{x, x + triW, x + triW / 2}, new int[]{mid + 3, mid + 3, mid - 3}, 3)
				: new Polygon(new int[]{x, x + triW, x + triW / 2}, new int[]{mid - 3, mid - 3, mid + 3}, 3);
			g.setColor(color);
			g.fill(tri);
			textX = x + triW + 4;
		}
		g.setFont(deltaFont);
		text(g, deltaText, textX, y, color);
		return (textX - x) + deltaFm.stringWidth(deltaText);
	}

	private static Font deltaFont(Font smallFont)
	{
		return smallFont.deriveFont(Font.BOLD, smallFont.getSize2D() * 1.15f);
	}

	private static void drawBarText(Graphics2D g, String s, int w, int barTop, int barH, Color color)
	{
		Font old = g.getFont();
		g.setFont(BAR_TEXT_FONT);
		FontMetrics fm = g.getFontMetrics();
		int tx = (w - fm.stringWidth(s)) / 2;
		int ty = barTop + (barH + fm.getAscent() - fm.getDescent()) / 2;
		g.setColor(color);
		g.drawString(s, tx, ty);
		g.setFont(old);
	}

	private static void drawSkull(Graphics2D g, int x, int baselineY, int size, Color color)
	{
		double top = baselineY - size;
		g.setColor(color);
		g.fill(new Ellipse2D.Double(x, top, size, size * 0.78));
		g.fill(new RoundRectangle2D.Double(x + size * 0.22, top + size * 0.58, size * 0.56, size * 0.42, 2, 2));
		g.setColor(EYE);
		double eyeR = size * 0.15;
		double eyeY = top + size * 0.26;
		g.fill(new Ellipse2D.Double(x + size * 0.18, eyeY, eyeR * 2, eyeR * 2));
		g.fill(new Ellipse2D.Double(x + size * 0.82 - eyeR * 2, eyeY, eyeR * 2, eyeR * 2));
		g.fillRect((int) Math.round(x + size * 0.5 - 0.5), (int) Math.round(top + size * 0.78), 1,
			(int) Math.max(1, Math.round(size * 0.2)));
	}

	private static void drawChart(Graphics2D g, MarketGraph graph, MarketGraph secondLine, Color color,
		Color secondColor, int left, int top, int right, int bottom, FontMetrics smallFm, long nowTick)
	{
		g.setStroke(GRID_STROKE);
		int labelGap = smallFm.getAscent() + 1;
		int chartH = bottom - top;
		for (int pct : new int[]{0, 50, 100})
		{
			int gy = yFor(pct, top, bottom);
			g.setColor(GRID);
			g.drawLine(left, gy, right, gy);
			boolean fits = pct == 100 || (pct == 0 ? chartH >= labelGap : chartH / 2 >= labelGap);
			if (fits)
			{
				text(g, pct + "%", right + 4, gy + smallFm.getAscent() / 2 - 1, MUTED);
			}
		}

		if (secondLine != null)
		{
			drawLine(g, secondLine, secondColor, left, top, right, bottom, nowTick);
		}
		drawLine(g, graph, color, left, top, right, bottom, nowTick);
	}

	private static void drawLine(Graphics2D g, MarketGraph graph, Color color, int left, int top, int right,
		int bottom, long nowTick)
	{
		int n = graph.size();
		if (n < 2)
		{
			return;
		}

		double[] shown = rigid(graph);
		Path2D.Double line = new Path2D.Double();
		double lastX = right;
		double lastY = bottom;
		for (int i = 0; i < n; i++)
		{
			double px = left + (double) (right - left) * i / (n - 1);
			double py = top + (bottom - top) * (1 - shown[i] / 100.0);
			if (i == 0)
			{
				line.moveTo(px, py);
			}
			else
			{
				line.lineTo(px, py);
			}
			lastX = px;
			lastY = py;
		}

		Path2D.Double area = new Path2D.Double(line);
		area.lineTo(right, bottom);
		area.lineTo(left, bottom);
		area.closePath();
		g.setPaint(new GradientPaint(0, top, withAlpha(color, 70), 0, bottom, withAlpha(color, 0)));
		g.fill(area);

		g.setStroke(LINE_STROKE);
		g.setColor(color);
		g.draw(line);

		double pulse = (nowTick % PULSE_PERIOD_TICKS) / (double) PULSE_PERIOD_TICKS;
		double haloR = 3 + 5 * pulse;
		g.setColor(withAlpha(color, (int) (130 * (1 - pulse))));
		g.fill(new Ellipse2D.Double(lastX - haloR, lastY - haloR, haloR * 2, haloR * 2));
		g.setColor(color);
		g.fill(new Ellipse2D.Double(lastX - 2.5, lastY - 2.5, 5, 5));
	}

	/**
	 * The values as drawn: the line stays put until the odds have moved RIGID_STEP points, then jumps to the new level,
	 * which gives flat holds and sharp turns instead of a smooth curve. The newest point is always the real value, so the
	 * dot matches the big number.
	 */
	static double[] rigid(MarketGraph graph)
	{
		int n = graph.size();
		double[] shown = new double[n];
		double level = n == 0 ? 0 : graph.get(0);
		for (int i = 0; i < n; i++)
		{
			double v = graph.get(i);
			if (Math.abs(v - level) >= RIGID_STEP)
			{
				level = v;
			}
			shown[i] = level;
		}
		if (n > 0)
		{
			shown[n - 1] = graph.get(n - 1);
		}
		return shown;
	}

	private static int yFor(int pct, int top, int bottom)
	{
		return (int) Math.round(top + (bottom - top) * (1 - pct / 100.0));
	}

	private static void text(Graphics2D g, String s, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(s, x + 1, y + 1);
		g.setColor(color);
		g.drawString(s, x, y);
	}

	private static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
	}

	static String formatPop(long v)
	{
		if (v >= 1_000_000_000L)
		{
			return String.format(Locale.US, "$%.1fB", v / 1e9);
		}
		if (v >= 1_000_000L)
		{
			return String.format(Locale.US, "$%.1fM", v / 1e6);
		}
		return String.format(Locale.US, "$%dK", v / 1000);
	}

	static String formatVolume(long v)
	{
		if (v >= 1_000_000_000L)
		{
			return String.format(Locale.US, "$%.1fB Vol.", v / 1e9);
		}
		if (v >= 1_000_000L)
		{
			return String.format(Locale.US, "$%.1fM Vol.", v / 1e6);
		}
		return String.format(Locale.US, "$%,dK Vol.", v / 1000);
	}

	static List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines)
	{
		return wrap(text, fm::stringWidth, maxWidth, maxLines);
	}

	static List<String> wrap(String text, ToIntFunction<String> width, int maxWidth, int maxLines)
	{
		return wrap(text, width, maxWidth, maxWidth, maxLines);
	}

	/** Wraps into lines where the first may be narrower than the rest (room left for a badge beside it). */
	static List<String> wrap(String text, ToIntFunction<String> width, int firstMax, int restMax, int maxLines)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++)
		{
			String candidate = current.length() == 0 ? words[i] : current + " " + words[i];
			int maxWidth = lines.isEmpty() ? firstMax : restMax;
			if (width.applyAsInt(candidate) <= maxWidth || current.length() == 0)
			{
				current.setLength(0);
				current.append(candidate);
				continue;
			}
			if (lines.size() == maxLines - 1)
			{
				lines.add(ellipsize(candidate, width, maxWidth));
				return lines;
			}
			lines.add(current.toString());
			current.setLength(0);
			current.append(words[i]);
		}
		if (current.length() > 0)
		{
			lines.add(lines.size() == maxLines - 1
				? ellipsize(current.toString(), width, lines.isEmpty() ? firstMax : restMax) : current.toString());
		}
		return lines;
	}

	private static String ellipsize(String s, ToIntFunction<String> width, int maxWidth)
	{
		if (width.applyAsInt(s) <= maxWidth)
		{
			return s;
		}
		String out = s;
		while (out.length() > 1 && width.applyAsInt(out + "...") > maxWidth)
		{
			out = out.substring(0, out.length() - 1);
		}
		return out.trim() + "...";
	}
}
