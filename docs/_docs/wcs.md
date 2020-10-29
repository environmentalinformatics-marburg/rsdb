---
title: WCS
---

Web Coverage Service (WCS) [(specification)](http://www.opengeospatial.org/standards/wcs), [(Wikipedia entry)](https://en.wikipedia.org/wiki/Web_Coverage_Service) provides access to original raster data by WCS-clients, e.g. [Qgis](https://www.qgis.org).

RSDB web interface provides generated WCS layer URLs at "Layer"-tab - "RasterDBs"-layer detail-view - "Applications"-section - "WCS Access"-Button.

RSDB implements **WCS 1.0.0**.

URL structure: You need to replace LAYER_ID with your chosen RasterDB layer ID.

`http://IP:PORT/rasterdb/LAYER_ID/wcs`

`https://IP:SECURE_PORT/rasterdb/LAYER_ID/wcs`

examples with RasterDB layers `layer1` and `my_layer`
~~~
http://127.0.0.1:8081/rasterdb/layer1/wcs
http://example.com:8081/rasterdb/my_layer/wcs
https://127.0.0.1:8082/rasterdb/layer1/wcs
https://example.com:8082/rasterdb/my_layer/wcs
~~~

---

Following some WCS technical details. 
(A WCS-client, e.g. Qgis, does automatically follow that structure.)

### GetCapabilities

Get basic layer information as XML-file.

`http://127.0.0.1:8081/rasterdb/my_layer/wcs?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=1.0.0`


### DescribeCoverage

Get detailed layer information as XML-file.

`http://127.0.0.1:8081/rasterdb/my_layer/wcs?SERVICE=WCS&REQUEST=DescribeCoverage&VERSION=1.0.0&COVERAGE=my_layer`

### GetCoverage

Get raster data as GeoTIFF-file.

`http://127.0.0.1:8081/rasterdb/my_layer/wcs?SERVICE=WCS&VERSION=1.0.0&REQUEST=GetCoverage&FORMAT=GeoTIFF&COVERAGE=my_layer&BBOX=xmin,ymin,xmax,ymax&CRS=EPSG:epsg&RESPONSE_CRS=EPSG:epsg&WIDTH=width&HEIGHT=height`

Settable parameters:
- **BBOX=xmin,ymin,xmax,ymax** Bounding box of requested raster in projection coordinates.  
- **WIDTH=width** Width in pixel of requested raster.  
- **HEIGHT=height** Height in pixel of requested raster.  