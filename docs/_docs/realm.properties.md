---
title: realm.properties
---

This file lists local user accounts. If file is missing default no user accounts are aviable and RSDB may be operated in mode without login ( login: false in config.yaml).

[<i class="fa fa-arrow-right"/> structure](#structure)

[<i class="fa fa-arrow-right"/> example](#example)

### structure

Fileformat is [.properties](https://en.wikipedia.org/wiki/.properties). Lines starting with '#' are ignored (interpreted as comment).

one entry per line:

**user:password,role**

Special role 'admin' includes all roles.

| entry | description |
| ------------- | ------------- |
| **user**  | user name  |
| **password**  | password |
| **roles**  | list of roles  |

~~~ conf
# list of of accounts with name, password, roles
# (role "admin": access to all roles)
#
# Lines starting with # are ignored (interpreted as comment)
#
# format:
#user:password,role
#
# e.g.
#user1:passwor1,role1
#user2:password2,role1,role2
#
#local_user:change_password,admin
~~~

### example
~~~  conf
local_user:change_passwordA,admin
user1:change_passwordB,admin
user2:change_passwordC,role1,role2  
~~~