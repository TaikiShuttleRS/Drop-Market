package com.dropmarket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Item order is the tiebreak when nothing has a GE price (e.g. pets), so the most iconic item comes first.
final class DropTables
{
	static final String CHAMBERS_OF_XERIC = "Chambers of Xeric";
	static final String THEATRE_OF_BLOOD = "Theatre of Blood";
	static final String TOMBS_OF_AMASCUT = "Tombs of Amascut";
	// The fourth raid, released 20 October 2026.
	static final String FRACTURED_ARCHIVE = "Fractured Archive";
	static final String THE_GAUNTLET = "The Gauntlet";
	static final String CORRUPTED_GAUNTLET = "The Corrupted Gauntlet";
	static final String WINTERTODT = "Wintertodt";
	static final String TEMPOROSS = "Tempoross";
	static final String GUARDIANS_OF_THE_RIFT = "Guardians of the Rift";
	static final String HERBIBOAR = "Herbiboar";
	static final String TREASURE_TRAILS = "Treasure Trails";
	static final String UNSIRED = "Unsired";

	// Raids only pay out their unique drops from the reward chest at the very end, so room/sub-boss kills
	// (aliased to the raid name for market display) shouldn't bump the raid's completion count.
	private static final Set<String> RAIDS = new HashSet<>(
		Arrays.asList(CHAMBERS_OF_XERIC, THEATRE_OF_BLOOD, TOMBS_OF_AMASCUT, FRACTURED_ARCHIVE));

	private static final Map<String, List<String>> TABLES = new HashMap<>();
	private static final Map<String, String> ALIASES = new HashMap<>();
	private static final Set<String> PET_FIRST = new HashSet<>();
	private static final Set<String> PETS = new HashSet<>();

