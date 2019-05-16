---
title: Tasks
---

Tasks specify instructions to be executed on RSDB server. They are grouped into categories: *task_rasterdb*, *task_pointdb*, *task_pointcloud*, *task_vectordb*

One mode of operation for tasks is in offline mode: RSDB server is stopped and instruction is send by command line. e.g.
~~~bash
./rsdb.sh task {task_pointdb: "to_pointcloud", pointdb: "area", pointcloud: "cloud", transactions: false}
~~~

---

| task_rasterdb | description |
| ------------- | ------------- |
| [**create**](#task_rasterdb-create) | create new rasterdb layer |

| task_pointdb | description |
| ------------- | ------------- |
| [**index_raster**](#task_pointdb-index_raster) | fill a raster with point index metrics |
| [**to_pointcloud**](#task_pointdb-to_pointcloud) | convert pointdb layer to pointcloud layer |

| task_pointcloud | description |
| ------------- | ------------- |
| [**rasterize**](#task_pointcloud-rasterize) | create visualization raster from points |

| task_vectordb | description |
| ------------- | ------------- |
| |  |

---

### task_rasterdb: **"create"**

create new rasterdb layer

| name | value |
| ------------- | ------------- |
| **rasterdb** | rasterdb layer name (delete if existing and create new)|
| **pixel_size** | (optional, but recommended) pixel size one number for both x,y or two numbers [x, y])|
| **offset** | (optional) offset of crs origin two numbers [x, y]|
| **code** | (optional) crs code|
| **proj4** | (optional) PROJ4 text|

~~~json
{task_rasterdb: "create", rasterdb: "indices", pixel_size: [5, 5], offset: [100, 100], code: "EPSG:25832", proj4: "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "}
~~~

### task_pointdb: **"index_raster"**

Pixels of an existing rasterdb layer are filled with index metrics calculated from pointdb layer.

| name | value |
| ------------- | ------------- |
| **pointdb** | source: pointdb layer name (needs to be existing)|
| **rasterdb** | target: rasterdb layer name (needs to be existing) |
| **indices** | list of index metrics to calculate  |
| **rect** | specifies a rectangle in target rasterdb that should be processed |
| **mask_band** | (optional) band number in target rasterdb. specifies which pixels should be calculated, if not specified all pixels will be calculated |

~~~json
{task_pointdb: "index_raster", pointdb: "area", rasterdb: "indices", rect: [100, 100, 200, 200], mask_band: 1}
~~~

### task_pointdb: **"to_pointcloud"**

Conversion from pointdb layer to pointcloud layer

| name | value |
| ------------- | ------------- |
| **pointdb** | source: pointdb layer name (needs to be existing)|
| **pointcloud** | target: pointcloud layer name (delete if existing and create new) |
| **transactions** | (optional, default: true) true/false (true: processed part of data will be accessible when server is terminated in middle of operation, false: processing is faster) |

~~~json
{task_pointdb: "to_pointcloud", pointdb: "area", pointcloud: "cloud", transactions: false}
~~~

### task_pointcloud: **"rasterize"**

create visualization raster from points

| name | value |
| ------------- | ------------- |
| **pointcloud** | source: pointcloud layer name (needs to be existing)|
| **rasterdb** | target: rasterdb layer name (delete if existing and create new) |
| **transactions** | (optional, default: true) true/false (true: processed part of data will be accessible when server is terminated in middle of operation, false: processing is faster) |

~~~json
{task_pointcloud: "rasterize", pointcloud: "cloud", rasterdb: "raster",  transactions: false}
~~~