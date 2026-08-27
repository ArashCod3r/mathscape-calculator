# Mathscape Calculator

A fully-featured calculator plugin for RuneLite, designed for quick in-game arithmetic without leaving Old School RuneScape.

## Features

- **Full arithmetic support** — addition, subtraction, multiplication, division
- **Keyboard input** — type calculations directly when the panel is focused
- **Toggle sign (±)** — switch between positive and negative values
- **Comma formatting** — large numbers displayed with thousand separators (e.g. 1,000,000)
- **Calculation history** — scrollable history with click-to-restore and one-click copy
- **Overflow protection** — results exceeding ±999,999,999,999,999 display as "Error"
- **Configurable** — toggle history visibility and set max history entries

## Usage

Click the calculator icon in the RuneLite sidebar to open the panel. Calculations can be performed by clicking the on-screen buttons or typing on your keyboard when the panel is active.

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `0-9` | Enter digits |
| `.` | Decimal point |
| `+`, `-`, `*`, `/` | Operators |
| `Enter` or `=` | Calculate result |
| `Backspace` | Delete last digit |
| `Escape` | Clear current entry |
| `Delete` | Clear all |
| `_` (Shift + `-`) | Toggle negative/positive |

Click any history entry to restore its result to the calculator input. Click the **copy** button on any history row to copy the result to your clipboard.

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