#!/usr/bin/env sh
# Install the pinned check tools that have no distribution package: ktfmt
# (formatting) and osv-scanner (dependency audit).
#
# Developers run this once; CI runs it on every job that needs the tools, so
# both use the same versions. Each download is verified against the SHA-256
# recorded here before it is installed.
#
# Usage: install-check-tools.sh [install-dir]    (default: ~/.local/bin)
set -eu

ktfmt_version=0.65
ktfmt_sha256=3684327b092db9b76fffcb9525decf73f8331ed17e00ce3232769c11cd36d352
osv_scanner_version=2.7.1
osv_scanner_sha256=fb7dab19a8bd9bbe5c3189f09d4b7ee1820c9553a23bac202648a56efd2f6ee8

install_dir=${1:-$HOME/.local/bin}

if [ "$(uname -s)-$(uname -m)" != "Linux-x86_64" ]; then
    echo >&2 "install-check-tools: only Linux x86_64 downloads are pinned here."
    echo >&2 "Install ktfmt $ktfmt_version and osv-scanner $osv_scanner_version by hand for $(uname -s) $(uname -m)."
    exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Download a file and stop unless it has the expected SHA-256.
fetch() {
    url=$1
    expected=$2
    target=$3
    curl --fail --silent --show-error --location --output "$target" "$url"
    actual=$(sha256sum "$target" | cut -d' ' -f1)
    if [ "$actual" != "$expected" ]; then
        echo >&2 "install-check-tools: checksum mismatch for $url"
        echo >&2 "  expected $expected"
        echo >&2 "  actual   $actual"
        exit 1
    fi
}

mkdir -p "$install_dir"

fetch "https://github.com/Kotlin/ktfmt/releases/download/v$ktfmt_version/ktfmt-linux-x86_64-$ktfmt_version.tar.gz" \
    "$ktfmt_sha256" "$work/ktfmt.tar.gz"
tar -xzf "$work/ktfmt.tar.gz" -C "$work"
install -m 755 "$work/ktfmt-linux-x86_64-$ktfmt_version/ktfmt" "$install_dir/ktfmt"

fetch "https://github.com/google/osv-scanner/releases/download/v$osv_scanner_version/osv-scanner_linux_amd64" \
    "$osv_scanner_sha256" "$work/osv-scanner"
install -m 755 "$work/osv-scanner" "$install_dir/osv-scanner"

echo "Installed ktfmt $ktfmt_version and osv-scanner $osv_scanner_version in $install_dir"
case ":$PATH:" in
    *":$install_dir:"*) ;;
    *) echo "Add $install_dir to PATH so the git hooks can find them." ;;
esac
