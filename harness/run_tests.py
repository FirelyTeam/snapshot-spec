#!/usr/bin/env python3
"""Snapshot-spec harness driver (Phase 4 packet 2: batch mode).

Three-way comparison per fhir-test-cases snapshot-generation test:
  .NET (Firely SDK, NuGet Hl7.Fhir.R5 6.2.1)  vs
  Java oracle (org.hl7.fhir.r5 engine from validator_cli 6.10.2, JUnit-driver config)  vs
  golden -expected file (fhir-test-cases 1.7.67).

Batch architecture (one process per side for the whole selection):
  1. DotNetRunner -batch: core loaded once; cross-linked test bases resolve through a
     "universe" of every test's -expected + include/register fixtures (mirrors the Java
     JUnit driver's getByUrl). Writes out/<id>/dotnet.xml + dotnet.log.
  2. BatchRunner.java: one JVM, shared cumulative context in manifest order (exactly the
     JUnit driver's model), faithful testGen/testSort/register/getSD replication. Also
     performs all three equalsDeep compares inline and emits RESULT lines.
  3. This driver merges both RESULT streams into out/summary.tsv (fresh per run).

Usage:
  python run_tests.py t1 t2 obs-2b        # specific test ids
  python run_tests.py --all               # every test in the manifest (gen + sort + fail)
  python run_tests.py --list              # list test ids + attributes
Flags:
  --pkgs        also give the .NET side the THO/uv.extensions/sdc/xver packages the Java
                oracle loads (package parity; off by default so environment-artifact diffs
                can be told apart from engine deviations)
  --net-only / --java-only   run just one side (java-only reuses existing dotnet.xml)

Summary columns: id, mode, java, jerr, net, nerr, nvj, note
  java: EQUAL|DIFF|THREW|ERRORS|NO-THROW|ERROR  (fail tests: THREW/ERRORS = JUnit "pass")
  net:  EQUAL|DIFF|MISSING (gen tests, vs golden) / THREW|ERRORS|NO-THROW|SETUP-FAIL (fail tests)
  jerr/nerr: ERROR-level message counts per side (JUnit fails a test on any ERROR, :677)
  nvj:  .NET vs Java direct equalsDeep
"""

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANSI = re.compile(r"\x1b\[[0-9;]*m")  # the Java engine's logger colors stdout

HARNESS = Path(__file__).parent
TESTS = HARNESS.parent / "fhir-test-cases" / "r5" / "snapshot-generation"
CORE_TGZ = HARNESS.parent / "fhir-test-cases" / "r5" / "packages" / "hl7.fhir.r5.core.tgz"
VALIDATOR = HARNESS.parent / "tools" / "validator_cli.jar"
PKG_DIR = HARNESS.parent / "tools" / "pkgs"
DOTNET_EXE = HARNESS / "runner-dotnet" / "bin" / "Release" / "net9.0" / "DotNetRunner.exe"
OUT = HARNESS / "out"
NET_PKGS = ["hl7.terminology.r5", "hl7.fhir.uv.extensions", "hl7.fhir.uv.sdc", "hl7.fhir.xver-extensions"]


def fixture(stem: str) -> Path | None:
    for ext in (".xml", ".json"):
        p = TESTS / f"{stem}{ext}"
        if p.exists():
            return p
    return None


def load_manifest() -> dict[str, dict]:
    tree = ET.parse(TESTS / "manifest.xml")
    tests = {}
    for t in tree.getroot().findall("test"):
        a = t.attrib
        tests[a["id"]] = {
            "id": a["id"],
            "gen": a.get("gen") == "true",
            "sort": a.get("sort") == "true",
            "fail": a.get("fail") == "true",
            "nsp": a.get("new-slice-processing", "true") != "false",
            "include": a.get("include", ""),
            "register": a.get("register", ""),
            "allow": a.get("allow", ""),
            "regex": a.get("regex", ""),
            "description": a.get("description", ""),
        }
    return tests


def deps_for(test: dict) -> list[Path]:
    """Dependency fixture files (include + register) for the .NET side."""
    names = []
    if test["include"]:
        names.append(test["include"])
    if test["register"]:
        names.extend(test["register"].split(","))
    return [p for n in names if (p := fixture(n.strip()))]


def universe_files(tests: dict) -> list[Path]:
    """Every test's -expected file + every include/register fixture (getByUrl's search space)."""
    seen, files = set(), []
    for t in tests.values():
        for stem in ([f"{t['id']}-expected"]
                     + ([t["include"]] if t["include"] else [])
                     + (t["register"].split(",") if t["register"] else [])):
            p = fixture(stem.strip())
            if p and p not in seen:
                seen.add(p)
                files.append(p)
    return files


def stream(cmd: list[str], logfile: Path) -> list[str]:
    """Run cmd, teeing stdout lines to console + logfile; returns RESULT/DONE lines."""
    results = []
    with open(logfile, "w", encoding="utf-8") as lf:
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, encoding="utf-8", errors="replace", cwd=HARNESS)
        assert proc.stdout is not None
        for line in proc.stdout:
            lf.write(line)
            line = ANSI.sub("", line).rstrip("\n")
            if line.startswith("RESULT\t") or line == "DONE":
                results.append(line)
                print(line, flush=True)
        proc.wait()
    return results


