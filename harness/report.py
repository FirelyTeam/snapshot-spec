#!/usr/bin/env python3
"""Noise-filtered structural diff report for the snapshot-spec harness.

Reads out/<id>/{dotnet,java,expected}.norm.json (all three serialized by the same Java
pretty-JSON composer, narrative stripped) and out/summary.tsv, and classifies every
.NET-vs-Java property difference against the known deviation classes / noise ledger
(harness README + extracts/harness-first-runs-2026-08-26.md). Output: out/report.md.

Primary diff = .NET vs Java (the deviation signal). java != EQUAL rows are listed as
harness-health problems, never classified. Element-set mismatches (an element id present
on one side only) get their own bucket — id alignment is not assumed.

Classes:
  NOISE-JAVA-BASEVERSION   Java stamps tools/snapshot-base-version extension (PU:1091-1093)
  EXT-SLICING-STAMP        .NET stamps url-discriminator slicing on every .extension element
  SD-MAPPING               Java copies base SD's mapping declarations; .NET doesn't
  ELEM-MAPPING             element .mapping merge differences (Java comma-joins same-identity)
  REL-LINKS                Java absolutizes spec-relative markdown links (updateURLs)
  CONSTRAINT-ORDER         same constraint keys, different order
  FALSE-VS-ABSENT          boolean explicit-false on one side vs property absent on the other
  ELEMENT-SET              element present on one side only
  NEW                      everything else (the actual review queue)
"""

import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

HARNESS = Path(__file__).parent
OUT = HARNESS / "out"

SPEC_PREFIX = re.compile(r"https?://hl7\.org/fhir/(R[456B]+/)?")
MD_PROPS = ("definition", "comment", "requirements", "meaningWhenMissing")


def load(p: Path):
    if not p.exists():
        return None
    return json.loads(p.read_text(encoding="utf-8"))


def flatten(obj, prefix=""):
    """dict -> {jsonpath: leaf} with list indices; lists of dicts flattened per index."""
    leaves = {}
    if isinstance(obj, dict):
        for k, v in obj.items():
            leaves.update(flatten(v, f"{prefix}.{k}" if prefix else k))
    elif isinstance(obj, list):
        for i, v in enumerate(obj):
            leaves.update(flatten(v, f"{prefix}[{i}]"))
    else:
        leaves[prefix] = obj
    return leaves


def elem_key(e):
    return e.get("id") or e.get("path", "?") + (":" + e["sliceName"] if "sliceName" in e else "")


def strip_prop(path: str) -> str:
    """'mapping[2].map' -> 'mapping.map' (indices removed) for classification."""
    return re.sub(r"\[\d+\]", "", path)


# keyed list properties: aligned by semantic key instead of index (kills index-shift cascades)
LIST_KEYS = {"constraint": "key", "mapping": "identity", "extension": "url",
             "type": "code", "code": "code", "discriminator": "path"}


def classify_scalar(prop: str, jv, nv, elem_path: str) -> str:
    base = strip_prop(prop)
    if base.startswith("slicing") and (elem_path.endswith(".extension") or elem_path.endswith(".modifierExtension")):
        return "EXT-SLICING-STAMP"
    if base.startswith("mapping"):
        return "ELEM-MAPPING"
    if base.split(".")[0] in MD_PROPS and isinstance(jv, str) and isinstance(nv, str):
        if SPEC_PREFIX.sub("", jv) == SPEC_PREFIX.sub("", nv):
            return "REL-LINKS"
    if (jv is None and nv is False) or (jv is False and nv is None):
        return "FALSE-VS-ABSENT"
    return "NEW"


def keyed(items: list, key: str) -> dict:
    """Multimap key -> items (key value suffixed #n on repeats to keep pairs aligned)."""
    out = {}
    seen = Counter()
    for it in items:
        k = str(it.get(key)) if isinstance(it, dict) else str(it)
        seen[k] += 1
        out[k if seen[k] == 1 else f"{k}#{seen[k]}"] = it
    return out


