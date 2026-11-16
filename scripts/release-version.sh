#!/usr/bin/env sh
# Derive the app's version from a release tag.
#
# A release tag is vMAJOR.MINOR.PATCH. The version name is the tag without the
# "v", which is what Obtainium compares with the installed app. The version
# code is MAJOR * 10000 + MINOR * 100 + PATCH, so it rises with every release,
# which Android requires for an update; MINOR and PATCH therefore stop at 99.
# See docs/decisions/release-versioning-and-signing.md.
#
# Usage: release-version.sh <tag>
# Prints two lines, versionName=<name> and versionCode=<code>, in the form a
# GitHub Actions step appends to $GITHUB_OUTPUT. Exits 1 for any other tag, and for v0.0.0.
set -eu

tag=${1:-}

# Numbers have no leading zeros, so each version has exactly one tag.
number='\(0\|[1-9][0-9]*\)'
small='\(0\|[1-9][0-9]\{0,1\}\)'
if ! printf '%s\n' "$tag" | grep -q "^v$number\.$small\.$small\$" || [ "$(printf '%s\n' "$tag" | wc -l)" -ne 1 ]; then
    echo >&2 "release-version: '$tag' is not a release tag. Use vMAJOR.MINOR.PATCH with MINOR and PATCH from 0 to 99, for example v1.4.2."
    exit 1
fi

name=${tag#v}
major=${name%%.*}
rest=${name#*.}
minor=${rest%%.*}
patch=${rest#*.}

# Android requires a positive version code, and 0.0.0 would give zero.
if [ "$name" = "0.0.0" ]; then
    echo >&2 "release-version: '$tag' would give version code 0, which Android does not accept."
    exit 1
fi

# Google Play accepts no version code above 2100000000. koj is not published
# there, but staying within that limit keeps the code a number every tool accepts.
if [ "${#major}" -gt 6 ] || [ "$major" -gt 209999 ]; then
    echo >&2 "release-version: '$tag' has a major version too large for an Android version code."
    exit 1
fi

printf 'versionName=%s\n' "$name"
printf 'versionCode=%s\n' $((major * 10000 + minor * 100 + patch))
