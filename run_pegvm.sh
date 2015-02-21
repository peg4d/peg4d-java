#!/bin/sh
DATADIR=../benchNEZVM/input
BYTECODE=../benchNEZVM/bytecodeFast
BUILD=./vm/
make clean -C $BUILD
make -C $BUILD
echo "\nparse earthquake.geojson\n"
$BUILD/pegvm -t stat -p $BYTECODE/json.bin -i $DATADIR/earthquake.geojson

echo "\nparse benchmark4.json\n"
$BUILD/pegvm -t stat -p $BYTECODE/json.bin -i $DATADIR/benchmark4.json

echo "\nparse xmark5m.xml\n"
$BUILD/pegvm -t stat -p $BYTECODE/xml.bin -i $DATADIR/xmark5m.xml

echo "\nparse nss_cache.c\n"
$BUILD/pegvm -t stat -p $BYTECODE/c99.bin -i $DATADIR/nss_cache.c
