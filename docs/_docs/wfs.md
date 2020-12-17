---
title: WFS
---

Web Feature Service (WFS) [(specification)](http://www.opengeospatial.org/standards/wfs), [(Wikipedia entry)](https://en.wikipedia.org/wiki/Web_Feature_Service) provides access to vector data from VectorDB layers by WFS-clients, e.g. [Qgis](https://www.qgis.org).

RSDB web interface provides generated WFS layer URLs at "Layer"-tab - "VectorDBs"-layer detail-view - "Applications"-section - "WFS Access"-Button.

RSDB implements **WFS 1.1.0**.

URL structure: You need to replace LAYER_ID with your chosen VectorDB layer ID.

`http://IP:PORT/vecotdbs/LAYER_ID/wfs`

`https://IP:SECURE_PORT/vecotdbs/LAYER_ID/wfs`

examples with VectorDB layers `layer1` and `my_layer`
~~~
http://127.0.0.1:8081/vecotdbs/layer1/wfs
http://example.com:8081/vecotdbs/my_layer/wfs
https://127.0.0.1:8082/vecotdbs/layer1/wfs
https://example.com:8082/vecotdbs/my_layer/wfs
~~~

---

Following some WFS technical details. 
(A WFS-client, e.g. Qgis, does automatically follow that structure.)

### GetCapabilities

Get basic layer information as XML-file.

`http://127.0.0.1:8081/vectordb/my_layer/wfs`  
or  
`http://127.0.0.1:8081/vectordb/my_layer/wfs?SERVICE=WFS&REQUEST=GetCapabilities`


### DescribeFeatureType

Get detailed layer information as XML-file.  
Feature structure is specified by XSD (XML Schema Definition).

`http://127.0.0.1:8081/vectordbs/my_layer/wfs?SERVICE=WFS&REQUEST=DescribeFeatureType`

Currently all further parameters are ignored and the feature structure is returned always.

### GetFeature

Get vector feature data as XML-file.  
Features are returned as GML 3.1.1 (Geography Markup Language).

`http://127.0.0.1:8081/vectordbs/my_layer/wfs?SERVICE=WFS&REQUEST=GetFeature`

Currently all further parameters are ignored and all features including all properties are returned always.