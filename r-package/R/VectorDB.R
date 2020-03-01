VectorDB_public <- list( #      *********** public *********************************

  initialize = function(base_url, name, curlHandle, rsdbConnector) {
    private$rsdbConnector <- rsdbConnector

    private$name_ <- name
    private$curlHandle <- curlHandle
    private$base_url <- base_url
    private$path_url <-paste0(private$base_url, "/pointclouds")
    private$url <- paste0(private$path_url, "/", private$name_)
    m <- private$rsdbConnector$GET(paste0("/vectordbs/", private$name_))
    private$meta_ <- m$vectordb
  },

  getVectors = function(epsg=NULL) {
    query <- list()
    if(!is.null(epsg)) {
      stopifnot(is.character(epsg))
      stopifnot(length(epsg) == 1)
      query[["epsg"]] <- epsg
    }
    path <- paste0("/vectordbs/", private$name_, "/geometry.json")
    data <- private$rsdbConnector$GET(path, query)
    tempFileName <- tempfile()
    writeBin(data, tempFileName)
    tempFileName
    #vectors <- rgdal::readOGR(tempFileName)
    vectors <- sf::st_read(tempFileName, quiet=TRUE)
    if(is.null(epsg) & self$epsg == "") {
      if(self$proj4 == "") {
        sf::st_crs(vectors) <- sf::NA_crs_
      } else {
        sf::st_crs(vectors) <- self$proj4
      }
    }
    file.remove(tempFileName)
    return(vectors)
  },

  getVectorsWGS84 = function() {
    vectors <- getVectors(epsg = "4326")
    return(vectors)
  }

) #***********************************************************************************

VectorDB_active <- list( #      *********** active *********************************

  name = function() {
    return(private$name_)
  },

  meta = function() {
    return(private$meta_)
  },

  epsg = function() {
    return(private$meta_$details$epsg)
  },

  proj4 = function() {
    return(private$meta_$details$proj4)
  },

  title = function() {
    return(private$meta_$title)
  },

  description = function() {
    return(private$meta_$description)
  },

  attributes = function() {
    return(private$meta_$details$attributes)
  },

  name_attribute = function() {
    return(private$meta_$details$name_attribute)
  }

) #***********************************************************************************

VectorDB_private <- list( #      *********** private *******************************

  name_ = NULL,
  curlHandle = NULL,
  base_url = NULL,
  path_url = NULL,
  url = NULL,
  meta_ = NULL,
  rsdbConnector = NULL

) #***********************************************************************************


#' VectorDB class
#'
#' VectorDB class provides methods to access to vector data of Remote Sensing Database.
#'
#' @keywords vector polygon line point
#' @seealso \link{RemoteSensing}
#' @author woellauer
#'
#' @usage
#' # connect to remote sensing database
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#'
#' # get VectorDB names
#' remotesensing$vectordbs
#'
#' # open one VectorDB in connected remote sensing database
#' vectordb <- remotesensing$vectordb(name)
#'
#' # operate on opened VectorDB:
#'
#' vectordb$getVectors(epsg=NULL)
#' vectordb$getVectorsWGS84()
#'
#' vectordb$attributes
#' vectordb$description
#' vectordb$epsg
#' vectordb$meta
#' vectordb$name
#' vectordb$name_attribute
#' vectordb$proj4
#' vectordb$title
#'
#' @format
#' VectorDB \link{R6Class} object.
#'
#' @section Details:
#'
#' In the Following methods of VectorDB are described. Use instance objects to call that methods (e.g. vecotdb$method(paramters) ).
#' Use instance of \link{RemoteSensing} class to create instances of VectorDB.
#'
#' \describe{
#'
#' \item{$getVectors(epsg=NULL)}{Get vector data.
#'
#' returns: vector data
#'
#' parameters:
#'
#' \strong{epsg}: desired reprojection.
#'
#' EPSG code of desired reprojection. Code needs to be number as character. e.g. '4326'. If paramter is missing data is returned in original projection.
#'
#'
#' }
#'
#' \item{$getVectorsWGS84()}{Get vector data in WGS84 longlat projection (EPSG:4326). Shortcut of vectordb$getVectors('4326').
#'
#' returns: vector data
#'
#' }
#'
#' }
#'
#' @examples
#'
#' library(RSDB)
#' #remotesensing <- RemoteSensing$new("http://localhost:8081", "user:password") # local
#' remotesensing <- RemoteSensing$new("http://example.com:8081", "user:password") # remote server
#'
#' # get VectorDB names
#' vectordbs <- remotesensing$vectordbs
#'
#' # open vectordb
#' vectordb <- remotesensing$vectordb("ep_plot_polygons")
#'
#' # get vecotor data in WGS84 longlat projection (EPSG:4326)
#' data <- vectordb$getVectorsWGS84()
#'
#' # static visualize vector data
#' plot(data)
#'
#' # dynamic visualize vector data
#' library(mapview)
#' mapview::mapview(data)
#'
#' @export
VectorDB <- R6::R6Class("VectorDB",
                          public = VectorDB_public,
                          active = VectorDB_active,
                          private = VectorDB_private,
                          lock_class = TRUE,
                          lock_objects = TRUE,
                          portable = FALSE,
                          class = TRUE,
                          cloneable = FALSE
)
