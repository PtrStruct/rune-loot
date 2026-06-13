package com.runeloot;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("rune-loot")
public interface RuneLootConfig extends Config
{
	@ConfigSection(name = "Sources", description = "Which loot sources trigger the popup", position = 0)
	String sourcesSection = "sources";

	@ConfigSection(name = "Filter", description = "Value and tradability filters", position = 1)
	String filterSection = "filter";

	@ConfigSection(name = "Text", description = "Title text for each drop type", position = 2)
	String textSection = "text";

	@ConfigSection(name = "Colors", description = "Popup colors", position = 3)
	String colorsSection = "colors";

	@ConfigSection(name = "Display", description = "Timing and value display", position = 4)
	String displaySection = "display";

	@ConfigSection(name = "Sound", description = "Notification sound settings", position = 5)
	String soundSection = "sound";

	@ConfigSection(name = "Test", description = "Test the popup in-game", position = 6)
	String testSection = "test";

	@ConfigItem(
		keyName = "showNpcDrops",
		name = "NPC Drops",
		description = "Show popup for drops from NPCs and bosses",
		section = "sources",
		position = 0
	)
	default boolean showNpcDrops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlayerDrops",
		name = "Player Drops (PvP)",
		description = "Show popup for drops from killed players",
		section = "sources",
		position = 1
	)
	default boolean showPlayerDrops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRaidLoot",
		name = "Raids",
		description = "Show popup for Chambers of Xeric, Theatre of Blood, Tombs of Amascut, etc.",
		section = "sources",
		position = 2
	)
	default boolean showRaidLoot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showClueRewards",
		name = "Clue Scroll Rewards",
		description = "Show popup for clue scroll reward caskets",
		section = "sources",
		position = 3
	)
	default boolean showClueRewards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOtherEvents",
		name = "Other Events",
		description = "Show popup for other event loot (Barrows, minigames, etc.)",
		section = "sources",
		position = 4
	)
	default boolean showOtherEvents()
	{
		return true;
	}

	@ConfigItem(
		keyName = "valueThreshold",
		name = "Value Threshold (gp)",
		description = "Minimum GE value (per item x quantity) to trigger the popup",
		section = "filter",
		position = 0
	)
	default int valueThreshold()
	{
		return 100_000;
	}

	@ConfigItem(
		keyName = "showUntradeable",
		name = "Show Untradeable Drops",
		description = "Show popup for drops that cannot be sold on the Grand Exchange",
		section = "filter",
		position = 1
	)
	default boolean showUntradeable()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tradeableTitle",
		name = "Tradeable Drop Title",
		description = "Title shown when the drop is GE-tradeable",
		section = "text",
		position = 0
	)
	default String tradeableTitle()
	{
		return "Valuable Drop!";
	}

	@ConfigItem(
		keyName = "untradeableTitle",
		name = "Untradeable Drop Title",
		description = "Title shown when the drop is not GE-tradeable",
		section = "text",
		position = 1
	)
	default String untradeableTitle()
	{
		return "Untradeable Drop!";
	}

	@ConfigItem(
		keyName = "tradeableTitleColor",
		name = "Tradeable Title Color",
		description = "Color of the title text for tradeable drops",
		section = "colors",
		position = 0
	)
	default Color tradeableTitleColor()
	{
		return new Color(255, 215, 0);
	}

	@ConfigItem(
		keyName = "untradeableTitleColor",
		name = "Untradeable Title Color",
		description = "Color of the title text for untradeable drops",
		section = "colors",
		position = 1
	)
	default Color untradeableTitleColor()
	{
		return new Color(255, 165, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "backgroundColor",
		name = "Background Color",
		description = "Background fill color and opacity",
		section = "colors",
		position = 2
	)
	default Color backgroundColor()
	{
		return new Color(62, 45, 19, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "titleBarColor",
		name = "Title Bar Color",
		description = "Title bar fill color and opacity",
		section = "colors",
		position = 3
	)
	default Color titleBarColor()
	{
		return new Color(39, 27, 10, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "borderColor",
		name = "Border Color",
		description = "Color of the popup border",
		section = "colors",
		position = 4
	)
	default Color borderColor()
	{
		return new Color(28, 20, 7, 255);
	}

	@ConfigItem(
		keyName = "valueLowColor",
		name = "Value Color (under 10M)",
		description = "Color of the value text for drops under 10,000,000 gp",
		section = "colors",
		position = 5
	)
	default Color valueLowColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "valueMidColor",
		name = "Value Color (10M - 1B)",
		description = "Color of the value text for drops between 10M and 1B gp",
		section = "colors",
		position = 6
	)
	default Color valueMidColor()
	{
		return new Color(0, 200, 83);
	}

	@ConfigItem(
		keyName = "valueHighColor",
		name = "Value Color (1B+)",
		description = "Color of the value text for drops at or above 1,000,000,000 gp",
		section = "colors",
		position = 7
	)
	default Color valueHighColor()
	{
		return new Color(255, 215, 0);
	}

	@ConfigItem(
		keyName = "displayDuration",
		name = "Display Duration (seconds)",
		description = "How long each popup is fully visible before it exits",
		section = "display",
		position = 0
	)
	@Range(min = 1, max = 30)
	default int displayDuration()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "showValue",
		name = "Show Item Value",
		description = "Display the item's GP value in the popup",
		section = "display",
		position = 1
	)
	default boolean showValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "screenshotEnabled",
		name = "Screenshot on Popup",
		description = "Save a screenshot when the popup fully appears. Saved to .runelite/rune-loot/screenshots/",
		section = "display",
		position = 2
	)
	default boolean screenshotEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGpSuffix",
		name = "Show 'gp' Suffix",
		description = "Append 'gp' after the value",
		section = "display",
		position = 3
	)
	default boolean showGpSuffix()
	{
		return true;
	}

	@ConfigItem(
		keyName = "abbreviateValue",
		name = "Abbreviate Value",
		description = "Show 15.8M instead of 15,821,990",
		section = "display",
		position = 4
	)
	default boolean abbreviateValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "soundEnabled",
		name = "Play Sound on Popup",
		description = "Play a local audio file when a popup appears",
		section = "sound",
		position = 0
	)
	default boolean soundEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "soundFilePath",
		name = "Sound File Path",
		description = "Full path to a custom WAV file. Leave blank to use the built-in sound. MP3 is not supported.",
		section = "sound",
		position = 1
	)
	default String soundFilePath()
	{
		return "";
	}

	@ConfigItem(
		keyName = "soundVolume",
		name = "Volume (dB)",
		description = "Volume adjustment in decibels. 0 = original level, negative = quieter, positive = louder.",
		section = "sound",
		position = 2
	)
	@Range(min = -40, max = 6)
	default int soundVolume()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "testItemName",
		name = "Item Name or ID",
		description = "Item name (e.g. Dragon bones) or numeric item ID to use for the test popup",
		section = "test",
		position = 0
	)
	default String testItemName()
	{
		return "Dragon bones";
	}

	@ConfigItem(
		keyName = "testTrigger",
		name = "Show Test Popup",
		description = "Toggle on to fire a test popup for the item above (auto-resets so you can toggle again)",
		section = "test",
		position = 1
	)
	default boolean testTrigger()
	{
		return false;
	}
}