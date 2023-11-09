PointCloud_private <- list( #      *********** private *******************************

  name_ = NULL,
  curlHandle = NULL,
  base_url = NULL,
  path_url = NULL,
  url = NULL,
  meta_ = NULL,
  rsdbConnector = NULL

) #***********************************************************************************

#' PointCloud class
#' @description PointCloud class provides methods to access (LiDAR) point-clouds of Remote Sensing Database.
#'
#' Use instance of \link{RemoteSensing} class to create instances of PointCloud class.
#'
#' In section 'Active bindings' class fields and in section 'Methods' class functions of pointcloud class are described.
#'
#' Use instance objects of PointCloud class to call that functionality (e.g. pointcloud$name ).
#'
#' @keywords LiDAR point point-cloud database remote-sensing
#' @seealso \link{RemoteSensing} \link{extent} \link{Polygon} \link{extent_diameter} \link{extent_radius} \link{RasterLayer-class} \link{visualise_raster}
#' @author woellauer
#'
#' @usage
#' # connect to remote sensing database
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#'
#' # open one pointcloud in connected remote sensing database
#' pointcloud <- remotesensing$pointcloud(name)
#'
#' # operate on opened pointcloud:
#'
#' pointcloud$points(ext, columns=NULL, filter=NULL, normalise=NULL, time_slice_id=NULL, time_slice_name=NULL)
#' pointcloud$raster(ext, res=1, type="point_count", fill=10, time_slice_id=NULL, time_slice_name=NULL)
#' pointcloud$volume(ext, res=1, zres=res, time_slice_id=NULL, time_slice_name=NULL)
#' pointcloud$indices(areas, functions, omit_empty_areas=TRUE, time_slice_id=NULL, time_slice_name=NULL)
#' pointcloud$set_meta(meta)
#'
#' pointcloud$name
#' pointcloud$meta
#' pointcloud$geocode
#' pointcloud$proj4
#' pointcloud$extent
#' pointcloud$description
#' pointcloud$raster_list
#' pointcloud$attribute_list
#' pointcloud$index_list
#' pointcloud$time_slices
#'
#' @format
#' PointCloud \link{R6Class} object.
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
#' remotesensing$pointclouds
#'
#' # open pointcloud
#' pointcloud <- remotesensing$pointcloud("jannik_uniwald_sequoia")
#'
#' # get projection as proj4
#' pointcloud$proj4
#'
#' #create position
#' pos <- c(477662.863, 5632129.623)
#'
#' # create extent at position and diameter of 10 meter
#' ext <- extent_diameter(x=pos[1], y=pos[2], 10)
#'
#' # get data.frame of points in extent
#' df <- pointcloud$points(ext, columns=c("x", "y", "z", "classification", "returns"))
#'
#' # get raster of DSM at 0.5 meter resolution
#' r <- pointcloud$raster(ext, res=0.5,  type="dsm", fill=10)
#' plot(r)
#' visualise_raster(r)
#'
#' #calculate all indices for ext and name ext as 'e1'
#' df <- pointcloud$indices(list(e1=ext), pointcloud$index_list$name)
#'
#' @export
PointCloud <- R6::R6Class("PointCloud",
  public = PointCloud_public <- list( #      *********** public *********************************

    #' @description
    #' Use instance of \link{RemoteSensing} class to create instances of PointCloud class.
    #' This initialize function is for internal use only.
    #' @param base_url RSDB server URL.
    #' @param name Pointcloud name.
    #' @param curlHandle connection handler.
    #' @param rsdbConnector rsdb server connection.
    initialize = function(base_url, name, curlHandle, rsdbConnector) {
      private$rsdbConnector <- rsdbConnector

      private$name_ <- name
      private$curlHandle <- curlHandle
      private$base_url <- base_url
      private$path_url <-paste0(private$base_url, "/pointclouds")
      private$url <- paste0(private$path_url, "/", private$name_)
      #m <- query_json(private$path_url, private$name_, curl = RCurl::dupCurlHandle(private$curlHandle))
      m <- private$rsdbConnector$GET(paste0("/pointclouds/", private$name_))
      private$meta_ <- m$pointcloud
    },

    #' @description
    #' Retrieve subsets of pointcloud points.
    #'
    #' @return data.frame of points.
    #'
    #' @param ext Subset of pointcloud.
    #'
    #' ext is of type 'bbox' or 'Extent' (rectangular area) or 'Polygon' (polygon area, see \link{convert_Polygon_to_matrix}) or 'polygons' (polygon area, see \link{convert_Polygons_to_matrix}) or 'Matrix' (polygon area, two columns (x,y) and one row per point, first and last point need to be same for closed polygons).
    #'
    #' @param columns Requested attributes.
    #'
    #' Vector of column-names. See "$attribute_list" for available attributes.
    #'
    #' @param filter Point filter.
    #'
    #' Vector of conjunctive connected filter elements:
    #'
    #' \itemize{
    #' \item \strong{'ground'} Ground classified points only.
    #' \item \strong{'vegetation'} Vegetation classified points only
    #' \item \strong{'non_ground'} All not ground classified points.
    #' \item \strong{'non_vegetation'} All not vegetation classified points.
    #' \item \strong{'classification=x'} e.g. 'classification=1' Points of that classification only.
    #' \item \strong{'return=x'} e.g. 'return=1' Points of that return only.
    #' \item \strong{'last_return=x'} e.g. 'last_return=1' Points of that last_return only (returns counted in reverse beginning with last).
    #' }
    #'
    #' @param normalise (deprecated) Point normalisation.
    #'
    #' \itemize{
    #' \item \strong{'extremes'} Removes points that are outliers in z resp. elevation coordinate.
    #' \item \strong{'ground'} Moves z resp. elevation coordinate of LiDAR points to elevation zero. The resulting z coordinates represent vegetation above ground.
    #' }
    #'
    #' Multiple Normalisations may be combined by list of characters or by comma separated character. e.g. \code{"extremes,ground"} or \code{list("extremes","ground")}
    #'
    #' @param time_slice_id Optionally, get points of specified time slice id.
    #'
    #' @param time_slice_name Optionally, get points of specified time slice name.
    points = function(ext, columns=NULL, filter=NULL, normalise=NULL, time_slice_id=NULL, time_slice_name=NULL) {
      if(is(ext, "bbox")) {
        warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
        ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
      }
      param_list <- c()
      if(is(ext, "Extent")) {
        extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
        param_list <- c(param_list, ext=extText)
      } else if (is(ext, "Polygons")) {
        m <- convert_Polygons_to_matrix(ext)
        polygonText <- paste0(m, collapse=" ")
        param_list <- c(param_list, polygon=polygonText)
      } else if (is(ext, "Polygon")) {
        m <- convert_Polygon_to_matrix(ext)
        polygonText <- paste0(m, collapse=" ")
        param_list <- c(param_list, polygon=polygonText)
      } else if (is.matrix(ext)) {
        stopifnot(ncol(ext) == 2) # x and y coordinate
        stopifnot(nrow(ext) >= 3) # at least three vertices and first and last same
        polygonText <- paste0(ext, collapse=" ")
        param_list <- c(param_list, polygon=polygonText)
      } else {
        stop("Parameter ext needs to be of type 'Extent' or 'Polygon' or 'Polygons' or 'Matrix'.")
      }

      if(!is.null(columns)) {
        stopifnot(is.character(columns))
        columnsText <- paste0(columns, collapse=" ")
        param_list <- c(param_list, columns=columnsText)
      }
      if(!is.null(filter)) {
        stopifnot(is.character(filter))
        filterText <- paste0(filter, collapse=";")
        param_list <- c(param_list, filter=filterText)
      }
      if(!is.null(normalise)) {
        stopifnot(is.character(normalise))
        normaliseText <- paste0(normalise, collapse=",")
        param_list <- c(param_list, normalise=normaliseText)
      }
      if(!is.null(time_slice_id)) {
        stopifnot(is.character(time_slice_id))
        param_list <- c(param_list, time_slice_id=time_slice_id)
      }
      if(!is.null(time_slice_name)) {
        stopifnot(is.character(time_slice_name))
        param_list <- c(param_list, time_slice_name=time_slice_name)
      }
      #return(query_RDAT(private$url, "points.rdat", param_list, curl = RCurl::dupCurlHandle(private$curlHandle)))
      path <- paste0("/pointclouds/", private$name_, "/points.rdat")
      query <- as.list(param_list)
      rdat <- private$rsdbConnector$GET(path, query)
      return(rdat)
    },

    #' @description
    #' Process pointcloud to raster.
    #'
    #' @return RasterLayer
    #'
    #' @param ext Subset of pointcloud.
    #'
    #' ext is of type 'bbox' or 'Extent' (rectangular area).
    #'
    #' @param res Pixel size of raster.
    #'
    #' @param type Type of raster processing.
    #'
    #' For a complete list of available types see "$raster_list". Most used types are 'dsm', 'dtm', 'chm'.
    #'
    #' @param fill If pixel value is missing, fill it with nearest pixels of at most given pixel count distance.
    #'
    #' @param time_slice_id Optionally, process specified time slice id.
    #'
    #' @param time_slice_name Optionally, process specified time slice name.
    raster = function(ext, res=1, type="point_count", fill=10, time_slice_id=NULL, time_slice_name=NULL) {
      if(is(ext, "bbox")) {
        warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
        ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
      }
      stopifnot(is(ext, "Extent"))
      extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
      param_list <- c(ext=extText)
      if(!is.null(res)) {
        stopifnot(is.numeric(res))
        stopifnot(length(res) == 1)
        param_list <- c(param_list, res=res)
      }
      if(!is.null(type)) {
        stopifnot(is.character(type))
        stopifnot(length(type) == 1)
        param_list <- c(param_list, type=type)
      }
      if(!is.null(fill)) {
        stopifnot(is.numeric(fill))
        stopifnot(length(fill) == 1)
        param_list <- c(param_list, fill=fill)
      }
      if(!is.null(time_slice_id)) {
        stopifnot(is.character(time_slice_id))
        param_list <- c(param_list, time_slice_id=time_slice_id)
      }
      if(!is.null(time_slice_name)) {
        stopifnot(is.character(time_slice_name))
        param_list <- c(param_list, time_slice_name=time_slice_name)
      }
      #return(query_RDAT(private$url, "raster.rdat", param_list, curl = RCurl::dupCurlHandle(private$curlHandle)))
      path <- paste0("/pointclouds/", private$name_, "/raster.rdat")
      query <- as.list(param_list)
      rdat <- private$rsdbConnector$GET(path, query)
      return(rdat)
    },

    #' @description
    #' Process pointcloud to voxels (multiple layers of rasters).
    #'
    #' @return RasterStack
    #'
    #' @param ext Subset of pointcloud.
    #'
    #' ext is of type 'bbox' or 'Extent' (rectangular area).
    #'
    #' @param res Pixel (voxel) size of raster.
    #'
    #' @param zres Height of voxel (height per layer).
    #'
    #' @param time_slice_id Optionally, process specified time slice id.
    #'
    #' @param time_slice_name Optionally, process specified time slice name.
    volume = function(ext, res=1, zres=res, time_slice_id=NULL, time_slice_name=NULL) {
      if(is(ext, "bbox")) {
        warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
        ext <- raster::extent(ext$xmin, ext$xmax, ext$ymin, ext$ymax) # convert bbox to Extent
      }
      stopifnot(is(ext, "Extent"))
      extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
      param_list <- c(ext=extText)
      if(!is.null(res)) {
        stopifnot(is.numeric(res))
        stopifnot(length(res) == 1)
        param_list <- c(param_list, res=res)
      }
      if(!is.null(zres)) {
        stopifnot(is.numeric(zres))
        stopifnot(length(zres) == 1)
        param_list <- c(param_list, zres=zres)
      }
      if(!is.null(time_slice_id)) {
        stopifnot(is.character(time_slice_id))
        param_list <- c(param_list, time_slice_id=time_slice_id)
      }
      if(!is.null(time_slice_name)) {
        stopifnot(is.character(time_slice_name))
        param_list <- c(param_list, time_slice_name=time_slice_name)
      }
      #return(query_RDAT(private$url, "volume.rdat", param_list, curl = RCurl::dupCurlHandle(private$curlHandle)))
      path <- paste0("/pointclouds/", private$name_, "/volume.rdat")
      query <- as.list(param_list)
      rdat <- private$rsdbConnector$GET(path, query)
      return(rdat)
    },

    #' @description
    #' Calculate metrices at areas.
    #'
    #' @return data.frame
    #'
    #' @param areas Areas is of type:
    #'
    #'  'SpatialPolygons' (several polygon areas) Polygons are extracted with IDs as names. See details of conversion in \link{convert_SpatialPolygons_to_named_matrix_list}
    #'
    #'  'SpatialPolygonsDataFrame' (several polygon areas) Polygons are extracted with IDs as names. See details of conversion in \link{convert_SpatialPolygonsDataFrame_to_named_matrix_list}
    #'
    #'  or one area or list of following types:
    #'
    #' 'bbox' or 'Extent' (one rectangular area) Polygons are extracted as rectangles and with names (if list elements are named)
    #'
    #' 'Polygon' (one polygon area) Polygons are extracted with names (if list elements are named). . See details of conversion in \link{convert_Polygon_to_matrix}
    #'
    #' 'Polygons' (one polygon area) Polygons are extracted with names (if list elements are named). . See details of conversion in \link{convert_Polygons_to_matrix}
    #'
    #' 'Matrix' (one polygon area) Polygons are extracted as polygons and with names (if list elements are named), matrix of two columns (x,y) and one row per point, first and last point need to be same for closed polygons.
    #'
    #'
    #' If list entries are named (e.g. list(name1=ext1, name2=ext2) ) then the returned data.fram contains that names in a 'name'-column.
    #'
    #' @param functions One index name or a list of index names.
    #'
    #' For available index names see "$index_list".
    #'
    #' @param omit_empty_areas Don't include areas with no point data in result (default).
    #'
    #' @param time_slice_id Optionally, process specified time slice id.
    #'
    #' @param time_slice_name Optionally, process specified time slice name.
    #'
    indices = function(areas, functions, omit_empty_areas=TRUE, time_slice_id=NULL, time_slice_name=NULL) {
      stopifnot(is(omit_empty_areas, "logical"))
      if(is(areas, "SpatialPolygons")) {
        areas <- convert_SpatialPolygons_to_named_matrix_list(areas)
      }
      if(is(areas, "SpatialPolygonsDataFrame")) {
        areas <- convert_SpatialPolygonsDataFrame_to_named_matrix_list(areas)
      }
      translate_area <- function(e, name) {
        if(is(e, "bbox")) {
          warning("convert sf::st_bbox to raster::extent by ignoring bbox crs")
          e <- raster::extent(e$xmin, e$xmax, e$ymin, e$ymax) # convert bbox to Extent
        }
        if(is(e, "Extent")) { # bbox
          return(list(name=name, bbox=list(e@xmin, e@ymin, e@xmax, e@ymax)))
        } else if(is(e, "Polygons")) {
          p <- convert_Polygons_to_matrix(e)
          return(list(name=name, polygon=p))
        } else if(is(e, "Polygon")) {
          p <- convert_Polygon_to_matrix(e)
          return(list(name=name, polygon=p))
        } else if(is.matrix(e)) { # polygon coordinates
          stopifnot(ncol(e) == 2) # x and y coordinate
          stopifnot(nrow(e) >= 3) # at least three vertices and first and last same
          return(list(name=name, polygon=e))
        } else {
          stop("unknown area type of: ", e, " in ", name, "  type: ", typeof(e), "   valid types are 'Extent' or 'Polygons' or 'Polygon' or 'Matrix'")
        }
      }
      titles <- names(areas)
      if(is.null(titles)) {
        titles <- c(1:length(areas))
      }
      if(is(areas, "bbox") || is(areas, "Extent") || is(areas, "Polygon") || is(areas, "Polygons") || is.matrix(areas)) { #convert one object to list
        areas <- list(areas)
      }
      translated_areas <- mapply(FUN=translate_area, areas, titles, SIMPLIFY=FALSE, USE.NAMES=FALSE)
      data <- list(areas=translated_areas, functions=functions, omit_empty_areas=omit_empty_areas)

      if(!is.null(time_slice_id)) {
        stopifnot(is.character(time_slice_id))
        data <- c(data, time_slice_id=time_slice_id)
      }
      if(!is.null(time_slice_name)) {
        stopifnot(is.character(time_slice_name))
        data <- c(data, time_slice_name=time_slice_name)
      }

      #return(post_json_get_rdat(data=data, private$url, method="indices.rdat", curl=RCurl::dupCurlHandle(private$curlHandle)))
      path <- paste0("/pointclouds/", private$name_, "/indices.rdat")
      query <- list()
      result <- private$rsdbConnector$POST_json(path, query, data)
      return(result)
    },

    #' @description
    #' Change meta data of this pointcloud layer
    #'
    #' @return result
    #'
    #' @param meta Named list of meta data entries to be set. Named entries can be:
    #' title, description, corresponding_contact, acquisition_date, code, proj4,
    #' acl, acl_mod, acl_owner, tags, associated, properties
    #'
    set_meta = function(meta) {
      path <- paste0("/pointclouds/", private$name_)
      json = list(pointcloud = meta)
      result <- private$rsdbConnector$POST_json(path, data = json)
      #self$refresh_meta()
      return(result)
    }

  ), #***********************************************************************************
  active = list( #      *********** active *********************************

    #' @field name Pointcloud name.
    name = function() {
      return(private$name_)
    },

    #' @field meta Meta data.
    meta = function() {
      return(private$meta_)
    },

    #' @field geocode Geo code (e.g. EPSG code).
    geocode = function() {
      return(private$meta_$code)
    },

    #' @field proj4 Pointcloud Projection as PROJ4.
    proj4 = function() {
      return(private$meta_$proj4)
    },

    #' @field extent Full extent of pointcloud as 'Extent' object. Note: Do not use this full extent to request data as the full pointcloud may be many gigabytes large!
    extent = function() {
      if(is.null(private$meta_$extent)) {
        #m <- query_json(private$path_url, paste0(private$name_, "?extent"), curl = RCurl::dupCurlHandle(private$curlHandle))
        path <- paste0("/pointclouds/", private$name_)
        query <- list(extent=TRUE)
        m <- private$rsdbConnector$GET(path, query)
        private$meta_ <- m$pointcloud
      }
      m <- private$meta_$extent
      return(raster::extent(m[1], m[3], m[2], m[4]))
    },

    #' @field description Textual pointcloud data description.
    description = function() {
      return(private$meta_$description)
    },

    #' @field raster_list Names of available raster processing types, that can be requested by "$raster()" method.
    raster_list = function() {
      return(private$meta_$raster_types)
    },

    #' @field attribute_list Names of point attributes (x, y, z, intensity, etc.), that are available in this pointcloud.
    attribute_list = function() {
      return(private$meta_$attributes)
    },

    #' @field index_list Names and description of LiDAR indices, that can be requested by "$indices()" method.
    index_list = function() {
      #m <- query_json(private$url, "index_list", curl = RCurl::dupCurlHandle(private$curlHandle))
      path <- paste0("/pointclouds/", private$name_, "/index_list")
      m <- private$rsdbConnector$GET(path)
      return(m$index_list)
    },

    #' @field time_slices List of all time slices in this pointcloud.
    time_slices = function() {
      return(private$meta_$time_slices)
    }

  ), #***********************************************************************************
  private = PointCloud_private,
  lock_class = TRUE,
  lock_objects = TRUE,
  portable = FALSE,
  class = TRUE,
  cloneable = FALSE
)
