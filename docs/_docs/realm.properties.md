---
title: realm.properties
---

This file lists local user accounts. If file is missing default no user accounts are aviable and RSDB may be operated in mode without login ( login: false in config.yaml).

[<i class="fa fa-arrow-right"/> structure](#structure)

[<i class="fa fa-arrow-right"/> example](#example)

### structure

Fileformat is [.properties](https://en.wikipedia.org/wiki/.properties).

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
# format:
#user:password,role
#
# e.g.
#user1:passwor1,role1
#user2:password2,role1,role2
~~~

### example
~~~  conf
user1:password1,admin
user2:password2,role1,role2  
~~~