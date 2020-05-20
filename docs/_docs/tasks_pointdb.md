---
title: Tasks of category task_pointdb
---
*Note: More tasks are present on the web interface.*

| task_pointdb | description |
| ------------- | ------------- |
| [**import**](#task_pointdb-import) | import point data from files |
| [**rasterize**](#task_pointdb-rasterize) | create visualization raster from points |
| [**index_raster**](#task_pointdb-index_raster) | fill a raster with point index metrics |
| [**to_pointcloud**](#task_pointdb-to_pointcloud) | convert pointdb layer to pointcloud layer |

---

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