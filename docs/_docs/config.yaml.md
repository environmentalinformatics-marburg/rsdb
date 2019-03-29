---
title: config.yaml
---

This file specifies configuration of RSDB. If file or entries are missing default values will be used.

[<i class="fa fa-arrow-right"/> structure](#structure)

[<i class="fa fa-arrow-right"/> example](#example)

### structure

Fileformat is [YAML](https://yaml.org/).

| top level entry | description |
| ------------- | ------------- |
| **server**  | server configuration  |
| **jws**  | JWS configuration  |

~~~ yaml
# server config
server:
  # HTTP port (default 8081) 
  #port: 8081
  
  # HTTPS port (default 8082) 
  #secure_port: 8082

  # password of certificate store (default none)
  #keystore_password: password  
  
  # login protected HTTP and HTTPS ports (default true)  
  #login: true
  
  # JWS port (default none)
  #jws_port: 8083

# JWS config: list of JWS provider entries  
jws:
  # first entry of list
    # URL of provider
  #- provider_url: https://example.com/sso

    # text that is shown on link to provider
    #link_text: '--> authenticate by <b>example provider</b> <--'

    # text that is shown as description
    #link_description: 'get account infos from example provider'

    # client id
    #client_key_id: rsdb

    # client private key
    #client_private_key: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    # provider key id
    #provider_key_id: example

    # provider public key
    #provider_public_key: bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

~~~

### example
~~~ yaml
server:
  port: 8081  
  secure_port: 8082
  keystore_password: password 
  login: true
  jws_port: 8083

jws:
  - provider_url: https://example.com/sso
    link_text: '--> authenticate by <b>example provider</b> <--'
    link_description: 'get account infos from example provider'
    client_key_id: rsdb
    client_private_key: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    provider_key_id: example
    provider_public_key: bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb   
~~~