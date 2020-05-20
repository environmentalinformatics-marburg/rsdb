---
title: PointCloud layer (point cloud data)
---

**Points** in a point cloud have **x,y,z** coordinates and associated **properties**.

From point cloud points structural indices can be calculated. Details to **point cloud indices** are located in the [RSDB wiki](https://github.com/environmentalinformatics-marburg/rsdb/wiki/Point-cloud-indices).

## Data import

Point cloud data can be in size of several terabytes which is unwieldy to be done by web interface file upload. For this reason point cloud import files need to be located at a folder on the same server as RSDB is running.

Import is initiated by [task_pointcloud import](../tasks_pointcloud/#task_pointcloud-import).