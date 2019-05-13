---
title: realm_ip.csv
---

This file lists IP addresses that are associated with one an account directly without login. If file is missing, no IPs are associated.

[<i class="fa fa-arrow-right"/> structure](#structure)

[<i class="fa fa-arrow-right"/> example](#example)

### structure

Fileformat is [CSV](https://en.wikipedia.org/wiki/Comma-separated_values). Lines starting with '#' are ignored (interpreted as comment).

first line csv header and one entry per line:

**ip,user**

| entry | description |
| ------------- | ------------- |
| **ip**  | IPv4 or IPv6 |
| **user**  | user name, needs to be specified in 'realm.properties' file  |

~~~ conf
ip,user
#127.0.0.1,user1
#0:0:0:0:0:0:0:1,user1
~~~

### example
User on local computer does not need to login. (All other users are prompted for login.)
~~~ conf
ip,user
127.0.0.1,user1
0:0:0:0:0:0:0:1,user1
~~~