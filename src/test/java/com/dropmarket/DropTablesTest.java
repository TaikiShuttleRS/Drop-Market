package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DropTablesTest
{
	@Test
	public void normalizeStripsCaseParentheticalsAndPunctuation()
	{
		assertEquals("scythe of vitur", DropTables.normalize("Scythe of vitur (uncharged)"));
		assertEquals("kril tsutsaroth", DropTables.normalize("K'ril Tsutsaroth"));
		assertEquals("ba ba", DropTables.normalize("Ba-Ba"));
	}

	@Test
	public void raidSubBossesAndAliasesMapToRaid()
	{
		assertEquals(DropTables.CHAMBERS_OF_XERIC, DropTables.canonicalSource("Great Olm"));
		assertEquals(DropTables.CHAMBERS_OF_XERIC, DropTables.canonicalSource("CoX"));
		assertEquals(DropTables.THEATRE_OF_BLOOD, DropTables.canonicalSource("Verzik Vitur"));
		assertEquals(DropTables.TOMBS_OF_AMASCUT, DropTables.canonicalSource("Tumeken's Warden"));
		assertEquals("Barrows", DropTables.canonicalSource("Dharok the Wretched"));
		assertEquals("Zulrah", DropTables.canonicalSource("Zulrah"));
		assertEquals("Goblin", DropTables.canonicalSource("Goblin"));
	}

	@Test
	public void findPetLocatesThePetRegardlessOfPosition()
	{
		assertEquals("Beaver", DropTables.findPet(DropTables.lookup("Woodcutting")));
		assertEquals("Olmlet", DropTables.findPet(DropTables.lookup(DropTables.CHAMBERS_OF_XERIC)));
		assertNull(DropTables.findPet(DropTables.lookup("Abyssal demon")));
	}

	@Test
	public void lookupIsCaseInsensitive()
	{
		assertNotNull(DropTables.lookup("zulrah"));
		assertNotNull(DropTables.lookup("Kree'arra"));
		assertNull(DropTables.lookup("Chicken"));
	}

	@Test
	public void parsePrioritiesResolvesAliasesAndSeparators()
	{
		Map<String, List<String>> p = DropTables.parsePriorities(
			"CoX: Dexterous prayer scroll\n"
				+ "Zulrah = Serpentine visage\r\n"
				+ "\n"
				+ "garbage line\n"
				+ "tob:   Avernic defender hilt  ");
		assertEquals(List.of("Dexterous prayer scroll"), p.get(DropTables.normalize(DropTables.CHAMBERS_OF_XERIC)));
		assertEquals(List.of("Serpentine visage"), p.get("zulrah"));
		assertEquals(List.of("Avernic defender hilt"), p.get(DropTables.normalize(DropTables.THEATRE_OF_BLOOD)));
		assertEquals(3, p.size());
	}

	@Test
	public void parsePrioritiesKeepsEveryLineForTheSameSourceToRollBetween()
	{
		Map<String, List<String>> p = DropTables.parsePriorities(
			"Zulrah: Serpentine visage\n"
				+ "Zulrah: Tanzanite mutagen\n"
				+ "Zulrah: Serpentine visage");
		assertEquals(List.of("Serpentine visage", "Tanzanite mutagen", "Serpentine visage"), p.get("zulrah"));
	}

	@Test
	public void chestBossesAndWavesRollAgainstTheChest()
	{
		assertEquals(DropTables.CORRUPTED_GAUNTLET, DropTables.canonicalSource("Corrupted Hunllef"));
		assertEquals(DropTables.THE_GAUNTLET, DropTables.canonicalSource("Crystalline Hunllef"));
		assertEquals("Moons of Peril", DropTables.canonicalSource("Blood Moon"));
		assertEquals("The Inferno", DropTables.canonicalSource("Jal-Nib"));
		assertEquals("TzHaar Fight Cave", DropTables.canonicalSource("TzTok-Jad"));
		assertEquals("Fortis Colosseum", DropTables.canonicalSource("Sol Heredit"));
		assertEquals("Barbarian Assault", DropTables.canonicalSource("Penance Queen"));
		assertEquals("Revenants", DropTables.canonicalSource("Revenant dragon"));
		assertEquals("Minotaur", DropTables.canonicalSource("Minotaur"));
	}

	@Test
	public void theFracturedArchiveIsARaidLikeTheOthers()
	{
		java.util.List<String> table = DropTables.lookup(DropTables.FRACTURED_ARCHIVE);
		assertEquals("Obligator (uncharged)", table.get(0));
		assertTrue(table.contains("Zorya's tome (uncharged)"));
		assertTrue(table.contains("Fragment of burning fire"));
		// The pet is last, like Olmlet and Lil' Zik.
		assertEquals("Orion", table.get(table.size() - 1));
		assertTrue(DropTables.isPet("Orion"));
		// Every spelling of the raid, and each of its bosses, opens the same market.
		for (String name : new String[]{"The Fractured Archive", "fractured archives", "FA", "TFA", "Fred", "Moia", "Xarth",
			"Ocellus", "Ezekiel", "Bork", "The Smorks", "Dagon'hai Elder", "Dreadnoth the Jellied", "The Archive Guardian"})
		{
			assertEquals(name, DropTables.FRACTURED_ARCHIVE, DropTables.canonicalSource(name));
		}
		assertEquals("TFA", DropTables.abbreviation(DropTables.FRACTURED_ARCHIVE));
		// The Obligator is the mega-rare, so it is the market's fixed headline item; other raids pick by value.
		assertEquals("Obligator (uncharged)", DropTables.featuredItem("Fractured Archive"));
		assertEquals("Obligator (uncharged)", DropTables.featuredItem("The Fractured Archive"));
		assertNull(DropTables.featuredItem(DropTables.THEATRE_OF_BLOOD));
		assertNull(DropTables.featuredItem(null));
		// The Obligator is an item, not a boss.
		assertEquals("Obligator", DropTables.canonicalSource("Obligator"));
		// A kill count comes from the reward chest, whichever way its name is written, never from a boss kill.
		assertTrue(DropTables.isCompletion(DropTables.FRACTURED_ARCHIVE, "Fractured Archive"));
		assertTrue(DropTables.isCompletion(DropTables.FRACTURED_ARCHIVE, "The Fractured Archive"));
		assertTrue(DropTables.isCompletion(DropTables.FRACTURED_ARCHIVE, "Fractured Archives"));
		assertFalse(DropTables.isCompletion(DropTables.FRACTURED_ARCHIVE, "Moia"));
		assertFalse(DropTables.isCompletion(DropTables.FRACTURED_ARCHIVE, "Ocellus"));
		// The older raids count completions exactly as before.
		assertTrue(DropTables.isCompletion(DropTables.THEATRE_OF_BLOOD, "Theatre of Blood"));
		assertFalse(DropTables.isCompletion(DropTables.THEATRE_OF_BLOOD, "Verzik Vitur"));
		assertTrue(DropTables.isCompletion(DropTables.TOMBS_OF_AMASCUT, "Tombs of Amascut"));
		assertFalse(DropTables.isCompletion(DropTables.CHAMBERS_OF_XERIC, "Tekton"));
	}

	@Test
	public void petFirstTablesLeadWithAPet()
	{
		String[] sources = {"Mining", "Woodcutting", "Fishing", "Agility", "Thieving", "Farming", "Runecraft",
			"Sailing", DropTables.HERBIBOAR, HunterTargets.CHINCHOMPAS, HunterTargets.GOATS, HunterTargets.RUMOURS, "Chompy bird", "Zalcano", DropTables.WINTERTODT,
			DropTables.TEMPOROSS, DropTables.GUARDIANS_OF_THE_RIFT};
		for (String source : sources)
		{
			assertTrue(source, DropTables.isPetFirst(source));
			assertTrue(source, DropTables.isPet(DropTables.lookup(source).get(0)));
		}
		assertFalse(DropTables.isPetFirst("Zulrah"));
	}

	@Test
	public void recognisesPetsLooselyAndNotRegularItems()
	{
		assertTrue(DropTables.isPet("Rock golem"));
		assertTrue(DropTables.isPet("Gull (pet)"));
		assertTrue(DropTables.isPet("Lil' Zik"));
		assertTrue(DropTables.isPet("Vet'ion Jr."));
		assertTrue(DropTables.isPet("Muphin"));
		assertFalse(DropTables.isPet("Twisted bow"));
	}

	@Test
	public void championScrollsAreAttachedToTheirMonsters()
	{
		assertTrue(DropTables.lookup("Goblin").contains("Goblin champion scroll"));
		assertTrue(DropTables.lookup("Zombie pirate").contains("Zombie champion scroll"));
		assertTrue(DropTables.lookup("Skogre").contains("Skeleton champion scroll"));
		assertTrue(DropTables.lookup("Skogre").contains("Zombie champion scroll"));
		List<String> vetion = DropTables.lookup("Vet'ion");
		assertEquals("Ring of the gods", vetion.get(0));
		assertTrue(vetion.contains("Skeleton champion scroll"));
		assertNull("Warriors' Guild cyclopes don't drop it", DropTables.lookup("Cyclops"));
		assertNull("GWD bodyguards stay out", DropTables.lookup("Sergeant Strongstack"));
	}

	@Test
	public void clueHelpers()
	{
		assertTrue(DropTables.isClueItem("Clue scroll (elite)"));
		assertTrue(DropTables.isClueItem("Clue bottle (hard)"));
		assertTrue(DropTables.isClueItem("Scroll box (master)"));
		assertFalse(DropTables.isClueItem("Goblin champion scroll"));
		assertTrue(DropTables.isTrailItem("Clue scroll (hard)"));
		assertTrue(DropTables.isTrailItem("Reward casket (elite)"));
		assertTrue(DropTables.isTrailItem("Challenge scroll (medium)"));
		assertFalse("unopened bottles aren't progress", DropTables.isTrailItem("Clue bottle (hard)"));
		assertEquals(DropTables.TREASURE_TRAILS, DropTables.canonicalSource("Clues"));
	}

	@Test
	public void matchItemAcceptsLooseNames()
	{
		assertEquals("Dexterous prayer scroll",
			DropTables.matchItem("dex scroll", DropTables.lookup(DropTables.CHAMBERS_OF_XERIC)));
		assertEquals("Serpentine visage", DropTables.matchItem("serpentine visage", DropTables.lookup("Zulrah")));
		assertEquals("Scythe of vitur (uncharged)",
			DropTables.matchItem("Scythe of Vitur", DropTables.lookup(DropTables.THEATRE_OF_BLOOD)));
		assertEquals("Golden tench", DropTables.matchItem("Golden tench", DropTables.lookup("Zulrah")));
		assertEquals("Bronze dagger", DropTables.matchItem("Bronze dagger", null));
	}

	@Test
	public void displayNameDropsSuffix()
	{
		assertEquals("Tumeken's shadow", DropTables.displayName("Tumeken's shadow (uncharged)"));
		assertEquals("Twisted bow", DropTables.displayName("Twisted bow"));
	}

	@Test
	public void theSireOnlyHasTheUnsiredAndTheUnsiredHasItsRewards()
	{
		assertEquals(java.util.Arrays.asList("Unsired"), DropTables.lookup("Abyssal Sire"));
		assertTrue(DropTables.lookup("Unsired").containsAll(java.util.Arrays.asList("Bludgeon claw", "Bludgeon spine",
			"Bludgeon axon", "Abyssal dagger", "Abyssal whip", "Jar of miasma", "Abyssal head", "Abyssal orphan")));
		assertTrue(DropTables.isAlwaysCelebrated("Unsired"));
	}
}
