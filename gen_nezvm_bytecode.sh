#!/bin/sh
PEGDIR=../benchNEZVM/peg
PEGDIR=../benchNEZVM/bytecodeFast

ant clean
ant

java -jar nez-0.9.2.jar conv --pegvm 7 -p ../benchNEZVM/peg/json.p4d -o ../benchNEZVM/bytecodeFast/json.bin > ../benchNEZVM/dump_bytecodeFast/json_bytecode.txt
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 7 -p ../benchNEZVM/peg/xml.p4d -o ../benchNEZVM/bytecodeFast/xml.bin > ../benchNEZVM/dump_bytecodeFast/xml_bytecode.txt
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 7 -p ../benchNEZVM/peg/c99.p4d -o ../benchNEZVM/bytecodeFast/c99.bin > ../benchNEZVM/dump_bytecodeFast/c99_bytecode.txt
echo "\ngenerate c99.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 8 -p ../benchNEZVM/peg/json.p4d -o ../benchNEZVM/bytecodeFast/json_noobj.bin > ../benchNEZVM/dump_bytecodeFast/json_noObj_bytecode.txt
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 8 -p ../benchNEZVM/peg/xml.p4d -o ../benchNEZVM/bytecodeFast/xml_noobj.bin > ../benchNEZVM/dump_bytecodeFast/xml_noObj_bytecode.txt
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 8 -p ../benchNEZVM/peg/c99.p4d -o ../benchNEZVM/bytecodeFast/c99_noobj.bin > ../benchNEZVM/dump_bytecodeFast/c99_noObj_bytecode.txt
echo "\ngenerate c99.bin\n"