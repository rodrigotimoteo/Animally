#!/usr/bin/env python3
"""Generate Animally's local medical-term index from NLM MeSH descriptors.

The generated index is intentionally local at runtime. Sending an unknown word
to a terminology service to classify it could disclose a patient or owner name,
so vocabulary discovery happens during a deliberate source-data refresh instead
of while a user is asking a question.

Usage:
    python3 scripts/update-medical-vocabulary.py --year 2026
    python3 scripts/update-medical-vocabulary.py --year 2026 \
        --source /path/to/desc2026.gz
"""

from __future__ import annotations

import argparse
import gzip
import re
import unicodedata
import urllib.request
import xml.etree.ElementTree as ElementTree
from collections import defaultdict
from contextlib import contextmanager
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = (
    ROOT
    / "shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/vetreference/generated/"
    / "NlmMedicalVocabulary.kt"
)
SOURCE_TEMPLATE = "https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc{year}.gz"

# High-frequency words are useful in prose but poor as privacy-safe lookup
# triggers. The hand-curated matcher still owns generic question modifiers.
STOP_WORDS = frozenset(
    """
    a about after again against all also am an and any are as at be because been
    before being between both but by can could did do does doing down during each
    few for from further had has have having he her here hers herself him himself
    his how i if in into is it its itself just me more most my myself no nor not
    of off on once only or other our ours ourselves out over own same she should
    so some such than that the their theirs them themselves then there these they
    this those through to too under until up very was we were what when where
    which while who whom why will with would you your yours yourself yourselves
    case cases clinical common current general health human humans medical medicine
    patient patients research source study studies use used using
    """.split(),
)
NORMALIZED_TOKEN_PATTERN = re.compile(r"[a-z]{3,}")
MAX_DESCRIPTOR_FREQUENCY = 120


def normalize_token(value: str) -> str | None:
    folded = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii").casefold()
    return folded if NORMALIZED_TOKEN_PATTERN.fullmatch(folded) and folded not in STOP_WORDS else None


def first_text(element: ElementTree.Element, path: str) -> str | None:
    node = element.find(path)
    return node.text.strip() if node is not None and node.text else None


@contextmanager
def open_source(source: str):
    if source.startswith(("http://", "https://")):
        request = urllib.request.Request(
            source,
            headers={"User-Agent": "Animally medical vocabulary updater"},
        )
        response = urllib.request.urlopen(request, timeout=60)
        try:
            with gzip.GzipFile(fileobj=response) as stream:
                yield stream
        finally:
            response.close()
    else:
        with gzip.open(source, "rb") as stream:
            yield stream


def collect_terms(source: str) -> tuple[set[str], dict[str, str]]:
    descriptor_frequency: dict[str, int] = defaultdict(int)
    alias_candidates: dict[str, set[str]] = defaultdict(set)

    with open_source(source) as stream:
        for _, descriptor in ElementTree.iterparse(stream, events=("end",)):
            if descriptor.tag != "DescriptorRecord":
                continue

            tree_numbers = {
                node.text.strip()
                for node in descriptor.findall("./TreeNumberList/TreeNumber")
                if node.text
            }
            if not any(tree.startswith(("C", "D")) for tree in tree_numbers):
                descriptor.clear()
                continue

            preferred = (
                first_text(descriptor, ".//Concept[@PreferredConceptYN='Y']/ConceptName/String")
                or first_text(descriptor, "./DescriptorName/String")
            )
            preferred_token = normalize_token(preferred or "")
            record_tokens: set[str] = set()
            for term_node in descriptor.findall(".//TermList/Term/String"):
                raw_term = term_node.text or ""
                single_token = normalize_token(raw_term)
                if single_token:
                    record_tokens.add(single_token)
                    if preferred_token:
                        alias_candidates[single_token].add(preferred_token)

            if preferred_token:
                record_tokens.add(preferred_token)
            for token in record_tokens:
                descriptor_frequency[token] += 1
            descriptor.clear()

    terms = {
        token
        for token, frequency in descriptor_frequency.items()
        if frequency <= MAX_DESCRIPTOR_FREQUENCY
    }
    aliases = {
        alias: next(iter(candidates))
        for alias, candidates in alias_candidates.items()
        if len(candidates) == 1
        and next(iter(candidates)) in terms
        and alias in terms
        and alias != next(iter(candidates))
    }
    return terms, aliases


