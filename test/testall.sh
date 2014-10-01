#!/bin/bash

OUT=/dev/null
PASS="((PASS))"
FAIL="((FAIL))"
NL=""
#PASS="\e[34m((PASS))"
#FAIL="\e[91m((FAIL))"
#NL="\e[0m"

function test_failure {
	java -ea -jar peg4d.jar -p $1 test/empty >> OUT
	if [ $? -eq 0 ]; then
        	echo -e "$FAIL $1 $NL"
	else
        	echo -e "$PASS $1 $NL"
	fi
}

function test_match {
	java -ea -jar peg4d.jar -p $1 test/empty >> OUT
	if [ $? -eq 0 ]; then
		echo -e "$PASS $1 $NL"
	else
		echo -e "$FAIL $1 $NL"
	fi
}

function test_tag {
	RESULT=$(java -ea -jar peg4d.jar -p $1 -t tag $2)
	if [ $? -eq 0 -a {$RESULT} = {$3} ]; then
		echo -e "$PASS $1 $RESULT $NL"
	else
		echo -e "$FAIL $1 $RESULT $NL"
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

test_tag test/xml.p4d test/build.xml "#Attr:129#Element:88#Name:217#Text:3#Value:129"

test_tag test/c.p4d test/test.c "#Add:1#Address:2#Apply:186#ArrayName:10#Assign:100#Block:113#Break:9#Cast:4#Character:1#Declaration:69#Define:2#Div:1#Empty:1#Equals:45#ExpressionStatement:215#Field:46#Function:34#GreaterThan:11#If:65#Include:3#Integer:31#Key:19#KeyValue:19#LeftShift:1#LessThan:1#List:68#Minus:1#Name:792#Not:3#NotEquals:5#Param:79#PointerField:17#PrefixInc:1#Return:69#Source:1#Star:10#String:3#Sub:6#Switch:2#SwitchCase:4#SwitchDefault:2#TConst:9#TEnum:30#TInt:38#TName:13#TPointer:57#TStruct:17#TVoid:11#Value:19#VarDecl:68#While:6"


