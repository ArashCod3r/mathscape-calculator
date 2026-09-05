package com.github.ArashCod3r.calculator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("runelite-calculator")
public interface CalculatorConfig extends Config
{
	String GROUP = "runelite-calculator";
	String KEY_PRICE_SEARCH = "priceSearchEnabled";

	@ConfigItem(
		keyName = "priceSearchEnabled",
		name = "Item Price Search",
		description = "Search for item prices from the OSRS Wiki and click any price to enter it into the calculator",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean priceSearchEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "historyEnabled",
		name = "Show History",
		description = "Show calculation history in the calculator panel"
	)
	default boolean historyEnabled()
	{
		return true;
	}

	@Range(
		min = 1,
		max = 100
	)
	@ConfigItem(
		keyName = "maxHistory",
		name = "Max History Entries",
		description = "Maximum number of history entries to store (1-100)"
	)
	default int maxHistory()
	{
		return 50;
	}
}