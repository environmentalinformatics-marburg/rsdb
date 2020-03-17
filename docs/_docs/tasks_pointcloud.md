---
title: Tasks of category task_pointcloud
---
Note: More tasks are present on the web interface.

| task_pointcloud | description |
| ------------- | ------------- |
| [**rasterize**](#task_pointcloud-rasterize) | create visualization raster from points |

---

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