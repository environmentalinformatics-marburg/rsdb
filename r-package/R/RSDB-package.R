#' RSDB (Remote Sensing Database) manages (hyperspectral) rasters and (LiDAR) point-clouds as well as auxiliary vector data, ROIs (regions of interest as named polygons) and POIs (points of interest as named points). Contained data can be queried, processed and analysed.
#' This R package connects to an RSDB server running at local or remote computer.
#'
#' Central class is \code{\link{RemoteSensing}} that connects to remote sensing database.
#' Methods of \code{\link{RemoteSensing}} provide access to raster (\code{\link{RasterDB}}),
#' to point clouds (\code{\link{PointCloud}}), to voxels (\code{\link{VoxelDB}}), (deprecated \code{\link{PointDB}}), to vector (\code{\link{VectorDB}}), to region of interest (ROI) and to poin of interest (POI) (at \code{\link{RemoteSensing}}) layers and meta data.
#'
#' @name RSDB-package
#' @docType package
#' @title RSDB
#' @author woellauer
#' @keywords package
#' @examples
#'# load RSDB package
#'library(RSDB)
#'
#'# show package overview
#'?`RSDB-package`
#'
#'#show index of package documentation pages
#'help(package="RSDB")
#'
#'#list available functions/classes in package
#'ls(pos="package:RSDB")
#'
#'#documentation of central class RemoteSensing
#'?RSDB::RemoteSensing
#'
#'#documentation of (hyperspectral) raster class RasterDB
#'?RSDB::RasterDB
#'
#'#documentation of (RGB) point-cloud class PointCloud
#'?RSDB::PointCloud
#'
#'#documentation of (LiDAR) point-cloud class PointDB (deprecated)
#'?RSDB::PointDB
#'
#'#documentation of vector layer data class VectorDB
#'?RSDB::VectorDB
#'
#'#show package description file
#'packageDescription("RSDB")
#'
#'#show package version
#'packageDescription("RSDB")$Version
NULL
