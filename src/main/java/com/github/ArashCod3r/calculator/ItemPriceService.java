package com.github.ArashCod3r.calculator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.QuantityFormatter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches item prices from the OSRS Wiki real-time prices API
 * (<a href="https://prices.runescape.wiki/api/v2/osrs">prices.runescape.wiki</a>).
 * The mapping and latest endpoints are fetched whole and indexed locally to avoid
 * hammering the API with per-item requests.
 */
@Slf4j
@Singleton
public class ItemPriceService
{
	public static class SearchResult
	{
		public final int itemId;
		public final String name;
		public final long high;
		public final long low;
		public final long gePrice;
		public final long highAlch;

		private SearchResult(int itemId, String name, long high, long low, long highAlch)
		{
			this.itemId = itemId;
			this.name = name;
			this.high = high;
			this.low = low;
			this.gePrice = high > 0 && low > 0 ? (high + low) / 2 : (high > 0 ? high : low);
			this.highAlch = highAlch;
		}

		public boolean hasHigh()
		{
			return high > 0;
		}

		public boolean hasLow()
		{
			return low > 0;
		}

		public boolean hasAlch()
		{
			return highAlch > 0;
		}
	}

	private static final String BASE_URL = "https://prices.runescape.wiki/api/v2/osrs";
	private static final String MAPPING_URL = BASE_URL + "/mapping";
	private static final String LATEST_URL = BASE_URL + "/latest";
	private static final String USER_AGENT = "MathscapeCalculator/1.0 (contact: ArashCod3r)";
	private static final long MAPPING_TTL_MS = 60L * 60 * 1000L;
	private static final long PRICES_TTL_MS = 5L * 60 * 1000L;
	static final int MAX_RESULTS = 15;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	private volatile Map<Integer, String> itemNames = Collections.emptyMap();
	private volatile Map<Integer, Long> highByItem = Collections.emptyMap();
	private volatile Map<Integer, Long> lowByItem = Collections.emptyMap();
	private volatile Map<Integer, Integer> highAlchByItem = Collections.emptyMap();
	private volatile long mappingFetchedAt;
	private volatile long pricesFetchedAt;
	private CompletableFuture<Void> mappingFuture;
	private CompletableFuture<Void> pricesFuture;

	/**
	 * Search for items by name and invoke the callback with matching prices.
	 * The callback is invoked on the Swing EDT.
	 */
	public void search(String query, Consumer<List<SearchResult>> callback)
	{
		final String normalized = query.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty())
		{
			SwingUtilities.invokeLater(() -> callback.accept(Collections.emptyList()));
			return;
		}

