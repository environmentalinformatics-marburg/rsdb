#!/bin/bash
exec ./rsdb.sh server 2>&1 | tee -a server_log.txt
