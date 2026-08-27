package com.github.ArashCod3r.calculator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

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

	static final double CLAMP_LIMIT = 999_999_999_999_999.0;

	private final JTextField display = new JTextField();
	private final JPanel historyPanel = new JPanel();
	private JScrollPane historyScrollPane;
	private final JCheckBox historyToggle = new JCheckBox("History");
	private final JButton clearHistoryBtn = new JButton("clear");

	private final CalculatorConfig config;
	private final CalculatorHistory history = new CalculatorHistory(50);

	private String currentInput = "0";
	private double storedValue = 0;
	private String pendingOp = "";
	private boolean newEntry = true;

	private final Map<String, Runnable> buttonHandlers = new LinkedHashMap<>();

	public CalculatorPanel(CalculatorConfig config)
	{
		this.config = config;

		setLayout(new BorderLayout(0, 4));
		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		buildHistorySection();
		buildDisplay();
		buildButtonGrid();

		registerKeyHandlers();
		addKeyListener(new CalculatorKeyHandler());
		setFocusable(true);
		setupFocusOnClick(this);
	}

	private void setupFocusOnClick(Container container)
	{
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
		JPanel historyHeader = new JPanel(new BorderLayout());
		historyHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
		historyHeader.add(historyToggle, BorderLayout.WEST);
		historyHeader.add(clearHistoryBtn, BorderLayout.EAST);

		JPanel northPanel = new JPanel(new BorderLayout(0, 4));
		northPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		northPanel.add(historyHeader, BorderLayout.NORTH);
		northPanel.add(historyScrollPane, BorderLayout.CENTER);
		northPanel.add(display, BorderLayout.SOUTH);

		JPanel buttonPanel = new JPanel(new GridBagLayout());
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

		add(northPanel, BorderLayout.NORTH);
		add(buttonPanel, BorderLayout.CENTER);
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
	}

	private void handleDigit(String digit)
	{
		if (newEntry || currentInput.equals("0"))
		{
			currentInput = digit;
			newEntry = false;
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
		else if (!currentInput.contains("."))
		{
			currentInput += ".";
		}
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
			return Double.parseDouble(currentInput);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
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
		if (currentInput.contains("."))
		{
			String[] parts = currentInput.split("\\.");
			String intPart = parts[0];
			String decPart = parts.length == 2 ? parts[1] : "";
			if (intPart.startsWith("-"))
			{
				return "-" + formatWithCommas(intPart.substring(1)) + "." + decPart;
			}
			return formatWithCommas(intPart) + "." + decPart;
		}
		if (currentInput.startsWith("-"))
		{
			return "-" + formatWithCommas(currentInput.substring(1));
		}
		return formatWithCommas(currentInput);
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

	static BufferedImage createIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(ColorScheme.BRAND_ORANGE);
		g.fillRoundRect(1, 1, 14, 14, 4, 4);
		g.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif", Font.BOLD, 10));
		FontMetrics fm = g.getFontMetrics();
		String text = "+";
		int x = (16 - fm.stringWidth(text)) / 2;
		int y = 14 - fm.getDescent();
		g.drawString(text, x, y);
		g.dispose();
		return img;
	}

	@Override
	public void onActivate()
	{
		SwingUtilities.invokeLater(() -> {
			requestFocusInWindow();
			SwingUtilities.invokeLater(this::requestFocusInWindow);
		});
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
				String formatted = CalculatorPanel.formatNumber(entry.getResult());
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
					pendingOp = "";
					newEntry = true;
					currentInput = CalculatorPanel.rawFormat(entry.getResult());
					storedValue = entry.getResult();
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