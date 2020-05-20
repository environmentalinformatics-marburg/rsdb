---
title: Tasks of category task_pointcloud
---
*Note: More tasks are present on the web interface.*

| task_pointcloud | description |
| ------------- | ------------- |
| [**import**](#task_pointcloud-import) | import point cloud data files |
| [**rasterize**](#task_pointcloud-rasterize) | create visualization raster from points |

---

### task_pointcloud: **"import"**
Import all *.las and *.laz files at a folder (and subfolders) on the server into a new PointCloud layer.

| name | value |
| ------------- | ------------- |
| **pointcloud** | ID of new PointCloud layer (target).<br>**format**: `layer ID` **example**: `pointcloud1`
| **source** | Folder with *.las / *.laz files to import (located on server) (recursive).<br>**format**: `path`  **example**: `las/folder1`
| epsg | EPSG projection code (If epsg is left empty and proj4 parameter is set a automatic epsg search will be tried. Note: multiple EPSG may refer to one proj4).<br>**format**: `number`  **example**: `25832`
| proj4 | PROJ4 projection (If proj4 is left empty and epsg parameter is set a automatic proj4 generation will be tried.).<br>**format**: `text`  **example**: `+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs `
| rect | Only points inside of rect are imported - prevents import of points with erroneous x,y coordinates.<br>**format**: `list of coordinates: xmin, ymin, xmax, ymax`  **example**: `609000.1, 5530100.7, 609094.1, 5530200.9`
| cellsize | Size of cells.<br>**format**: `number`  **example**: `10`  **default**: `100` -> 100 meter 
| cellscale | Resolution of points.<br>**format**: `number`  **example**: `1000`  **default**: `100` -> resolution of points 1/100 = 0.01 meter 
| storage_type | Storage type of new PointCloud.<br>**format**: `RasterUnit` or `TileStorage`  **default**: `TileStorage`
| transactions | Use power failer safe (and slow) PointCloud operation mode (obsolete for TileStorage).	<br>**format**: `true` or `false`  **default**: `false` 



~~~json
{ task_pointcloud: "import", pointcloud: "pointcloud1", source: "/media/folder/lasfolder", epsg: "25832", proj4: "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs " }
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