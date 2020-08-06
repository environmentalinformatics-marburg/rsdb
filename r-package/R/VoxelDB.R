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

  voxels = function(x, y, z) {
    args <- list(format = 'rdat', x = x, y = y, z = z)
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
