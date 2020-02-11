---
title: R Package Installation
---

For installation of R package paste the following lines into an R terminal.
~~~ R
if(!require("remotes")) install.packages("remotes")
remotes::install_github("environmentalinformatics-marburg/rsdb/rPointDB")
library(rPointDB)
??rPointDB
~~~

Documentation
~~~ R
# load rPointDB package
library(rPointDB)

# show package overview
??"rPontDB-package"

#show index of package documentation pages
help(package="rPointDB")

#list available functions/classes in package
ls(pos="package:rPointDB")

#documentation of central class RemoteSensing
?rPointDB::RemoteSensing

#documentation of (hyperspectral) raster class RasterDB
?rPointDB::RasterDB

#documentation of (LiDAR) point-cloud class PointDB
?rPointDB::PointDB

#documentation of (RGB) point-cloud class PointCloud
?rPointDB::PointCloud

#show package description file
packageDescription("rPointDB")

#show package version
packageDescription("rPointDB")$Version
~~~

Usage
~~~ R
# load rPointDB package
library(rPointDB)

# open connection to RSDB server
remoteSensing <- rPointDB::RemoteSensing$new(url="http://127.0.0.1:8081", userpwd="user:password")

# list Raster layers
raserLayersDF <- remoteSensing$rasterdbs

# list PointCloud layers
raserLayersDF <- remoteSensing$pointdbs

# list (RGB) PointCloud layers
raserLayersDF <- remoteSensing$plointclouds
~~~