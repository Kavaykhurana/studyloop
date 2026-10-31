#!/usr/bin/env python3
"""Calibrate StudyLoop's similarity thresholds against the real Gemini embedding model (build phase P4).

Embeds the seeded campus exactly the way the backend does (EmbeddingText.forDoubt for stored doubts,
EmbeddingText.forQuery for radar queries), then measures:

* 10 reworded duplicates of solved seed doubts against the doubt they duplicate,
* 10 unrelated study questions against their closest solved seed doubt,
* every solved doubt's best similarity to each helper's other accepted answers (the routing "semantic" signal).

It prints the recommended SIM_* values: duplicate threshold midway between the lowest duplicate and the highest
unrelated score, grounding 0.10 lower, semantic floor/ceiling at the 10th/90th percentile of the routing signal.

Usage (stdlib only, Python 3.11+):
    GEMINI_API_KEY=... python3 scripts/calibrate_similarity.py
Optional: GEMINI_EMBED_MODEL (default gemini-embedding-2), GEMINI_BASE_URL.
"""

from __future__ import annotations

import json
import math
import os
import statistics
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

SEED = Path(__file__).resolve().parent.parent / "backend/src/main/resources/seed/demo-doubts.json"
BASE_URL = os.environ.get("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta").rstrip("/")
DIMENSIONS = 768
MAX_BODY = 1500

SUBJECT_NAMES = {
    "DSA": "Data Structures & Algorithms",
    "DBMS": "Database Management Systems",
    "OS": "Operating Systems",
    "CN": "Computer Networks",
    "OOPJ": "Object-Oriented Programming (Java)",
    "MATH": "Discrete Maths & Probability",
}

# Reworded the way a different student would ask the same thing; each must surface the keyed seed doubt.
DUPLICATES = [
    ("dsa-01", "BST becomes very slow when keys are inserted in increasing order",
     "I add 1 to 1000 one by one into my binary search tree and searching is now as slow as a list. Why?"),
    ("dsa-04", "Binary search never terminates for some inputs",
     "My while (low < high) loop with mid = (low + high) / 2 keeps running forever when the key is missing."),
    ("dbms-01", "Is R(A,B,C,D) in BCNF if C -> A and AB is a key?",
     "FDs are AB->C, C->A, AB->D. I think there are two keys. How do I check BCNF and decompose it?"),
    ("dbms-05", "COUNT(*) inside WHERE gives an error in SQL",
     "I want departments with more than 5 employees but WHERE COUNT(*) > 5 fails. What should I use instead?"),
    ("os-01", "Producer consumer program hangs forever with semaphores",
     "I use mutex, empty and full semaphores. The producer waits on mutex first, then empty, and it gets stuck."),
    ("os-02", "How to compute effective memory access time with TLB hit ratio",
     "TLB lookup 20 ns, memory access 100 ns, hit ratio 0.9. What is the effective access time?"),
    ("cn-01", "CRC: dividing 1101011011 by 10011, what is the remainder?",
     "I do the XOR division but get a different remainder than the book. What frame is transmitted?"),
    ("cn-02", "Divide a /24 network into six subnets",
     "Network 192.168.10.0/24 must be split into 6 subnets. What is the subnet mask and the range of each?"),
    ("oopj-01", "Removing items from an ArrayList while iterating throws an exception",
     "for (String s : list) { if (...) list.remove(s); } gives ConcurrentModificationException. How to fix?"),
    ("math-01", "Chance of getting at least one 6 when a die is rolled 4 times",
     "Is it 4/6? My friend says use the complement. What is the correct probability?"),
]

# Study questions on topics no solved seed doubt covers; none should clear the duplicate threshold.
UNRELATED = [
    ("What is the difference between a process and a thread?", "Do threads share the heap and the stack?"),
    ("How does Kruskal's algorithm avoid cycles?", "Why do we need union-find when building the MST?"),
    ("What is a weak entity in an ER diagram?", "How is it drawn and what is a partial key?"),
    ("How does the DNS resolution process work?", "What happens between typing a URL and getting an IP?"),
    ("What are Java generics wildcards?", "When should I use ? extends T versus ? super T?"),
    ("Explain the pigeonhole principle with an example", "How many socks to pick to guarantee a matching pair?"),
    ("What is thrashing in an operating system?", "Why does adding processes reduce CPU utilisation?"),
    ("Difference between TCP and UDP headers", "Which fields does UDP not have and why?"),
    ("How do I find eigenvalues of a 2x2 matrix?", "Solve det(A - lambda I) = 0 for A = [[2,1],[1,2]]."),
    ("What does a write-ahead log guarantee in recovery?", "Why must the log record be flushed before the page?"),
]


def call(path: str, body: dict, key: str) -> dict:
    req = urllib.request.Request(BASE_URL + path, data=json.dumps(body).encode(), method="POST",
                                 headers={"Content-Type": "application/json", "x-goog-api-key": key})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                return json.load(resp)
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 503) and attempt < 2:
                time.sleep(2 * (attempt + 1))
                continue
            sys.exit(f"Gemini returned HTTP {e.code}: {e.read().decode()[:300]}")
        except urllib.error.URLError as e:
            sys.exit(f"Could not reach Gemini: {e.reason}")
    raise RuntimeError("unreachable")


