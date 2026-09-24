package com.github.ArashCod3r.calculator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

public class CalculatorPanel extends PluginPanel
{
	static final Color DISPLAY_BG = ColorScheme.DARKER_GRAY_COLOR;
	static final Color DISPLAY_FG = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color HISTORY_BG = ColorScheme.DARKER_GRAY_COLOR;
	static final Color HISTORY_FG = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color BUTTON_BG = ColorScheme.DARKER_GRAY_COLOR;
	static final Color BUTTON_FG = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color OP_BUTTON_BG = ColorScheme.MEDIUM_GRAY_COLOR;
	static final Color OP_BUTTON_FG = Color.WHITE;
	static final Color EQUALS_BUTTON_BG = ColorScheme.BRAND_ORANGE;
	static final Color EQUALS_BUTTON_FG = Color.WHITE;
	static final Color CLEAR_BUTTON_BG = new Color(140, 50, 50);
	static final Color CLEAR_BUTTON_FG = Color.WHITE;
	static final Color HISTORY_TOGGLE_FG = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color HIGH_PRICE_BG = new Color(30, 80, 50);
	static final Color HIGH_PRICE_FG = new Color(100, 220, 130);
	static final Color LOW_PRICE_BG = new Color(80, 40, 40);
	static final Color LOW_PRICE_FG = new Color(220, 110, 110);
	static final Color GE_PRICE_BG = new Color(40, 55, 80);
	static final Color GE_PRICE_FG = new Color(130, 180, 240);
	static final Color ALCH_PRICE_BG = new Color(90, 75, 30);
	static final Color ALCH_PRICE_FG = new Color(255, 215, 0);

	private static final String SEARCH_FONT_NAME = "Arial";
	static final Font SEARCH_FONT = new Font(SEARCH_FONT_NAME, Font.PLAIN, 13);
	static final Font SEARCH_NAME_FONT = new Font(SEARCH_FONT_NAME, Font.PLAIN, 12);
	static final Font SEARCH_BUTTON_FONT = new Font(SEARCH_FONT_NAME, Font.PLAIN, 11);

	static final double CLAMP_LIMIT = 999_999_999_999_999.0;

	private final JTextField display = new JTextField();
	private final JPanel historyPanel = new JPanel();
	private JScrollPane historyScrollPane;
	private final JCheckBox historyToggle = new JCheckBox("History");
	private final JButton clearHistoryBtn = new JButton("clear");

	private final CalculatorConfig config;
	private final CalculatorHistory history;
	private final ItemManager itemManager;
	private final ItemPriceService priceService;

	private String currentInput = "0";
	private double storedValue = 0;
	private String pendingOp = "";
	private boolean newEntry = true;

	private final Map<String, Runnable> buttonHandlers = new LinkedHashMap<>();

	private JTextField searchField;
	private JPanel searchResultsPanel;
	private JScrollPane searchScrollPane;
	private JPanel priceSearchContainer;
	private JPanel buttonPanel;
	private int searchSeq;
	private final Timer searchDebounce = new Timer(300, e -> doSearch());

	public CalculatorPanel(CalculatorConfig config, ItemManager itemManager, ItemPriceService priceService)
	{
		super(false);
		this.config = config;
		this.itemManager = itemManager;
		this.priceService = priceService;
		this.history = new CalculatorHistory(config.maxHistory());

		setLayout(new BorderLayout(0, 4));
		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		buildHistorySection();
		buildDisplay();
		buildButtonGrid();
		buildSearchSection();

		JPanel historyHeader = new JPanel(new BorderLayout());
		historyHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
		historyHeader.add(historyToggle, BorderLayout.WEST);
		historyHeader.add(clearHistoryBtn, BorderLayout.EAST);

		JPanel calcSection = new JPanel(new BorderLayout(0, 4));
		calcSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		calcSection.add(historyHeader, BorderLayout.NORTH);
		calcSection.add(historyScrollPane, BorderLayout.CENTER);
		JPanel displayAndButtons = new JPanel(new BorderLayout(0, 4));
		displayAndButtons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		displayAndButtons.add(display, BorderLayout.NORTH);
		displayAndButtons.add(buttonPanel, BorderLayout.CENTER);
		calcSection.add(displayAndButtons, BorderLayout.SOUTH);

		add(calcSection, BorderLayout.NORTH);
		add(priceSearchContainer, BorderLayout.CENTER);
		registerKeyHandlers();
		addKeyListener(new CalculatorKeyHandler());
		setFocusable(true);
		setupFocusOnClick(this);
	}

