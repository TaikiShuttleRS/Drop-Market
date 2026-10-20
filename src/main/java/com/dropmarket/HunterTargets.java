package com.dropmarket;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Set;

/**
 * What a Hunter action was aimed at. Hunter has four pets and each comes from one specific activity, so only those
 * activities get a market and a counter: chinchompas (Baby chinchompa), wyrmscraig goats (Mr McGroot), the herbiboar
 * (Herbi, handled by the plugin from its Hunter + Herblore XP) and expert or master Hunters' loot sacks (Quetzin).
 * Any other Hunter XP, such as birds or kebbits, isn't a pet roll and is ignored.
 */
final class HunterTargets
{
	static final String CHINCHOMPAS = "Chinchompas";
	static final String GOATS = "Wyrmscraig goats";
	static final String RUMOURS = "Hunter rumours";

	// Caught chinchompas show up in the inventory; these are their item names.
	private static final Set<String> CHINCHOMPA_ITEMS = ImmutableSet.of(
		"chinchompa", "red chinchompa", "black chinchompa", "carnivorous chinchompa");
	// Hunting a wyrmscraig goat drops these.
	private static final Set<String> GOAT_ITEMS = ImmutableSet.of(
		"goat horn", "wyrmscraig goat fur", "wyrmscraig goat hoof");

	private HunterTargets()
	{
	}

	/** The market an item gained while hunting points to, or null if it isn't from one of the pet activities. */
	static String marketForGainedItem(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}
		String name = itemName.trim().toLowerCase(Locale.ROOT);
		if (CHINCHOMPA_ITEMS.contains(name))
		{
			return CHINCHOMPAS;
		}
		if (GOAT_ITEMS.contains(name))
		{
			return GOATS;
		}
		return null;
	}

	/** An opened expert or master Hunters' loot sack (the ones that can give Quetzin); novice and adept sacks can't. */
	static boolean isPetRollSack(String lootName)
	{
		if (lootName == null)
		{
			return false;
		}
		String name = lootName.toLowerCase(Locale.ROOT);
		return name.contains("loot sack") && (name.contains("(expert)") || name.contains("(master)"));
	}
}
