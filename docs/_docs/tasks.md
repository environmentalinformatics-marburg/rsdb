---
title: Tasks
---

Tasks specify operation to be executed on RSDB server.  
Tasks are submitted to RSDB and progress can be viewed as tasks may run for a long time.

Short description of tasks is included in the web interface, some more details can be found on following sub-pages. Tasks are grouped into categories: 

* [task_rasterdb](../tasks_rasterdb) - operations on **raster** layers (RasterDB)
* [task_pointdb](../tasks_pointdb) - operations on **point cloud** layers (PointDB)
* [task_pointcloud](../tasks_pointcloud) - operations on **point cloud** layers (PointCloud)
* [task_vectordb](../tasks_vectordb) - operations on **vector** layers (VectorDB)

## Task usage at the web interface (online)

At the web interface tasks can be submitted at tab **Tools** - subtab **Task**.  
Select the **Category** and the **Task**, type parameters, and **Submit** the task.  
At subtab **Status** list of running tasks can be viewed.

![web interface task view](../../assets/images/web_tasks.png)

## Task usage at the command line (offline)

One mode to run tasks is in offline mode: RSDB server is stopped and instruction is send by command line. Command format is `./rsdb.sh task '{MY_TASK_PARAMETERS}'` 
Note the single quote around '{MY_TASK_PARAMETERS}' to preserve double quotes inside.

{MY_TASK_PARAMETERS} is a JSON-Object with properties of the task.

examples
~~~bash
./rsdb.sh task '{task_pointdb: "import", pointdb: "layer1", source: "/media/lidar_data"}'
./rsdb.sh task '{task_pointdb: "rasterize", pointdb: "layer1", rasterdb: "layer1_rasterized"}'
~~~