def diff_elements(je, ne, sd_url: str):
    """Per-element property diffs between java and .NET element dicts.

    Keyed lists (constraint/mapping/extension/type/...) are aligned by their semantic key;
    unmatched entries become one finding each. Everything else diffs by flattened path.
    """
    out = []
    epath = je.get("path", ne.get("path", ""))
    props = set(je) | set(ne)
    for prop in sorted(props):
        jv, nv = je.get(prop), ne.get(prop)
        if jv == nv:
            continue
        if prop in LIST_KEYS and isinstance(jv, list) and isinstance(nv, list):
            jm, nm = keyed(jv, LIST_KEYS[prop]), keyed(nv, LIST_KEYS[prop])
            # pure order difference?
            if jm.keys() == nm.keys() and all(jm[k] == nm[k] for k in jm):
                out.append((prop, "CONSTRAINT-ORDER" if prop == "constraint" else f"{prop.upper()}-ORDER",
                            "[order]", "[order]"))
                continue
            for k in sorted(jm.keys() | nm.keys()):
                ji, ni = jm.get(k), nm.get(k)
                if ji == ni:
                    continue
                if ji is None or ni is None:
                    only, item = ("java", ji) if ni is None else (".NET", ni)
                    if prop == "constraint" and only == ".NET" and isinstance(item, dict) \
                            and item.get("source") not in (None, sd_url):
                        cls = "TYPE-CONSTRAINT"  # .NET propagates datatype-level constraints
                    elif prop == "mapping":
                        cls = "ELEM-MAPPING"
                    else:
                        cls = "NEW"
                    out.append((f"{prop}[{k}] only in {only}", cls, ji, ni))
                else:
                    jf, nf = flatten(ji), flatten(ni)
                    for sp in sorted(set(jf) | set(nf)):
                        if jf.get(sp) == nf.get(sp):
                            continue
                        cls = classify_scalar(f"{prop}.{sp}", jf.get(sp), nf.get(sp), epath)
                        out.append((f"{prop}[{k}].{sp}", cls, jf.get(sp), nf.get(sp)))
            continue
        jf = flatten(jv, prop) if isinstance(jv, (dict, list)) else {prop: jv}
        nf = flatten(nv, prop) if isinstance(nv, (dict, list)) else {prop: nv}
        for sp in sorted(set(jf) | set(nf)):
            sjv, snv = jf.get(sp), nf.get(sp)
            if sjv == snv:
                continue
            out.append((sp, classify_scalar(sp, sjv, snv, epath), sjv, snv))
    return out


BASEVERSION_URL = "http://hl7.org/fhir/tools/StructureDefinition/snapshot-base-version"


def diff_test(tid: str):
    """Returns (per-class counter, list of NEW findings, note) for one test, or None."""
    j = load(OUT / tid / "java.norm.json")
    n = load(OUT / tid / "dotnet.norm.json")
    if j is None or n is None:
        return None
    counts = Counter()
    news = []

    # SD-level (everything except snapshot/differential/text)
    jt = {k: v for k, v in j.items() if k not in ("snapshot", "differential", "text")}
    nt = {k: v for k, v in n.items() if k not in ("snapshot", "differential", "text")}
    # noise: java-only snapshot-base-version extension
    if "extension" in jt:
        jt["extension"] = [e for e in jt["extension"] if e.get("url") != BASEVERSION_URL]
        if not jt["extension"]:
            del jt["extension"]
        if jt != {k: v for k, v in j.items() if k not in ("snapshot", "differential", "text")}:
            counts["NOISE-JAVA-BASEVERSION"] += 1
    jf, nf = flatten(jt), flatten(nt)
    for prop in sorted(set(jf) | set(nf)):
        jv, nv = jf.get(prop), nf.get(prop)
        if jv == nv:
            continue
        base = strip_prop(prop)
        if base.startswith("mapping"):
            counts["SD-MAPPING"] += 1
        else:
            counts["NEW"] += 1
            news.append((f"SD.{prop}", jv, nv))

    # snapshot elements by id
    jel = {elem_key(e): e for e in j.get("snapshot", {}).get("element", [])}
    nel = {elem_key(e): e for e in n.get("snapshot", {}).get("element", [])}
    for k in jel.keys() | nel.keys():
        if k not in nel:
            counts["ELEMENT-SET"] += 1
            news.append((f"[java-only element] {k}", "", ""))
            continue
        if k not in jel:
            counts["ELEMENT-SET"] += 1
            news.append((f"[.NET-only element] {k}", "", ""))
            continue
        for prop, cls, jv, nv in diff_elements(jel[k], nel[k], j.get("url", "")):
            counts[cls] += 1
            if cls == "NEW":
                news.append((f"{k} :: {prop}", jv, nv))
    return counts, news


