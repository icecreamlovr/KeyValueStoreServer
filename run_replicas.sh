#!/bin/bash

# Check if argument is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <comma-separated-list-of-ports>"
    echo "Example: $0 3333,3334,3335,3336,3337"
    exit 1
fi

#gradle runServer --args "3333 3333,3334,3335,3336,3337" &

all_ports=$1

# Parse the comma-separated list of ports
IFS=',' read -ra ports <<< "$1"

# Process each integer using a for loop
for port in "${ports[@]}"; do
    gradle runServer --args "$port $all_ports" &
done