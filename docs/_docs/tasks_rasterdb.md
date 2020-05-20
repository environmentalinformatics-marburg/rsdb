---
title: Tasks of category task_rasterdb
---
*Note: More tasks are present on the web interface.*

| task_rasterdb | description |
| ------------- | ------------- |
| [**create**](#task_rasterdb-create) | create new rasterdb layer |

---

### task_rasterdb: **"create"**

create new rasterdb layer

| name | value |
| ------------- | ------------- |
| **rasterdb** | rasterdb layer name (delete if existing and create new)|
| **pixel_size** | (optional, but recommended) pixel size: one number for both x,y-coordinates or two numbers [x, y])|
| **offset** | (optional) internal offset to crs origin: two numbers [x, y]|
| **code** | (optional) crs code: should be an EPSG code|
| **proj4** | (optional) PROJ4 text|

~~~json
{task_rasterdb: "create", rasterdb: "indices", pixel_size: [5, 5], offset: [100, 100], code: "EPSG:25832", proj4: "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "}
~~~