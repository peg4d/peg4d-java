#!/bin/sh
PEGDIR=../benchNezVM/peg
BYTECODE=../benchNezVM/bytecodeFast
DUMP=../benchNezVM/dump_bytecodeFast

ant clean
ant

java -jar nez-0.9.2.jar conv --pegvm 7 -p $PEGDIR/json.p4d -o $BYTECODE/json.bin > $DUMP/json_bytecode.txt
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 7 -p $PEGDIR/xml.p4d -o $BYTECODE/xml.bin > $DUMP/xml_bytecode.txt
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 7 -p $PEGDIR/c99.p4d -o $BYTECODE/c99.bin > $DUMP/c99_bytecode.txt
echo "\ngenerate c99.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 8 -p $PEGDIR/json.p4d -o $BYTECODE/json_noobj.bin > $DUMP/json_noObj_bytecode.txt
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 8 -p $PEGDIR/xml.p4d -o $BYTECODE/xml_noobj.bin > $DUMP/xml_noObj_bytecode.txt
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 8 -p $PEGDIR/c99.p4d -o $BYTECODE/c99_noobj.bin > $DUMP/c99_noObj_bytecode.txt
echo "\ngenerate c99.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 9 -p $PEGDIR/json.p4d -o $BYTECODE/json_noInline.bin > $DUMP/json_noInline_bytecode.txt
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 9 -p $PEGDIR/xml.p4d -o $BYTECODE/xml_noInline.bin > $DUMP/xml_noInline_bytecode.txt
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 9 -p $PEGDIR/c99.p4d -o $BYTECODE/c99_noInline.bin > $DUMP/c99_noInline_bytecode.txt
echo "\ngenerate c99.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 10 -p $PEGDIR/json.p4d -o $BYTECODE/json_noobj_noInline.bin > $DUMP/json_noObj_noInline_bytecode.txt
echo "\ngenerate json.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 10 -p $PEGDIR/xml.p4d -o $BYTECODE/xml_noobj_noInline.bin > $DUMP/xml_noObj_noInline_bytecode.txt
echo "\ngenerate xml.bin\n"

java -jar nez-0.9.2.jar conv --pegvm 10 -p $PEGDIR/c99.p4d -o $BYTECODE/c99_noobj_noInline.bin > $DUMP/c99_noObj_noInline_bytecode.txt
echo "\ngenerate c99.bin\n"
