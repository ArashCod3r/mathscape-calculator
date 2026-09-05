package com.github.ArashCod3r.calculator;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

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

	@Inject
	private ItemManager itemManager;

	@Inject
	private ItemPriceService priceService;

	@Inject
	private EventBus eventBus;

	private CalculatorPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		panel = new CalculatorPanel(config, itemManager, priceService);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Mathscape Calculator")
			.icon(icon)
			.priority(100)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		eventBus.register(this);

		log.debug("Calculator started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(this);
		panel.dispose();
		clientToolbar.removeNavigation(navButton);

		log.debug("Calculator stopped!");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(CalculatorConfig.GROUP))
		{
			return;
		}
		if (event.getKey().equals(CalculatorConfig.KEY_PRICE_SEARCH))
		{
			panel.setPriceSearchEnabled(config.priceSearchEnabled());
		}
	}

	@Provides
	CalculatorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CalculatorConfig.class);
	}
}