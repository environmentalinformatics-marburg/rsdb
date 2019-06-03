---
title: config.yaml
---

This file specifies configuration of RSDB. If file or entries are missing default values will be used.

[<i class="fa fa-arrow-right"/> structure](#structure)

[<i class="fa fa-arrow-right"/> examples](#example)

### structure

Fileformat is [YAML](https://yaml.org/): Line-indentation structures entries. Use **[SPACE]**-key for line-indentation, not [TAB].

YAML examples (here top level is always key value):
~~~ yaml
# comments start by # character
top_level_key1:
  key1: value1
  key2: value2
top_level_key2:
  - "list entry 1"
  - "list entry 2" 
top_level_key3:
  - list_entry1_key1: value1
    list_entry1_key2: value2
  - list_entry2_key1: value1
  - list_entry3_key1: value1
    list_entry3_key2: value2
    list_entry3_key3: [sublist_entry1, "sublist entry1", sublist_entry3]    
~~~    


| top level entry | description |
| ------------- | ------------- |
| **server**  | server configuration (key-value pairs)  |
| **jws**  | JWS configuration (list of jws-entries that contain key-value pairs) |
| **pointdb**  | PointDB layer properties (list of pointdb-layer-entries that contain key-value pairs) |

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
    #client_private_key: keykeykey

    # provider key id
    #provider_key_id: example

    # provider public key
    #provider_public_key: keykeykey

# list of PointDB properties
pointdb:
  # first entry of list
    # layer name
  #- name: layer1    
    
    # storage path of layer (relative or absolute)
    #db: pointdb/layer1
    
    # title of layer
    #title: Layer One

    # description of layer
    #description: This is layer one 
    
    # PROJ4 of projection
    #proj4: "+proj=utm +zone=32 +ellps=WGS84 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "
    
    # EPSG code (proj4 and epsg should specified the same projection)
    #epsg: 32632
    
    # if points are pre classified: one of 'ground' or 'vegetation' or 'true' (both ground and vegetation)
    #classified: ground
    
    # access roles that can read this layer
    #acl: someLayers
    
    # one or list of tags
    #tags: exploratories

    # associated rasterdb (rasterized point data for visualisation)
    #rasterdb: layer1_rasterized  

    # POI group one layer name or list of names
    #poi: some_poi

    # ROI group one layer name or list of names
    #roi: some_roi
~~~

### examples

~~~ yaml
server:
  port: 8081  

pointdb:
  - name: layer1    
    db: pointdb/layer1
    proj4: "+proj=utm +zone=32 +ellps=WGS84 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "
    epsg: 32632
    rasterdb: layer1_rasterized   
~~~

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
    client_private_key: keykeykey
    provider_key_id: example
    provider_public_key: keykeykey
    
pointdb:
  - name: layer1    
    db: pointdb/layer1
    title: Layer One
    description: This is layer one 
    proj4: "+proj=utm +zone=32 +ellps=WGS84 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "
    epsg: 32632
    classified: ground
    acl: public_layers
    tags: point_data
    rasterdb: layer1_rasterized  
    poi: some_poi
    roi: some_roi    
~~~