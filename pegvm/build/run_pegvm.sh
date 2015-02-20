#!/bin/sh
DATADIR=../../../benchNEZVM/input
BYTECODE=../../../benchNEZVM/bytecodeFast
make clean
make
echo "\nparse earthquake.geojson\n"
./pegvm -t stat -p $BYTECODE/json.bin -i $DATADIR/earthquake.geojson

echo "\nparse benchmark4.json\n"
./pegvm -t stat -p $BYTECODE/json.bin -i $DATADIR2/benchmark4.json

echo "\nparse xmark5m.xml\n"
./pegvm -t stat -p $BYTECODE/xml.bin -i $DATADIR/xmark5m.xml

echo "\nparse nss_cache.c\n"
./pegvm -t stat -p $BYTECODE/c99.bin -i $DATADIR/nss_cache.c
