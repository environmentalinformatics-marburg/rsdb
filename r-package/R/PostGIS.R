#' PostGIS class
#' @description PostGIS class provides methods to access PostGIS tables of Remote Sensing Database.
#'
#' Use instance of \link{RemoteSensing} class to create instances of PostGIS class.
#'
#' In section 'Active bindings' class properties and in section 'Methods' class functions of PostGIS class are described.
#'
#' Use instance objects of PostGIS class to call that functionality (e.g. postgis$name ).
#'
#' @keywords database remote-sensing
#' @seealso \link{RemoteSensing}
#' @author woellauer
#'
#' @usage
#' # connect to remote sensing database
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#'
#' # open one PostGIS table in connected remote sensing database
#' postgis <- remotesensing$postgis(name)
#'
#' # operate on opened postgis:
#'
#'
#' @format
#' PostGIS \link{R6Class} object.
#'
#'
#' @examples
#'
#' # open remote sensing database
#' library(RSDB)
#' #remotesensing <- RemoteSensing$new("http://localhost:8081", "user:password") # local
#' remotesensing <- RemoteSensing$new("http://example.com:8081", "user:password") # remote server
#'
#'
#' @export
PostGIS <- R6::R6Class("PostGIS",
  public = list( #      *********** public *********************************

    #' @description
    #' Use instance of \link{RemoteSensing} class to create instances of PostGIS class (corresponding to one table in a PostGIS database).
    #' This initialize function is for internal use only.
    #' @param rsdbConnector rsdb server connection.
    #' @param name PostGIS table name.
    initialize = function(rsdbConnector, name) {
      private$rsdbConnector <- rsdbConnector
      private$name_ <- name
      m <- private$rsdbConnector$GET(paste0("/postgis/layers/", private$name_))
      private$meta_ <- m
    },

    getVectors = function(bbox = NULL) {
      args <- list()
      if(!is.null(bbox)) {
        if(class(bbox) != 'bbox') {
          stop('optional parameter bbox needs to be of class bbox')
        }
        args$bbox <- paste(bbox$xmin, bbox$ymin, bbox$xmax, bbox$ymax, sep = ',')
      }
      response <- private$rsdbConnector$GET(paste0("/postgis/layers/", private$name_, '/geojson'), args)
      tempFileName <- tempfile()
      writeBin(response, tempFileName)
      vectors <- sf::st_read(tempFileName, quiet=TRUE)
      file.remove(tempFileName)
      return(vectors)
    },

    getRaster = function(bbox, field = NULL, dx = 1, dy = 1) {
      if(is.null(bbox)) {
        stop('missing bbox')
      }
      if(is.na(sf::st_crs(bbox))) {
        sf::st_crs(bbox) <- self$epsg
      }
      vectors <- self$getVectors(bbox)
      if(!is.null(field)) {
        vectors <- vectors[field]
      } else if(length(self$class_fields) > 0) {
        vectors <- vectors[self$class_fields[1]]
      }
      template <- stars::st_as_stars(bbox, dx, dy)
      r <- stars::st_rasterize(sf = vectors, template = template)
      return(r)
    }

  ),
  active = list( #      *********** active *********************************

    #' @field name PostGIS table name.
    name = function() {
      return(private$name_)
    },

    #' @field meta Meta data.
    meta = function() {
      return(private$meta_)
    },

    #' @field epsg EPSG code.
    epsg = function() {
      return(private$meta_$epsg)
    },

    #' @field fields database table fields.
    fields = function() {
      return(private$meta_$fields)
    },

    #' @field class_fields database table fields that are marked as class fields.
    class_fields = function() {
      return(private$meta_$class_fields)
    }

  ),
  private = list( #      *********** private *******************************

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