def resolve_model(key: str, configured: str) -> str:
    req = urllib.request.Request(BASE_URL + "/models?pageSize=200", headers={"x-goog-api-key": key})
    with urllib.request.urlopen(req, timeout=30) as resp:
        models = {m["name"].removeprefix("models/"): m.get("supportedGenerationMethods", [])
                  for m in json.load(resp).get("models", [])}
    usable = sorted(name for name, methods in models.items() if "embedContent" in methods)
    base = configured.removesuffix("-preview")
    for candidate in (configured, base + "-preview", base):
        if candidate in usable:
            return candidate
    extended = sorted((n for n in usable if n.startswith(base + "-")), key=lambda n: (len(n), n))
    if extended:
        return extended[0]
    sys.exit(f"No embedding model matching {configured!r}. Available: {', '.join(usable)}")


def embed(texts: list[str], model: str, key: str) -> list[list[float]]:
    out: list[list[float]] = []
    for i in range(0, len(texts), 50):
        chunk = texts[i:i + 50]
        body = {"requests": [{"model": f"models/{model}", "content": {"parts": [{"text": t}]},
                              "outputDimensionality": DIMENSIONS} for t in chunk]}
        for item in call(f"/models/{model}:batchEmbedContents", body, key)["embeddings"]:
            v = item["values"]
            if len(v) != DIMENSIONS:
                sys.exit(f"Expected {DIMENSIONS} dimensions, got {len(v)}")
            norm = math.sqrt(sum(x * x for x in v)) or 1.0
            out.append([x / norm for x in v])
        time.sleep(1)
    return out


def cosine(a: list[float], b: list[float]) -> float:
    return sum(x * y for x, y in zip(a, b))


def for_doubt(d: dict) -> str:
    return f"Title: {d['title']}\nSubject: {SUBJECT_NAMES[d['subject']]} / {d['topic']}\n\n{d['body'][:MAX_BODY]}"


def for_query(title: str, body: str) -> str:
    return f"Title: {title}\n\n{body[:MAX_BODY]}"


def percentile(values: list[float], p: float) -> float:
    ordered = sorted(values)
    k = (len(ordered) - 1) * p
    lo, hi = math.floor(k), math.ceil(k)
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (k - lo)


def main() -> None:
    key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not key:
        sys.exit("Set GEMINI_API_KEY.")
    model = resolve_model(key, os.environ.get("GEMINI_EMBED_MODEL", "gemini-embedding-2"))
    print(f"Embedding model: {model}")

    doubts = json.loads(SEED.read_text())["doubts"]
    # Only peer-solved doubts are searchable by the radar and count as helper evidence.
    solved = [d for d in doubts if d["status"] == "SOLVED" and d["resolution"] == "PEER_ANSWER"]
    by_key = {d["key"]: i for i, d in enumerate(solved)}
    stored = embed([for_doubt(d) for d in solved], model, key)
    dup_vecs = embed([for_query(t, b) for _, t, b in DUPLICATES], model, key)
    unrelated_vecs = embed([for_query(t, b) for t, b in UNRELATED], model, key)

    print("\nDuplicates (query -> the seed doubt it rewords; rank among solved doubts)")
    dup_scores = []
    for (seed_key, title, _), vec in zip(DUPLICATES, dup_vecs):
        sims = [cosine(vec, s) for s in stored]
        target = sims[by_key[seed_key]]
        rank = 1 + sum(1 for s in sims if s > target)
        dup_scores.append(target)
        print(f"  {target:.3f}  rank {rank}  {title}")

    print("\nUnrelated (query -> closest solved seed doubt)")
    unrelated_scores = []
    for (title, _), vec in zip(UNRELATED, unrelated_vecs):
        sims = [cosine(vec, s) for s in stored]
        best = max(range(len(sims)), key=sims.__getitem__)
        unrelated_scores.append(sims[best])
        print(f"  {sims[best]:.3f}  {title}  (closest: {solved[best]['title'][:60]})")

    helper_of = [next(a["author"] for a in d["answers"] if a.get("accepted")) for d in solved]
    semantic = []
    for i in range(len(solved)):
        for helper in set(helper_of):
            others = [cosine(stored[i], stored[j]) for j in range(len(solved)) if j != i and helper_of[j] == helper]
            if others:
                semantic.append(max(others))

    low_dup, high_unrelated = min(dup_scores), max(unrelated_scores)
    duplicate = round((low_dup + high_unrelated) / 2, 2)
    print(f"\nLowest duplicate {low_dup:.3f}, highest unrelated {high_unrelated:.3f}"
          + ("" if low_dup > high_unrelated else "  (overlap: the threshold will trade recall for precision)"))
    print("\nRecommended settings (paste into Render -> Environment):")
    print(f"SIM_DUPLICATE_THRESHOLD={duplicate:.2f}")
    print(f"SIM_GROUNDING_THRESHOLD={max(0.0, duplicate - 0.10):.2f}")
    print(f"SIM_SEMANTIC_FLOOR={percentile(semantic, 0.10):.2f}")
    print(f"SIM_SEMANTIC_CEIL={percentile(semantic, 0.90):.2f}")
    print(f"(routing signal: n={len(semantic)}, median {statistics.median(semantic):.3f})")


if __name__ == "__main__":
    main()
