#!/usr/bin/env python3
"""Generate deterministic screenshot data for the Sudoku app.

The script writes two files:

* ``sudoku_data.json`` — a current game plus solved and unfinished games;
* ``sudoku.xml`` — the matching global settings.

The data is used only for store screenshots. Timestamps are relative to the
moment the script runs, so the history in the screenshot always looks recent.

Usage::

    python3 scripts/screenshot-data.py --out /tmp/sudoku-screenshots

Options ``--straight-grid``, ``--dark`` and ``--no-fast-input`` change the
generated settings file for screenshots that need another state.
"""

from __future__ import annotations

import argparse
import json
import random
from datetime import datetime, timedelta
from pathlib import Path

CELLS = 81
SEED = 20261113


def can_place(grid: list[int], index: int, digit: int) -> bool:
    row, col = divmod(index, 9)

    for offset in range(9):
        if grid[row * 9 + offset] == digit or grid[offset * 9 + col] == digit:
            return False

    box_row, box_col = (row // 3) * 3, (col // 3) * 3
    for r in range(box_row, box_row + 3):
        for c in range(box_col, box_col + 3):
            if grid[r * 9 + c] == digit:
                return False

    return True


def fill(grid: list[int], rng: random.Random) -> bool:
    for index in range(CELLS):
        if grid[index] != 0:
            continue

        digits = list(range(1, 10))
        rng.shuffle(digits)

        for digit in digits:
            if can_place(grid, index, digit):
                grid[index] = digit
                if fill(grid, rng):
                    return True
                grid[index] = 0

        return False

    return True


def count_solutions(grid: list[int], limit: int = 2) -> int:
    best_index = None
    best_candidates: list[int] | None = None

    for index in range(CELLS):
        if grid[index] != 0:
            continue

        candidates = [digit for digit in range(1, 10) if can_place(grid, index, digit)]
        if not candidates:
            return 0
        if best_candidates is None or len(candidates) < len(best_candidates):
            best_index = index
            best_candidates = candidates
            if len(candidates) == 1:
                break

    if best_index is None:
        return 1

    total = 0
    for digit in best_candidates or []:
        grid[best_index] = digit
        total += count_solutions(grid, limit)
        grid[best_index] = 0
        if total >= limit:
            break

    return total


def make_puzzle(clues: int, rng: random.Random) -> tuple[list[int], list[int]]:
    solution = [0] * CELLS
    fill(solution, rng)

    puzzle = solution[:]
    cells = list(range(CELLS))
    rng.shuffle(cells)
    removed = 0

    for index in cells:
        if removed >= CELLS - clues:
            break

        saved = puzzle[index]
        puzzle[index] = 0
        if count_solutions(puzzle[:]) != 1:
            puzzle[index] = saved
        else:
            removed += 1

    return puzzle, solution


def as_text(values: list[int]) -> str:
    return "".join(str(value) for value in values)


def candidates_for(values: list[int], index: int) -> list[int]:
    row, col = divmod(index, 9)
    used = set(values[row * 9:(row + 1) * 9])
    used.update(values[offset * 9 + col] for offset in range(9))

    box_row, box_col = (row // 3) * 3, (col // 3) * 3
    used.update(
        values[r * 9 + c]
        for r in range(box_row, box_row + 3)
        for c in range(box_col, box_col + 3)
    )

    return [digit for digit in range(1, 10) if digit not in used]


def partially_filled(
    puzzle: list[int],
    solution: list[int],
    empty_count: int,
    rng: random.Random,
) -> list[int]:
    values = solution[:]
    empty_cells = [index for index, value in enumerate(puzzle) if value == 0]
    rng.shuffle(empty_cells)

    for index in empty_cells[:empty_count]:
        values[index] = 0

    return values


def add_notes(values: list[int], note_cells: int) -> dict[str, list[int]]:
    empty_cells = [index for index, value in enumerate(values) if value == 0]
    by_candidates = sorted(
        empty_cells,
        key=lambda index: len(candidates_for(values, index)),
        reverse=True,
    )

    notes: dict[str, list[int]] = {}
    for index in by_candidates[:note_cells]:
        candidates = candidates_for(values, index)
        if len(candidates) >= 2:
            notes[str(index)] = candidates[:3]

    return notes


def game(
    game_id: str,
    difficulty: str,
    puzzle: list[int],
    solution: list[int],
    *,
    show_errors: bool,
    errors: int,
    hints: int,
    elapsed: int,
    created: datetime,
    updated: datetime,
    solved: datetime | None = None,
    values: list[int] | None = None,
    notes: dict[str, list[int]] | None = None,
) -> dict[str, object]:
    return {
        "id": game_id,
        "difficulty": difficulty,
        "showErrors": show_errors,
        "errors": errors,
        "puzzle": as_text(puzzle),
        "solution": as_text(solution),
        "values": as_text(values if values is not None else solution),
        "notes": notes or {},
        "hintsUsed": hints,
        "elapsedSeconds": elapsed,
        "createdAt": int(created.timestamp() * 1000),
        "updatedAt": int(updated.timestamp() * 1000),
        "solvedAt": int(solved.timestamp() * 1000) if solved is not None else None,
    }


def build_data() -> dict[str, object]:
    rng = random.Random(SEED)
    puzzles = {
        name: make_puzzle(clues, rng)
        for name, clues in (
            ("easy", 45),
            ("medium", 36),
            ("hard", 31),
            ("expert", 26),
        )
    }

    expert_puzzle, expert_solution = puzzles["expert"]
    expert_values = partially_filled(expert_puzzle, expert_solution, 24, rng)
    expert_notes = add_notes(expert_values, 3)

    hard_puzzle, hard_solution = puzzles["hard"]
    hard_values = partially_filled(hard_puzzle, hard_solution, 34, rng)

    medium_puzzle, medium_solution = puzzles["medium"]
    easy_puzzle, easy_solution = puzzles["easy"]

    now = datetime.now().replace(microsecond=0)
    current_created = now - timedelta(minutes=45)
    current_updated = now - timedelta(minutes=27)

    games = [
        game(
            "a1000000-0000-4000-8000-000000000001",
            "expert",
            expert_puzzle,
            expert_solution,
            show_errors=True,
            errors=2,
            hints=1,
            elapsed=1122,
            created=current_created,
            updated=current_updated,
            values=expert_values,
            notes=expert_notes,
        ),
        game(
            "a1000000-0000-4000-8000-000000000002",
            "hard",
            hard_puzzle,
            hard_solution,
            show_errors=True,
            errors=3,
            hints=0,
            elapsed=725,
            created=now - timedelta(hours=6, minutes=30),
            updated=now - timedelta(hours=6, minutes=19),
            values=hard_values,
        ),
        game(
            "a1000000-0000-4000-8000-000000000003",
            "medium",
            medium_puzzle,
            medium_solution,
            show_errors=True,
            errors=1,
            hints=0,
            elapsed=446,
            created=now - timedelta(hours=10),
            updated=now - timedelta(hours=9, minutes=53),
            solved=now - timedelta(hours=9, minutes=53),
        ),
        game(
            "a1000000-0000-4000-8000-000000000004",
            "easy",
            easy_puzzle,
            easy_solution,
            show_errors=True,
            errors=0,
            hints=0,
            elapsed=228,
            created=now - timedelta(hours=12),
            updated=now - timedelta(hours=11, minutes=56),
            solved=now - timedelta(hours=11, minutes=56),
        ),
        game(
            "a1000000-0000-4000-8000-000000000005",
            "expert",
            expert_puzzle,
            expert_solution,
            show_errors=True,
            errors=4,
            hints=2,
            elapsed=1471,
            created=(now - timedelta(days=1)).replace(hour=20, minute=10),
            updated=(now - timedelta(days=1)).replace(hour=20, minute=35),
            solved=(now - timedelta(days=1)).replace(hour=20, minute=35),
        ),
        game(
            "a1000000-0000-4000-8000-000000000006",
            "hard",
            hard_puzzle,
            hard_solution,
            show_errors=True,
            errors=0,
            hints=0,
            elapsed=892,
            created=(now - timedelta(days=1)).replace(hour=18, minute=0),
            updated=(now - timedelta(days=1)).replace(hour=18, minute=15),
            solved=(now - timedelta(days=1)).replace(hour=18, minute=15),
        ),
    ]

    return {
        "version": 1,
        "currentGameId": games[0]["id"],
        "games": games,
    }


def settings_xml(*, straight_grid: bool, dark: bool, fast_input: bool) -> str:
    theme = "dark" if dark else "light"
    return (
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
        "<map>\n"
        '    <boolean name="notes_mode" value="false" />\n'
        f'    <boolean name="fast_input" value="{str(fast_input).lower()}" />\n'
        f'    <string name="theme_mode">{theme}</string>\n'
        f'    <boolean name="straight_grid" value="{str(straight_grid).lower()}" />\n'
        '    <boolean name="same_highlight" value="true" />\n'
        '    <boolean name="show_errors" value="true" />\n'
        "</map>\n"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("/tmp/sudoku-screenshots"),
        help="directory for sudoku_data.json and sudoku.xml",
    )
    parser.add_argument(
        "--straight-grid",
        action="store_true",
        help="enable straight grid lines in the settings file",
    )
    parser.add_argument(
        "--dark",
        action="store_true",
        help="select the dark theme in the settings file",
    )
    parser.add_argument(
        "--no-fast-input",
        action="store_true",
        help="disable the fast mode in the settings file",
    )
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    data_path = args.out / "sudoku_data.json"
    settings_path = args.out / "sudoku.xml"

    data_path.write_text(
        json.dumps(build_data(), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    settings_path.write_text(
        settings_xml(
            straight_grid=args.straight_grid,
            dark=args.dark,
            fast_input=not args.no_fast_input,
        ),
        encoding="utf-8",
    )

    print(data_path)
    print(settings_path)


if __name__ == "__main__":
    main()
