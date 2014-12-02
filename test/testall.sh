#!/bin/bash

OUT=/dev/null
#PASS="((PASS))"
#FAIL="((FAIL))"
#NL=""
PASS="\0033[34m((PASS))"
FAIL="\0033[91m((FAIL))"
NL="\0033[0m"
JAR="nez-0.9.jar"

function test_failure {
	java -ea -jar $JAR parse -p $1 $OPTION -i test/empty >> OUT
	if [ $? -eq 0 ]; then
        	echo -e "$FAIL $1 $NL"
	else
        	echo -e "$PASS $1 $NL"
	fi
}

function test_match {
	java -ea -jar $JAR parse -p $1 $OPTION -i test/empty >> OUT
	if [ $? -eq 0 ]; then
		echo -e "$PASS $1 $NL"
	else
		echo -e "$FAIL $1 $NL"
	fi
}

function test_tag {
	RESULT=$(java -ea -jar $JAR parse -p $1 -t tag $OPTION -i $2)
	if [ $? -eq 0 -a {$RESULT} = {$3} ]; then
		echo -e "$PASS $1 $NL"
	else
		echo -e "$FAIL $1"
		echo -e "{$RESULT}$NL"
		echo "{$3}"
	fi
}

test_failure test/always_fail.p4d
test_failure test/example.p4d
test_failure test/bad-example.p4d
test_failure test/leftrecursion.p4d
test_failure test/indirectleftrecursion.p4d

test_match test/peg.p4d
test_match test/rule.p4d
test_match test/aaa.p4d
test_match test/object.p4d
test_match test/not.p4d
test_match test/choice.p4d
test_match test/flag.p4d
test_match test/flag2.p4d

test_match test/cyclic.p4d

test_tag test/PostScript.p4d test/test.ps "#Add:3#Integer:4"

test_tag test/xml.p4d test/build.xml "#Attr:129#Element:88#Name:217#Text:3#Value:129"
test_tag test/import.p4d test/build.xml "#Attr:129#Element:88#Name:217#Text:3#Value:129"

test_tag test/c.p4d test/test.c "#Add:9#Address:21#Apply:203#ArrayName:10#Assign:100#Block:113#Break:9#Cast:4#Character:1#Declaration:69#Define:2#Div:1#Empty:1#Equals:45#ExpressionStatement:215#Field:50#Function:34#GreaterThan:11#If:65#Include:3#Integer:52#Key:26#KeyValue:26#LeftShift:1#LessThan:1#List:341#Minus:1#Name:1164#Not:3#NotEquals:5#Param:81#PointerField:59#PrefixInc:1#Return:69#SizeOf:2#Source:1#Star:14#String:75#Sub:14#Switch:2#SwitchCase:4#SwitchDefault:2#TConst:14#TEnum:50#TFunc:1#TInt:65#TName:17#TPointer:86#TStruct:32#TVoid:13#Value:26#VarDecl:68#While:6"