	static
	{
		table(CHAMBERS_OF_XERIC, "Twisted bow", "Kodai insignia", "Elder maul", "Ancestral robe top",
			"Ancestral robe bottom", "Ancestral hat", "Dragon claws", "Dragon hunter crossbow", "Twisted buckler",
			"Dinh's bulwark", "Dexterous prayer scroll", "Arcane prayer scroll", "Twisted ancestral colour kit",
			"Metamorphic dust", "Olmlet");
		alias(CHAMBERS_OF_XERIC, "cox", "cm", "cox cm", "chambers", "raids", "raids 1", "raids1",
			"chambers of xeric challenge mode", "great olm", "tekton", "vasa nistirio", "vespula", "muttadile",
			"vanguard");

		table(THEATRE_OF_BLOOD, "Scythe of vitur (uncharged)", "Ghrazi rapier", "Sanguinesti staff (uncharged)",
			"Justiciar faceguard", "Justiciar chestguard", "Justiciar legguards", "Avernic defender hilt",
			"Sanguine dust", "Holy ornament kit", "Sanguine ornament kit", "Lil' zik");
		alias(THEATRE_OF_BLOOD, "tob", "hmt", "theatre", "raids 2", "raids2", "theatre of blood hard mode",
			"theatre of blood entry mode", "the maiden of sugadinti", "pestilent bloat", "nylocas vasilias",
			"sotetseg", "xarpus", "verzik vitur");

		table(TOMBS_OF_AMASCUT, "Tumeken's shadow (uncharged)", "Masori body", "Masori chaps", "Masori mask",
			"Lightbearer", "Osmumten's fang", "Elidinis' ward", "Masori crafting kit", "Menaphite ornament kit",
			"Cursed phalanx", "Tumeken's guardian");
		alias(TOMBS_OF_AMASCUT, "toa", "tombs", "raids 3", "raids3", "tombs of amascut expert mode",
			"tombs of amascut entry mode", "kephri", "akkha", "ba-ba", "zebak", "tumeken's warden",
			"elidinis' warden");

		// Day-one list from the wiki's Rewards section (the raid's drop rates and its full 17-item collection log aren't
		// published in structured form yet; the monthly data refresh adds whatever else the wiki records). Obligator is
		// the Crush mega-rare and Orion the pet.
		table(FRACTURED_ARCHIVE, "Obligator (uncharged)", "Zorya's tome (uncharged)", "Ascension crossbows", "Rondache",
			"Horn of Bran", "Archivist's folio (inert)", "Fragment of whispering wind", "Fragment of deep water",
			"Fragment of living earth", "Fragment of burning fire", "Orion");
		// The raid's eight bosses (five are fought per run) alias to it so fighting any of them opens its market.
		alias(FRACTURED_ARCHIVE, "the fractured archive", "fractured archives", "the fractured archives", "fa", "tfa",
			"fred", "raids 4", "raids4", "the archive guardian", "archive guardian", "ezekiel", "bork", "moia",
			"dreadnoth the jellied", "xarth", "the obligator", "obligator", "ocellus");

		table("Nex", "Zaryte vambraces", "Torva platebody (damaged)", "Torva platelegs (damaged)",
			"Torva full helm (damaged)", "Nihil horn", "Ancient hilt", "Nexling");
		table("The Nightmare", "Inquisitor's mace", "Nightmare staff", "Inquisitor's hauberk",
			"Inquisitor's plateskirt", "Inquisitor's great helm", "Eldritch orb", "Harmonised orb", "Volatile orb",
			"Jar of dreams", "Little nightmare");
		alias("The Nightmare", "nightmare");
		table("Phosani's Nightmare", "Inquisitor's mace", "Nightmare staff", "Inquisitor's hauberk",
			"Inquisitor's plateskirt", "Inquisitor's great helm", "Eldritch orb", "Harmonised orb", "Volatile orb",
			"Parasitic egg", "Jar of dreams", "Little nightmare");
		alias("Phosani's Nightmare", "phosani");

		table("General Graardor", "Bandos chestplate", "Bandos tassets", "Bandos boots", "Bandos hilt",
			"Pet general graardor");
		table("Kree'arra", "Armadyl chestplate", "Armadyl chainskirt", "Armadyl helmet", "Armadyl hilt",
			"Pet kree'arra");
		table("Commander Zilyana", "Armadyl crossbow", "Saradomin hilt", "Saradomin sword", "Saradomin's light",
			"Pet zilyana");
		table("K'ril Tsutsaroth", "Zamorakian spear", "Staff of the dead", "Steam battlestaff", "Zamorak hilt",
			"Pet k'ril tsutsaroth");
		table("Corporeal Beast", "Elysian sigil", "Spectral sigil", "Arcane sigil", "Spirit shield", "Holy elixir",
			"Jar of spirits", "Pet dark core");
		alias("Corporeal Beast", "corp");

		table("Zulrah", "Tanzanite fang", "Magic fang", "Serpentine visage", "Uncut onyx", "Tanzanite mutagen",
			"Magma mutagen", "Jar of swamp", "Pet snakeling");
		table("Vorkath", "Dragonbone necklace", "Draconic visage", "Skeletal visage", "Vorkath's head",
			"Jar of decay", "Vorki");
		table("Alchemical Hydra", "Hydra's claw", "Hydra leather", "Hydra tail", "Hydra's eye", "Hydra's fang",
			"Hydra's heart", "Dragon thrownaxe", "Dragon knife", "Jar of chemicals", "Ikkle hydra");
		table("Cerberus", "Primordial crystal", "Pegasian crystal", "Eternal crystal", "Smouldering stone",
			"Jar of souls", "Hellpuppy");
		table("Kraken", "Trident of the seas (full)", "Kraken tentacle", "Jar of dirt", "Pet kraken");
		table("Thermonuclear smoke devil", "Occult necklace", "Smoke battlestaff", "Dragon chainbody",
			"Jar of smoke", "Pet smoke devil");
		alias("Thermonuclear smoke devil", "thermy");
		// Every Sire unique comes out of the Unsired, so the Sire itself only has the Unsired to chase; what the Unsired turns
		// into is its own table, shown at the Font of Consumption.
		table("Abyssal Sire", "Unsired");
		table(UNSIRED, "Bludgeon claw", "Bludgeon spine", "Bludgeon axon", "Abyssal dagger", "Abyssal whip", "Jar of miasma",
			"Abyssal head", "Abyssal orphan");
		table("Grotesque Guardians", "Black tourmaline core", "Granite maul", "Granite gloves", "Granite ring",
			"Granite hammer", "Jar of stone", "Noon");
		alias("Grotesque Guardians", "dusk", "dawn", "grotesque guardians");
		table("Kalphite Queen", "Dragon chainbody", "Dragon 2h sword", "Jar of sand", "Kalphite princess");
		table("King Black Dragon", "Draconic visage", "Dragon pickaxe", "Prince black dragon");
		table("Dagannoth Rex", "Berserker ring", "Warrior ring", "Dragon axe", "Pet dagannoth rex");
		table("Dagannoth Prime", "Seers ring", "Mud battlestaff", "Dragon axe", "Pet dagannoth prime");
		table("Dagannoth Supreme", "Archers ring", "Seercull", "Dragon axe", "Pet dagannoth supreme");
		table("Giant Mole", "Mole claw", "Mole skin", "Baby mole");
		table("Sarachnis", "Sarachnis cudgel", "Jar of eyes", "Sraracha");
		table("Phantom Muspah", "Venator shard", "Ancient icon", "Muphin");
		table("Scurrius", "Scurrius' spine", "Scurry");

		table("Callisto", "Tyrannical ring", "Voidwaker hilt", "Claws of callisto", "Dragon pickaxe",
			"Callisto cub");
		table("Artio", "Tyrannical ring", "Voidwaker hilt", "Claws of callisto", "Dragon pickaxe", "Callisto cub");
		table("Vet'ion", "Ring of the gods", "Voidwaker blade", "Skull of vet'ion", "Dragon pickaxe",
			"Vet'ion jr.");
		table("Calvar'ion", "Ring of the gods", "Voidwaker blade", "Skull of vet'ion", "Dragon pickaxe",
			"Vet'ion jr.");
		table("Venenatis", "Treasonous ring", "Voidwaker gem", "Fangs of venenatis", "Dragon pickaxe",
			"Venenatis spiderling");
		table("Spindel", "Treasonous ring", "Voidwaker gem", "Fangs of venenatis", "Dragon pickaxe",
			"Venenatis spiderling");
		table("Chaos Elemental", "Dragon pickaxe", "Dragon 2h sword", "Pet chaos elemental");
		table("Scorpia", "Odium shard 3", "Malediction shard 3", "Scorpia's offspring");

		table("Duke Sucellus", "Magus vestige", "Eye of the duke", "Virtus robe top", "Virtus robe bottom",
			"Virtus mask", "Chromium ingot", "Baron");
		table("The Leviathan", "Venator vestige", "Leviathan's lure", "Virtus robe top", "Virtus robe bottom",
			"Virtus mask", "Chromium ingot", "Lil'viathan");
		alias("The Leviathan", "leviathan");
		table("Vardorvis", "Ultor vestige", "Executioner's axe head", "Virtus robe top", "Virtus robe bottom",
			"Virtus mask", "Chromium ingot", "Butch");
		table("The Whisperer", "Bellator vestige", "Siren's staff", "Virtus robe top", "Virtus robe bottom",
			"Virtus mask", "Chromium ingot", "Wisp");
		alias("The Whisperer", "whisperer");
		table("Araxxor", "Araxyte fang", "Noxious pommel", "Noxious point", "Noxious blade", "Araxyte head",
			"Jar of venom", "Nid");
		table("The Hueycoatl", "Dragon hunter wand", "Tome of earth (empty)", "Hueycoatl hide", "Huberte");
		alias("The Hueycoatl", "hueycoatl");
		table("Yama", "Soulflame horn", "Oathplate chest", "Oathplate legs", "Oathplate helm", "Yami");
		table("Doom of Mokhaiotl", "Eye of ayak (uncharged)", "Avernic treads", "Mokhaiotl cloth", "Dom");
		table("Royal Titans", "Deadeye prayer scroll", "Mystic vigour prayer scroll", "Fire element staff crown",
			"Ice element staff crown", "Bran");
		alias("Royal Titans", "branda the fire queen", "eldric the ice king");

		table("Barrows", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's hood", "Ahrim's staff", "Dharok's helm",
			"Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe", "Guthan's helm", "Guthan's platebody",
			"Guthan's chainskirt", "Guthan's warspear", "Karil's coif", "Karil's leathertop",
			"Karil's leatherskirt", "Karil's crossbow", "Torag's helm", "Torag's platebody", "Torag's platelegs",
			"Torag's hammers", "Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
		alias("Barrows", "ahrim the blighted", "dharok the wretched", "guthan the infested", "karil the tainted",
			"torag the corrupted", "verac the defiled");
		// Chest-reward bosses: the boss and its waves/prep monsters all roll against the chest's table.
		table(THE_GAUNTLET, "Enhanced crystal weapon seed", "Crystal armour seed", "Crystal weapon seed",
			"Youngllef");
		alias(THE_GAUNTLET, "gauntlet", "crystalline hunllef");
		table(CORRUPTED_GAUNTLET, "Enhanced crystal weapon seed", "Crystal armour seed",
			"Crystal weapon seed", "Youngllef");
		alias(CORRUPTED_GAUNTLET, "corrupted gauntlet", "cg", "corrupted hunllef");
		table("Fortis Colosseum", "Tonalztics of ralos (uncharged)", "Echo crystal", "Sunfire fanatic cuirass",
			"Sunfire fanatic chausses", "Sunfire fanatic helm", "Smol heredit");
		alias("Fortis Colosseum", "colosseum", "sol heredit", "fremennik warband archer",
			"fremennik warband seer", "fremennik warband berserker", "serpent shaman", "javelin colossus",
			"jaguar warrior", "manticore", "shockwave colossus");
		table("TzHaar Fight Cave", "Fire cape", "Tzrek-jad");
		alias("TzHaar Fight Cave", "jad", "fight caves", "fight cave", "tztok-jad", "tz-kih", "tz-kek",
			"tok-xil", "yt-mejkot", "ket-zek", "yt-hurkot");
		table("The Inferno", "Infernal cape", "Jal-nib-rek");
		alias("The Inferno", "zuk", "inferno", "tzkal-zuk", "jal-nib", "jal-mejrah", "jal-ak", "jal-akrek-mej",
			"jal-akrek-xil", "jal-akrek-ket", "jal-imkot", "jal-xil", "jal-zek", "jaltok-jad", "jal-mejjak");
		table("Moons of Peril", "Eclipse atlatl", "Dual macuahuitl", "Blue moon spear", "Eclipse moon chestplate",
			"Eclipse moon tassets", "Eclipse moon helm", "Blood moon chestplate", "Blood moon tassets",
			"Blood moon helm", "Blue moon chestplate", "Blue moon tassets", "Blue moon helm");
		alias("Moons of Peril", "moons", "lunar chest", "blood moon", "blue moon", "eclipse moon");
		table("Barbarian Assault", "Pet penance queen");
		alias("Barbarian Assault", "ba", "penance queen", "penance fighter", "penance ranger", "penance runner",
			"penance healer");
		table("Soul Wars", "Lil' creator");
		alias("Soul Wars", "avatar of creation", "avatar of destruction");

		// Skilling minigames and skilling bosses default to the pet rather than the priciest item.
		petFirst(WINTERTODT, "Phoenix", "Tome of fire (empty)", "Dragon axe", "Pyromancer garb",
			"Pyromancer robe", "Pyromancer hood", "Pyromancer boots", "Warm gloves", "Bruma torch");
		alias(WINTERTODT, "wt", "todt");
		petFirst(TEMPOROSS, "Tiny tempor", "Dragon harpoon", "Tome of water (empty)", "Big harpoonfish",
			"Fish barrel", "Tackle box", "Spirit angler headband", "Spirit angler top", "Spirit angler waders",
			"Spirit angler boots");
		petFirst(GUARDIANS_OF_THE_RIFT, "Abyssal protector", "Abyssal lantern", "Abyssal needle",
			"Abyssal red dye", "Abyssal green dye", "Abyssal blue dye", "Catalytic talisman");
		alias(GUARDIANS_OF_THE_RIFT, "gotr", "rifts", "great guardian");
		petFirst("Zalcano", "Smolcano", "Zalcano shard", "Crystal tool seed", "Uncut onyx");
		petFirst(HERBIBOAR, "Herbi");
		petFirst("Chompy bird", "Chompy chick");
		petFirst("Mining", "Rock golem");
		petFirst("Woodcutting", "Beaver");
		petFirst("Fishing", "Heron");
		petFirst("Agility", "Giant squirrel");
		petFirst("Thieving", "Rocky");
		petFirst("Farming", "Tangleroot");
		petFirst("Runecraft", "Rift guardian");
		alias("Runecraft", "runecrafting", "rc");
		// Hunter has four pets, each from one activity (see HunterTargets); other Hunter actions have no market.
		petFirst(HunterTargets.CHINCHOMPAS, "Baby chinchompa");
		petFirst(HunterTargets.GOATS, "Mr McGroot");
		petFirst(HunterTargets.RUMOURS, "Quetzin");
		alias(HunterTargets.RUMOURS, "hunters loot sack");
		petFirst("Sailing", "Soup");

		table("Brutus", "Cow slippers", "Bottomless milk bucket (empty)", "Beef");
		table("Shellbane Gryphon", "Belle's folly (tarnished)", "Jar of feathers", "Gull");
		table("Maggot King", "Elder venator fang", "Crimson kisten", "Maggot egg (writhing)", "Maggot marquess");
		table("Mad Angel", "Hallowfell", "Jar of light", "Aggy");
		table("Amoxliatl", "Glacial temotli", "Pendant of ates (inert)", "Moxi");
		table("Hespori", "Bottomless compost bucket", "Tangleroot");
		table("Skotizo", "Dark claw", "Uncut onyx", "Jar of darkness", "Skotos");
		table("Obor", "Hill giant club");
		table("Bryophyta", "Bryophyta's essence");
		table("Chaos Fanatic", "Odium shard 1", "Malediction shard 1", "Pet chaos elemental");
		table("Crazy archaeologist", "Odium shard 2", "Malediction shard 2", "Fedora");
		table("Revenants", "Craw's bow (u)", "Viggora's chainmace (u)", "Thammaron's sceptre (u)",
			"Amulet of avarice", "Ancient relic", "Ancient effigy", "Ancient medallion", "Ancient statuette");
		alias("Revenants", "revenant imp", "revenant goblin", "revenant pyrefiend", "revenant hobgoblin",
			"revenant cyclops", "revenant hellhound", "revenant demon", "revenant ork", "revenant dark beast",
			"revenant knight", "revenant dragon", "revenant maledictus");

		table("Abyssal demon", "Abyssal whip", "Abyssal dagger", "Abyssal head");
		table("Gargoyle", "Granite maul", "Mystic robe top (dark)");
		table("Kurask", "Leaf-bladed battleaxe", "Leaf-bladed sword", "Mystic robe top (light)");
		table("Smoke devil", "Occult necklace", "Dragon chainbody");
		table("Dust devil", "Dragon chainbody", "Dust battlestaff");
		table("Cave kraken", "Trident of the seas (full)", "Kraken tentacle");
		table("Hellhound", "Smouldering stone");
		table("Drake", "Drake's tooth", "Drake's claw", "Dragon thrownaxe", "Dragon knife");
		table("Wyrm", "Dragon sword", "Dragon harpoon", "Dragon knife");
		table("Hydra", "Hydra's eye", "Hydra's fang", "Hydra's heart", "Hydra tail", "Hydra leather",
			"Dragon thrownaxe", "Dragon knife");
		table("Skeletal Wyvern", "Granite legs", "Draconic visage");
		table("Basilisk Knight", "Basilisk jaw");
		table("Lizardman shaman", "Dragon warhammer");
		table("Spiritual mage", "Dragon boots");
		table("Dark beast", "Dark bow");
		table("Demonic gorilla", "Zenyte shard", "Ballista spring", "Ballista limbs", "Heavy frame",
			"Light frame", "Monkey tail");
		table("Tormented Demon", "Tormented synapse", "Burning claw");
		table("Cave horror", "Black mask");
		table("Rune dragon", "Dragon limbs", "Dragon metal lump");
		table("Adamant dragon", "Dragon metal slice");
		table("Vyrewatch Sentinel", "Blood shard");
		for (String wyvern : new String[]{"Ancient Wyvern", "Spitting Wyvern", "Taloned Wyvern", "Long-tailed Wyvern"})
		{
			table(wyvern, "Wyvern visage", "Granite longsword", "Granite boots");
		}

		// From the wiki's reverse lookup; boss minions (GWD sergeants, Vet'ion hounds) and revenants are left out
		// so they don't steal the boss market.
		addDrop("Earth warrior champion scroll", "Earth warrior");
		addDrop("Ghoul champion scroll", "Ghoul");
		addDrop("Giant champion scroll", "Black Knight Titan", "Bryophyta", "Fire giant", "Hill Giant",
			"Ice giant", "Moss giant", "Obor", "Reanimated giant");
		addDrop("Goblin champion scroll", "Goblin", "Cave goblin", "Cave goblin guard", "Goblin guard",
			"Reanimated goblin");
		addDrop("Hobgoblin champion scroll", "Hobgoblin");
		addDrop("Imp champion scroll", "Imp", "Reanimated imp");
		addDrop("Jogre champion scroll", "Jogre");
		addDrop("Lesser demon champion scroll", "Lesser demon", "Zakl'n Gritch");
		addDrop("Skeleton champion scroll", "Skeleton", "Skeleton Mage", "Skeleton brute", "Skeleton heavy",
			"Skeleton hero", "Skeleton thug", "Skeleton warlord", "Giant skeleton", "Nazastarool", "Skogre",
			"Undead one", "Vet'ion", "Calvar'ion");
		addDrop("Zombie champion scroll", "Zombie", "Zombie pirate", "Zombie swab", "Armoured zombie",
			"Monkey Zombie", "Afflicted", "Nazastarool", "Skogre", "Slash Bash", "Sorebones", "Undead Druid",
			"Undead Lumberjack", "Undead Zealot", "Undead one", "Zogre");

		alias(TREASURE_TRAILS, "clue", "clues", "clue scroll", "clue scrolls", "treasure trail", "tt");

		PETS.addAll(Arrays.asList("abyssal orphan", "aggy", "baby mole", "baron", "bran", "beef", "butch",
			"callisto cub", "dom", "gull", "hellpuppy", "huberte", "ikkle hydra", "jal nib rek", "kalphite princess",
			"lil zik", "lilviathan", "little nightmare", "maggot marquess", "moxi", "muphin", "nexling", "nid",
			"noon", "olmlet", "pet chaos elemental", "pet dagannoth prime", "pet dagannoth rex",
			"pet dagannoth supreme", "pet dark core", "pet general graardor", "pet kril tsutsaroth", "pet kraken",
			"pet kreearra", "pet smoke devil", "pet snakeling", "pet zilyana", "phoenix", "prince black dragon",
			"scorpias offspring", "scurry", "skotos", "smolcano", "smol heredit", "sraracha", "tiny tempor",
			"tumekens guardian", "tzrek jad", "venenatis spiderling", "vetion jr", "vorki", "wisp", "yami",
			"youngllef", "baby chinchompa", "beaver", "giant squirrel", "heron", "rift guardian", "rock golem",
			"rocky", "soup", "tangleroot", "abyssal protector", "bloodhound", "chompy chick", "herbi",
			"lil creator", "pet penance queen", "quetzin", "mr mcgroot", "orion"));
	}

	private DropTables()
	{
	}

	private static void table(String source, String... items)
	{
		TABLES.put(normalize(source), new ArrayList<>(Arrays.asList(items)));
	}

	private static void addDrop(String item, String... sources)
	{
		for (String source : sources)
		{
			TABLES.computeIfAbsent(normalize(source), k -> new ArrayList<>()).add(item);
		}
	}

	/** Drops that are always worth celebrating on their own (everything the Sire has comes from the Unsired). */
	static boolean isAlwaysCelebrated(String item)
	{
		return normalize(item).equals("unsired");
	}

	/** Clue scrolls and their containers never count as collection log chases. */
	static boolean isClueItem(String item)
	{
		String n = normalize(item);
		return n.startsWith("clue ") || n.startsWith("scroll box") || n.startsWith("challenge scroll")
			|| n.startsWith("reward casket");
	}

	/** Items that mean a treasure trail is underway (not bottles/nests/geodes, which are unopened). */
	static boolean isTrailItem(String item)
	{
		String n = normalize(item);
		return n.startsWith("clue scroll") || n.startsWith("challenge scroll") || n.startsWith("reward casket")
			|| n.startsWith("scroll box") || n.startsWith("puzzle box") || n.startsWith("light box");
	}

	private static void petFirst(String source, String... items)
	{
		table(source, items);
		PET_FIRST.add(normalize(source));
	}

	static boolean isPetFirst(String source)
	{
		return PET_FIRST.contains(normalize(source));
	}

	static boolean isPet(String item)
	{
		return PETS.contains(normalize(item));
	}

	/** The pet in a drop table, if any - regardless of whether it's already been obtained. */
	static String findPet(List<String> table)
	{
		for (String item : table)
		{
			if (isPet(item))
			{
				return item;
			}
		}
		return null;
	}

	private static void alias(String source, String... aliases)
	{
		for (String a : aliases)
		{
			ALIASES.put(normalize(a), source);
		}
	}

	/** Maps aliases and raid sub-boss names to their market name; anything else passes through. */
	static String canonicalSource(String name)
	{
		String canonical = ALIASES.get(normalize(name));
		return canonical != null ? canonical : name.trim();
	}

	/**
	 * True once {@code rawName} is the actual completion event for {@code canonicalSource} — the raid's own
	 * reward chest, or a non-raid source that doesn't alias away from its own name. False for a raid's
	 * room/sub-boss kills, which only alias to the raid for market display, not for counting a completion.
	 */
	static boolean isCompletion(String canonicalSource, String rawName)
	{
		return !RAIDS.contains(canonicalSource) || chestName(canonicalSource).equals(chestName(rawName));
	}

	/** A raid's name for comparing chest names: "The Fractured Archive" and "Fractured Archives" are the same raid. */
	private static String chestName(String name)
	{
		String n = normalize(name);
		if (n.startsWith("the "))
		{
			n = n.substring(4);
		}
		return n.endsWith("s") && n.contains("archive") ? n.substring(0, n.length() - 1) : n;
	}

	/** Common OSRS Wiki short names, for the compact layouts. */
	private static final Map<String, String> ABBREVIATIONS = new HashMap<>();

	static
	{
		String[][] pairs = {
			{CHAMBERS_OF_XERIC, "CoX"}, {THEATRE_OF_BLOOD, "ToB"}, {TOMBS_OF_AMASCUT, "ToA"}, {FRACTURED_ARCHIVE, "FA"},
			{GUARDIANS_OF_THE_RIFT, "GotR"}, {THE_GAUNTLET, "Gauntlet"}, {CORRUPTED_GAUNTLET, "CG"},
			{"The Nightmare", "Nightmare"}, {"Phosani's Nightmare", "Phosani"}, {"General Graardor", "Graardor"},
			{"Kree'arra", "Kree"}, {"Commander Zilyana", "Zilyana"}, {"K'ril Tsutsaroth", "K'ril"},
			{"Corporeal Beast", "Corp"}, {"Alchemical Hydra", "Hydra"}, {"Thermonuclear smoke devil", "Thermy"},
			{"Abyssal Sire", "Sire"}, {"Grotesque Guardians", "Gargs"}, {"Kalphite Queen", "KQ"},
			{"King Black Dragon", "KBD"}, {"Dagannoth Rex", "Rex"}, {"Dagannoth Prime", "Prime"},
			{"Dagannoth Supreme", "Supreme"}, {"Giant Mole", "Mole"}, {"Phantom Muspah", "Muspah"},
			{"Chaos Elemental", "Chaos Ele"}, {"Duke Sucellus", "Duke"}, {"The Leviathan", "Leviathan"},
			{"The Whisperer", "Whisperer"}, {"The Hueycoatl", "Huey"}, {"Doom of Mokhaiotl", "Doom"},
			{"Fortis Colosseum", "Colosseum"}, {"Moons of Peril", "Moons"}, {"TzHaar Fight Cave", "Fight Cave"},
			{"The Inferno", "Inferno"}, {"Crazy archaeologist", "Crazy Arch"}, {"Bryophyta", "Bryo"},
			{"Cerberus", "Cerb"}, {"Tormented Demon", "TD"}, {"Royal Titans", "Titans"}, {TREASURE_TRAILS, "Clues"}
		};
		for (String[] pair : pairs)
		{
			ABBREVIATIONS.put(normalize(pair[0]), pair[1]);
		}
	}

	/** What players call common uniques, for when a compact title would otherwise need a third line. */
	private static final Map<String, String> ITEM_ABBREVIATIONS = new HashMap<>();

	static
	{
		String[][] pairs = {
			{"Twisted bow", "Tbow"}, {"Dragon hunter crossbow", "DHCB"}, {"Dragon hunter lance", "Lance"},
			{"Dragon claws", "Claws"}, {"Elder maul", "Maul"}, {"Kodai insignia", "Kodai"},
			{"Dexterous prayer scroll", "Dex"}, {"Arcane prayer scroll", "Arcane"}, {"Twisted buckler", "Buckler"},
			{"Ancestral robe top", "Robe top"}, {"Ancestral robe bottom", "Robe bottom"},
			{"Scythe of vitur", "Scythe"}, {"Ghrazi rapier", "Rapier"}, {"Sanguinesti staff", "Sang"},
			{"Justiciar faceguard", "Faceguard"}, {"Justiciar chestguard", "Chestguard"},
			{"Justiciar legguards", "Legguards"}, {"Avernic defender hilt", "Hilt"},
			{"Thread of elidinis", "Thread"}, {"Eye of the corruptor", "Eye"}, {"Jewel of the sun", "Jewel"},
			{"Jewel of amascut", "Jewel"}, {"Breach of the scarab", "Breach"},
			{"Tanzanite fang", "Tanz fang"}, {"Serpentine visage", "Visage"}, {"Skeletal visage", "Visage"},
			{"Draconic visage", "Visage"}, {"Dragonbone necklace", "Necklace"}, {"Dragon warhammer", "DWH"},
			{"Dragon pickaxe", "D pick"}, {"Armadyl crossbow", "ACB"}, {"Armadyl helmet", "Helm"},
			{"Armadyl chestplate", "Chestplate"}, {"Armadyl chainskirt", "Chainskirt"},
			{"Bandos chestplate", "Chestplate"}, {"Bandos tassets", "Tassets"}, {"Saradomin sword", "Sara sword"},
			{"Staff of the dead", "SOTD"}, {"Abyssal bludgeon", "Bludgeon"}, {"Spectral spirit shield", "Spectral"},
			{"Arcane spirit shield", "Arcane"}, {"Elysian spirit shield", "Elysian"}, {"Elysian sigil", "Sigil"},
			{"Zaryte vambraces", "Vambs"}, {"Nihil horn", "Horn"}, {"Ancient hilt", "Hilt"},
			{"Torva platebody", "Torva body"}, {"Torva platelegs", "Torva legs"}, {"Torva full helm", "Torva helm"},
			{"Eldritch nightmare staff", "Eldritch"}, {"Volatile nightmare staff", "Volatile"},
			{"Harmonised nightmare staff", "Harmonised"}, {"Hydra leather", "Leather"}, {"Hydra tail", "Tail"},
			{"Hydra claw", "Claw"}, {"Primordial crystal", "Primordial"}, {"Pegasian crystal", "Pegasian"},
			{"Eternal crystal", "Eternal"}, {"Smouldering stone", "Smouldering"}, {"Uncut onyx", "Onyx"},
			{"Trident of the seas", "Trident"}, {"Occult necklace", "Occult"}, {"Tormented bracelet", "Bracelet"},
			{"Amulet of torture", "Torture"}, {"Necklace of anguish", "Anguish"}, {"Ring of suffering", "Suffering"},
		};
		for (String[] pair : pairs)
		{
			ITEM_ABBREVIATIONS.put(normalize(pair[0]), pair[1]);
		}
	}

	/**
	 * A short name for an item: the usual nickname if there is one (Tbow, DWH, Sang), else an owner's name dropped
	 * ("Osmumten's fang" becomes "fang", "Tumeken's shadow" becomes "shadow"), else the item itself.
	 */
	static String itemAbbreviation(String item)
	{
		String nickname = ITEM_ABBREVIATIONS.get(normalize(item));
		if (nickname != null)
		{
			return nickname;
		}
		int space = item.indexOf(' ');
		if (space > 0 && space < item.length() - 1)
		{
			String first = item.substring(0, space);
			if (first.endsWith("'s") || first.endsWith("'"))
			{
				return item.substring(space + 1);
			}
		}
		return item;
	}

	/** The wiki's short name for a source (ToB, CoX, KBD...), or the source itself if it has none. */
	static String abbreviation(String source)
	{
		String abbreviation = ABBREVIATIONS.get(normalize(source));
		return abbreviation != null ? abbreviation : source;
	}

	static List<String> lookup(String source)
	{
		List<String> table = TABLES.get(normalize(source));
		return table == null ? null : Collections.unmodifiableList(table);
	}

	/** Lowercase, drop parentheticals like "(uncharged)" and punctuation, collapse whitespace. */
	static String normalize(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace(' ', ' ')
			.toLowerCase(Locale.ROOT)
			.replaceAll("\\(.*?\\)", " ")
			.replaceAll("[-_]", " ")
			.replaceAll("[^a-z0-9 ]", "")
			.replaceAll("\\s+", " ")
			.trim();
	}

	/** Matches loose input like "dex scroll" to "Dexterous prayer scroll" in the table; else returns input. */
	static String matchItem(String input, List<String> table)
	{
		if (table == null)
		{
			return input;
		}
		String wanted = normalize(input);
		for (String item : table)
		{
			if (normalize(item).equals(wanted))
			{
				return item;
			}
		}
		String[] wantedWords = wanted.split(" ");
		for (String item : table)
		{
			String[] itemWords = normalize(item).split(" ");
			boolean all = true;
			for (String w : wantedWords)
			{
				boolean found = false;
				for (String iw : itemWords)
				{
					if (iw.startsWith(w))
					{
						found = true;
						break;
					}
				}
				if (!found)
				{
					all = false;
					break;
				}
			}
			if (all)
			{
				return item;
			}
		}
		return input;
	}

	/** Item name for the market title, without suffixes like "(uncharged)". */
	static String displayName(String item)
	{
		return item.replaceAll("\\s*\\(.*?\\)", "").trim();
	}

	/**
	 * Parses "Source: Item" lines into normalized canonical source -> items, so "CoX: Dex scroll" works.
	 * Multiple lines for the same source are all kept (not overwritten), so the caller can roll between
	 * them - each line is an equally-weighted entry, so repeating one gives it more weight.
	 */
	static Map<String, List<String>> parsePriorities(String config)
	{
		Map<String, List<String>> out = new HashMap<>();
		if (config == null)
		{
			return out;
		}
		for (String line : config.split("\\r?\\n"))
		{
			int sep = indexOfSeparator(line);
			if (sep <= 0)
			{
				continue;
			}
			String source = line.substring(0, sep).trim();
			String item = line.substring(sep + 1).trim();
			if (!source.isEmpty() && !item.isEmpty())
			{
				out.computeIfAbsent(normalize(canonicalSource(source)), k -> new ArrayList<>()).add(item);
			}
		}
		return out;
	}

	private static int indexOfSeparator(String line)
	{
		int colon = line.indexOf(':');
		int equals = line.indexOf('=');
		if (colon < 0)
		{
			return equals;
		}
		return equals < 0 ? colon : Math.min(colon, equals);
	}
}
