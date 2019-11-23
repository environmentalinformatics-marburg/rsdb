#' Remote sensing database manages (hyperspectral) \strong{rasters} and (LiDAR) \strong{point-clouds}
#' as well as auxiliary ROIs (regions of interest as named polygons) and POIs (points of interest as named points).
#'
#' Central class is \code{\link{RemoteSensing}} that connects to remote sensing database.
#' Methods of \code{\link{RemoteSensing}} provide access to raster (\code{\link{RasterDB}})
#' and point-cloud (\code{\link{PointDB}}) units and meta data.
#'
#' @name rPontDB-package
#' @docType package
#' @title Remote Sensing Database
#' @author woellauer
#' @keywords package
#' @examples
#'# load rPointDB package
#'library(rPointDB)
#'
#'# show package overview
#'??"rPontDB-package"
#'
#'#show index of package documentation pages
#'help(package="rPointDB")
#'
#'#list available functions/classes in package
#'ls(pos="package:rPointDB")
#'
#'#documentation of central class RemoteSensing
#'?rPointDB::RemoteSensing
#'
#'#documentation of (hyperspectral) raster class RasterDB
#'?rPointDB::RasterDB
#'
#'#documentation of (LiDAR) point-cloud class PointDB
#'?rPointDB::PointDB
#'
#'#documentation of (RGB) point-cloud class PointCloud
#'?rPointDB::PointCloud
#'
#'#show package description file
#'packageDescription("rPointDB")
#'
#'#show package version
#'packageDescription("rPointDB")$Version
NULL
