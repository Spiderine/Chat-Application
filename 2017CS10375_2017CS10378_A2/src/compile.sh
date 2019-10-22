#!/bin/bash
read mode

	if [  $mode = "Normal" ]
	  then javac server1.java client1.java
	elif [ $mode = "Encrypt" ]
	  then javac server2.java client2.java
	else [ $mode = "Signature" ]
          javac server3.java client3.java
	fi

