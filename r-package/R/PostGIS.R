#' PostGIS class
#' @description PostGIS class provides methods to access PostGIS tables of Remote Sensing Database.
#'
#' Use instance of \link{RemoteSensing} class to create instances of PostGIS class.
#'
#' In section 'Active bindings' class fields and in section 'Methods' class functions of PostGIS class are described.
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

    getVectors = function() {
      response <- private$rsdbConnector$GET(paste0("/postgis/layers/", private$name_, '/geojson'))
      tempFileName <- tempfile()
      writeBin(response, tempFileName)
      vectors <- sf::st_read(tempFileName, quiet=TRUE)
      file.remove(tempFileName)
      return(vectors)
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
    }

  ),
  private = list( #      *********** private *******************************

    name_ = NULL,
    curlHandle = NULL,
    base_url = NULL,
    path_url = NULL,
    url = NULL,
    meta_ = NULL,
    rsdbConnector = NULL

  ),
  lock_class = TRUE,
  lock_objects = TRUE,
  portable = FALSE,
  class = TRUE,
  cloneable = FALSE
)
