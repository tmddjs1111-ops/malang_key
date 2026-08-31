#!/usr/bin/env python3
"""Build MalangKey's bundled Japanese conversion dictionary from JMdict XML."""

from __future__ import annotations

import argparse
import gzip
import os
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import BinaryIO, Iterable
from xml.etree import ElementTree


SCHEMA_VERSION = 1
BATCH_SIZE = 10_000


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert JMdict XML (plain or .gz) into MalangKey SQLite format.",
    )
    parser.add_argument("--input", required=True, type=Path, help="JMdict XML or JMdict_e.gz")
    parser.add_argument("--output", required=True, type=Path, help="Output japanese_dict.sqlite3")
    return parser.parse_args()


def open_source(path: Path) -> BinaryIO:
    if path.suffix.lower() == ".gz":
        return gzip.open(path, "rb")
    return path.open("rb")


def priority_score(tags: Iterable[str]) -> int:
    """Translate JMdict priority markers into a stable candidate ranking."""
    score = 100
    weights = {
        "ichi1": 10_000,
        "news1": 9_500,
        "spec1": 9_000,
        "gai1": 8_500,
        "ichi2": 7_000,
        "news2": 6_500,
        "spec2": 6_000,
        "gai2": 5_500,
    }
    for tag in tags:
        score = max(score, weights.get(tag, 0))
        if tag.startswith("nf") and tag[2:].isdigit():
            rank = int(tag[2:])
            if 1 <= rank <= 48:
                score = max(score, 8_400 - (rank - 1) * 100)
    return score


def rows_from_entry(entry: ElementTree.Element) -> Iterable[tuple[str, str, int]]:
    kanji_elements: list[tuple[str, tuple[str, ...]]] = []
    for element in entry.findall("k_ele"):
        word = element.findtext("keb")
        if word:
            kanji_elements.append((word, tuple(tag.text or "" for tag in element.findall("ke_pri"))))

    if not kanji_elements:
        return

    for reading_element in entry.findall("r_ele"):
        reading = reading_element.findtext("reb")
        if not reading or reading_element.find("re_nokanji") is not None:
            continue

        reading_priorities = tuple(tag.text or "" for tag in reading_element.findall("re_pri"))
        restrictions = {tag.text for tag in reading_element.findall("re_restr") if tag.text}

        for word, word_priorities in kanji_elements:
            if restrictions and word not in restrictions:
                continue
            if reading == word:
                continue
            yield reading, word, max(
                priority_score(reading_priorities),
                priority_score(word_priorities),
            )


def create_database(source: Path, output: Path) -> tuple[int, int]:
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary_output = output.with_name(f"{output.name}.tmp")
    if temporary_output.exists():
        temporary_output.unlink()

    connection = sqlite3.connect(temporary_output)
    parsed_entries = 0
    batch: list[tuple[str, str, int]] = []

    try:
        connection.executescript(
            """
            PRAGMA page_size = 4096;
            PRAGMA journal_mode = OFF;
            PRAGMA synchronous = OFF;
            PRAGMA temp_store = MEMORY;

            CREATE TABLE dictionary (
                id INTEGER PRIMARY KEY,
                reading TEXT NOT NULL,
                word TEXT NOT NULL,
                frequency INTEGER NOT NULL DEFAULT 0,
                UNIQUE(reading, word)
            );

            CREATE TABLE metadata (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
            """
        )

        upsert_sql = """
            INSERT INTO dictionary (reading, word, frequency)
            VALUES (?, ?, ?)
            ON CONFLICT(reading, word) DO UPDATE SET
                frequency = MAX(dictionary.frequency, excluded.frequency)
        """

        with open_source(source) as source_stream:
            for _, element in ElementTree.iterparse(source_stream, events=("end",)):
                if element.tag != "entry":
                    continue
                parsed_entries += 1
                batch.extend(rows_from_entry(element))
                element.clear()

                if len(batch) >= BATCH_SIZE:
                    connection.executemany(upsert_sql, batch)
                    batch.clear()
                    if parsed_entries % 50_000 == 0:
                        print(f"Parsed {parsed_entries:,} JMdict entries...", flush=True)

        if batch:
            connection.executemany(upsert_sql, batch)

        actual_rows = connection.execute("SELECT COUNT(*) FROM dictionary").fetchone()[0]
        connection.executemany(
            "INSERT INTO metadata (key, value) VALUES (?, ?)",
            (
                ("schema_version", str(SCHEMA_VERSION)),
                ("source", "JMdict"),
                ("source_file", source.name),
                ("generated_at_utc", datetime.now(timezone.utc).isoformat()),
                ("jmdict_entry_count", str(parsed_entries)),
                ("dictionary_row_count", str(actual_rows)),
            ),
        )
        connection.execute(f"PRAGMA user_version = {SCHEMA_VERSION}")
        connection.execute(
            "CREATE INDEX idx_dictionary_reading_frequency "
            "ON dictionary(reading, frequency DESC)"
        )
        connection.commit()
        connection.execute("ANALYZE")
        connection.execute("VACUUM")
        connection.commit()
    except Exception:
        connection.close()
        if temporary_output.exists():
            temporary_output.unlink()
        raise
    else:
        connection.close()

    os.replace(temporary_output, output)
    return parsed_entries, actual_rows


def main() -> int:
    args = parse_args()
    source = args.input.resolve()
    output = args.output.resolve()
    if not source.is_file():
        print(f"Input file not found: {source}", file=sys.stderr)
        return 2

    entries, rows = create_database(source, output)
    print(f"Created {output}")
    print(f"JMdict entries: {entries:,}; conversion rows: {rows:,}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
