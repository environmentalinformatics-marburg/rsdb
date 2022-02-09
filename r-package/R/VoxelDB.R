#' R6 Class Representing a VoxelDB layer
#'
#' @description
#' A VoxelDB layer contains voxel data.
#'
#' @details
#' Instance object of VoxelDB class are created by instances of RemoteSensing class.
#'
#' @export
VoxelDB <- R6::R6Class("VoxelDB",
public = list(

  initialize = function(rsdbConnector, name) {
    #print("---START---")
    private$rsdbConnector <- rsdbConnector
    private$name_ <- name
    url_path <- paste0("/voxeldbs/", private$name_)
    args <- list(extent=TRUE)
    response <- private$rsdbConnector$GET(url_path, args)
    private$meta_ <- response$voxeldb
  },

  voxels = function(ext = NULL, xmin = NULL, xmax = NULL, ymin = NULL, ymax = NULL, zmin = NULL, zmax = NULL, product = "count", time_slice_id = NULL, time_slice_name = NULL) {
    args <- list(format = 'rdat', product = product)

    #if(!is.null(x) && !is.null(y) && !is.null(z)) {
    #  args$x <- x
    #  args$y <- y
    #  args$z <- z
    #}

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

  name = function() {
    return(private$name_)
  },
  meta = function() {
    return(private$meta_)
  },
  time_slices = function() {
    return(private$meta_$time_slices)
  },
  extent = function() {
    ext3d <- private$meta_$extent
    ext <- raster::extent(ext3d$xmin, ext3d$xmax, ext3d$ymin, ext3d$ymax)
    return(ext)
  },
  extent3d = function() {
    ext3d <- private$meta_$extent
    return(ext3d)
  },
  xmin = function() {
    return(private$meta_$extent$xmin)
  },
  xmax = function() {
    return(private$meta_$extent$xmax)
  },
  ymin = function() {
    return(private$meta_$extent$ymin)
  },
  ymax = function() {
    return(private$meta_$extent$ymax)
  },
  zmin = function() {
    return(private$meta_$extent$zmin)
  },
  zmax = function() {
    return(private$meta_$extent$zmax)
  },
  res = function() {
    voxel_size <- private$meta_$ref$voxel_size
    vs <- c(x = voxel_size$x, y = voxel_size$y, z = voxel_size$z)
    return(vs)
  },
  PROJ4 = function() {
    ref <- private$meta_$ref
    return(ref$proj4)
  },
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
