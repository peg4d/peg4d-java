#!/bin/sh
PEGDIR=../benchNEZVM/peg
PEGDIR=../benchNEZVM/bytecodeFast

ant clean
ant

java -jar nez-0.9.2.jar conv --pegvm 7 -p ../benchNEZVM/peg/json.p4d -o ../benchNEZVM/bytecodeFast/json.bin
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 7 -p ../benchNEZVM/peg/xml.p4d -o ../benchNEZVM/bytecodeFast/xml.bin
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 7 -p ../benchNEZVM/peg/c99.p4d -o ../benchNEZVM/bytecodeFast/c99.bin
echo "\ngenerate c99.bin\n"