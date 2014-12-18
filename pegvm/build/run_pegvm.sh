#!/bin/sh
make clean
make
echo "\nparse earthquake.geojson\n"
./pegvm -t stat -p ../../bytecode/json.bin ../../data/earthquake.geojson

echo "\nparse benchmark4.json\n"
./pegvm -t stat -p ../../bytecode/json.bin ../../../camp2014f/sample/JSON/benchmark4.json

echo "\nparse xmark5m.xml\n"
./pegvm -t stat -p ../../bytecode/xml.bin ../../data/xmark5m.xml

echo "\nparse nss_cache.c\n"
./pegvm -t stat -p ../../bytecode/c99.bin ../../data/nss_cache.c