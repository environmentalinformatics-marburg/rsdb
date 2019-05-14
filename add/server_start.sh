#!/bin/bash

sessions=$(screen -ls rsdb | grep rsdb | wc -l)

if [ $sessions -ne 0 ]
then
	#echo
	echo "NOTE: screen session 'rsdb' already running ==> not started new session."
	#echo
	exit 10
fi

screen -S rsdb -d -m ./rsdb.sh server

for i in {1..60}
do
	sleep 1
	./server_status.sh --quiet
	return_code=$?
	if [ $return_code -ne 0 ]
	then
		#echo
		echo "waiting for rsdb server to start. Waiting $i of 60 seconds..."
		#echo
	fi
	if [ $return_code -eq 0 ]
	then
		#echo
		#echo "OK rsdb server is running."
		#echo
		#exit 0
                break
	fi
done

./server_status.sh
return_code=$?
exit $return_code