def main():
    summary = {}
    sfile = OUT / "summary.tsv"
    if sfile.exists():
        for ln in sfile.read_text(encoding="utf-8").splitlines()[1:]:
            p = ln.split("\t")
            if len(p) >= 8:
                summary[p[0]] = dict(zip("id mode java jerr net nerr nvj note".split(), p))

    ids = sys.argv[1:] or sorted(summary)
    total = Counter()
    per_test = {}
    all_news = {}
    health = []
    for tid in ids:
        s = summary.get(tid, {})
        if s.get("mode", "").startswith("fail"):
            continue
        if s.get("java") not in ("EQUAL", None) and s.get("java") != "EQUAL":
            health.append((tid, s.get("java"), s.get("note", "")))
        r = diff_test(tid)
        if r is None:
            continue
        counts, news = r
        per_test[tid] = counts
        total.update(counts)
        if news:
            all_news[tid] = news

    lines = ["# Harness structural report (.NET vs Java, noise-classified)", ""]
    lines.append(".NET = Hl7.Fhir.R5 6.2.1 | Java engine = 6.10.2 (d06577dbc5c6) | "
                 "golden = fhir-test-cases 1.7.67 | core = hl7.fhir.r5.core 5.0.0")
    lines.append("")
    if health:
        lines.append("## HARNESS HEALTH — java-vs-golden not EQUAL (investigate, do NOT classify)")
        lines.append("")
        for tid, v, note in health:
            lines.append(f"- **{tid}**: java={v} {note}")
        lines.append("")
    lines.append("## Class totals (property-level findings across all compared tests)")
    lines.append("")
    lines.append("| class | count |")
    lines.append("|---|---|")
    for cls, cnt in total.most_common():
        lines.append(f"| {cls} | {cnt} |")
    lines.append("")
    lines.append("## Per-test class breakdown")
    lines.append("")
    classes = [c for c, _ in total.most_common()]
    lines.append("| test | " + " | ".join(classes) + " |")
    lines.append("|---|" + "---|" * len(classes))
    for tid, counts in sorted(per_test.items()):
        if not counts:
            continue
        lines.append(f"| {tid} | " + " | ".join(str(counts.get(c, "")) for c in classes) + " |")
    lines.append("")
    lines.append("## NEW / unclassified findings (review queue)")
    lines.append("")
    for tid, news in sorted(all_news.items()):
        lines.append(f"### {tid}")
        lines.append("")
        for loc, jv, nv in news[:40]:
            js = json.dumps(jv, ensure_ascii=False) if not isinstance(jv, str) else jv
            ns = json.dumps(nv, ensure_ascii=False) if not isinstance(nv, str) else nv
            lines.append(f"- `{loc}`\n  - java: `{str(js)[:200]}`\n  - .NET: `{str(ns)[:200]}`")
        if len(news) > 40:
            lines.append(f"- ... {len(news) - 40} more (capped; see norm.json pair)")
        lines.append("")

    (OUT / "report.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"report -> {OUT / 'report.md'}")
    print("\nTotals:", dict(total))


if __name__ == "__main__":
    main()
