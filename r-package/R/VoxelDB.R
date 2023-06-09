#' VoxelDB class
#' @description VoxelDB class provides methods to access voxel data of Remote Sensing Database. Voxel layers can be generated from \link{PointCloud}.
#'
#' Use instance of \link{RemoteSensing} class to create instances of VoxelDB class.
#'
#' In section 'Active bindings' class fields and in section 'Methods' class functions of VoxelDB class are described.
#'
#' Use instance objects of VoxelDB class to call that functionality (e.g. voxeldb$name ).
#'
#' @keywords voxel database remote-sensing
#' @seealso \link{RemoteSensing} \link{extent} \link{PointCloud}
#' @author woellauer
#'
#' @usage
#' # connect to remote sensing database
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#'
#' # open one voxel layer in connected remote sensing database
#' voxeldb <- remotesensing$voxeldb(name)
#'
#' # operate on opened voxeldb:
#'
#' voxeldb$voxels(ext = NULL, xmin = NULL, xmax = NULL, ymin = NULL, ymax = NULL, zmin = NULL, zmax = NULL, product = "count", time_slice_id = NULL, time_slice_name = NULL)
#' voxeldb$aggregated_voxels(ext = NULL, xmin = NULL, xmax = NULL, ymin = NULL, ymax = NULL, zmin = NULL, zmax = NULL, time_slice_id = NULL, time_slice_name = NULL, aggregation_factor = NULL, aggregation_factor_x = NULL, aggregation_factor_y = NULL, aggregation_factor_z = NULL, crop = FALSE, product = "sum")
#' voxeldb$raster(ext, zmin = NULL, zmax = NULL, time_slice_id = NULL, time_slice_name = NULL, aggregation_factor = NULL, aggregation_factor_x = NULL, aggregation_factor_y = NULL, product = "sum")
#'
#' voxeldb$name
#' voxeldb$meta
#' voxeldb$time_slices
#' voxeldb$extent
#' voxeldb$extent3d
#' voxeldb$xmin
#' voxeldb$xmax
#' voxeldb$ymin
#' voxeldb$ymax
#' voxeldb$zmin
#' voxeldb$zmax
#' voxeldb$res
#' voxeldb$PROJ4
#' voxeldb$EPSG
#'
#' @format
#' VoxelDB \link{R6Class} object.
#'
#'
#' @section Extent objects:
#'
#' An Extent (in 'raster'-package) is a rectangle, parallel to the coordinate system axes.
#'
#' It can be directlty created by \code{extent(...)}
#'
#' or based on position and diameter \code{extent_diameter(x, y, d)}
#'
#' Alternativly objects of class 'bbox' (in 'sf'-package) can be used.
#'
#' @section Polygon objects:
#'
#' A Polygon (in 'sp'-package) is a closed sequence of points that cover an area. First and last point of the sequence need to be same.
#'
#' Manual creation of a Polygon
#' \preformatted{
#' # create Matrix of point seqence
#' x <- 477662.863 + c(1,2,1.5,1,1)
#' y <- 5632129.623 + c(1,1,2.7,2,1)
#' coords <- cbind(x, y)
#'
#' # create Polygon from Matrix
#' p <- Polygon(coords=coords)
#' }
#'
#' Create Polygon from database ROI
#' \preformatted{
#' roi <- remotesensing$roi(group_name="hai", roi_name="HEW02")
#' p <- Polygon(coords=roi$polygon)
#' }
#'
#' Convert Polygon to Extent (bounding box)
#' \preformatted{
#' x <- p@coords[,1]
#' y <- p@coords[,2]
#' ext <- raster::extent(x=min(x), xmax=max(x), ymin=min(y), ymax=max(y))
#' }
#'
#' @examples
#'
#' # open remote sensing database
#' library(RSDB)
#' #remotesensing <- RemoteSensing$new("http://localhost:8081", "user:password") # local
#' remotesensing <- RemoteSensing$new("http://example.com:8081", "user:password") # remote server
#'
#' # get pointcloud names
#' remotesensing$voxeldbs
#'
#' # open voxeldb
#' voxeldb <- remotesensing$voxeldb("voxeldb_forest")
#'
#' # get projection as EPSG code
#' voxeldb$EPSG
#'
#' @export
VoxelDB <- R6::R6Class("VoxelDB",
public = list(

  #' @description
  #' Use instance of \link{RemoteSensing} class to create instances of VoxelDB class.
  #' This initialize function is for internal use only.
  #' @param base_url RSDB server URL.
  #' @param name VoxelDB name.
  #' @param curlHandle connection handler.
  #' @param rsdbConnector rsdb server connection.
  initialize = function(rsdbConnector, name) {
    private$rsdbConnector <- rsdbConnector
    private$name_ <- name
    url_path <- paste0("/voxeldbs/", private$name_)
    args <- list(extent=TRUE)
    response <- private$rsdbConnector$GET(url_path, args)
    private$meta_ <- response$voxeldb
  },

  #' @description
  #' Retrieve subsets of voxeldb voxels.
  #'
  #' @return voxels.
  #'
  #' @param ext Subset of voxeldb.
  #'
  #' ext is of type 'bbox' or 'Extent' (rectangular area) or 'Polygon' (polygon area, see \link{convert_Polygon_to_matrix}) or 'polygons' (polygon area, see \link{convert_Polygons_to_matrix}) or 'Matrix' (polygon area, two columns (x,y) and one row per point, first and last point need to be same for closed polygons).
  #'
  #' @param xmin Optional.
  #' @param xmax Optional.
  #' @param ymin Optional.
  #' @param ymax Optional.
  #' @param zmin Optional.
  #' @param zmax Optional.
  #' @param product Optional.
  #'
  #' @param time_slice_id Optionally, get voxels of specified time slice id.
  #'
  #' @param time_slice_name Optionally, get voxels of specified time slice name.
  voxels = function(ext = NULL, xmin = NULL, xmax = NULL, ymin = NULL, ymax = NULL, zmin = NULL, zmax = NULL, product = "count", time_slice_id = NULL, time_slice_name = NULL) {
    args <- list(format = 'rdat', product = product)

    if(!is.null(ext)) {

      if(is(ext, 'sfc_MULTIPOLYGON')) {
        ext <- sf:::as_Spatial(ext)
      }
      if(is(ext, 'sfc_POLYGON')) {
        ext <- sf:::as_Spatial(ext)
      }
      if(is(ext, 'SpatialPolygons')) {
        warning('convert SpatialPolygons to raster::extent by ignoring crs')
        spMask <- ext
        ext <- raster::extent(ext)
      }
      if(is(ext, "bbox")) {
        warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
        ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
      }
      if(is(ext, "Extent")) {
        extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
      } else {
        extText <- paste0(ext, collapse = ' ')
      }
      args$ext = extText
    }

    if(!is.null(xmin)) {
      args$xmin <- xmin
    }
    if(!is.null(xmax)) {
      args$xmax <- xmax
    }
    if(!is.null(ymin)) {
      args$ymin <- ymin
    }
    if(!is.null(ymax)) {
      args$ymax <- ymax
    }
    if(!is.null(zmin)) {
      args$zmin <- zmin
    }
    if(!is.null(zmax)) {
      args$zmax <- zmax
    }

    if(!is.null(time_slice_id)) {
      args$time_slice_id <- time_slice_id
    }
    if(!is.null(time_slice_name)) {
      args$time_slice_name <- time_slice_name
    }
    response <- private$rsdbConnector$GET(paste0("/voxeldbs/", private$name_, "/voxels"), args)
    return(response)
  },

  #' @description
  #' Retrieve subsets of aggregated voxeldb voxels.
  #'
  #' @return voxels.
  #'
  #' @param ext Subset of voxeldb.
  #'
  #' ext is of type 'bbox' or 'Extent' (rectangular area) or 'Polygon' (polygon area, see \link{convert_Polygon_to_matrix}) or 'polygons' (polygon area, see \link{convert_Polygons_to_matrix}) or 'Matrix' (polygon area, two columns (x,y) and one row per point, first and last point need to be same for closed polygons).
  #'
  #' @param xmin Optional.
  #' @param xmax Optional.
  #' @param ymin Optional.
  #' @param ymax Optional.
  #' @param zmin Optional.
  #' @param zmax Optional.
  #'
  #' @param time_slice_id Optionally, get voxels of specified time slice id.
  #'
  #' @param time_slice_name Optionally, get voxels of specified time slice name.
  #'
  #' @param aggregation_factor Optional.
  #' @param aggregation_factor_x Optional.
  #' @param aggregation_factor_y Optional.
  #' @param aggregation_factor_z Optional.
  #' @param crop Optional.
  #' @param product Optional.
  aggregated_voxels = function(ext = NULL, xmin = NULL, xmax = NULL, ymin = NULL, ymax = NULL, zmin = NULL, zmax = NULL, time_slice_id = NULL, time_slice_name = NULL, aggregation_factor = NULL, aggregation_factor_x = NULL, aggregation_factor_y = NULL, aggregation_factor_z = NULL, crop = FALSE, product = "sum") {
    args <- list(format = 'rdat')

    if(!is.null(ext)) {

      if(is(ext, 'sfc_MULTIPOLYGON')) {
        ext <- sf:::as_Spatial(ext)
      }
      if(is(ext, 'sfc_POLYGON')) {
        ext <- sf:::as_Spatial(ext)
      }
      if(is(ext, 'SpatialPolygons')) {
        warning('convert SpatialPolygons to raster::extent by ignoring crs')
        spMask <- ext
        ext <- raster::extent(ext)
      }
      if(is(ext, "bbox")) {
        warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
        ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
      }
      if(is(ext, "Extent")) {
        extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
      } else {
        extText <- paste0(ext, collapse = ' ')
      }
      args$ext = extText
    }

    if(!is.null(xmin)) {
      args$xmin <- xmin
    }
    if(!is.null(xmax)) {
      args$xmax <- xmax
    }
    if(!is.null(ymin)) {
      args$ymin <- ymin
    }
    if(!is.null(ymax)) {
      args$ymax <- ymax
    }
    if(!is.null(zmin)) {
      args$zmin <- zmin
    }
    if(!is.null(zmax)) {
      args$zmax <- zmax
    }

    if(!is.null(aggregation_factor)) {
      args$aggregation_factor <- aggregation_factor
    }
    if(!is.null(aggregation_factor_x)) {
      args$aggregation_factor_x <- aggregation_factor_x
    }
    if(!is.null(aggregation_factor_y)) {
      args$aggregation_factor_y <- aggregation_factor_y
    }
    if(!is.null(aggregation_factor_z)) {
      args$aggregation_factor_z <- aggregation_factor_z
    }

    if(!is.null(time_slice_id)) {
      args$time_slice_id <- time_slice_id
    }
    if(!is.null(time_slice_name)) {
      args$time_slice_name <- time_slice_name
    }
    if(!is.null(crop)) {
      args$crop <- crop
    }
    if(!is.null(product)) {
      args$product <- product
    }
    response <- private$rsdbConnector$GET(paste0("/voxeldbs/", private$name_, "/aggregated_voxels"), args)
    return(response)
  },

  #' @description
  #' Process subsets of aggregated voxeldb voxels to raster.
  #'
  #' @return raster.
  #'
  #' @param ext Subset of voxeldb.
  #'
  #' ext is of type 'bbox' or 'Extent' (rectangular area) or 'Polygon' (polygon area, see \link{convert_Polygon_to_matrix}) or 'polygons' (polygon area, see \link{convert_Polygons_to_matrix}) or 'Matrix' (polygon area, two columns (x,y) and one row per point, first and last point need to be same for closed polygons).
  #'
  #' @param zmin Optional.
  #' @param zmax Optional.
  #'
  #' @param time_slice_id Optionally, get voxels of specified time slice id.
  #'
  #' @param time_slice_name Optionally, get voxels of specified time slice name.
  #'
  #' @param aggregation_factor Optional.
  #' @param aggregation_factor_x Optional.
  #' @param aggregation_factor_y Optional.
  #' @param product Optional.
  raster = function(ext, zmin = NULL, zmax = NULL, time_slice_id = NULL, time_slice_name = NULL, aggregation_factor = NULL, aggregation_factor_x = NULL, aggregation_factor_y = NULL, product = "sum") {
    args <- list(format = 'rdat')

    if(!is.null(ext)) {

      if(is(ext, 'sfc_MULTIPOLYGON')) {
        ext <- sf:::as_Spatial(ext)
      }
      if(is(ext, 'sfc_POLYGON')) {
        ext <- sf:::as_Spatial(ext)
      }
      if(is(ext, 'SpatialPolygons')) {
        warning('convert SpatialPolygons to raster::extent by ignoring crs')
        spMask <- ext
        ext <- raster::extent(ext)
      }
      if(is(ext, "bbox")) {
        warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
        ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
      }
      if(is(ext, "Extent")) {
        extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
      } else {
        extText <- paste0(ext, collapse = ' ')
      }
      args$ext = extText
    }

    if(!is.null(zmin)) {
      args$zmin <- zmin
    }
    if(!is.null(zmax)) {
      args$zmax <- zmax
    }

    if(!is.null(aggregation_factor)) {
      args$aggregation_factor <- aggregation_factor
    }
    if(!is.null(aggregation_factor_x)) {
      args$aggregation_factor_x <- aggregation_factor_x
    }
    if(!is.null(aggregation_factor_y)) {
      args$aggregation_factor_y <- aggregation_factor_y
    }

    if(!is.null(time_slice_id)) {
      args$time_slice_id <- time_slice_id
    }
    if(!is.null(time_slice_name)) {
      args$time_slice_name <- time_slice_name
    }
    if(!is.null(product)) {
      args$product <- product
    }
    response <- private$rsdbConnector$GET(paste0("/voxeldbs/", private$name_, "/raster"), args)
    return(response)
  }

),
active = list(

  #' @field name VoxelDB name.
  name = function() {
    return(private$name_)
  },

  #' @field meta Meta data.
  meta = function() {
    return(private$meta_)
  },

  #' @field time_slices List of all time slices in this voxeldb.
  time_slices = function() {
    return(private$meta_$time_slices)
  },

  #' @field extent Full extent of voxel as 'Extent' object. Note: Do not use this full extent to request data as the full voxeldb may be many gigabytes large!
  extent = function() {
    ext3d <- private$meta_$extent
    ext <- raster::extent(ext3d$xmin, ext3d$xmax, ext3d$ymin, ext3d$ymax)
    return(ext)
  },

  #' @field extent3d Full extent of voxel as range value in x,y,z dimensions. Note: Do not use this full extent to request data as the full voxeldb may be many gigabytes large!
  extent3d = function() {
    ext3d <- private$meta_$extent
    return(ext3d)
  },

  #' @field xmin Minimum coordinate at x-dimension.
  xmin = function() {
    return(private$meta_$extent$xmin)
  },

  #' @field xmax Maximum coordinate at x-dimension.
  xmax = function() {
    return(private$meta_$extent$xmax)
  },

  #' @field ymin Minimum coordinate at y-dimension.
  ymin = function() {
    return(private$meta_$extent$ymin)
  },

  #' @field ymax Maximum coordinate at y-dimension.
  ymax = function() {
    return(private$meta_$extent$ymax)
  },

  #' @field zmin Minimum coordinate at z-dimension.
  zmin = function() {
    return(private$meta_$extent$zmin)
  },

  #' @field zmax Maximum coordinate at z-dimension.
  zmax = function() {
    return(private$meta_$extent$zmax)
  },

  #' @field res Voxel resolution (voxel size) at x,y,z-dimensions
  res = function() {
    voxel_size <- private$meta_$ref$voxel_size
    vs <- c(x = voxel_size$x, y = voxel_size$y, z = voxel_size$z)
    return(vs)
  },

  #' @field PROJ4 Projection in PROJ4.
  PROJ4 = function() {
    ref <- private$meta_$ref
    return(ref$proj4)
  },

  #' @field EPSG Projection as EPSG code.
  EPSG = function() {
    ref <- private$meta_$ref
    return(ref$epsg)
  }
),
private = list(

  rsdbConnector = NULL,
  name_ = NULL,
  meta_ = NULL

),
lock_class = TRUE,
lock_objects = TRUE,
portable = FALSE,
class = TRUE,
cloneable = FALSE
)
