#!/bin/sh
DATADIR=../../data
DATADIR2=../../../camp2014f
BYTECODE=../../bytecode
make clean
make
echo "\nparse earthquake.geojson\n"
./pegvm -t stat -p $BYTECODE/json.bin $DATADIR/earthquake.geojson

echo "\nparse benchmark4.json\n"
./pegvm -t stat -p $BYTECODE/json.bin $DATADIR2/sample/JSON/benchmark4.json

echo "\nparse xmark5m.xml\n"
./pegvm -t stat -p $BYTECODE/xml.bin $DATADIR/xmark5m.xml

echo "\nparse nss_cache.c\n"
./pegvm -t stat -p $BYTECODE/c99.bin $DATADIR/nss_cache.c
