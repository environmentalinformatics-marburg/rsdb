---
title: postgis layer (vector data)
---

Layers of type postgis contain vector data from a database table of a connected PostGIS server.

## PostGIS server connection

The connection to a PostGIS server is specified in the [config.yaml](../config.yaml)-file.

## postgis layers

In the RSDB root-folder the postgis layers are located in the folder `postgis`. You may create that folder if it does not exist.

Directly in the `postgis`-folder, the contained files are the postgis layers (in contrast to the other layer types that are structured in sub-folders).

The file names are the layer names and end with `.yaml`. E.g. a `postgis`-folder may contain the files `mylayer1.yaml` and `mylayer2.yaml`, the layer names would be then `mylayer1` and `mylayer2`.

## postgis layer configuration

**The layer name needs to be the exact name of the corresponding PostGIS table name.**

If the table is located in a PostGIS hierarchy, the path needs to be included in the name separated by dot (the notation in PostGIS). E.g. table 'table1' contained in 'base1' --> `base1.table1.yaml`

The configuration is specified in the layer YAML file.

Minimal content of a `layer.yaml`-file:
~~~ yaml
type: PostGIS
~~~

When the layer file is placed in the `postgis`-folder, several common properties can be managed by the web-interface like in the other layers. Common properties are the informal properties (like description or tags), access control (acl, acl_mod, acl_owner) and structured_access (poi, roi). The common properties are omitted in the following.

Example with named class fields and visualisation style:
~~~ yaml
type: PostGIS
name_field: ''
class_fields: [class_0, class_1, class_2]
style:
  type: basic
  stroke_color: '#F5F5F5F5'
  stroke_width: 1.0
  fill_color: '#EAEAEAD9'
  value_field: class_0
  values:
    '1': {type: basic, stroke_color: '#7C878AF5', stroke_width: 1.0, fill_color: '#B5C4C8F5'}
    '2': {type: basic, stroke_color: '#7E7E7EF5', stroke_width: 1.0, fill_color: '#B0B0B0D9'}
    '3': {type: basic, stroke_color: '#DBD023F5', stroke_width: 1.0, fill_color: '#ECE024F5'}
    '4': {type: basic, stroke_color: '#1B760FF5', stroke_width: 1.0, fill_color: '#229912F5'}
    '5': {type: basic, stroke_color: '#588C24F5', stroke_width: 1.0, fill_color: '#76BF2DF5'}
    '6': {type: basic, stroke_color: '#84B33DF5', stroke_width: 1.0, fill_color: '#99D047F5'}
    '7': {type: basic, stroke_color: '#B0A11CF5', stroke_width: 1.0, fill_color: '#DDCA1FF5'}
    '8': {type: basic, stroke_color: '#A68535F5', stroke_width: 1.0, fill_color: '#DDB044F5'}
    '9': {type: basic, stroke_color: '#3673A8F5', stroke_width: 1.0, fill_color: '#4BABFFF5'}
~~~
