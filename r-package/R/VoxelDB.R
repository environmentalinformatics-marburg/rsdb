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
    private$rsdbConnector <- rsdbConnector
    private$name_ <- name
    response <- private$rsdbConnector$GET(paste0("/voxeldbs/", private$name_))
    private$meta_ <- response$voxeldb
  },

  voxels = function(ext = NULL, x = NULL, y = NULL, z = NULL, product, time_slice_id = NULL, time_slice_name = NULL) {
    args <- list(format = 'rdat', product = product)

    # spMask <- NULL
    # if(is(ext, 'sfc_MULTIPOLYGON')) {
    #   ext <- sf:::as_Spatial(ext)
    # }
    # if(is(ext, 'SpatialPolygons')) {
    #   warning('convert SpatialPolygons to raster::extent by ignoring crs')
    #   spMask <- ext
    #   ext <- raster::extent(ext)
    # }
    # if(is(ext, "bbox")) {
    #   warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
    #   ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
    # }
    # if(!is.null(ext)) {
    #   extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
    #   args$ext <- extText
    # } else {
    #   args$x <- x
    #   args$y <- y
    #   args$z <- z
    # }

    if(!is.null(x) && !is.null(y) && !is.null(z)) {
      args$x <- x
      args$y <- y
      args$z <- z
    }

    if(!is.null(ext)) {
      extText <- paste0(ext, collapse = ' ')
      args$ext = extText
    }

    if(!is.null(time_slice_id)) {
      args$time_slice_id <- time_slice_id
    }
    if(!is.null(time_slice_name)) {
      args$time_slice_name <- time_slice_name
    }
    response <- private$rsdbConnector$GET(paste0("/voxeldbs/", private$name_, "/voxels"), args)
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
