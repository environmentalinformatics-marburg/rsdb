---
title: R Package Installation
---

For installation of R package paste the following lines into an R terminal.
~~~ R
if(!require("remotes")) install.packages("remotes")
remotes::install_github("environmentalinformatics-marburg/rsdb/r-package")
library(RSDB)
??RSDB
~~~

Documentation
~~~ R
# load RSDB package
library(RSDB)

# show package overview
??"RSDB-package"

#show index of package documentation pages
help(package="RSDB")

#list available functions/classes in package
ls(pos="package:RSDB")

#documentation of central class RemoteSensing
?RSDB::RemoteSensing

#documentation of (hyperspectral) raster class RasterDB
?RSDB::RasterDB

#documentation of (LiDAR) point-cloud class PointDB
?RSDB::PointDB

#documentation of (RGB) point-cloud class PointCloud
?RSDB::PointCloud

#show package description file
packageDescription("RSDB")

#show package version
packageDescription("RSDB")$Version
~~~

Usage
~~~ R
# load RSDB package
library(RSDB)

# open connection to RSDB server
remoteSensing <- RSDB::RemoteSensing$new(url="http://127.0.0.1:8081", userpwd="user:password")

# list Raster layers
raserLayersDF <- remoteSensing$rasterdbs

# list PointCloud layers
raserLayersDF <- remoteSensing$pointdbs

# list (RGB) PointCloud layers
raserLayersDF <- remoteSensing$plointclouds
~~~