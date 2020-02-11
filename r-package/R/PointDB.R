PointDB_public <- list( # **************** public ***********************

  initialize = function(url, db, curlHandle, rsdbConnector) {
    private$rsdbConnector <- rsdbConnector
    
    private$base_url <- url
    private$db_url <- paste0(url, "/pointdb")
    private$db <- db
    private$curlHandle <- curlHandle
    #if(!RCurl::url.exists(paste0(private$db_url, "/"), curl = RCurl::dupCurlHandle(private$curlHandle))) {
    #  stop("no connection to PointDB: ",private$db_url)
    #}
    private$rsdbConnector$GET("/pointdb/")
  },

  #process_OLD = function(subset, script) {
  #  .Deprecated("$process")
  #  subsetFinal <- paste0(subset, collapse=";")
  #  scriptFinal <- paste0(script, collapse=";")
  #  #j <- query_json(private$db_url, "process", c(db=private$db, subset=subsetFinal, script=scriptFinal, format="json"), curlHandle=private$curlHandle)
  #  #return(j)
  #  df <- query_RDAT(private$db_url, "process", c(db=private$db, subset=subsetFinal, script=scriptFinal, format="rdat"), curlHandle=private$curlHandle)
  #  return(df)
  #},

  process = function(areas, functions) {
    if(is(areas, "SpatialPolygons")) {
      areas <- convert_SpatialPolygons_to_named_matrix_list(areas)
    }
    if(is(areas, "SpatialPolygonsDataFrame")) {
      areas <- convert_SpatialPolygonsDataFrame_to_named_matrix_list(areas)
    }
    translate_area <- function(e, name) {
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
    if(is(areas, "Extent") || is(areas, "Polygon") || is(areas, "Polygons") || is.matrix(areas)) { #convert one object to list
      areas <- list(areas)
    }
    translated_areas <- mapply(FUN=translate_area, areas, titles, SIMPLIFY=FALSE, USE.NAMES=FALSE)
    data <- list(areas=translated_areas, functions=functions)
    param_list <- c(db=private$db, format="rdat")
    #post_json_get_rdat(data=data, api_url=private$db_url, method="process", param_list=param_list, curl=RCurl::dupCurlHandle(private$curlHandle))
    path <- "/pointdb/process"
    query <- as.list(param_list)
    query <- query[query != ""] # remove empty entries
    rdat <- private$rsdbConnector$POST_json(path, query, data)
    return(rdat)
  },

  query = function(ext, filter=NULL, columns=NULL, normalise=NULL) {
    extText <- extToText(ext)
    param_list <- c(db=private$db,
                    ext=extText,
                    filter=paste0(filter, collapse=";"),
                    columns=paste0(columns, collapse=","),
                    normalise=paste0(normalise, collapse=","))
    #query_RDAT(private$db_url,"query",param_list, curlHandle=private$curlHandle)
    path <- "/pointdb/query"
    query <- as.list(param_list)
    query <- query[query != ""] # remove empty entries
    rdat <- private$rsdbConnector$GET(path, query)
    return(rdat)
  },

  query_polygon = function(polygon, filter=NULL, columns=NULL, normalise=NULL) {
    param_list <- c(db=private$db,
                    polygon=createPolygonParameter(polygon),
                    filter=paste0(filter, collapse=";"),
                    columns=paste0(columns, collapse=","),
                    normalise=paste0(normalise, collapse=","))
    #query_RDAT(private$db_url, "polygon", param_list, curlHandle=private$curlHandle)
    path <- "/pointdb/polygon"
    query <- as.list(param_list)
    query <- query[query != ""] # remove empty entries
    rdat <- private$rsdbConnector$GET(path, query)
    return(rdat)
  },

  query_raster = function(ext, type) {
    extText <- extToText(ext)
    param_list <- c(db=private$db,
                    ext=extText,
                    type=paste0(type, collapse=","))
    #query_RDAT(private$db_url, "query_raster", param_list, curlHandle=private$curlHandle)
    path <- "/pointdb/query_raster"
    query <- as.list(param_list)
    query <- query[query != ""] # remove empty entries
    rdat <- private$rsdbConnector$GET(path, query)
    return(rdat)
  }

  #query_dtm = function(ext) {
  #  .Deprecated("query_raster(type='dtm', ...)")
  #  extText <- extToText(ext)
  #  param_list <- c(db=private$db,
  #                  ext=extText)
  #  query_RDAT(private$db_url,"dtm",param_list, curlHandle=private$curlHandle)
  #},

  #query_dsm = function(ext) {
  #  .Deprecated("query_raster(type='dsm', ...)")
  #  extText <- extToText(ext)
  #  param_list <- c(db=private$db,
  #                  ext=extText)
  #  query_RDAT(private$db_url,"dsm",param_list, curlHandle=private$curlHandle)
  #},

  #query_chm = function(ext) {
  #  .Deprecated("query_raster(type='chm', ...)")
  #  extText <- extToText(ext)
  #  param_list <- c(db=private$db,
  #                  ext=extText)
  #  query_RDAT(private$db_url,"chm",param_list, curlHandle=private$curlHandle)
  #}

) # *********************************************************************

PointDB_active <- list( # **************** active ***********************

  info = function() {
    #l <- query_json(private$db_url, "info", c(db=private$db), curlHandle=private$curlHandle)
    path <- "/pointdb/info"
    query <- list(db=private$db)
    result <- private$rsdbConnector$GET(path, query)
    return(result)
  },

  processing_functions = function() {
    #df <- query_RDAT(private$db_url, "process_functions", c(format="rdat"), curlHandle=private$curlHandle)
    path <- "/pointdb/process_functions"
    query <- list(format="rdat")
    df <- private$rsdbConnector$GET(path, query)
    return(df)
  },

  roi_groups = function() {
    #api_url <- paste0(private$base_url, "/api")
    #l <- query_json(api_url, "roi_groups", c(pointdb=private$db), curlHandle=private$curlHandle)
    path <- "/api/roi_groups"
    query <- list(pointdb=private$db)
    result <- private$rsdbConnector$GET(path, query)
    return(result)
  },

  poi_groups = function() {
    #api_url <- paste0(private$base_url, "/api")
    #l <- query_json(api_url, "poi_groups", c(pointdb=private$db), curlHandle=private$curlHandle)
    path <- "/api/poi_groups"
    query <- list(pointdb=private$db)
    result <- private$rsdbConnector$GET(path, query)
    return(result)
  },

  raster_processing_types = function() {
    info <- self$info
    return(info$raster_processing_types)
  }

) # *********************************************************************

PointDB_private <- list( # **************** private *********************

  base_url = NULL,
  db_url = NULL,
  db = NULL,
  curlHandle = NULL,
  rsdbConnector = NULL   

) # *********************************************************************


#' PointDB class
#'
#' PointDB class provides methods to access (LiDAR) point-clouds of Remote Sensing Database.
#'
#' @keywords LiDAR point point-cloud database remote-sensing
#' @seealso \link{RemoteSensing} \link{extent} \link{extent_diameter} \link{extent_radius} \link{RasterLayer-class} \link{visualise_raster}
#' @author woellauer
#'
#' @usage
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#' pointdb <- remotesensing$pointdb(name)
#'
#' pointdb$query(ext, filter=NULL, columns=NULL, normalise=NULL)
#' pointdb$query_polygon(polygon, filter=NULL, columns=NULL, normalise=NULL)
#' pointdb$query_raster(ext, type)
#' pointdb$process(areas, functions)
#'
#' pointdb$info
#' pointdb$processing_functions
#' pointdb$raster_processing_types
#' pointdb$roi_groups
#' pointdb$poi_groups
#'
#' @format
#' RasterDB \link{R6Class} object.
#'
#' @section Details:
#'
#' In the Following methods of PointDB are described. Use instance objects to call that methods (e.g. pointdb$method(paramters) ).
#' Use instance of \link{RemoteSensing} class to create instances of PointDB.
#'
#' \describe{
#'
#' \item{$query(ext, filter=NULL, columns=NULL, normalise=NULL)}{Send query with exent rectangle to database and receive result as data.frame.
#'
#' returns data.frame of points. See section \code{query data.frame} for details.}
#'
#' \item{$query_polygon(polygon, filter=NULL, columns=NULL, normalise=NULL)}{Send query with polygon to database and receive result as data.frame.
#'
#' polygon is of class "Polygons" or "Polygon" or "Matrix"
#'
#' if polygon is matrix: one row per polygon vertex and two columns with x- and y-coordinates.
#'
#' Polygons may be received by remotesensing$roi(...) or remotesensing$roi_group(...).
#'
#' returns data.frame of points. See section \code{query data.frame} for details.}
#'
#' \item{$query_raster(ext, type)}{Query processing of LiDAR points as RasterLayer.
#' See section \code{query raster} for details.}
#'
#' \item{$process(areas, functions)}{Send (index) processing request of areas to database and receive result as data.frame.
#' See section \code{Process} for details.}
#'
#' \item{$info}{meta data of layer (e.g. coordinate reference system, extent).}
#'
#' \item{$processing_functions}{data.frame of processing functions name and description}
#'
#' \item{$raster_processing_types}{data.frame of raster processing type name and title}
#'
#' \item{$roi_groups}{get names of ROI-groups that are associated with this PointDB.
#' Use \link{RemoteSensing} to load ROI-groups by name.
#'
#' returns: data.frame of name and description}
#'
#' \item{$poi_groups}{get names of POI-groups that are associated with this PointDB.
#' Use \link{RemoteSensing} to load POI-groups by name.
#'
#' returns: data.frame of name and description}
#'
#' }
#'
#' @section Query data.frame:
#' A query of this type returns a data.frame with one row per LiDAR point.
#'
#' Meta data is attached to resulting data.frame \code{df}:
#'
#' \itemize{
#'
#'  \item \strong{\code{attr(df,"proj4")}}
#'
#'  coordinate reference system
#'
#' }
#'
#' @section Coordinates:
#' x and y values are in a project specific planar coordinate space.
#'
#' z values are perpendicular to the x,y-plane. e.g. elevation above sea level in meter.
#'
#' For queries parameter \code{ext} is an \code{\link{extent}} object which may be created by \code{ext <- \link{extent}(xmin,xmax,ymin,ymax)} or by \code{ext <- \link{extent_diameter}(x, y, diameter)} or by \code{ext <- \link{extent_radius}(x, y, radius)}
#'
#' @section Columns:
#' The columns paramter is a character-vector or a list of character-vectors. It describes which columns should be contained in the data.frame.
#' The column-names have to be separated by comma. If no columns paramter is specified all columns are included in the data.frame.
#'
#' Currently following columns are possible:
#'
#' \itemize{
#'  \item \strong{x} coordinate.
#'  \item \strong{y} coordinate.
#'  \item \strong{z} coordinate.
#'  \item \strong{intensity} of returned laser pulse echo.
#'  \item \strong{returnNumber} number of returned echo of one laser pulse.
#'  \item \strong{returns} total number of returned echos of one laser pulse.
#'  \item \strong{scanAngleRank} angle in degrees of laser pulse relative to the perpendicular vector of x,y-plane. So a value of 0 degrees indicates that the laser beam exactly points in z-direction.
#'  \item \strong{classification} number value of classification type. The data source may not conatain classification data. Values of 0 and 1 denote unclassified points.
#'  \item \strong{classificationFlags} number that indicates some additional classification information.
#' }
#' example: \code{"x,y,intensity"} The data.frame should contain x and y coordinates and intensity values.
#' example: \code{c("x","y","z")} The data.frame should contain x, y and z coordinates.
#'
#' @section Filter:
#' A filter is a character-vector or a list of character-vectors. It specifies which points of a given set of points should be returned in the data.frame.
#' Without a filter all points are returned.
#'
#' \itemize{
#'  \item \strong{\code{"return=n"}}
#'
#'  returns all points that are the n-th echo of one laser-pulse.
#'
#'  example: filter="return=2" returns alle point that are the second echo.
#'
#'  \item \strong{\code{"last_return=n"}}
#'
#'  returns all points that are the n-th last echo of one laser-pulse.
#'
#'  \item \strong{\code{"classification=n"}}
#'
#'  returns all points that are classified n. Multiple classes are separated by _
#'  
#'  example: filter="classification=1" returns alle point that are of class 1.
#'  
#'  example: filter="classification=1_2_4" returns alle point that are of class 1 or 2 or 4.
#' }
#'
#' Filters can be conjunctive combined with "\code{;}" separator or as elements of a list. Currently no spaces are allowed in the filter expressions.
#'
#' example: \code{"last_return=1"} returns all points that are the last echo.
#'
#' conjunctive example: \code{c("classification=2","last_return=1")} returns all points that are of class 2 and last return.
#'
#' @section Normalise:
#' Parameter \code{normalise} transformes LiDAR points for normalisation purposes.
#'
#' \itemize{
#'
#'  \item \strong{\code{"origin"}}
#'
#'  moves (x,y) coordinates of points to origin (0,0) by moving the query extent lowest coordinate to (0,0). This normalisation may be usefull to have smaller (x,y) coordinate values and to compare different queries.
#'
#'  \item \strong{\code{"extremes"}}
#'
#'  removes LiDAR points that are outliers in z resp. elevation coordinate
#'
#'  \item \strong{\code{"ground"}}
#'
#'  moves z resp. elevation coordinate of LiDAR points to elevation zero. The resulting z coodinates represent vegetation above ground.
#'
#' }
#'
#' Multiple Normalisations may be combined by list of characters or by comma separated character. e.g. \code{"origin,extremes"} or \code{list("extremes","ground")}
#'
#' @section Query raster:
#' A query of this type processes LiDAR points and returns a RasterLayer (or RasterBrick).
#'
#' Parameter 'type' determines processing method.
#'
#' All available types can be listed by pontdb property raster_processing_types (e.g. 'pointdb$raster_processing_types').
#'
#' Type Description:
#'
#' \itemize{
#'
#' \item \strong{\code{DTM}}
#'
#' Digital Terrain Model: ground surface without vegetation
#'
#' \item \strong{\code{DSM}}
#'
#' Digital Surface Model: surface including vegetation
#'
#' \item \strong{\code{CHM}}
#'
#' Canopy Height Model: surface of vegetation above ground (with local ground normalised to height zero). "DSM minus DTM"
#'
#' \item \strong{\code{DTM_slope}}
#'
#' Slope of DTM.
#'
#' \item \strong{\code{DSM_slope}}
#'
#' Slope of DSM.
#'
#' \item \strong{\code{CHM_slope}}
#'
#' Slope of CHM.
#'
#' \item \strong{\code{voxel}}
#'
#' point count per voxel (1x1x1 meter cube)
#' returns RasterBrick with one RasterLayer per one meter height (one slice of voxels of one height step)
#'
#' }
#'
#' Meta data is attached to resulting raster \code{r}:
#'
#' \itemize{
#'
#'  \item \strong{\code{r@crs}}
#'
#'  coordinate reference system
#'
#'  \item \strong{\code{r@extent}}
#'
#'  geographic \code{\link{extent}}
#'
#'  \item \strong{\code{r@ncols}} and \strong{\code{r@nrows}}
#'
#'  extent in raster pixels
#'
#'  \item \strong{\code{names(r)}}
#'
#'  name of this raster may contain processing information.
#'
#' }
#'
#' @section Process:
#'
#' Process applies functions to each area and returns results as data.frame.
#'
#' \strong{Parameter areas:}
#'
#' areas is of type \strong{SpatialPolygons}, \strong{SpatialPolygonsDataFrame} or of type \strong{list}:
#'
#' SpatialPolygons or SpatialPolygonsDataFrame:
#'
#' Polygons are extracted with IDs as names. See details of conversion in \link{convert_SpatialPolygonsDataFrame_to_named_matrix_list}
#'
#' List of Polygons:
#'
#' Polygons are extracted with names (if list elements are named). . See details of conversion in \link{convert_Polygons_to_matrix}
#'
#' List of Polygon:
#'
#' Polygons are extracted with names (if list elements are named). . See details of conversion in \link{convert_Polygon_to_matrix}
#'
#'
#' List of Extent:
#'
#' Polygons are extracted as rectangles and with names (if list elements are named)
#'
#' List of Matrix:
#'
#' Polygons are extracted as polygons and with names (if list elements are named), matrix of two columns (x,y) and one row per point, first and last point need to be same for closed polygons.
#'
#' Example create three areas with names a1, a2, a3 of rectangle, square, triangle:
#'
#' \code{
#' areas <- list(
#'    a1=raster::extent(x=1000, ymin=3000, xmax=1010, ymax=3005),
#'    a2=extent_diameter(x=1200, y=3100, 10),
#'    a3=matrix(c(10,71,12,71,12,72,11,71), ncol=2, byrow=TRUE)
#' )
#' }
#'
#' Example create areas of all ROIs in a ROI-group:
#'
#' \code{rois <- remotesensing$roi_group(group_name="kili_A")}
#'
#' \code{areas <- rois$polygon}
#'
#' Example create areas of all POIs in a POI-group with 10 meter squares:
#'
#' \code{pois <- remotesensing$poi_group(group_name="kili")}
#'
#' \code{areas <- mapply(function(name, x, y) {return(extent_diameter(x, y, 10))}, pois$name, pois$x, pois$y)}
#'
#' \strong{Parameter functions:}
#'
#' A vector or list of function names.
#'
#' Example some functions: \code{functions <- c("BE_H_MAX", "BE_H_P20", "pulse_density")}
#'
#' Example all functions: \code{functions <- pointdb$processing_functions$name}
#'
#' \strong{Returns:}
#'
#' A data.frame with one row per area and one column per function (first column wih area names).
#'
#' Areas are named with names of the named areas-paramter or if areas-parameter is not named with ascending numbers.
#'
#' Areas that are not covered by LiDAR points are not included in result.
#'
#' @examples
#' # open remote sensing database
#' library(RSDB)
#' # remotesensing <- RemoteSensing$new("http://localhost:8081", "user:password") # local
#' remotesensing <- RemoteSensing$new("http://example.com:8081", "user:password") # remote server
#'
#' # get names of PointDBs
#' remotesensing$pointdbs
#'
#' # get one pointdb
#' pointdb <- remotesensing$pointdb("kili")
#'
#' # get names of ROI groups associated with this PointDB
#' pointdb$roi_groups
#'
#' # get names of POI groups associated with this PointDB
#' pointdb$poi_groups
#'
#' # get meta data of PointDB
#' pointdb$info
#'
#' # get one ROI group
#' rois <- remotesensing$roi_group("kili_A")
#'
#' # get one ROI
#' roi <- remotesensing$roi(group_name="kili_A", roi_name="cof3_A")
#'
#' # get one POI group
#' pois <- remotesensing$poi_group("kili")
#'
#' # get one POI
#' poi <- remotesensing$poi(group_name="kili", poi_name="cof3")
#'
#' # get points of point-cloud that are covered by polygon cof3_A
#' df <- pointdb$query_polygon(roi$polygon[[1]])
#' library(rgl)
#' plot3d(df$x, df$y, df$z)
#'
#' # create extent around poi of 10 meter edge length
#' ext <- extent_diameter(poi$x, poi$y, 10)
#'
#' # get points of point-cloud that are covered by ext,
#' # with last return only, just x,y,z coordinates and x,y moved to origin of ext
#' df <- pointdb$query(ext, filter="last_return=1", columns=c("x","y","z"), normalise="origin")
#' library(rgl)
#' plot3d(df$x, df$y, df$z)
#'
#' # get RasterLayer of ext, processed as DTM
#' r <- pointdb$query_raster(ext, "dtm")
#' plot(r)
#' visualise_raster(r)
#'
#' @export
PointDB <- R6::R6Class("PointDB",
                   public = PointDB_public,
                   active = PointDB_active,
                   private = PointDB_private,
                   lock_class = TRUE,
                   lock_objects = TRUE,
                   portable = FALSE,
                   class = TRUE,
                   cloneable = FALSE
)