MAX_CHUNK_CHARS = 24_000


def text_chunks(values: list[str]) -> list[str]:
    chunks: list[str] = []
    current: list[str] = []
    current_length = 0
    for value in values:
        next_length = current_length + len(value) + (1 if current else 0)
        if current and next_length > MAX_CHUNK_CHARS:
            chunks.append("\n".join(current))
            current = []
            current_length = 0
        current.append(value)
        current_length += len(value) + (1 if current_length else 0)
    if current:
        chunks.append("\n".join(current))
    return chunks


def render_chunks(constant_prefix: str, chunks: list[str]) -> tuple[str, str]:
    declarations = []
    names = []
    for index, chunk in enumerate(chunks):
        name = f"{constant_prefix}_{index:03d}"
        declarations.append(f'    private const val {name} = """{chunk}"""')
        names.append(name)
    words = constant_prefix.lower().split("_")
    array_name = words[0] + "".join(word.title() for word in words[1:]) + "s"
    declarations.append(
        "    private val "
        + array_name
        + " =\n        arrayOf(\n"
        + "\n".join(f"            {name}," for name in names)
        + "\n        )",
    )
    return "\n".join(declarations), array_name


def render(year: int, source: str, terms: set[str], aliases: dict[str, str]) -> str:
    sorted_terms = sorted(terms)
    sorted_aliases = sorted(aliases.items())
    term_declarations, term_chunks_name = render_chunks("TERM_CHUNK", text_chunks(sorted_terms))
    alias_declarations, alias_chunks_name = render_chunks(
        "ALIAS_CHUNK",
        text_chunks([f"{alias}\t{canonical}" for alias, canonical in sorted_aliases]),
    )
    return f'''// Generated by scripts/update-medical-vocabulary.py. Do not edit manually.
// Source: {source}
// NLM MeSH descriptor data release: {year}

package com.github.rodrigotimoteo.animally.domain.vetreference.generated

/**
 * High-signal single-word terms extracted from NLM Medical Subject Headings
 * disease (C) and drug/chemical (D) descriptors and entry terms.
 */
internal object NlmMedicalVocabulary {{
{term_declarations}
{alias_declarations}
    val terms: Set<String> =
        {term_chunks_name}
            .asSequence()
            .flatMap {{ it.lineSequence() }}
            .filter(String::isNotBlank)
            .toSet()

    /** Unambiguous single-word MeSH entry-term aliases. */
    val aliases: Map<String, String> =
        {alias_chunks_name}
            .asSequence()
            .flatMap {{ it.lineSequence() }}
            .filter(String::isNotBlank)
            .associate {{ line ->
                val (alias, canonical) = line.split('\\t', limit = 2)
                alias to canonical
            }}
}}
'''


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--year", type=int, default=date.today().year)
    parser.add_argument(
        "--source",
        help="Local .gz dataset or URL; defaults to the NLM MeSH descriptor release for --year.",
    )
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    source = args.source or SOURCE_TEMPLATE.format(year=args.year)
    terms, aliases = collect_terms(source)
    output = args.output if args.output.is_absolute() else ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    source_label = source if source.startswith(("http://", "https://")) else SOURCE_TEMPLATE.format(year=args.year)
    output.write_text(render(args.year, source_label, terms, aliases), encoding="utf-8")
    print(f"Generated {len(terms)} terms and {len(aliases)} aliases in {output}")


if __name__ == "__main__":
    main()