def count_errors(log: Path) -> int:
    if not log.exists():
        return 0
    return sum(1 for ln in log.read_text(encoding="utf-8", errors="replace").splitlines()
               if ln.startswith("MSG: [Error") or ln.startswith("MSG: [ERROR"))


def main():
    tests = load_manifest()
    args = [a for a in sys.argv[1:]]
    pkgs = "--pkgs" in args
    net_only = "--net-only" in args
    java_only = "--java-only" in args
    defaults = "--defaults" in args  # .NET side: SDK default SnapshotGeneratorSettings (DEV-029 re-run)
    args = [a for a in args if not a.startswith("--")]
    run_all = "--all" in sys.argv[1:]

    if "--list" in sys.argv[1:]:
        for t in tests.values():
            flags = "".join(c for c, on in [("g", t["gen"]), ("s", t["sort"]),
                                            ("F", t["fail"]), ("N", not t["nsp"])] if on)
            print(f"{t['id']:32} {flags:5} {t['description'][:70]}")
        return

    ids = list(tests) if run_all else args
    if not ids:
        print("no test ids given (use --all)", file=sys.stderr)
        return
    for tid in ids:
        if tid not in tests:
            print(f"{tid}: NOT IN MANIFEST", file=sys.stderr)
    ids = [i for i in ids if i in tests]
    OUT.mkdir(exist_ok=True)

    # ---- phase 1: .NET batch ----
    net_rc: dict[str, int] = {}
    if not java_only:
        jobs = OUT / "jobs.tsv"
        with open(jobs, "w", encoding="utf-8") as f:
            for tid in ids:
                t = tests[tid]
                inp = fixture(f"{tid}-input")
                if not inp:
                    continue
                tdir = OUT / tid
                tdir.mkdir(exist_ok=True)
                deps = ",".join(str(d) for d in deps_for(t))
                f.write(f"{tid}\t{inp}\t{tdir / 'dotnet.xml'}\t{deps}\n")
        uni = OUT / "universe.txt"
        uni.write_text("\n".join(str(p) for p in universe_files(tests)), encoding="utf-8")
        cmd = [str(DOTNET_EXE), "-core", str(CORE_TGZ), "-batch", str(jobs), "-universe", str(uni)]
        if pkgs:
            cmd += ["-pkgs", ",".join(str(PKG_DIR / f"{p}.tgz") for p in NET_PKGS)]
        if defaults:
            cmd += ["-defaults"]
        print(f"== .NET batch ({len(ids)} tests){' +pkgs' if pkgs else ''}{' +defaults' if defaults else ''} ==", flush=True)
        for line in stream(cmd, OUT / "dotnet-batch.log"):
            p = line.split("\t")
            if len(p) >= 3 and p[0] == "RESULT":
                net_rc[p[1]] = int(p[2])

    # ---- phase 2: Java batch (+ inline compares) ----
    rows: dict[str, dict] = {}
    if not net_only:
        cmd = ["java", "-Xmx4g", "-cp", str(VALIDATOR), "BatchRunner.java",
               "-core", str(CORE_TGZ), "-tests", str(TESTS), "-out", str(OUT),
               "-pkgdir", str(PKG_DIR), "-ids", ",".join(ids)]
        print(f"== Java batch ({len(ids)} tests) ==", flush=True)
        stream(cmd, OUT / "java-batch.log")
        # authoritative results come from the dedicated file (stdout can carry logger noise)
        rfile = OUT / "java-results.tsv"
        if rfile.exists():
            for line in rfile.read_text(encoding="utf-8").splitlines():
                p = line.split("\t")
                if len(p) >= 8 and p[0] == "RESULT":
                    rows[p[1]] = {"id": p[1], "mode": p[2], "java": p[3], "jerr": p[4],
                                  "net": p[5], "nvj": p[6], "note": p[7]}

    # ---- merge + summary (fresh file per run) ----
    with open(OUT / "summary.tsv", "w", encoding="utf-8") as f:
        f.write("id\tmode\tjava\tjerr\tnet\tnerr\tnvj\tnote\n")
        for tid in ids:
            t = tests[tid]
            r = rows.get(tid, {"id": tid, "mode": "?", "java": "-", "jerr": "-",
                               "net": "-", "nvj": "-", "note": ""})
            nerr = count_errors(OUT / tid / "dotnet.log")
            if t["fail"]:  # .NET verdict for fail tests comes from the runner's exit code
                rc = net_rc.get(tid)
                r["net"] = ("-" if rc is None else "THREW" if rc == 2
                            else "SETUP-FAIL" if rc == 3 else "ERRORS" if nerr else "NO-THROW")
            f.write(f"{r['id']}\t{r['mode']}\t{r['java']}\t{r['jerr']}\t{r['net']}\t{nerr}\t{r['nvj']}\t{r['note']}\n")
    print(f"\nsummary -> {OUT / 'summary.tsv'}")


if __name__ == "__main__":
    main()
