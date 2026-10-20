package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HunterTargetsTest
{
	@Test
	public void chinchompasAndGoatDropsPointToTheirMarkets()
	{
		for (String item : new String[]{"Chinchompa", "Red chinchompa", "Black chinchompa", "Carnivorous chinchompa", "  RED CHINCHOMPA "})
		{
			assertEquals(item, HunterTargets.CHINCHOMPAS, HunterTargets.marketForGainedItem(item));
		}
		for (String item : new String[]{"Goat horn", "Wyrmscraig goat fur", "Wyrmscraig goat hoof"})
		{
			assertEquals(item, HunterTargets.GOATS, HunterTargets.marketForGainedItem(item));
		}
	}

	@Test
	public void everythingElseHunterRelatedIsNotAPetRoll()
	{
		// Birds, kebbits, implings, bait and the pet itself don't start a pet market.
		for (String item : new String[]{"Raw bird meat", "Bones", "Bird snare", "Box trap", "Baby chinchompa", "Chinchompa tuft",
			"Kebbit spike", "Coins", "", null})
		{
			assertNull(item, HunterTargets.marketForGainedItem(item));
		}
	}

	@Test
	public void onlyExpertAndMasterSacksRollForQuetzin()
	{
		assertTrue(HunterTargets.isPetRollSack("Hunters' loot sack (expert)"));
		assertTrue(HunterTargets.isPetRollSack("Hunters' loot sack (Master)"));
		assertFalse(HunterTargets.isPetRollSack("Hunters' loot sack (basic)"));
		assertFalse(HunterTargets.isPetRollSack("Hunters' loot sack (adept)"));
		assertFalse(HunterTargets.isPetRollSack("Hallowed sack"));
		assertFalse(HunterTargets.isPetRollSack(null));
	}

	@Test
	public void theFourHunterPetsEachHaveAMarketThatLeadsWithThem()
	{
		assertEquals("Baby chinchompa", DropTables.lookup(HunterTargets.CHINCHOMPAS).get(0));
		assertEquals("Mr McGroot", DropTables.lookup(HunterTargets.GOATS).get(0));
		assertEquals("Quetzin", DropTables.lookup(HunterTargets.RUMOURS).get(0));
		assertEquals("Herbi", DropTables.lookup(DropTables.HERBIBOAR).get(0));
		for (String market : new String[]{HunterTargets.CHINCHOMPAS, HunterTargets.GOATS, HunterTargets.RUMOURS})
		{
			assertTrue(market, DropTables.isPetFirst(market));
			assertTrue(market, DropTables.isPet(DropTables.lookup(market).get(0)));
		}
		// The old catch-all Hunter market is gone: ordinary Hunter XP no longer bets on the Baby chinchompa.
		assertNull(DropTables.lookup("Hunter"));
		assertEquals(HunterTargets.RUMOURS, DropTables.canonicalSource("Hunters' loot sack (expert)"));
	}

	@Test
	public void counterWordsDescribeWhatIsActuallyCounted()
	{
		assertEquals("rocks", DropMarketPlugin.skillUnit("Mining"));
		assertEquals("logs", DropMarketPlugin.skillUnit("Woodcutting"));
		assertEquals("catches", DropMarketPlugin.skillUnit("Fishing"));
		assertEquals("crafts", DropMarketPlugin.skillUnit("Runecraft"));
		// One XP drop is an obstacle or an action, not a lap or a voyage.
		assertEquals("actions", DropMarketPlugin.skillUnit("Agility"));
		assertEquals("actions", DropMarketPlugin.skillUnit("Sailing"));
		assertEquals("catches", DropMarketPlugin.skillUnit(HunterTargets.CHINCHOMPAS));
		assertEquals("hunts", DropMarketPlugin.skillUnit(HunterTargets.GOATS));
		assertEquals("sacks", DropMarketPlugin.skillUnit(HunterTargets.RUMOURS));
		assertNull(DropMarketPlugin.skillUnit("Hunter"));
	}
}