		CompletableFuture.allOf(ensureMapping(), ensurePrices())
			.whenComplete((v, ex) ->
			{
				if (ex != null)
				{
					log.debug("Failed to load item data for search '{}'", normalized, ex);
				}
				List<SearchResult> results = filter(normalized);
				SwingUtilities.invokeLater(() -> callback.accept(results));
			});
	}

	private CompletableFuture<Void> ensureMapping()
	{
		synchronized (this)
		{
			long now = System.currentTimeMillis();
			if (now - mappingFetchedAt < MAPPING_TTL_MS)
			{
				return CompletableFuture.completedFuture(null);
			}
			if (mappingFuture != null)
			{
				return mappingFuture;
			}

			CompletableFuture<Void> future = fetchMapping();
			future.whenComplete((v, ex) -> {
				synchronized (this)
				{
					mappingFuture = null;
				}
			});
			mappingFuture = future;
			return future;
		}
	}

	private CompletableFuture<Void> ensurePrices()
	{
		synchronized (this)
		{
			long now = System.currentTimeMillis();
			if (now - pricesFetchedAt < PRICES_TTL_MS)
			{
				return CompletableFuture.completedFuture(null);
			}
			if (pricesFuture != null)
			{
				return pricesFuture;
			}

			CompletableFuture<Void> future = fetchPrices();
			future.whenComplete((v, ex) -> {
				synchronized (this)
				{
					pricesFuture = null;
				}
			});
			pricesFuture = future;
			return future;
		}
	}

	private CompletableFuture<Void> fetchMapping()
	{
		CompletableFuture<Void> future = new CompletableFuture<>();
		enqueue(MAPPING_URL, future, body -> {
			JsonArray array = gson.fromJson(body, JsonArray.class);
			Map<Integer, String> names = new LinkedHashMap<>(array.size());
			Map<Integer, Integer> alchs = new LinkedHashMap<>(array.size());
			for (JsonElement element : array)
			{
				JsonObject object = element.getAsJsonObject();
				names.put(object.get("id").getAsInt(), object.get("name").getAsString());
				JsonElement ha = object.get("highalch");
				if (ha != null && !(ha instanceof JsonNull))
				{
					alchs.put(object.get("id").getAsInt(), ha.getAsInt());
				}
			}
			synchronized (this)
			{
				itemNames = names;
				highAlchByItem = alchs;
				mappingFetchedAt = System.currentTimeMillis();
			}
			log.debug("Loaded {} item mappings", names.size());
		});
		return future;
	}

	private CompletableFuture<Void> fetchPrices()
	{
		CompletableFuture<Void> future = new CompletableFuture<>();
		enqueue(LATEST_URL, future, body -> {
			JsonObject root = gson.fromJson(body, JsonObject.class);
			JsonObject data = root.getAsJsonObject("data");
			Map<Integer, Long> high = new HashMap<>(data.size());
			Map<Integer, Long> low = new HashMap<>(data.size());
			for (Map.Entry<String, JsonElement> entry : data.entrySet())
			{
				int id = Integer.parseInt(entry.getKey());
				JsonObject price = entry.getValue().getAsJsonObject();
				JsonElement highValue = price.get("high");
				JsonElement lowValue = price.get("low");
				if (highValue != null && !(highValue instanceof JsonNull))
				{
					high.put(id, highValue.getAsLong());
				}
				if (lowValue != null && !(lowValue instanceof JsonNull))
				{
					low.put(id, lowValue.getAsLong());
				}
			}
			synchronized (this)
			{
				highByItem = high;
				lowByItem = low;
				pricesFetchedAt = System.currentTimeMillis();
			}
			log.debug("Loaded prices for {} items", data.size());
		});
		return future;
	}

	private void enqueue(String url, CompletableFuture<Void> future, Consumer<String> onBody)
	{
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Request to {} failed", url, e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response resp = response)
				{
					if (!resp.isSuccessful())
					{
						future.completeExceptionally(new IOException("HTTP " + resp.code() + " from " + url));
						return;
					}
					String body = resp.body().string();
					onBody.accept(body);
					future.complete(null);
				}
				catch (Exception e)
				{
					log.debug("Failed to handle response from {}", url, e);
					future.completeExceptionally(e);
				}
			}
		});
	}

	private List<SearchResult> filter(String query)
	{
		List<SearchResult> startsWith = new ArrayList<>();
		List<SearchResult> contains = new ArrayList<>();

		for (Map.Entry<Integer, String> entry : itemNames.entrySet())
		{
			String name = entry.getValue();
			String lowerName = name.toLowerCase(Locale.ROOT);
			if (!lowerName.contains(query))
			{
				continue;
			}

			long high = highByItem.getOrDefault(entry.getKey(), -1L);
			long low = lowByItem.getOrDefault(entry.getKey(), -1L);
			int highAlch = highAlchByItem.getOrDefault(entry.getKey(), 0);
			if (high <= 0 && low <= 0 && highAlch <= 0)
			{
				continue;
			}

			SearchResult result = new SearchResult(entry.getKey(), name, high, low, highAlch);
			if (lowerName.startsWith(query))
			{
				startsWith.add(result);
			}
			else
			{
				contains.add(result);
			}
		}

		List<SearchResult> results = new ArrayList<>();
		results.addAll(startsWith);
		results.addAll(contains);
		int limit = Math.min(MAX_RESULTS, results.size());
		return results.subList(0, limit);
	}

	/**
	 * Format a price for display in the compact price buttons.
	 */
	static String formatPrice(long value)
	{
		if (value <= 0)
		{
			return "--";
		}
		if (value >= 1_000_000)
		{
			return QuantityFormatter.quantityToStackSize(value);
		}
		return QuantityFormatter.formatNumber(value);
	}
}