#!/usr/bin/env python3
"""Generate a Sudoku backup JSON with synthetic game history.

The file uses the same versioned format as the app's JSON export, so it can be
imported through Settings -> Import or copied into the app's files directory on
a debug build.

Examples:
    python3 scripts/generate-history.py
    python3 scripts/generate-history.py --count 250 --seed 7
    python3 scripts/generate-history.py --count 1000 --output /tmp/history.json
"""

from __future__ import annotations

import argparse
import json
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path

SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
DIFFICULTIES = ("easy", "medium", "hard", "expert")
ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = ROOT / "build" / "sudoku-history.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate a Sudoku backup JSON with one game per day.",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=1000,
        help="number of games; each game gets its own day (default: 1000)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f"output JSON file (default: {DEFAULT_OUTPUT})",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=1,
        help="seed for deterministic puzzle layouts (default: 1)",
    )
    parser.add_argument(
        "--start-date",
        help="latest game date in YYYY-MM-DD (default: today, UTC)",
    )
    parser.add_argument(
        "--current-index",
        type=int,
        default=3,
        help="index of the game marked as current; -1 means no current game (default: 3)",
    )
    args = parser.parse_args()

    if args.count <= 0:
        parser.error("--count must be positive")

    return args


def parse_start_date(value: str | None) -> datetime:
    if value is None:
        return datetime.now(timezone.utc).replace(hour=12, minute=0, second=0, microsecond=0)

    try:
        return datetime.strptime(value, "%Y-%m-%d").replace(
            hour=12,
            tzinfo=timezone.utc,
        )
    except ValueError as error:
        raise SystemExit(f"invalid --start-date: {value}") from error


def puzzle_for(index: int, seed: int) -> str:
    digits = list(SOLUTION)
    # Leave a different set of givens for each game, always 36-45 clues.
    blanks = 36 + (index % 10)
    positions = list(range(81))
    random.Random(seed + index).shuffle(positions)

    for position in positions[:blanks]:
        digits[position] = "0"

    return "".join(digits)


def build_game(index: int, start_date: datetime, seed: int) -> dict[str, object]:
    created = start_date - timedelta(days=index)
    created_ms = int(created.timestamp() * 1000)
    seconds = 120 + (index * 37) % 5400
    solved = index % 3 != 0
    show_errors = index % 11 != 0
    errors = (index % 4) if show_errors else 0
    hints = (index // 3) % 4
    puzzle = puzzle_for(index, seed)
    values = SOLUTION if solved else puzzle
    updated_ms = created_ms + seconds * 1000

    return {
        "id": f"test-game-{index:04d}",
        "difficulty": DIFFICULTIES[index % len(DIFFICULTIES)],
        "showErrors": show_errors,
        "errors": errors,
        "puzzle": puzzle,
        "solution": SOLUTION,
        "values": values,
        "notes": {},
        "hintsUsed": hints,
        "elapsedSeconds": seconds,
        "createdAt": created_ms,
        "updatedAt": updated_ms,
        "solvedAt": updated_ms if solved else None,
    }


def validate(games: list[dict[str, object]]) -> None:
    assert len({game["id"] for game in games}) == len(games)

    for game in games:
        assert len(game["puzzle"]) == 81
        assert len(game["solution"]) == 81
        assert len(game["values"]) == 81
        assert set(game["solution"]) <= set("123456789")

        for puzzle_digit, solution_digit, value_digit in zip(
            game["puzzle"],
            game["solution"],
            game["values"],
        ):
            assert puzzle_digit in "0123456789"
            assert solution_digit in "123456789"
            assert value_digit in "0123456789"

            if puzzle_digit != "0":
                assert puzzle_digit == solution_digit
                assert value_digit == puzzle_digit

        assert 0 <= game["errors"]
        assert 0 <= game["hintsUsed"] <= 3
        assert game["showErrors"] or game["errors"] == 0

        if game["solvedAt"] is not None:
            assert game["values"] == game["solution"]


def main() -> None:
    args = parse_args()
    start_date = parse_start_date(args.start_date)
    games = [build_game(index, start_date, args.seed) for index in range(args.count)]

    current_game_id = None
    if 0 <= args.current_index < len(games):
        current_game_id = games[args.current_index]["id"]

    data = {
        "version": 1,
        "currentGameId": current_game_id,
        "games": games,
    }
    validate(games)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )

    print(f"wrote {args.output}")
    print(f"games: {len(games)}")
    print(f"days: {start_date.date()} .. {(start_date - timedelta(days=len(games) - 1)).date()}")
    print(f"current: {current_game_id or 'none'}")


if __name__ == "__main__":
    main()
