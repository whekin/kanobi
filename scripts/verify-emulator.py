#!/usr/bin/env python3
"""Regression check for Journal -> Settings -> Zero on an English-language emulator.

Run after installing the APK: python3 scripts/verify-emulator.py --serial emulator-5554
Only uses the test emulator; does not erase app data or saved measurements.
"""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--full', action='store_true', help='Also check hold, keyboard entry, rapid double save, persistence and deletion.')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    parser.error('Use a disposable emulator, not a physical phone.')


def adb(*parts):
    return subprocess.check_output(['adb', '-s', args.serial, *parts], text=True, timeout=30)


def snapshot():
    adb('shell', 'uiautomator', 'dump', '/sdcard/kanobi-check.xml')
    output = adb('shell', 'cat', '/sdcard/kanobi-check.xml')
    match = re.search(r'<\?xml.*?</hierarchy>', output, re.S)
    if not match:
        raise AssertionError(f'No UI XML: {output[:150]}')
    return ET.fromstring(match.group())


def locate(root, *, text=None, desc=None):
    return next((n for n in root.iter('node') if
                 (text is not None and n.get('text') == text) or
                 (desc is not None and n.get('content-desc', '').startswith(desc))), None)


def wait_for(**selector):
    deadline = time.monotonic() + 25
    while True:
        root = snapshot()
        node = locate(root, **selector)
        if node is not None:
            return root, node
        if time.monotonic() >= deadline:
            raise AssertionError(f'Missing {selector}; text: {[n.get("text") for n in root.iter("node") if n.get("text")]}')
        time.sleep(.3)


def tap(**selector):
    root, node = wait_for(**selector)
    parents = {child: parent for parent in root.iter() for child in parent}
    while node.get('clickable') != 'true' and node in parents:
        node = parents[node]
    assert node.get('clickable') == 'true', f'Not clickable: {selector}'
    assert node.get('enabled') == 'true', f'Disabled: {selector}'
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.5)


def main():
    adb('shell', 'am', 'force-stop', 'com.kanobi.app')
    adb('shell', 'cmd', 'locale', 'set-app-locales', 'com.kanobi.app', '--user', '0', '--locales', 'en')
    adb('emu', 'sensor', 'set', 'acceleration', '0:0:9.81')
    adb('shell', 'am', 'start', '-W', '-n', 'com.kanobi.app/.MainActivity')
    root = snapshot()
    if locate(root, text='Got it') is not None:
        tap(text='Got it')
    tap(desc='Journal,')
    tap(desc='Settings')
    tap(text='Zero calibration')
    wait_for(text='Ready to zero')
    root, node = wait_for(text='Set zero')
    parents = {child: parent for parent in root.iter() for child in parent}
    while node.get('clickable') != 'true' and node in parents:
        node = parents[node]
    assert node.get('enabled') == 'true', 'Set zero must be enabled from the journal'
    print('PASS: calibration from journal has fresh stable readings and an enabled Set zero button')
    if args.full:
        verify_save_flow()


def verify_save_flow():
    tap(text='Set zero')
    tap(desc='Close journal')
    tap(desc='Lock')
    wait_for(desc='Save')
    adb('emu', 'sensor', 'set', 'acceleration', '0:4.905:8.496')
    time.sleep(3)
    root = snapshot()
    assert any(n.get('text') == '0.0' for n in root.iter('node')), 'Locked angle changed with phone tilt'
    print('PASS: hold stays at 0.0 after changing the virtual tilt', flush=True)
    tap(desc='Save')
    wait_for(text='Save measurement')
    tap(text='Name')
    root = snapshot()
    assert any(n.get('class') == 'android.widget.EditText' and n.get('focused') == 'true'
               for n in root.iter('node')), 'Name field not focused'
    name = 'KanobiRegression' + str(int(time.time()))
    adb('shell', 'input', 'text', name)
    root, node = wait_for(text='Save')
    parents = {child: parent for parent in root.iter() for child in parent}
    while node.get('clickable') != 'true' and node in parents:
        node = parents[node]
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    for _ in range(2):
        adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    tap(desc='Journal,')
    root, _ = wait_for(text=name)
    assert sum(n.get('text') == name for n in root.iter('node')) == 1
    print('PASS: keyboard entry and rapid double save produce one journal entry', flush=True)
    adb('shell', 'am', 'force-stop', 'com.kanobi.app')
    adb('shell', 'am', 'start', '-W', '-n', 'com.kanobi.app/.MainActivity')
    tap(desc='Journal,')
    wait_for(text=name)
    print('PASS: saved measurement survives process restart', flush=True)
    tap(desc='Delete ' + name)
    tap(text='Delete')
    root = snapshot()
    assert locate(root, text=name) is None
    print('PASS: deletion with confirmation', flush=True)



if __name__ == "__main__":
    main()
