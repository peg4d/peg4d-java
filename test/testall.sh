#!/bin/bash

OUT=/dev/null

function test_failure {
	java -ea -jar peg4d.jar -p $1 test/empty >> OUT
	if [ $? -eq 0 ]; then
        	echo "((FAIL)) $1"
	else
        	echo "((PASS)) $1"
	fi
}

function test_match {
	java -ea -jar peg4d.jar -p $1 test/empty >> OUT
	if [ $? -eq 0 ]; then
		echo "((PASS)) $1"
	else
		echo "((FAIL)) $1"
	fi
}

test_failure test/example.p4d
test_failure test/bad-example.p4d

test_match test/peg.p4d
test_match test/rule.p4d
test_match test/aaa.p4d
test_match test/object.p4d
test_match test/not.p4d
test_match test/choice.p4d
