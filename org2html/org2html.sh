#!/bin/bash

set -e

host_dir="$(pwd)"/../blog/
target_dir=/mnt
output_file="$target_dir"/"$2"

docker run \
    --mount type=bind,source="$host_dir",target="$target_dir" \
    ryw/org2html ./org2html.py "$target_dir"/"$1" "$output_file"

# Move output to /tmp for cleanliness
tmpfile=$(mktemp)

mv "$host_dir"/"$2" "$tmpfile"

echo "Output HTML at: $tmpfile."
