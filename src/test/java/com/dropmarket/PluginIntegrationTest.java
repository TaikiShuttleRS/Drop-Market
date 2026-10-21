package com.dropmarket;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Filepath;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

/**
 * Runs the real plugin, wired by the real Guice graph, against stand-ins for the game. The unit tests only cover the
 * pure rules, which is how a package-private config enum slipped through; this is the test that sees the plugin the way
 * RuneLite does. In the normal runs nothing may fail inside the guard; in the hostile runs the game hands over nulls or
 * throws, and nothing may escape to the caller.
 */
public class PluginIntegrationTest
{
	private enum Mode
	{
		NORMAL, NULLS, THROWING, NULL_CONFIG
	}

	/** Creates an instance without running a constructor, for RuneLite classes that need a whole client to build. */
	@SuppressWarnings("unchecked")
	private static <T> T allocate(Class<T> type) throws Exception
	{
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return (T) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type);
	}

	// The constructors below never run (allocate skips them); they only satisfy the compiler.
	private static class StubClientThread extends ClientThread
	{
		@Override
		public void invoke(Runnable r)
		{
			r.run();
		}

		@Override
		public void invokeLater(Runnable r)
		{
			r.run();
		}

		@Override
		public void invokeLater(java.util.function.BooleanSupplier r)
		{
			// Runs to completion straight away: each call is one client tick in the real thing.
			while (!r.getAsBoolean())
			{
			}
		}
	}

	/** The overlay manager can't be stubbed (private constructor); the real one is never asked to do anything real. */
	private static final java.util.Set<String> OVERLAY_PLACES = new java.util.HashSet<>(
		Arrays.asList("startUp overlay", "shutDown overlay"));

	private static class StubProfileStore extends ProfileStore
	{
		private final Map<String, String> values = new HashMap<>();
		private final Map<String, String> settings = new HashMap<>();

		StubProfileStore()
		{
			super(null);
		}

		@Override
		String get(String key)
		{
			return values.get(key);
		}

		@Override
		void set(String key, String value)
		{
			values.put(key, value);
		}

		@Override
		String getSetting(String key)
		{
			return settings.get(key);
		}

		@Override
		void setSetting(String key, String value)
		{
			settings.put(key, value);
		}
	}

	private static class StubItemLookup extends ItemLookup
	{
		private final Mode mode;

		StubItemLookup(Mode mode)
		{
			super(null);
			this.mode = mode;
		}

		@Override
		String nameOf(int id)
		{
			if (id == CHINCHOMPA_ID)
			{
				return "Chinchompa";
			}
			if (id == GOAT_FUR_ID)
			{
				return "Wyrmscraig goat fur";
			}
			return mode == Mode.NULLS ? "" : NAMES[Math.floorMod(id, NAMES.length)];
		}

		@Override
		String canonicalNameOf(int id)
		{
			return nameOf(id);
		}

		@Override
		List<ItemPrice> search(String name)
		{
			return mode == Mode.NORMAL ? Collections.singletonList(price(name)) : Collections.emptyList();
		}

		@Override
		long priceOf(int id)
		{
			return mode == Mode.NORMAL ? id * 1_000L : 0;
		}

		private static ItemPrice price(String name)
		{
			ItemPrice price = new ItemPrice();
			price.setName(name);
			// The Obligator is the sort of drop a market bets on (5m); everything else stays cheap.
			price.setId(name.startsWith("Obligator") ? 5_000 : name.length());
			return price;
		}
	}

	/** Records what the plugin asks of the custom sound instead of touching the speakers or opening a window. */
	private static class StubCustomSound extends CustomSound
	{
		final List<Integer> plays = new ArrayList<>();
		final List<SoundEvent> events = new ArrayList<>();
		int prepared;

		StubCustomSound()
		{
			super(null, null);
		}

		@Override
		void play(SoundEvent event, int percent, Runnable onFailure)
		{
			events.add(event);
			plays.add(percent);
		}

		@Override
		void prepareFolder()
		{
			prepared++;
		}

		@Override
		String folderPath()
		{
			return "stub-sound-folder";
		}
	}

	private static final int CHINCHOMPA_ID = 10033;
	private static final int GOAT_FUR_ID = 1_000_001;

	private static final String[] NAMES = {"Tanzanite fang", "Dragon bones", "Coins", "Scythe of vitur", "Emerald",
		"Pet snakeling", "Abyssal whip", "Uncut diamond"};

	private interface Answer
	{
		Object apply(java.lang.reflect.Method method, Object[] args) throws Throwable;
	}

	@SuppressWarnings("unchecked")
	private static <T> T stub(Class<T> type, Answer answer)
	{
		InvocationHandler handler = (proxy, method, args) ->
		{
			if (method.getDeclaringClass() == Object.class)
			{
				return method.getName().equals("hashCode") ? 1 : method.getName().equals("equals") ? proxy == args[0] : "stub";
			}
			return answer.apply(method, args);
		};
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
	}

	private static Object defaultFor(Class<?> type)
	{
		if (type == boolean.class)
		{
			return false;
		}
		if (type == int.class || type == short.class || type == byte.class)
		{
			return 0;
		}
		if (type == long.class)
		{
			return 0L;
		}
		if (type == double.class)
		{
			return 0d;
		}
		if (type == float.class)
		{
			return 0f;
		}
		return null;
	}

	private static NPC npc(String name, int id)
	{
		return stub(NPC.class, (m, args) ->
		{
			switch (m.getName())
			{
				case "getName":
					return name;
				case "getId":
					return id;
				case "getCombatLevel":
					return 500;
				default:
					return defaultFor(m.getReturnType());
			}
		});
	}

	private static Client client(Mode mode, List<int[]> sounds)
	{
		Player player = stub(Player.class, (m, args) ->
		{
			switch (m.getName())
			{
				case "getName":
					return "Taiki Shuttle";
				case "getWorldLocation":
					return new WorldPoint(3000, 3000, 0);
				default:
					return defaultFor(m.getReturnType());
			}
		});
		return stub(Client.class, (m, args) ->
		{
			if (mode == Mode.THROWING && !m.getName().equals("toString"))
			{
				throw new IllegalStateException("client not ready: " + m.getName());
			}
			if (mode == Mode.NULLS)
			{
				return defaultFor(m.getReturnType());
			}
			switch (m.getName())
			{
				case "playSoundEffect":
					sounds.add(new int[]{(Integer) args[0], args.length > 1 ? (Integer) args[1] : -1});
					return null;
				case "getGameState":
					return GameState.LOGGED_IN;
				case "getLocalPlayer":
					return player;
				case "getTickCount":
					return 100;
				default:
					return defaultFor(m.getReturnType());
			}
		});
	}

	/** The real config, built the way RuneLite does it: a proxy of the interface that answers with the defaults. */
	@SuppressWarnings("unchecked")
	private static DropMarketConfig config(WinSound winSound, boolean hostile)
	{
		InvocationHandler handler = (proxy, method, args) ->
		{
			if (hostile)
			{
				return defaultFor(method.getReturnType());
			}
			if (winSound != null && method.getName().equals("winSound"))
			{
				return winSound;
			}
			if (method.isDefault())
			{
				return java.lang.invoke.MethodHandles.privateLookupIn(DropMarketConfig.class,
						java.lang.invoke.MethodHandles.lookup())
					.unreflectSpecial(method, DropMarketConfig.class).bindTo(proxy).invokeWithArguments(args);
			}
			return defaultFor(method.getReturnType());
		};
		return (DropMarketConfig) Proxy.newProxyInstance(DropMarketConfig.class.getClassLoader(),
			new Class<?>[]{DropMarketConfig.class}, handler);
	}

	private static class Rig
	{
		final DropMarketPlugin plugin;
		final DropMarketOverlay overlay;
		final Guard guard;
		final Client client;
		final StubProfileStore store = new StubProfileStore();
		final StubCustomSound customSound = new StubCustomSound();

		final List<int[]> sounds = new ArrayList<>();

		Rig(Mode mode) throws Exception
		{
			this(mode, null);
		}

		Rig(Mode mode, WinSound winSound) throws Exception
		{
			client = client(mode, sounds);
			OverlayManager overlays = allocate(OverlayManager.class);
			ClientThread thread = allocate(StubClientThread.class);
			ItemLookup items = new StubItemLookup(mode);
			// RuneLite's real plugin folder is never touched in tests.
			plugin = new DropMarketPlugin()
			{
				@Override
				Filepath soundFolder()
				{
					return null;
				}
			};
			DropMarketConfig config = config(winSound, mode == Mode.NULL_CONFIG);
			Injector injector = Guice.createInjector(new AbstractModule()
			{
				@Override
				protected void configure()
				{
					bind(Client.class).toInstance(client);
					bind(ClientThread.class).toInstance(thread);
					bind(OverlayManager.class).toInstance(overlays);
					bind(ProfileStore.class).toInstance(store);
					bind(ItemLookup.class).toInstance(items);
					bind(DropMarketConfig.class).toInstance(config);
					bind(DropMarketPlugin.class).toInstance(plugin);
					bind(Gson.class).toInstance(new Gson());
					bind(OkHttpClient.class).toInstance(new OkHttpClient());
					bind(CustomSound.class).toInstance(customSound);
					bind(java.util.concurrent.ScheduledExecutorService.class).toInstance(
						java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable ->
						{
							Thread thread = new Thread(runnable);
							thread.setDaemon(true);
							return thread;
						}));
				}
			});
			injector.injectMembers(plugin);
			overlay = injector.getInstance(DropMarketOverlay.class);
			guard = injector.getInstance(Guard.class);
		}

		/** Places that failed inside the guard, other than the overlay manager calls the stand-in can't serve. */
		List<String> unexpectedFailures()
		{
			List<String> places = new ArrayList<>(guard.failedPlaces());
			places.removeAll(OVERLAY_PLACES);
			return places;
		}

		void render()
		{
			BufferedImage image = new BufferedImage(300, 220, BufferedImage.TYPE_INT_ARGB);
			overlay.render(image.createGraphics());
		}
	}

	private static List<ItemStack> loot(Random random)
	{
		List<ItemStack> items = new ArrayList<>();
		int count = 1 + random.nextInt(4);
		for (int i = 0; i < count; i++)
		{
			items.add(new ItemStack(random.nextInt(8_000), 1 + random.nextInt(50)));
		}
		return items;
	}

	/** One event picked at random from everything the game sends the plugin. */
	private static void fire(DropMarketPlugin plugin, Random random, int i)
	{
		String[] sources = {"Zulrah", "Vorkath", "Abyssal Sire", "Vardorvis", "Tombs of Amascut", "Chambers of Xeric",
			"Theatre of Blood", "Cow", "Barrows", "Yama"};
		String[] messages = {"Valuable drop: Scythe of vitur (1,200,000,000 coins)", "Untradeable drop: Pet snakeling",
			"New item added to your collection log: Tanzanite fang", "You catch a shark.", "", "Your Zulrah kill count is: 12.",
			"Congratulations, you've just advanced your Mining level.", "Valuable drop: Emerald (500 coins)"};
		switch (random.nextInt(12))
		{
			case 0:
				plugin.onNpcLootReceived(new NpcLootReceived(npc(sources[random.nextInt(sources.length)], 1 + random.nextInt(900)),
					loot(random)));
				break;
			case 1:
				plugin.onLootReceived(new LootReceived(sources[random.nextInt(sources.length)], 700, LootRecordType.NPC,
					loot(random), 1, null));
				break;
			case 2:
				plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", messages[random.nextInt(messages.length)],
					null, i));
				break;
			case 3:
				plugin.onStatChanged(new StatChanged(Skill.values()[random.nextInt(Skill.values().length)], i * 10, 50, 50));
				break;
			case 4:
				plugin.onItemContainerChanged(new ItemContainerChanged(random.nextBoolean() ? 93 : 95, container(random)));
				break;
			case 5:
				HitsplatApplied hit = new HitsplatApplied();
				hit.setActor(npc("Zulrah", 2042));
				hit.setHitsplat(stub(Hitsplat.class, (m, args) -> m.getName().equals("isMine") ? true : defaultFor(m.getReturnType())));
				plugin.onHitsplatApplied(hit);
				break;
			case 6:
				ConfigChanged changed = new ConfigChanged();
				changed.setGroup(DropMarketConfig.GROUP);
				changed.setKey(new String[]{"priorityDrops", "blacklistedDrops", "overlayLayout", "winSound", "testWinSound",
					"showSoundFolder", "valueUnit", null}[random.nextInt(8)]);
				changed.setNewValue(new String[]{"true", "false", "CUSTOM", null, "garbage"}[random.nextInt(5)]);
				plugin.onConfigChanged(changed);
				break;
			case 7:
				GameStateChanged state = new GameStateChanged();
				state.setGameState(random.nextBoolean() ? GameState.LOGGED_IN : GameState.LOADING);
				plugin.onGameStateChanged(state);
				break;
			case 8:
				plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged(null, "profile"));
				break;
			case 9:
				String[] options = {"Read", "Open", "Dig", "Attack", "Use"};
				String[] targets = {"<col=ff9040>Clue scroll (hard)</col>", "Casket", null, ""};
				plugin.onMenuOptionClicked(new MenuOptionClicked(stub(MenuEntry.class, (m, args) ->
				{
					switch (m.getName())
					{
						case "getOption":
							return options[random.nextInt(options.length)];
						case "getTarget":
							return targets[random.nextInt(targets.length)];
						default:
							return defaultFor(m.getReturnType());
					}
				})));
				break;
			case 10:
				GameObjectSpawned spawned = new GameObjectSpawned();
				spawned.setGameObject(gameObject(random.nextBoolean() ? 27029 : 1));
				plugin.onGameObjectSpawned(spawned);
				break;
			default:
				GameObjectDespawned despawned = new GameObjectDespawned();
				despawned.setGameObject(gameObject(random.nextBoolean() ? 27029 : 1));
				plugin.onGameObjectDespawned(despawned);
				break;
		}
	}

	private static GameObject gameObject(int id)
	{
		return stub(GameObject.class, (m, args) ->
		{
			switch (m.getName())
			{
				case "getId":
					return id;
				case "getWorldLocation":
					return new WorldPoint(3000, 3000, 0);
				default:
					return defaultFor(m.getReturnType());
			}
		});
	}

	private static ItemContainer container(Random random)
	{
		Item[] contents = new Item[1 + random.nextInt(6)];
		for (int i = 0; i < contents.length; i++)
		{
			int id = random.nextInt(4) == 0 ? -1 : random.nextInt(8_000);
			contents[i] = new Item(id, 1);
		}
		return stub(ItemContainer.class, (m, args) -> m.getName().equals("getItems") ? contents : defaultFor(m.getReturnType()));
	}

	private void playSession(Rig rig, long seed) throws Exception
	{
		Random random = new Random(seed);
		rig.plugin.startUp();
		GameStateChanged login = new GameStateChanged();
		login.setGameState(GameState.LOGGED_IN);
		rig.plugin.onGameStateChanged(login);
		for (int i = 0; i < 3_000; i++)
		{
			rig.plugin.onGameTick(new GameTick());
			if (i % 7 == 0)
			{
				fire(rig.plugin, random, i);
			}
			if (i % 3 == 0)
			{
				rig.render();
			}
		}
		rig.plugin.shutDown();
	}

	@Test
	public void normalSessionNeverTripsTheGuard() throws Exception
	{
		for (long seed = 1; seed <= 5; seed++)
		{
			Rig rig = new Rig(Mode.NORMAL);
			playSession(rig, seed);
			Assert.assertEquals("failed places: " + rig.unexpectedFailures(), Collections.emptyList(), rig.unexpectedFailures());
		}
	}

	@Test
	public void nothingEscapesWhenTheGameHandsOverNulls() throws Exception
	{
		Rig rig = new Rig(Mode.NULLS);
		playSession(rig, 7);
		System.out.println("null-game failures swallowed: " + rig.guard.failedPlaces());
	}

	@Test
	public void nothingEscapesWhenTheGameThrows() throws Exception
	{
		Rig rig = new Rig(Mode.THROWING);
		playSession(rig, 11);
	}

	/** Events whose every field is null, which the real game never sends, fired at every handler. Nothing may escape. */
	@Test
	public void emptyEventsNeverEscape() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.plugin.startUp();
		for (int i = 0; i < 50; i++)
		{
			rig.plugin.onGameTick(allocate(GameTick.class));
			rig.plugin.onGameStateChanged(allocate(GameStateChanged.class));
			rig.plugin.onChatMessage(allocate(ChatMessage.class));
			rig.plugin.onStatChanged(allocate(StatChanged.class));
			rig.plugin.onItemContainerChanged(allocate(ItemContainerChanged.class));
			rig.plugin.onHitsplatApplied(allocate(HitsplatApplied.class));
			rig.plugin.onConfigChanged(allocate(ConfigChanged.class));
			rig.plugin.onGameObjectSpawned(allocate(GameObjectSpawned.class));
			rig.plugin.onGameObjectDespawned(allocate(GameObjectDespawned.class));
			rig.plugin.onNpcLootReceived(allocate(NpcLootReceived.class));
			rig.plugin.onLootReceived(allocate(LootReceived.class));
			rig.plugin.onRuneScapeProfileChanged(allocate(RuneScapeProfileChanged.class));
			rig.plugin.onMenuOptionClicked(allocate(MenuOptionClicked.class));
			rig.render();
		}
		System.out.println("empty-event failures swallowed: " + rig.unexpectedFailures());
	}

	/** Older versions saved the two drop values in gp under other keys; they carry over into thousands, once. */
	@Test
	public void oldGpValuesMoveOverIntoThousands() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.store.settings.put("minimumDropValue", "250000");
		rig.store.settings.put("valuableDropThreshold", "2000000");
		rig.plugin.startUp();
		Assert.assertEquals("250", rig.store.settings.get("marketMinimumAmount"));
		Assert.assertEquals("2000", rig.store.settings.get("winMinimumAmount"));

		// A value already chosen in the new setting is never overwritten by the old one.
		rig.store.settings.put("winMinimumAmount", "5000");
		rig.plugin.startUp();
		Assert.assertEquals("5000", rig.store.settings.get("winMinimumAmount"));
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	/** The change shown during a celebration is the jump to 100%, held steady, even if more drops land meanwhile. */
	@Test
	public void winDeltasHoldSteadyThroughTheCelebration() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.plugin.startUp();
		ChatMessage first = new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 1);
		rig.plugin.onChatMessage(first);
		Assert.assertTrue("the drop resolved the market", rig.plugin.isResolved());
		int delta = rig.plugin.getDisplayDelta();
		int delta2 = rig.plugin.getDisplayDelta2();
		Assert.assertTrue("jump is shown, not 0%: " + delta, delta > 0);
		for (int i = 0; i < 30; i++)
		{
			rig.plugin.onGameTick(new GameTick());
			if (i % 5 == 0)
			{
				rig.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
					"New item added to your collection log: Tanzanite fang", null, 2 + i));
			}
			Assert.assertEquals(delta, rig.plugin.getDisplayDelta());
			Assert.assertEquals(delta2, rig.plugin.getDisplayDelta2());
		}
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	/** The win sound reaches the game at the set volume, and the bell rings six notes, up and down, once per win. */
	@Test
	public void winSoundsPlayAtFullVolumeAndTheBellRingsSixNotes() throws Exception
	{
		Rig bell = new Rig(Mode.NORMAL, WinSound.BELL_DING);
		bell.plugin.startUp();
		bell.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 1));
		Assert.assertEquals(6, bell.sounds.size());
		for (int i = 0; i < 6; i++)
		{
			Assert.assertEquals(i % 2 == 0 ? 3813 : 3817, bell.sounds.get(i)[0]);
			Assert.assertEquals(127, bell.sounds.get(i)[1]);
		}
		// The loot event and chat message for one drop land on the same tick and make the noise once.
		bell.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 2));
		Assert.assertEquals(6, bell.sounds.size());
		Assert.assertEquals(Collections.emptyList(), bell.unexpectedFailures());

		Rig gem = new Rig(Mode.NORMAL, WinSound.GEM_ALERT);
		gem.plugin.startUp();
		gem.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 1));
		Assert.assertEquals(1, gem.sounds.size());
		Assert.assertEquals(2655, gem.sounds.get(0)[0]);

		Rig off = new Rig(Mode.NORMAL, WinSound.OFF);
		off.plugin.startUp();
		off.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 1));
		Assert.assertEquals(0, off.sounds.size());
	}

	/** Every enum, colour and text setting comes back null (never happens in RuneLite, but nothing may escape or fail). */
	@Test
	public void aConfigThatAnswersNullDoesNotBreakAnything() throws Exception
	{
		Rig rig = new Rig(Mode.NULL_CONFIG);
		playSession(rig, 21);
		System.out.println("null-config failures swallowed: " + rig.unexpectedFailures());
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	/** A new collection log item uses collection-log.wav, an ordinary win uses win.wav, and each win resets the choice. */
	@Test
	public void eachKindOfWinAsksForItsOwnCustomSoundFile() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.CUSTOM);
		rig.plugin.startUp();
		rig.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 1));
		Assert.assertEquals(Collections.singletonList(SoundEvent.COLLECTION_LOG), rig.customSound.events);

		// The test button is an ordinary win sound.
		ConfigChanged test = new ConfigChanged();
		test.setGroup(DropMarketConfig.GROUP);
		test.setKey("testWinSound");
		test.setNewValue("true");
		rig.plugin.onConfigChanged(test);
		Assert.assertEquals(SoundEvent.WIN, rig.customSound.events.get(1));
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	@Test
	public void settingButtonsDoTheirJobAndUntickThemselves() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.CUSTOM);
		rig.plugin.startUp();

		ConfigChanged test = new ConfigChanged();
		test.setGroup(DropMarketConfig.GROUP);
		test.setKey("testWinSound");
		test.setNewValue("true");
		rig.plugin.onConfigChanged(test);
		Assert.assertEquals(Collections.singletonList(100), rig.customSound.plays);
		Assert.assertEquals("false", rig.store.settings.get("testWinSound"));

		ConfigChanged show = new ConfigChanged();
		show.setGroup(DropMarketConfig.GROUP);
		show.setKey("showSoundFolder");
		show.setNewValue("true");
		rig.plugin.onConfigChanged(show);
		Assert.assertEquals(1, rig.customSound.prepared);
		Assert.assertEquals("false", rig.store.settings.get("showSoundFolder"));

		// Un-ticking (the plugin's own reset, or the player) does nothing.
		show.setNewValue("false");
		rig.plugin.onConfigChanged(show);
		Assert.assertEquals(1, rig.customSound.prepared);
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	private static void inventory(Rig rig, int... itemIds)
	{
		Item[] items = new Item[itemIds.length];
		for (int i = 0; i < itemIds.length; i++)
		{
			items[i] = new Item(itemIds[i], 1);
		}
		ItemContainer container = stub(ItemContainer.class, (m, args) -> m.getName().equals("getItems") ? items
			: defaultFor(m.getReturnType()));
		rig.plugin.onItemContainerChanged(new ItemContainerChanged(93, container));
	}

	private static void hunterXp(Rig rig, int xp)
	{
		rig.plugin.onStatChanged(new StatChanged(Skill.HUNTER, xp, 50, 50));
	}

	private static void tick(Rig rig, int count)
	{
		for (int i = 0; i < count; i++)
		{
			rig.plugin.onGameTick(new GameTick());
		}
	}

	/** Ordinary Hunter XP (birds, kebbits...) is not a pet roll, so it opens no market and counts nothing. */
	@Test
	public void plainHunterXpIsIgnored() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.OFF);
		rig.plugin.startUp();
		hunterXp(rig, 1_000);
		tick(rig, 3);
		hunterXp(rig, 1_200);
		tick(rig, 5);
		hunterXp(rig, 1_400);
		tick(rig, 5);
		Assert.assertEquals(DropMarketPlugin.LIVE_ODDS, rig.plugin.getTitle());
		Assert.assertFalse(rig.plugin.getBadgeText(), rig.plugin.getBadgeText().contains("catches"));
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	/** A chinchompa gained with the Hunter XP is a chinchompa catch, whichever of the two arrives first. */
	@Test
	public void aChinchompaWithHunterXpCountsAsACatch() throws Exception
	{
		for (boolean itemFirst : new boolean[]{true, false})
		{
			Rig rig = new Rig(Mode.NORMAL, WinSound.OFF);
			rig.plugin.startUp();
			hunterXp(rig, 1_000);
			tick(rig, 3);
			if (itemFirst)
			{
				inventory(rig, CHINCHOMPA_ID);
				hunterXp(rig, 1_198);
			}
			else
			{
				hunterXp(rig, 1_198);
				inventory(rig, CHINCHOMPA_ID);
			}
			tick(rig, 2);
			Assert.assertTrue("market opened: " + rig.plugin.getTitle(), rig.plugin.getTitle().toLowerCase().contains("chinchompas"));
			Assert.assertEquals("1 catches", rig.plugin.getBadgeText());
			// A second catch adds to the same counter.
			hunterXp(rig, 1_396);
			inventory(rig, CHINCHOMPA_ID, CHINCHOMPA_ID);
			tick(rig, 2);
			Assert.assertEquals("2 catches", rig.plugin.getBadgeText());
			Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
		}
	}

	/** Taking chinchompas out of the bank is not catching them: no Hunter XP comes with it. */
	@Test
	public void withdrawingChinchompasIsNotACatch() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.OFF);
		rig.plugin.startUp();
		hunterXp(rig, 1_000);
		tick(rig, 3);
		inventory(rig, CHINCHOMPA_ID);
		tick(rig, 5);
		Assert.assertEquals(DropMarketPlugin.LIVE_ODDS, rig.plugin.getTitle());
	}

	@Test
	public void goatDropsWithHunterXpCountAsAGoatHunt() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.OFF);
		rig.plugin.startUp();
		hunterXp(rig, 1_000);
		tick(rig, 3);
		inventory(rig, GOAT_FUR_ID);
		hunterXp(rig, 1_300);
		tick(rig, 2);
		Assert.assertTrue(rig.plugin.getTitle(), rig.plugin.getTitle().toLowerCase().contains("wyrmscraig goats"));
		Assert.assertEquals("1 hunts", rig.plugin.getBadgeText());
	}

	@Test
	public void expertAndMasterSacksCountAsRumoursButBasicOnesDoNot() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.OFF);
		rig.plugin.startUp();
		rig.plugin.onLootReceived(new LootReceived("Hunters' loot sack (adept)", 0, LootRecordType.EVENT,
			Collections.singletonList(new ItemStack(1_000, 1)), 1, null));
		tick(rig, 3);
		Assert.assertEquals(DropMarketPlugin.LIVE_ODDS, rig.plugin.getTitle());
		rig.plugin.onLootReceived(new LootReceived("Hunters' loot sack (expert)", 0, LootRecordType.EVENT,
			Collections.singletonList(new ItemStack(1_000, 1)), 1, null));
		tick(rig, 2);
		Assert.assertTrue(rig.plugin.getTitle(), rig.plugin.getTitle().toLowerCase().contains("hunter rumours"));
		Assert.assertEquals("1 sacks", rig.plugin.getBadgeText());
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	/** Fighting any Fractured Archive boss opens the raid's market, and its reward chest counts a completion. */
	@Test
	public void theFracturedArchiveOpensItsMarketAndCountsCompletions() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL, WinSound.OFF);
		rig.plugin.startUp();
		HitsplatApplied hit = new HitsplatApplied();
		hit.setActor(npc("Moia", 12_345));
		hit.setHitsplat(stub(Hitsplat.class, (m, args) -> m.getName().equals("isMine") ? true : defaultFor(m.getReturnType())));
		rig.plugin.onHitsplatApplied(hit);
		tick(rig, 2);
		Assert.assertTrue(rig.plugin.getTitle(), rig.plugin.getTitle().toLowerCase().contains("fractured archive drops"));

		// A sub-boss kill is not a completion; the reward chest (however the loot tracker names it) is.
		rig.plugin.onLootReceived(new LootReceived("Moia", 0, LootRecordType.NPC,
			Collections.singletonList(new ItemStack(1_000, 1)), 1, null));
		tick(rig, 1);
		Assert.assertEquals("0 KC", rig.plugin.getBadgeText());
		rig.plugin.onLootReceived(new LootReceived("The Fractured Archive", 0, LootRecordType.EVENT,
			Collections.singletonList(new ItemStack(1_000, 1)), 1, null));
		tick(rig, 1);
		Assert.assertEquals("1 KC", rig.plugin.getBadgeText());
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	@Test
	public void survivesStartingTwiceAndStoppingFirst() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.plugin.shutDown();
		rig.plugin.startUp();
		rig.plugin.startUp();
		rig.plugin.onGameTick(new GameTick());
		rig.plugin.shutDown();
		rig.plugin.shutDown();
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}
}
