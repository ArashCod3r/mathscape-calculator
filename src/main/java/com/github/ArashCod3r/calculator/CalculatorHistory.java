package com.github.ArashCod3r.calculator;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class CalculatorHistory
{
	private final List<HistoryEntry> entries = new ArrayList<>();
	private final int maxEntries;

	public CalculatorHistory(int maxEntries)
	{
		this.maxEntries = maxEntries;
	}

	public void addEntry(String expression, double result)
	{
		entries.add(0, new HistoryEntry(expression, result));
		if (entries.size() > maxEntries)
		{
			entries.remove(entries.size() - 1);
		}
	}

	public void clear()
	{
		entries.clear();
	}

	public List<HistoryEntry> getEntries()
	{
		return entries;
	}

	@Getter
	public static class HistoryEntry
	{
		private final String expression;
		private final double result;

		public HistoryEntry(String expression, double result)
		{
			this.expression = expression;
			this.result = result;
		}

		@Override
		public String toString()
		{
			String formattedResult = formatResult(result);
			return expression + " = " + formattedResult;
		}

		private static String formatResult(double value)
		{
			if (Double.isNaN(value) || Double.isInfinite(value))
			{
				return "Error";
			}
			if (value == Math.floor(value) && !Double.isInfinite(value))
			{
				return formatWithCommas(String.valueOf((long) value));
			}
			String s = String.format("%.10f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
			if (s.length() > 15)
			{
				s = String.format("%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
			}
			return formatWithCommas(s);
		}

		private static String formatWithCommas(String number)
		{
			if (number == null || number.isEmpty())
			{
				return number;
			}
			String prefix = "";
			if (number.startsWith("-"))
			{
				prefix = "-";
				number = number.substring(1);
			}
			String[] parts = number.split("\\.");
			String intPart = parts[0];
			StringBuilder sb = new StringBuilder();
			int len = intPart.length();
			for (int i = 0; i < len; i++)
			{
				if (i > 0 && (len - i) % 3 == 0)
				{
					sb.append(",");
				}
				sb.append(intPart.charAt(i));
			}
			if (parts.length == 2)
			{
				sb.append(".").append(parts[1]);
			}
			return prefix + sb.toString();
		}
	}
}