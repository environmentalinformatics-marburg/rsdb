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

    #' @description
    #' Get vector features and properties.
    #' @param bbox Bounding box.
    #' @param crop Crop vector features to bbox.
    #' @param field One or more fields to return in addition to the geometry. If NULL, all fields are returned.
    getFeatures = function(bbox = NULL, crop = TRUE, field = NULL) {
      args <- list()
      if(!is.null(bbox)) {
        if(class(bbox) != 'bbox') {
          stop('optional parameter bbox needs to be of class bbox')
        }
        args$bbox <- paste(bbox$xmin, bbox$ymin, bbox$xmax, bbox$ymax, sep = ',')
      }
      args$crop <- crop
      response <- private$rsdbConnector$GET(paste0("/postgis/layers/", private$name_, '/geojson'), args)
      tempFileName <- tempfile()
      writeBin(response, tempFileName)
      features <- sf::st_read(tempFileName, quiet = TRUE)
      file.remove(tempFileName)
      if(!is.null(field)) {
        features <- features[field]
      }
      return(features)
    },

    #' @description
    #' Get raster from rasterized vector features.
    #'
    #' Raster pixel values are the values of field parameter.
    #'
    #' If priority parameter = NULL, order of feature painting is unspecified, so for overlapping features of different field values the final pixel value is the value of one of the overlapping features.
    #'
    #' \strong{Basic usage:} The priority parameter (as a vector of numbers) specifies order of feature painting. For overlapping features the last entry in the priority vector determines the pixel value.
    #' Only features values listed in the priority vector are painted, all other features are omitted in the final raster.
    #'
    #' \strong{Basic example:}
    #'
    #' feature field values: 1 2 3 4
    #'
    #' priority = c(2,3)  => Only features of value 2 and 3 are painted. For overlapping features value 3 is the pixel value.
    #'
    #' priority = c(3,2)  => Only features of value 2 and 3 are painted. For overlapping features value 2 is the pixel value.
    #'
    #'
    #' \strong{Advanced usage:} Merge multiple field values to one pixel value each.
    #'
    #' For this variant the priority parameter has to be a list of vectors of numbers.
    #'
    #' Each entry in the list (a vector of numbers) merges to one value (the first number in the vector).
    #'
    #' \strong{Advanced example:}
    #'
    #' feature field values: 1 2 3 4
    #'
    #' priority = list(c(2,3))  => Only features of value 2 and 3 are painted. The pixel values are 2.
    #'
    #' priority = list(c(1,2), c(3,4))  => Only features of value 1,2,3,4 are painted. The pixel values are 1 or 3. If overlapping it is 3.
    #'
    #' @param bbox Bounding box.
    #' @param field Value field for pixel values.
    #' @param dx Pixel x resolution.
    #' @param dy Pixel y resolution.
    #' @param priority Features rasterization order. Vector of numbers or List of vector of numbers.
    #' @param bypass Just return the prepared features before rasterize step. Utility to diagnose and fix issues.
    getRaster = function(bbox, field = NULL, dx = 1, dy = 1, priority = NULL, bypass = FALSE) {
      if(is.null(bbox)) {
        stop('missing bbox')
      }
      if(is.na(sf::st_crs(bbox))) {
        sf::st_crs(bbox) <- self$epsg
      }
      features <- self$getFeatures(bbox, crop = TRUE)
      if(is.null(field)) {
        if(length(self$class_fields) > 0) {
          field <- self$class_fields[1]
        } else {
          stop('Missing field parameter and no default.');
        }
      }
      if(!field %in% colnames(features)) {
        stop('Requested field does not exist: ', field);
      }
      features <- features[field]
      fieldCol <- features[[field]]
      if(!(is.numeric(fieldCol) || is.factor(fieldCol))) {
        fieldFactors <- as.factor(fieldCol)
        features[field] <- fieldFactors
      }
      if(!is.null(priority)) {
        if(!is.list(priority)) {
          matched <- match(features[[field]], priority)
          features_clean <- features[!is.na(matched), ]
          matched_clean <- matched[!is.na(matched)]
          ord <- order(matched_clean)
          features_clean_ord <- features_clean[ord, ]
          features <- features_clean_ord
        } else {
          features_prio_list <- lapply(priority, function(prio) {
            matched <- match(features[[field]], prio)
            features_matched <- features[!is.na(matched), ]
            if(nrow(features_matched) > 0) {
              features_agg <- features_matched
              features_agg[field] <- prio[1]
              return(features_agg)
            } else {
              return(NULL)
            }
          })
          features_prio_list <- features_prio_list[!sapply(features_prio_list, is.null)]
          features <- do.call(rbind, features_prio_list)
        }
      }
      if(bypass) {
        return(features)
      }
      template <- stars::st_as_stars(bbox, dx = dx, dy = dy)
      r <- stars::st_rasterize(sf = features, template = template)
      return(r)
    },

    #' @description
    #' Calculate indices from vector features.
    #' @param bbox Bounding box.
    #' @param field Classification field for feature aggregation.
    #' @param crop Crop vector features to bbox.
    #' @param values If raw values should be returned instead of summary statistical measures per class.
    getIndices = function(bbox, field = NULL, crop = TRUE, values = FALSE) {
      if(is.null(bbox)) {
        stop('missing bbox')
      }
      json <- list(bbox = as.list(bbox))
      json$crop = crop
      if(!is.null(field)) {
        json$field <- field
      }
      json$values = values
      df <- private$rsdbConnector$POST_json(paste0("/postgis/layers/", private$name_, '/indices'), data = json)
      return(df)
    }

  ),
  active = list( #      *********** active *********************************

    #' @field name PostGIS layer name.
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
