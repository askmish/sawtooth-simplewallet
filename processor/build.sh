#!/usr/bin/env bash

set -e

mkdir -p build
cd build
cmake ..
make
chmod +x bin/simplewallet-tp
bin/simplewallet-tp tcp://validator:4004
