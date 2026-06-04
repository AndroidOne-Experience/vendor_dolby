#!/usr/bin/env -S PYTHONPATH=../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

import os
from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'vendor/dolby/prebuilts',
]

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    if partition.startswith('_'):
        partition = partition[1:]  # Remove leading underscore

    if partition not in ('vendor', 'system'):
        return None

    return f'{lib}_{partition}'

lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    # (

    # ): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {

}  # fmt: skip

module = ExtractUtilsModule(
    'prebuilts',
    'dolby',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    device_rel_path='vendor/dolby',
)

# --- Cleanup Unlisted Blobs ---
ANDROID_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../"))
PROP_DIR = os.path.join(ANDROID_ROOT, "vendor/dolby/prebuilts/proprietary")
PROP_FILES_TXT = os.path.join(os.path.dirname(__file__), "proprietary-files.txt")

def get_expected_files():
    """Read proprietary-files.txt and return a set of expected file paths, ignoring SHA1SUM values and metadata."""
    expected_files = set()
    
    if not os.path.exists(PROP_FILES_TXT):
        print(f"Warning: {PROP_FILES_TXT} not found.")
        return expected_files

    with open(PROP_FILES_TXT, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue  # Ignore empty lines and comments
            
            # Extract part before | (to remove SHA1SUM) and before ; (to remove metadata)
            file_paths = line.split("|")[0].split(";")[0]

            # Split multiple paths by ':'
            for path in file_paths.split(":"):
                expected_files.add(path.lstrip("-"))  # Remove leading '-' if present

    return expected_files

def cleanup_unlisted_blobs():
    """Remove unlisted blobs from the proprietary folder."""
    if not os.path.exists(PROP_DIR):
        return

    expected_files = get_expected_files()

    for root, _, files in os.walk(PROP_DIR):
        for file in files:
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, PROP_DIR)

            # Ignore .part files silently
            if ".part" in file:
                continue

            if rel_path not in expected_files:
                print(f"Removing unlisted blob: {file_path}")
                os.remove(file_path)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
    cleanup_unlisted_blobs()  # Run cleanup after extraction
