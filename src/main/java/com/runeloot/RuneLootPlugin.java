package com.runeloot;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.http.api.loottracker.LootRecordType;

@Slf4j
@PluginDependency(LootTrackerPlugin.class)
@PluginDescriptor(
	name = "Rune Loot",
	description = "Shows an animated popup for drops at or above a configurable GP value threshold",
	tags = {"loot", "drop", "notification", "value", "popup", "rune"}
)
public class RuneLootPlugin extends Plugin
{
	@Inject private RuneLootConfig config;
	@Inject private RuneLootOverlay overlay;
	@Inject private OverlayManager overlayManager;
	@Inject private ItemManager itemManager;
	@Inject private ClientThread clientThread;
	@Inject private RuneLootSoundPlayer soundPlayer;
	@Inject private ConfigManager configManager;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		log.debug("Rune Loot started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlay.clearQueue();
		log.debug("Rune Loot stopped");
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		switch (event.getType())
		{
			case NPC:
			case PICKPOCKET:
				if (!config.showNpcDrops()) return;
				break;
			case PLAYER:
				if (!config.showPlayerDrops()) return;
				break;
			case EVENT:
			{
				String name = event.getName().toLowerCase(Locale.ROOT);
				boolean isRaid = name.contains("chambers") || name.contains("theatre")
					|| name.contains("tombs") || name.contains("raids");
				boolean isClue = name.contains("casket") || name.contains("clue");
				if (isRaid && !config.showRaidLoot()) return;
				if (isClue && !config.showClueRewards()) return;
				if (!isRaid && !isClue && !config.showOtherEvents()) return;
				break;
			}
			default:
				return;
		}
		checkItems(event.getItems());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"rune-loot".equals(event.getGroup())) return;
		if ("testTrigger".equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			configManager.setConfiguration("rune-loot", "testTrigger", false);
			String name = config.testItemName();
			if (name != null && !name.isEmpty())
			{
				clientThread.invokeLater(() -> fireTestNotification(name));
			}
		}
	}

	private void fireTestNotification(String input)
	{
		String trimmed = input.trim();
		int id;

		try
		{
			id = Integer.parseInt(trimmed);
		}
		catch (NumberFormatException ignored)
		{
			List<ItemPrice> results = itemManager.search(trimmed);
			if (results.isEmpty())
			{
				log.debug("No item found for test input: {}", trimmed);
				return;
			}
			id = results.get(0).getId();
		}

		var comp = itemManager.getItemComposition(id);
		int gePrice = itemManager.getItemPrice(id);
		boolean geTradeable = comp.isGeTradeable() || gePrice > 0;
		long value = gePrice > 0 ? gePrice : comp.getHaPrice();
		queueNotification(comp.getName(), value, itemManager.getImage(id, 1, comp.isStackable()), geTradeable);
	}

	private void checkItems(Collection<ItemStack> items)
	{
		for (ItemStack item : items)
		{
			var comp = itemManager.getItemComposition(item.getId());

			if (isExcluded(comp.getName())) continue;

			int gePrice = itemManager.getItemPrice(item.getId());

			boolean geTradeable = comp.isGeTradeable() || gePrice > 0;

			if (!geTradeable && !config.showUntradeable()) continue;

			long unitValue = gePrice > 0 ? gePrice : comp.getHaPrice();
			long totalValue = unitValue * item.getQuantity();

			if (geTradeable && totalValue < config.valueThreshold()) continue;

			queueNotification(comp.getName(), totalValue,
				itemManager.getImage(item.getId(), item.getQuantity(), comp.isStackable()),
				geTradeable);
		}
	}

	private void queueNotification(String name, long value, BufferedImage icon, boolean geTradeable)
	{
		overlay.addNotification(new RuneLootNotification(cleanName(name), value, icon, geTradeable));
		soundPlayer.playAsync();
	}

	private static String cleanName(String name)
	{
		return name.replaceAll("\\s*\\(Members'?\\)", "").trim();
	}

	private boolean isExcluded(String name)
	{
		String excluded = config.excludedItems();
		if (excluded == null || excluded.isEmpty()) return false;

		String cleaned = cleanName(name);
		for (String token : excluded.split(","))
		{
			if (token.trim().equalsIgnoreCase(cleaned)) return true;
		}
		return false;
	}

	@Provides
	RuneLootConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLootConfig.class);
	}
}