	private void setupFocusOnClick(Container container)
	{
		if (container == priceSearchContainer)
		{
			return;
		}
		container.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				requestFocusInWindow();
			}
		});
		for (Component comp : container.getComponents())
		{
			if (comp instanceof Container)
			{
				setupFocusOnClick((Container) comp);
			}
			else
			{
				comp.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(MouseEvent e)
					{
						requestFocusInWindow();
					}
				});
			}
		}
	}

	private void buildHistorySection()
	{
		historyToggle.setSelected(config.historyEnabled());
		historyToggle.setForeground(HISTORY_TOGGLE_FG);
		historyToggle.setBackground(ColorScheme.DARK_GRAY_COLOR);
		historyToggle.setFont(historyToggle.getFont().deriveFont(Font.BOLD, 12f));
		historyToggle.setIconTextGap(6);
		historyToggle.setFocusPainted(false);
		historyToggle.addActionListener(e -> {
			boolean visible = historyToggle.isSelected();
			historyScrollPane.setVisible(visible);
			clearHistoryBtn.setVisible(visible);
			revalidate();
			refocus();
		});

		clearHistoryBtn.setFont(clearHistoryBtn.getFont().deriveFont(Font.BOLD, 11f));
		clearHistoryBtn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearHistoryBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		clearHistoryBtn.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
		clearHistoryBtn.setFocusPainted(false);
		clearHistoryBtn.setToolTipText("Clear history");
		clearHistoryBtn.addActionListener(e -> {
			history.clear();
			historyPanel.removeAll();
			historyPanel.revalidate();
			historyPanel.repaint();
			refocus();
		});

		historyPanel.setLayout(new javax.swing.BoxLayout(historyPanel, javax.swing.BoxLayout.Y_AXIS));
		historyPanel.setBackground(HISTORY_BG);

		historyScrollPane = new JScrollPane(historyPanel);
		historyScrollPane.setPreferredSize(new Dimension(PANEL_WIDTH - 12, 120));
		historyScrollPane.setVisible(config.historyEnabled());
		historyScrollPane.setBackground(HISTORY_BG);
		historyScrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));
	}

	private void buildDisplay()
	{
		display.setPreferredSize(new Dimension(PANEL_WIDTH - 12, 45));
		display.setFont(display.getFont().deriveFont(Font.BOLD, 24f));
		display.setHorizontalAlignment(JTextField.RIGHT);
		display.setEditable(false);
		display.setBackground(DISPLAY_BG);
		display.setForeground(DISPLAY_FG);
		display.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			new EmptyBorder(5, 10, 5, 10)
		));
		display.setText("0");
		display.setFocusable(false);
	}

	private void buildButtonGrid()
	{
		buttonPanel = new JPanel(new GridBagLayout());
		buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(2, 2, 2, 2);

		JButton clearBtn = createStyledButton("C", CLEAR_BUTTON_BG, CLEAR_BUTTON_FG, 15f);
		clearBtn.addActionListener(e -> handleClear());
		gbc.gridx = 0;
		gbc.gridy = 0;
		buttonPanel.add(clearBtn, gbc);

		JButton clearEntryBtn = createStyledButton("CE", CLEAR_BUTTON_BG, CLEAR_BUTTON_FG, 15f);
		clearEntryBtn.addActionListener(e -> handleClearEntry());
		gbc.gridx = 1;
		buttonPanel.add(clearEntryBtn, gbc);

		JButton backspaceBtn = createStyledButton("\u232B", CLEAR_BUTTON_BG, CLEAR_BUTTON_FG, 15f);
		backspaceBtn.addActionListener(e -> handleBackspace());
		gbc.gridx = 2;
		buttonPanel.add(backspaceBtn, gbc);

		JButton divideBtn = createStyledButton("\u00F7", OP_BUTTON_BG, OP_BUTTON_FG, 17f);
		divideBtn.addActionListener(e -> handleOperator("\u00F7"));
		gbc.gridx = 3;
		buttonPanel.add(divideBtn, gbc);

		String[][] numButtons = {
			{"7", "8", "9"},
			{"4", "5", "6"},
			{"1", "2", "3"},
		};

		String[] ops = {"\u00D7", "-", "+"};

		for (int row = 0; row < numButtons.length; row++)
		{
			for (int col = 0; col < numButtons[row].length; col++)
			{
				JButton btn = createStyledButton(numButtons[row][col], BUTTON_BG, BUTTON_FG, 17f);
				String val = numButtons[row][col];
				btn.addActionListener(e -> handleDigit(val));
				gbc.gridx = col;
				gbc.gridy = row + 1;
				buttonPanel.add(btn, gbc);
			}

			String opText = ops[row];
			JButton opBtn = createStyledButton(opText, OP_BUTTON_BG, OP_BUTTON_FG, 17f);
			opBtn.addActionListener(e -> handleOperator(opText));
			gbc.gridx = 3;
			gbc.gridy = row + 1;
			buttonPanel.add(opBtn, gbc);
		}

		JButton negateBtn = createStyledButton("\u00B1", BUTTON_BG, BUTTON_FG, 17f);
		negateBtn.addActionListener(e -> handleNegate());
		gbc.gridx = 0;
		gbc.gridy = 4;
		buttonPanel.add(negateBtn, gbc);

		JButton zeroBtn = createStyledButton("0", BUTTON_BG, BUTTON_FG, 17f);
		zeroBtn.addActionListener(e -> handleDigit("0"));
		gbc.gridx = 1;
		gbc.gridy = 4;
		buttonPanel.add(zeroBtn, gbc);

		JButton decimalBtn = createStyledButton(".", BUTTON_BG, BUTTON_FG, 19f);
		decimalBtn.addActionListener(e -> handleDecimal());
		gbc.gridx = 2;
		gbc.gridy = 4;
		buttonPanel.add(decimalBtn, gbc);

		JButton equalsBtn = createStyledButton("=", EQUALS_BUTTON_BG, EQUALS_BUTTON_FG, 19f);
		equalsBtn.addActionListener(e -> handleEquals());
		gbc.gridx = 3;
		gbc.gridy = 4;
		buttonPanel.add(equalsBtn, gbc);

		buildSearchSection();
	}

	private void buildSearchSection()
	{
		searchField = new JTextField()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (getText().isEmpty())
				{
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
					g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
					g2.drawString("Search for an item...", 8, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
					g2.dispose();
				}
			}
		};
		searchField.setFont(SEARCH_FONT);
		searchField.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		searchField.setToolTipText("Search item prices by name");
		searchField.addActionListener(e -> doSearch());
		searchDebounce.setRepeats(false);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchDebounce.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchDebounce.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchDebounce.restart();
			}
		});

		searchResultsPanel = new JPanel();
		searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
		searchResultsPanel.setBackground(HISTORY_BG);

		searchScrollPane = new JScrollPane(searchResultsPanel);
		searchScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		searchScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		searchScrollPane.setBackground(HISTORY_BG);
		searchScrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

		priceSearchContainer = new JPanel(new BorderLayout(0, 4));
		priceSearchContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		priceSearchContainer.add(searchField, BorderLayout.NORTH);
		priceSearchContainer.add(searchScrollPane, BorderLayout.CENTER);
		priceSearchContainer.setVisible(config.priceSearchEnabled());
	}

	private void doSearch()
	{
		final String query = searchField.getText();
		searchSeq++;
		final int seq = searchSeq;
		if (query.trim().isEmpty())
		{
			renderSearchResults(null);
			return;
		}
		priceService.search(query, results ->
		{
			if (seq == searchSeq)
			{
				renderSearchResults(results);
			}
		});
	}

	private void renderSearchResults(List<ItemPriceService.SearchResult> results)
	{
		searchResultsPanel.removeAll();
		if (results != null && results.isEmpty())
		{
			JLabel noResults = new JLabel("No items found");
			noResults.setHorizontalAlignment(SwingConstants.CENTER);
			noResults.setForeground(HISTORY_FG);
			noResults.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
			searchResultsPanel.add(noResults);
		}
		else if (results != null)
		{
			for (int i = 0; i < results.size(); i++)
			{
				if (i > 0)
				{
					searchResultsPanel.add(Box.createVerticalStrut(6));
				}
				searchResultsPanel.add(new SearchResultRow(results.get(i)));
			}
		}
		searchResultsPanel.revalidate();
		searchResultsPanel.repaint();
	}

	void setPriceSearchEnabled(boolean enabled)
	{
		if (priceSearchContainer != null)
		{
			priceSearchContainer.setVisible(enabled);
			revalidate();
		}
	}

	void dispose()
	{
		searchDebounce.stop();
	}

	private JButton createStyledButton(String text, Color bg, Color fg, float fontSize)
	{
		JButton btn = new JButton(text);
		btn.setFont(btn.getFont().deriveFont(Font.BOLD, fontSize));
		btn.setBackground(bg);
		btn.setForeground(fg);
		btn.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(bg.darker()),
			new EmptyBorder(8, 4, 8, 4)
		));
		btn.setFocusPainted(false);
		btn.setRolloverEnabled(true);
		return btn;
	}

	private void registerKeyHandlers()
	{
		buttonHandlers.put("0", () -> handleDigit("0"));
		buttonHandlers.put("1", () -> handleDigit("1"));
		buttonHandlers.put("2", () -> handleDigit("2"));
		buttonHandlers.put("3", () -> handleDigit("3"));
		buttonHandlers.put("4", () -> handleDigit("4"));
		buttonHandlers.put("5", () -> handleDigit("5"));
		buttonHandlers.put("6", () -> handleDigit("6"));
		buttonHandlers.put("7", () -> handleDigit("7"));
		buttonHandlers.put("8", () -> handleDigit("8"));
		buttonHandlers.put("9", () -> handleDigit("9"));
		buttonHandlers.put(".", () -> handleDecimal());
		buttonHandlers.put("+", () -> handleOperator("+"));
		buttonHandlers.put("-", () -> handleOperator("-"));
		buttonHandlers.put("*", () -> handleOperator("\u00D7"));
		buttonHandlers.put("/", () -> handleOperator("\u00F7"));
		buttonHandlers.put("=", () -> handleEquals());
		buttonHandlers.put("\n", () -> handleEquals());
		buttonHandlers.put("\r", () -> handleEquals());
		buttonHandlers.put("C", () -> handleClear());
		buttonHandlers.put("c", () -> handleClear());
		buttonHandlers.put("E", () -> handleClearEntry());
		buttonHandlers.put("e", () -> handleClearEntry());
		buttonHandlers.put("_", () -> handleNegate());
		buttonHandlers.put("k", () -> handleSuffix('k'));
		buttonHandlers.put("K", () -> handleSuffix('k'));
		buttonHandlers.put("m", () -> handleSuffix('m'));
		buttonHandlers.put("M", () -> handleSuffix('m'));
		buttonHandlers.put("b", () -> handleSuffix('b'));
		buttonHandlers.put("B", () -> handleSuffix('b'));
	}

	private void handleDigit(String digit)
	{
		if (newEntry || currentInput.equals("0"))
		{
			currentInput = digit;
			newEntry = false;
		}
		else if (hasSuffix(currentInput))
		{
			refocus();
			return;
		}
		else
		{
			if (currentInput.length() < 15)
			{
				currentInput += digit;
			}
		}
		updateDisplay();
		refocus();
	}

	private void handleDecimal()
	{
		if (newEntry || currentInput.equals("0"))
		{
			currentInput = "0.";
			newEntry = false;
		}
		else if (hasSuffix(currentInput))
		{
			refocus();
			return;
		}
		else if (!currentInput.contains("."))
		{
			currentInput += ".";
		}
		updateDisplay();
		refocus();
	}

	private void handleSuffix(char suffix)
	{
		if (newEntry || currentInput == null || currentInput.isEmpty() || currentInput.equals("Error"))
		{
			refocus();
			return;
		}
		String base = currentInput;
		if (hasSuffix(base))
		{
			base = base.substring(0, base.length() - 1);
		}
		if (base.isEmpty())
		{
			refocus();
			return;
		}
		currentInput = base + Character.toLowerCase(suffix);
		updateDisplay();
		refocus();
	}

	private void handleNegate()
	{
		double val = parseInput();
		double negated = clamp(-val);
		currentInput = rawFormat(negated);
		if (newEntry)
		{
			storedValue = negated;
		}
		updateDisplay();
		refocus();
	}

	private void handleOperator(String op)
	{
		if (!pendingOp.isEmpty() && !newEntry)
		{
			computeAndStore();
		}
		else if (newEntry && !pendingOp.isEmpty())
		{

		}
		else
		{
			storedValue = clamp(parseInput());
		}

		pendingOp = op;
		newEntry = true;
		display.setText(formatNumber(storedValue) + " " + op);
		refocus();
	}

	void enterPrice(long value)
	{
		currentInput = String.valueOf(value);
		if (pendingOp.isEmpty())
		{
			storedValue = clamp(value);
			newEntry = true;
		}
		else
		{
			newEntry = false;
		}
		updateDisplay();
		refocus();
	}

	private void handleEquals()
	{
		if (pendingOp.isEmpty() && newEntry)
		{
			refocus();
			return;
		}

		String left = formatNumber(storedValue);
		String op = pendingOp;
		String right = newEntry ? formatNumber(storedValue) : formatNumber(parseInput());

		if (pendingOp.isEmpty())
		{
			refocus();
			return;
		}

		computeAndStore();

		String expr = left + " " + op + " " + right;
		display.setText(formatNumber(storedValue));

		if (!Double.isNaN(storedValue))
		{
			history.addEntry(left + " " + op + " " + right, storedValue);
			HistoryRow row = new HistoryRow(history.getEntries().get(0));
			historyPanel.add(row, 0);

			if (historyToggle.isSelected())
			{
				historyPanel.revalidate();
				historyPanel.repaint();
				historyScrollPane.getVerticalScrollBar().setValue(0);
			}
		}

		pendingOp = "";
		currentInput = rawFormat(storedValue);
		newEntry = true;
		refocus();
	}

	private void handleClear()
	{
		currentInput = "0";
		storedValue = 0;
		pendingOp = "";
		newEntry = true;
		display.setText("0");
		refocus();
	}

	private void handleClearEntry()
	{
		currentInput = "0";
		newEntry = true;
		updateDisplay();
		refocus();
	}

	private void handleBackspace()
	{
		if (newEntry || currentInput.length() <= 1)
		{
			currentInput = "0";
			newEntry = true;
		}
		else
		{
			currentInput = currentInput.substring(0, currentInput.length() - 1);
		}
		updateDisplay();
		refocus();
	}

	private void computeAndStore()
	{
		double rightOperand = parseInput();
		double result;

		switch (pendingOp)
		{
			case "+":
				result = storedValue + rightOperand;
				break;
			case "-":
				result = storedValue - rightOperand;
				break;
			case "\u00D7":
				result = storedValue * rightOperand;
				break;
			case "\u00F7":
				result = rightOperand != 0 ? storedValue / rightOperand : Double.NaN;
				break;
			default:
				result = rightOperand;
		}

		result = clamp(result);
		storedValue = result;
		currentInput = rawFormat(storedValue);
		newEntry = true;
	}

	static double clamp(double value)
	{
		if (Double.isNaN(value) || Double.isInfinite(value))
		{
			return Double.NaN;
		}
		if (value > CLAMP_LIMIT || value < -CLAMP_LIMIT)
		{
			return Double.NaN;
		}
		return value;
	}

	private double parseInput()
	{
		try
		{
			if (hasSuffix(currentInput))
			{
				String number = currentInput.substring(0, currentInput.length() - 1);
				if (!number.isEmpty())
				{
					return clamp(Double.parseDouble(number) * suffixMultiplier(currentInput.charAt(currentInput.length() - 1)));
				}
			}
			return Double.parseDouble(currentInput);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	static double suffixMultiplier(char c)
	{
		switch (Character.toLowerCase(c))
		{
			case 'k':
				return 1_000d;
			case 'm':
				return 1_000_000d;
			case 'b':
				return 1_000_000_000d;
			default:
				return 0;
		}
	}

	static boolean hasSuffix(String s)
	{
		return s != null && !s.isEmpty() && suffixMultiplier(s.charAt(s.length() - 1)) > 0;
	}

	private void updateDisplay()
	{
		display.setText(formatCurrentInput());
	}

	private String formatCurrentInput()
	{
		if (currentInput == null || currentInput.isEmpty() || currentInput.equals("Error"))
		{
			return currentInput;
		}
		String suffix = "";
		String number = currentInput;
		if (hasSuffix(number))
		{
			suffix = number.substring(number.length() - 1);
			number = number.substring(0, number.length() - 1);
		}
		if (number.isEmpty())
		{
			return suffix;
		}
		if (number.contains("."))
		{
			String[] parts = number.split("\\.");
			String intPart = parts[0];
			String decPart = parts.length == 2 ? parts[1] : "";
			if (intPart.startsWith("-"))
			{
				return "-" + formatWithCommas(intPart.substring(1)) + "." + decPart + suffix;
			}
			return formatWithCommas(intPart) + "." + decPart + suffix;
		}
		if (number.startsWith("-"))
		{
			return "-" + formatWithCommas(number.substring(1)) + suffix;
		}
		return formatWithCommas(number) + suffix;
	}

	private void refocus()
	{
		SwingUtilities.invokeLater(this::requestFocusInWindow);
	}

	static String rawFormat(double value)
	{
		if (Double.isNaN(value) || Double.isInfinite(value))
		{
			return "Error";
		}
		if (value == Math.floor(value) && !Double.isInfinite(value))
		{
			return String.valueOf((long) value);
		}
		String s = String.format("%.10f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
		if (s.length() > 15)
		{
			s = String.format("%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return s;
	}

	static String formatNumber(double value)
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

	static String formatWithCommas(String number)
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

	@Override
	public void onActivate()
	{
		SwingUtilities.invokeLater(() -> {
			requestFocusInWindow();
			SwingUtilities.invokeLater(this::requestFocusInWindow);
		});
	}

	private class SearchResultRow extends JPanel
	{
		private static final int IMAGE_SIZE = 34;
		private static final int ROW_HEIGHT = 72;
		private static final int MAX_NAME_LEN = 20;

		SearchResultRow(ItemPriceService.SearchResult result)
		{
			setLayout(new BorderLayout(4, 4));
			setBackground(HISTORY_BG);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARKER_GRAY_COLOR.brighter()),
				BorderFactory.createEmptyBorder(4, 4, 4, 4)
			));
			setPreferredSize(new Dimension(PANEL_WIDTH - 12, ROW_HEIGHT));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
			setMinimumSize(new Dimension(PANEL_WIDTH - 12, ROW_HEIGHT));

			JLabel iconLabel = new JLabel();
			iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
			iconLabel.setVerticalAlignment(SwingConstants.CENTER);
			iconLabel.setPreferredSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
			AsyncBufferedImage image = itemManager.getImage(result.itemId);
			if (image != null)
			{
				image.addTo(iconLabel);
			}

			JLabel nameLabel = new JLabel(truncateName(result.name, MAX_NAME_LEN));
			nameLabel.setFont(SEARCH_NAME_FONT);
			nameLabel.setForeground(Color.WHITE);
			if (result.name.length() > MAX_NAME_LEN)
			{
				nameLabel.setToolTipText(result.name);
			}

			JPanel header = new JPanel(new BorderLayout(6, 0));
			header.setBackground(HISTORY_BG);
			header.add(iconLabel, BorderLayout.WEST);
			header.add(nameLabel, BorderLayout.CENTER);

			JPanel priceRow = new JPanel(new GridLayout(1, 4, 2, 0));
			priceRow.setBackground(HISTORY_BG);
			priceRow.add(new PriceButton(result.high, result.hasHigh(), HIGH_PRICE_BG, "buy", result));
			priceRow.add(new PriceButton(result.low, result.hasLow(), LOW_PRICE_BG, "sell", result));
			priceRow.add(new PriceButton(result.gePrice, true, GE_PRICE_BG, "average", result));
			priceRow.add(new PriceButton(result.highAlch, result.hasAlch(), ALCH_PRICE_BG, "alch", result));

			add(header, BorderLayout.NORTH);
			add(priceRow, BorderLayout.SOUTH);
		}

		private String truncateName(String name, int maxLen)
		{
			return name.length() > maxLen ? name.substring(0, maxLen - 1) + "\u2026" : name;
		}
	}

	private class PriceButton extends JButton
	{
		private final long value;
		private final Color bg;
		private final Color hoverBg;
		private boolean hovering;

		PriceButton(long value, boolean enabled, Color bg, String typeLabel, ItemPriceService.SearchResult result)
		{
			this.value = value;
			this.bg = bg;
			this.hoverBg = bg.brighter();

			setFocusPainted(false);
			setFocusable(false);
			setRolloverEnabled(false);
			setEnabled(enabled);
			setBackground(bg);
setForeground(Color.WHITE);
setFont(SEARCH_BUTTON_FONT);
			setPreferredSize(new Dimension(0, 24));
			setMinimumSize(new Dimension(0, 24));
			setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			setToolTipText("Enter " + typeLabel + " price of " + result.name + " (" + ItemPriceService.formatPrice(value) + ")");
			addActionListener(e -> enterPrice(value));

			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hovering = true;
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hovering = false;
					setCursor(Cursor.getDefaultCursor());
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int w = getWidth();
			int h = getHeight();

			Color fillBg;
			if (!isEnabled())
			{
				fillBg = bg.darker().darker();
			}
			else if (hovering)
			{
				fillBg = bg.brighter();
			}
			else
			{
				fillBg = bg;
			}
			g2.setColor(fillBg);
			g2.fillRect(0, 0, w, h);
			g2.setColor(bg.darker());
			g2.drawRect(0, 0, w - 1, h - 1);

			g2.setColor(Color.WHITE);
			g2.setFont(SEARCH_BUTTON_FONT);
			FontMetrics fm = g2.getFontMetrics();
			String priceText = ItemPriceService.formatPrice(value);

			g2.drawString(priceText, (w - fm.stringWidth(priceText)) / 2, (h + fm.getAscent()) / 2 - 1);

			if (!isEnabled())
			{
				g2.setColor(new Color(0, 0, 0, 80));
				g2.fillRect(0, 0, w, h);
			}

			g2.dispose();
		}
	}

	private class HistoryRow extends JPanel
	{
		private final CalculatorHistory.HistoryEntry entry;
		private final JLabel label;
		private final JButton copyBtn;
		private boolean highlighted;

		private static final int ROW_HEIGHT = 26;
		private static final int COPY_BTN_WIDTH = 45;

		HistoryRow(CalculatorHistory.HistoryEntry entry)
		{
			this.entry = entry;
			setLayout(new BorderLayout(4, 0));
			setBackground(HISTORY_BG);
			setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			setPreferredSize(new Dimension(PANEL_WIDTH - 12, ROW_HEIGHT));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
			setMinimumSize(new Dimension(PANEL_WIDTH - 12, ROW_HEIGHT));

			label = new JLabel(entry.toString());
			label.setFont(label.getFont().deriveFont(14f));
			label.setForeground(HISTORY_FG);
			label.setOpaque(true);
			label.setBackground(HISTORY_BG);

			copyBtn = new JButton("copy");
			copyBtn.setFont(copyBtn.getFont().deriveFont(Font.BOLD, 11f));
			copyBtn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			copyBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			copyBtn.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
			copyBtn.setFocusPainted(false);
			copyBtn.setPreferredSize(new Dimension(COPY_BTN_WIDTH, ROW_HEIGHT));
			copyBtn.setMaximumSize(new Dimension(COPY_BTN_WIDTH, ROW_HEIGHT));
			copyBtn.setMinimumSize(new Dimension(COPY_BTN_WIDTH, ROW_HEIGHT));
			copyBtn.addActionListener(e -> {
				String formatted = CalculatorPanel.rawFormat(entry.getResult());
				StringSelection selection = new StringSelection(formatted);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
				refocus();
			});

			MouseAdapter highlightAdapter = new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					setHighlighted(true);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					setHighlighted(false);
				}

				@Override
				public void mouseClicked(MouseEvent e)
				{
					currentInput = CalculatorPanel.rawFormat(entry.getResult());
					if (pendingOp.isEmpty())
					{
						storedValue = entry.getResult();
						newEntry = true;
					}
					else
					{
						newEntry = false;
					}
					updateDisplay();
					setHighlighted(true);
					refocus();
				}
			};

			MouseAdapter copyHighlightAdapter = new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					setHighlighted(true);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					setHighlighted(false);
				}
			};

			addMouseListener(highlightAdapter);
			label.addMouseListener(highlightAdapter);
			copyBtn.addMouseListener(copyHighlightAdapter);

			add(label, BorderLayout.CENTER);
			add(copyBtn, BorderLayout.EAST);
		}

		private void setHighlighted(boolean highlighted)
		{
			this.highlighted = highlighted;
			Color bg = highlighted ? ColorScheme.DARKER_GRAY_COLOR.brighter() : HISTORY_BG;
			setBackground(bg);
			label.setBackground(bg);
			repaint();
		}
	}

	private class CalculatorKeyHandler extends KeyAdapter
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
			Runnable handler = buttonHandlers.get(String.valueOf(e.getKeyChar()));
			if (handler != null)
			{
				handler.run();
			}
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_BACK_SPACE:
					handleBackspace();
					break;
				case KeyEvent.VK_ENTER:
					handleEquals();
					break;
				case KeyEvent.VK_ESCAPE:
					handleClearEntry();
					break;
				case KeyEvent.VK_DELETE:
					handleClear();
					break;
			}
		}
	}
}