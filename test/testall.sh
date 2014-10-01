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
