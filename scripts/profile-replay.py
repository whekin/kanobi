#!/usr/bin/env python3
"""Capture an identical fast pitch/roll replay from the separate benchmark APK.

Build/install :app:assembleBenchmark first. The production app and real measurements are untouched.
"""
import argparse
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--effects', choices=['on', 'off'], default='on')
parser.add_argument('--seconds', type=int, default=12)
parser.add_argument('--warmup', type=int, default=4)
parser.add_argument('--gpu-load', type=float, default=1, help='Multiplier for background pixel work only (1..4).')
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
if not 5 <= args.seconds <= 60:
    parser.error('--seconds must be between 5 and 60')


def adb(*parts):
    return subprocess.check_output(['adb', '-s', args.serial, *parts], text=True, timeout=30)


package = 'com.kanobi.app.benchmark'
adb('shell', 'am', 'force-stop', package)
adb('shell', 'am', 'start', '-W', '-n', f'{package}/com.kanobi.app.benchmark.ReplayActivity', '--es', 'effects', args.effects, '--ef', 'gpu-load', str(args.gpu_load))
time.sleep(args.warmup)
adb('shell', 'dumpsys', 'gfxinfo', package, 'reset')
time.sleep(args.seconds)
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(adb('shell', 'dumpsys', 'gfxinfo', package, 'framestats'))
print(f'Saved {args.seconds}s replay with effects {args.effects}: {args.output}')
