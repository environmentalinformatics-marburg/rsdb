#!/bin/bash

quiet=false
port=8081

source yaml.sh
create_variables config.yaml config_
if [ -n "$config_server_port" ]; then port=$config_server_port; fi

while :; do
    case $1 in
        -q|--quiet)
            quiet=true
            ;;
        *)               # Default case: No more options, so break out of the loop.
            break
    esac

    shift
done


sessions=$(screen -ls rsdb | grep rsdb | wc -l)

if [ $sessions -eq 0 ]
then
	if [ "$quiet" = false ] ; then
	    #echo
	    echo "NOTE: screen session 'rsdb' is NOT running."
	    #echo
	fi
	exit 1
fi

#echo
#echo "screen session 'rsdb' is running."
#echo

listen=$(ss --numeric --listening --tcp "( sport = :$port )" | grep $port | wc -l)

if [ $listen -eq 0 ]
then
	if [ "$quiet" = false ] ; then
		#echo
		echo "NOTE: screen session 'rsdb' is running, but a server is NOT listening on port $port."
		#echo
	fi
	exit 2
fi

#echo
#echo "screen session 'rsdb' is running and a server is listening on port $port."
#echo

server=$(ss --numeric --listening --tcp "( sport = :$port )" | grep $port | wc -l)

if [ $server -eq 0 ]
then
	if [ "$quiet" = false ] ; then
		#echo
		echo "NOTE: screen session 'rsdb' is running and a server is listening on port $port, but maybe not rsdb server."
		#echo
	fi
	exit 3
fi

if [ "$quiet" = false ] ; then
	#echo
	echo "OK: screen session 'rsdb' is running and rsdb server is listening on port $port."
	#echo
fi
exit 0

