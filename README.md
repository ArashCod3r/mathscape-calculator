# Mathscape Calculator

A fully-featured calculator plugin for RuneLite, designed for quick in-game arithmetic without leaving Old School RuneScape.

## Features

- **Full arithmetic support** — addition, subtraction, multiplication, division (evaluated left-to-right)
- **Keyboard input** — type calculations directly when the panel is focused
- **Toggle sign (±)** — switch between positive and negative values
- **k/m/b shorthand suffixes** — type `2.5m` for 2,500,000, `1.2k` for 1,200, or `0.5b` for 500,000,000 anywhere a number can be entered (case-insensitive, works in expressions like `2.5m × 3`)
- **Comma formatting** — large numbers displayed with thousand separators (e.g. 1,000,000), including live formatting while typing
- **Calculation history** — scrollable, newest-first history with click-to-restore and one-click copy; clicking a row while an operator is pending uses it as the right-hand operand (e.g. `5 + <click>`), plus an in-panel toggle and clear button
- **Error handling** — results exceeding ±999,999,999,999,999 or dividing by zero display as "Error" (errors are not added to history)
- **Item Price Search** *(opt-in)* — search OSRS Wiki real-time prices by item name and click a buy, sell, average, or alch price button to enter it into the calculator; disabled by default with a third-party server warning
- **Configurable** — toggle item price search and history visibility, and set max history entries (1–100, default 50)

## Usage

Click the calculator icon in the RuneLite sidebar to open the panel. Calculations can be performed by clicking the on-screen buttons or typing on your keyboard when the panel is active.

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `0-9` | Enter digits |
| `.` | Decimal point |
| `+`, `-`, `*`, `/` | Operators |
| `k`, `m`, `b` | Append shorthand suffix (×1,000 / ×1,000,000 / ×1,000,000,000) |
| `Enter` or `=` | Calculate result |
| `Backspace` | Delete last character |
| `Escape` or `E`/`e` | Clear current entry |
| `Delete` or `C`/`c` | Clear all |
| `_` (Shift + `-`) | Toggle negative/positive |

Click any history entry to restore its result to the calculator input — if an operator is pending, the result is applied as the right-hand operand instead of replacing the input. Click the **copy** button on any history row to copy the result to your clipboard.

## Building

```bash
./gradlew build
```

## Running in development mode

```bash
./gradlew run
```

## License

BSD 2-Clause License. See [LICENSE](LICENSE).