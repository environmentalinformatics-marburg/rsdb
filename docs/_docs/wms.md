---
title: WMS
---

Web Map Service (WMS) [(specification)](https://www.ogc.org/standards/wms), [(Wikipedia entry)](https://en.wikipedia.org/wiki/Web_Map_Service) provides access to raster map visualizations by WMS-clients, e.g. [Qgis](https://www.qgis.org).

RSDB web interface provides generated WMS layer URLs at "Layer"-tab - "RasterDBs"-layer detail-view - "Applications"-section - "WMS Access"-Button.

---
### WMS access point: single layer

With one URL you can access one RasterDB layer. Bands and times are selectable within WMS client.

URL structure: You need to replace LAYER_ID with your chosen RasterDB layer ID.

`http://IP:PORT/rasterdb/LAYER_ID/wms`

`https://IP:SECURE_PORT/rasterdb/LAYER_ID/wms`

examples with RasterDB layers `layer1` and `my_layer`
~~~
http://127.0.0.1:8081/rasterdb/layer1/wms
http://example.com:8081/rasterdb/my_layer/wms
https://127.0.0.1:8082/rasterdb/layer1/wms
https://example.com:8082/rasterdb/my_layer/wms
~~~

### Qgis WMS configuration

Qgis does not support "HTTP digest authentication" (in RSDB default at HTTP for security reasons). If you are using HTTP you need to specify "HTTP basic authentication" in [config.yaml](../config.yaml)   `http_authentication: basic`

At HTTPS "HTTP basic authentication" is always used (supported by Qgis).

---
#### Add URL of RSDB to Qgis:

At Qgis main window section `Browser` right click on item `WMS/WMTS` - left click menu item `New Connection`

At the `Create a New WMS/WMTS Connection` dialog... 

Type a `Name` for your connection and your RSDB WMS `URL`.

(if RSDB login is activated) At section `Authentication` - `Configuration` click `(+)` to add a new Authentication.

At the `Authentication` dialog...

Type a name of your authentication, choose `Basic authentication`, type RSDB `Username` and `Password`, click `Save`. (you can reuse your authentication for multiple WMS entries)

Back at the `Create a New WMS/WMTS Connection` dialog click `OK`

Your new item on `WMS/WMTS` in `Browser` appears. Click on the arrow symbol to open subitems. Double-click to open an (sub-)sub-item as Qgis layer.

---
#### Troubleshooting Qgis WMS:

Qgis may cache several data without reload from source: authentication settings, connection settings, network settings, raster data. To clear cache go to main menu `Settting` - `Options` - `Network` section `Cache settings` - `Content` click `basket-button`, `Authentication` click `Clear...cache-button`. Additionally you may restart Qgis.


---
### WMS access point: full collection (obsolete)

With one URL you can access all RasterDB layers, but depending on WMS client you can not choose bands or times within a layer.

Initial connection may take up to one minute, if it is first connect after server start as all layer meta data needs to be loaded. 

URL structure:

`http://IP:PORT/rasterdb_wms`

`https://IP:SECURE_PORT/rasterdb_wms`

examples
~~~
http://127.0.0.1:8081/rasterdb_wms
https://127.0.0.1:8082/rasterdb_wms
~~~