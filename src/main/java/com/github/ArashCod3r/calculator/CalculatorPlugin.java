package com.github.ArashCod3r.calculator;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Mathscape Calculator",
	description = "A simple calculator for quick in-game math with keyboard support",
	tags = {"math", "calculate", "numbers", "calculator"}
)
public class CalculatorPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private CalculatorConfig config;

	private CalculatorPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		panel = new CalculatorPanel(config);

		navButton = NavigationButton.builder()
			.tooltip("Mathscape Calculator")
			.icon(CalculatorPanel.createIcon())
			.priority(100)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		log.debug("Calculator started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);

		log.debug("Calculator stopped!");
	}

	@Provides
	CalculatorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CalculatorConfig.class);
	}
}