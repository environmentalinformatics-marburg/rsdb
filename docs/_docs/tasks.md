---
title: Tasks
---

Tasks specify instructions to be executed on RSDB server. They are grouped into categories: 

* task_rasterdb
* task_pointdb
* task_pointcloud
* task_vectordb

One mode to run tasks is in offline mode: RSDB server is stopped and instruction is send by command line. Command format is `./rsdb.sh task '{MY_TASK_PARAMETERS}'` 
Note the single quote around '{MY_TASK_PARAMETERS}' to preserve double quotes inside.

{MY_TASK_PARAMETERS} is a JSON-Object with properties of the task.

examples
~~~bash
./rsdb.sh task '{task_pointdb: "import", pointdb: "layer1", source: "/media/lidar_data"}'
./rsdb.sh task '{task_pointdb: "rasterize", pointdb: "layer1", rasterdb: "layer1_rasterized"}'
~~~

---

| task_rasterdb | description |
| ------------- | ------------- |
| [**create**](#task_rasterdb-create) | create new rasterdb layer |

| task_pointdb | description |
| ------------- | ------------- |
| [**import**](#task_pointdb-import) | import point data from files |
| [**rasterize**](#task_pointdb-rasterize) | create visualization raster from points |
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
| **pixel_size** | (optional, but recommended) pixel size: one number for both x,y-coordinates or two numbers [x, y])|
| **offset** | (optional) internal offset to crs origin: two numbers [x, y]|
| **code** | (optional) crs code: should be an EPSG code|
| **proj4** | (optional) PROJ4 text|

~~~json
{task_rasterdb: "create", rasterdb: "indices", pixel_size: [5, 5], offset: [100, 100], code: "EPSG:25832", proj4: "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs "}
~~~

### task_pointdb: **"import"**

Import point data from files ( .las and .laz), recursive processing: Files in sub folders of source will be included.

pointdb properties needs to be specified in [config.yaml](../config.yaml). Target path will be created. Only one layer can be stored at one path (folder).

| name | value |
| ------------- | ------------- |
| **pointdb** | target: pointdb layer name (target path may not exist, if existing add points to existing layer) Note: Do not import same files multiple times, points in layer are then duplicated)|
| **source** | source directory with las/laz files as absolute path or relative to rsdb root folder path|

~~~json
{task_pointdb: "import", pointdb: "layer1", source: "/media/lidar_data"}
~~~

### task_pointdb: **"rasterize"**

create visualization raster from points

| name | value |
| ------------- | ------------- |
| **pointdb** | source: pointdb layer name (needs to be existing)|
| **rasterdb** | target: rasterdb layer name (delete if existing and create new) |
| **transactions** | (optional, default: true) true/false (true: processed part of data will be accessible when server is terminated in middle of operation, false: processing is faster) |

~~~json
{task_pointdb: "rasterize", pointdb: "layer1", rasterdb: "layer1_rasterized",  transactions: false}
~~~

### task_pointdb: **"index_raster"**

Pixels of an existing rasterdb layer are filled with index metrics calculated from pointdb layer.
rasterdb layer can be created with `task_rasterdb: "create"`, if it does not exist.

| name | value | format
| ------------- | ------------- |
| **pointdb** | source: pointdb layer name (needs to be existing)| "pointdbID" |
| **rasterdb** | target: rasterdb layer name (needs to be existing) | "rasterdbID" |
| **indices** | list of index metrics to calculate  | ["index1", "index2", "index3"] |
| **rect** | specifies a rectangle in target rasterdb that should be processed | [xmin, ymin, xmax, ymax] |
| **mask_band** | (optional) band number in target rasterdb. specifies which pixels should be calculated, if not specified all pixels will be calculated | bandIndex |

~~~json
{task_pointdb: "index_raster", pointdb: "myPointDB", "rasterdb": "myRasterDB", "indices": ["BE_H_MAX"], "rect": [608976, 5524981, 609094, 5525066]}
{task_pointdb: "index_raster", pointdb: "myPointDB", "rasterdb": "myRasterDB", "indices": ["area", "BE_H_MAX"], "rect": [608976, 5524981, 609094, 5525066]}
{task_pointdb: "index_raster", pointdb: "myPointDB", "rasterdb": "myRasterDB", "indices": ["area", "BE_H_MAX"], mask_band: 1}
{task_pointdb: "index_raster", pointdb: "myPointDB", "rasterdb": "myRasterDB", "indices": ["area", "BE_H_MAX"], mask_band: 1, "rect": [608976, 5524981, 609094, 5525066]}
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
{task_pointcloud: "rasterize", pointcloud: "cloud", rasterdb: "cloud_rasterized",  transactions: false}
~~~