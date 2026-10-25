#!/usr/bin/env python3
"""Summarize adb dumpsys gfxinfo framestats; optional gates make a device replay red/green.

Example: python3 scripts/measure-frame-stats.py frames.txt --max-jank-percent 5 --min-fps 110
CPU/GPU columns are pipeline spans, not isolated shader execution time. Per-frame rows are
Android's trailing sample; aggregate frame/jank counts cover the full reset-to-capture window.
"""
import argparse
import csv
import json
import pathlib
import re

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('files', nargs='+')
parser.add_argument('--max-jank-percent', type=float)
parser.add_argument('--min-fps', type=float)
args = parser.parse_args()
failed = False


def percentile(values, fraction):
    values = sorted(v for v in values if 0 <= v < 1000)
    if not values:
        return None
    return round(values[int((len(values) - 1) * fraction)], 2)


for filename in args.files:
    text = pathlib.Path(filename).read_text()
    chunks = text.split('---PROFILEDATA---')
    rows = []
    for i in range(1, len(chunks), 2):
        for raw in csv.DictReader(chunks[i].strip().splitlines()):
            try:
                row = {k: int(v) for k, v in raw.items() if k}
            except (ValueError, TypeError):
                continue
            if row.get('Flags') == 0 and row.get('FrameCompleted', 0) > 0:
                rows.append(row)
    result = {'file': filename, 'sampled_frames': len(rows)}
    uptime = re.search(r'Uptime: (\d+)', text)
    since = re.search(r'Stats since: (\d+)', text)
    count = re.search(r'Total frames rendered: (\d+)', text)
    jank = re.search(r'Janky frames: \d+ \(([\d.]+)%\)', text)
    if uptime and since and count:
        seconds = (int(uptime[1]) * 1_000_000 - int(since[1])) / 1e9
        result['fps'] = round(int(count[1]) / seconds, 2) if seconds > 0 else 0
    if jank:
        result['jank_percent'] = float(jank[1])

    for label, end, start in [
        ('total', 'FrameCompleted', 'IntendedVsync'),
        ('main_work', 'SyncQueued', 'FrameStartTime'),
        ('layout_record', 'SyncQueued', 'PerformTraversalsStart'),
        ('gpu_pipeline', 'GpuCompleted', 'IssueDrawCommandsStart'),
        ('start_late', 'Vsync', 'IntendedVsync'),
    ]:
        values = [(r[end] - r[start]) / 1e6 for r in rows
                  if r.get(end, 0) > 0 and r.get(start, 0) > 0]
        result[label] = {f'p{p}_ms': percentile(values, p / 100) for p in [50, 90, 95, 99]}
    print(json.dumps(result, indent=2))
    if args.max_jank_percent is not None:
        failed |= result.get('jank_percent', 100) > args.max_jank_percent
    if args.min_fps is not None:
        failed |= result.get('fps', 0) < args.min_fps
raise SystemExit(1 if failed else 0)
