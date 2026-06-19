#!/bin/bash
#
# Copyright (C) 2024 Telegram X authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

pushd "$THIRDPARTY_LIBRARIES/flac"

COMPAT_HEADER="include/share/compat.h"

if [[ ! -f "${COMPAT_HEADER}" ]]; then
  echo "Please make sure you have checked out libflac."
  exit 1
fi

# Android API >= 24 always declares fseeko/ftello in <stdio.h>. libFLAC's compat.h
# otherwise falls back to "#define fseeko fseek" whenever HAVE_FSEEKO is unset (which
# happens for 32-bit ABIs, where flac's own check_function_exists() returns a false
# negative). That macro then rewrites the platform's own fseeko declaration into a
# conflicting fseek, breaking the build at minSdk 24+. Guard the fallback so Android
# API >= 24 uses the real fseeko/ftello.
if grep -q "tgx-patch: fseeko" "${COMPAT_HEADER}"; then
  echo "libflac compat.h already patched. Skipping."
else
  perl -0pi -e 's/#ifndef HAVE_FSEEKO\n#define fseeko fseek/#if !defined(HAVE_FSEEKO) && !(defined(__ANDROID__) && __ANDROID_API__ >= 24) \/* tgx-patch: fseeko exists on Android API>=24 *\/\n#define fseeko fseek/' "${COMPAT_HEADER}"
  if ! grep -q "tgx-patch: fseeko" "${COMPAT_HEADER}"; then
    echo "Failed to patch ${COMPAT_HEADER}!"
    exit 1
  fi
  echo "Patched libflac compat.h for Android API >= 24 fseeko/ftello."
fi

popd
