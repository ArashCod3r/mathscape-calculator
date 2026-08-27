package com.github.ArashCod3r.calculator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("runelite-calculator")
public interface CalculatorConfig extends Config
{
	@ConfigItem(
		keyName = "historyEnabled",
		name = "Show History",
		description = "Show calculation history in the calculator panel"
	)
	default boolean historyEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maxHistory",
		name = "Max History Entries",
		description = "Maximum number of history entries to store"
	)
	default int maxHistory()
	{
		return 50;
	}
}