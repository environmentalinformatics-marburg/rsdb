RemoteSensing_public <- list( #      *********** public *********************************

  initialize = function(url, userpwd=NULL, ssl_verifypeer=TRUE) {
    if(is.null(userpwd)) {
      userpwd <- "user:password"
    }
    splitIndex <- regexpr(":", userpwd)
    if(splitIndex <= 0) {
      stop("parameter 'userpwd' is not of format: USER:PASSWORD")
    }
    user <- substring(userpwd, 0, splitIndex - 1)
    password <- substring(userpwd, splitIndex + 1)
    private$rsdbConnector <- RsdbConnector$new(base_url = url, username = user, password = password, ssl_verifypeer = ssl_verifypeer)

    #private$base_url <- url
    #test_url <- paste0(private$base_url, "/pointdb/")
    #private$curlHandle <- RCurl::getCurlHandle()
    #if(isUnauthorized(test_url, private$curlHandle)) { # only set AUTH_DIGEST if needed, if AUTH_DIGEST is set if not needed HTTP POST content will not be sent!
    #  #print("set userpwd with AUTH_DIGEST")
    #  private$curlHandle <- RCurl::getCurlHandle(httpauth = RCurl::AUTH_DIGEST, userpwd = userpwd, verbose = FALSE)
    #}
    #if(isUnauthorized(test_url, private$curlHandle)) { # only set AUTH_BASIC if needed, if AUTH_BASIC is set if not needed HTTP POST content will not be sent!
    #  #print("set userpwd with AUTH_BASIC")
    #  private$curlHandle <- RCurl::getCurlHandle(httpauth = RCurl::AUTH_BASIC, userpwd = userpwd, verbose = FALSE)
    #}
    #responesHeader <- RCurl::url.exists(test_url, .header = TRUE, curl = RCurl::dupCurlHandle(private$curlHandle))
    ##print(responesHeader)
    ##print(responesHeader["status"])
    #if(is.logical(responesHeader) && !responesHeader) {
    #  stop("No Connection to Remote Sensing Database.")
    #}
    #if(as.integer(as.integer(responesHeader["status"])/100) != 2) {
    #  stop("no connection to Remote Sensing Database: ",private$base_url, "    ", responesHeader["status"], "  ", responesHeader["statusMessage"])
    #}
  },

  lidar = function(layer) {
    pointdb <- PointDB$new(private$base_url, layer, curlHandle=private$curlHandle, rsdbConnector = private$rsdbConnector)
    return(pointdb)
  },

  pointdb = function(name) {
    pointdb <- PointDB$new(private$base_url, name, curlHandle=private$curlHandle, rsdbConnector = private$rsdbConnector)
    return(pointdb)
  },

  rasterdb = function(name) {
    rasterdb <- RasterDB$new(private$base_url, name, curlHandle=private$curlHandle, rsdbConnector = private$rsdbConnector)
    return(rasterdb)
  },

  lidar_layers = function() {
    #dbs <- query_json(paste0(private$base_url, "/pointdb"), "dbs.json", curlHandle=private$curlHandle)
    result <- private$rsdbConnector$GET("/pointdb/dbs.json")
    return(result)
  },

  web = function() {
    #web_url <- paste0(private$base_url, "/web")
    web_url <- paste0(private$rsdbConnector$private$base_url, "/web")
    browseURL(web_url)
  },

  poi_group = function(group_name) {
    #group <- query_json(paste0(private$base_url, "/api"), "poi_group", c(name=group_name), curlHandle=private$curlHandle)
    group <- private$rsdbConnector$GET("/api/poi_group", list(name=group_name))
    row.names(group) <- group$name
    #names(group$x) <- group$name # does not apply names
    #names(group$y) <- group$name # does not apply names
    return(group)
  },

  poi = function(group_name, poi_name) {
    group <- self$poi_group(group_name)
    df <- group[group$name==poi_name,]
    if(nrow(df)<1) {
      stop("named point in POI-group '",group_name,"' not found: ", poi_name)
    }
    p <- as.list(df)
    return(p)
  },

  roi_group = function(group_name) {
    #group <- query_json(paste0(private$base_url, "/api"), "roi_group", c(name=group_name), curlHandle=private$curlHandle)
    group <- private$rsdbConnector$GET("/api/roi_group", list(name=group_name))
    row.names(group) <- group$name
    names(group$polygon) <- group$name
    return(group)
  },

  roi = function(group_name, roi_name) {
    group <- self$roi_group(group_name)
    df <- group[group$name==roi_name,]
    if(nrow(df)<1) {
      stop("named point in ROI-group '",group_name,"' not found: ", roi_name)
    }
    p <- as.list(df)
    return(p)
  },

  create_rasterdb = function(name, proj4=NULL, resolution=NULL, storage_type=NULL, create_new = TRUE) {
    xres <- NULL
    yres <- NULL
    if(!is.null(resolution)) {
      if(length(resolution) == 1) {
        xres <- resolution[1]
        yres <- resolution[1]
      } else if(length(resolution) == 2) {
        xres <- resolution[1]
        yres <- resolution[2]
      } else stop("invalid parameter argument for resolution")
    }
    #result <- query_json(paste0(private$base_url, "/api"), "create_raster", c(name=name), curlHandle=private$curlHandle)
    result <- private$rsdbConnector$GET("/api/create_raster", list(name=name, proj4=proj4, xres=xres, yres=yres, storage_type=storage_type, create_new=create_new))
    return(result)
  },

  pointcloud = function(name) {
    return(PointCloud$new(private$base_url, name, curlHandle = private$curlHandle, rsdbConnector = private$rsdbConnector))
  },

  vectordb = function(name) {
    return(VectorDB$new(private$base_url, name, curlHandle = private$curlHandle, rsdbConnector = private$rsdbConnector))
  },

  voxeldb = function(name) {
    return(VoxelDB$new(private$rsdbConnector, name))
  },

  submit_task = function(task, wait = TRUE) {
    params <- task
    if(is.list(task)) {
      # nothing
    } else if(is.character(task)) {
      params <- jsonlite::fromJSON(task)
    } else {
      stop("Argument 'task' needs to be a list of parameters or character containing parameters as JSON text.")
    }
    response <- private$rsdbConnector$POST_json("/api/remote_tasks", data = list(remote_task = params))
    remote_task <- RemoteTask$new(private$rsdbConnector, response$remote_task$id)
    if(wait) {
      result <- remote_task$wait()
      return(result)
    } else {
      return(remote_task)
    }
  }

)

RemoteSensing_active <- list( #      *********** active *********************************

  roi_groups = function() {
    #groups <- query_json(paste0(private$base_url, "/api"), "roi_groups", curlHandle=private$curlHandle)
    groups <- private$rsdbConnector$GET("/api/roi_groups")
    return(groups)
  },

  poi_groups = function() {
    #groups <- query_json(paste0(private$base_url, "/api"), "poi_groups", curlHandle=private$curlHandle)
    groups <- private$rsdbConnector$GET("/api/poi_groups")
    return(groups)
  },

  pointdbs = function() {
    #dbs <- query_json(paste0(private$base_url, "/pointdb"), "dbs.json", curlHandle=private$curlHandle)
    dbs <- private$rsdbConnector$GET("/pointdb/dbs.json")
    return(dbs)
  },

  rasterdbs = function() {
    #meta <- query_json(private$base_url, "rasterdbs.json", curlHandle=private$curlHandle)
    meta <- private$rsdbConnector$GET("/rasterdbs.json")
    return(meta$rasterdbs)
  },

  pointclouds = function() {
    #json <- query_json(private$base_url, "pointclouds", curlHandle=private$curlHandle)
    json <- private$rsdbConnector$GET("/pointclouds")
    return(json$pointclouds)
  },

  vectordbs = function() {
    json <- private$rsdbConnector$GET("/vectordbs")
    return(json$vectordbs)
  },

  voxeldbs = function() {
    json <- private$rsdbConnector$GET("/voxeldbs")
    return(json$voxeldbs)
  },

  tasks = function() {
    res <- private$rsdbConnector$GET("/api/remote_task_entries")
    tasks <- res$remote_task_categories
    return(tasks)
  },

  tasks_rasterdb = function() {
    tr <- tasks$remote_task_entries[[match('task_rasterdb', tasks$category)]]
    return(tr)
  },

  tasks_pointdb = function() {
    tr <- tasks$remote_task_entries[[match('task_pointdb', tasks$category)]]
    return(tr)
  },

  tasks_pointcloud = function() {
    tr <- tasks$remote_task_entries[[match('task_pointcloud', tasks$category)]]
    return(tr)
  },

  tasks_voxeldb = function() {
    tr <- tasks$remote_task_entries[[match('task_voxeldb', tasks$category)]]
    return(tr)
  },

  tasks_vectordb = function() {
    tr <- tasks$remote_task_entries[[match('task_vectordb', tasks$category)]]
    return(tr)
  }
)

RemoteSensing_private <- list( #      *********** private *********************************

  base_url = NULL,
  curlHandle = NULL,
  rsdbConnector = NULL

)

#' RemoteSensing class
#'
#' Remote sensing database manages (hyperspectral) \strong{rasters} and (LiDAR) \strong{point-clouds} and \strong{vector} data
#' as well as auxiliary ROIs (regions of interest as named polygons) and POIs (points of interest as named points).
#'
#' Objects of RemoteSensing class encapsulate connections to one remote sensing database.
#'
#' @docType class
#' @export
#' @author woellauer
#' @seealso \link{RasterDB} \link{PointCloud} \link{PointDB} \link{VectorDB} \link{RemoteTask}
#'
#' @format
#' RemoteSensing \code{\link{R6Class}} object.
#'
#' Instance objects of R6Class are created by 'new':
#'
#' \code{remotesensing <- RemoteSensing$new(url, userpwd=NULL)}
#'
#' Methods of instance objects are called by '$':
#'
#' \code{result <- remotesensing$method(parameters)}
#'
#' @usage
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#'
#' remotesensing$rasterdbs
#' remotesensing$rasterdb(name)
#' remotesensing$create_rasterdb(name, proj4=NULL, resolution=NULL, storage_type=NULL, create_new=TRUE)
#'
#' remotesensing$pointclouds
#' remotesensing$pointcloud(name)
#'
#' remotesensing$pointdbs
#' remotesensing$pointdb(name)
#'
#' remotesensing$vectordbs
#' remotesensing$vectordb(name)
#'
#' remotesensing$roi_groups
#' remotesensing$roi_group(name)
#' remotesensing$roi(group_name, roi_name)
#'
#' remotesensing$poi_groups
#' remotesensing$poi_group(name)
#' remotesensing$poi(group_name, poi_name)
#'
#' remotesensing$tasks
#' remotesensing$tasks_rasterdb
#' remotesensing$tasks_pointdb
#' remotesensing$tasks_pointcloud
#' remotesensing$tasks_voxeldb
#' remotesensing$tasks_vectordb
#' remotesensing$submit_task(task, wait = TRUE)
#'
#' remotesensing$web()
#'
#' @section Methods:
#'
#' \describe{
#'
#' \item{RemoteSensing$new(url, userpwd=NULL)}{Open Remote Sensing Database.
#'
#' url: url of server. (local e.g. "http://localhost:8081" or remote e.g. "http://example.com:8081")
#'
#' userpwd: optional authentication (format: "user:password")
#'
#' returns: RemoteSensing object}
#'
#' \item{$rasterdbs}{get names of RasterDBs contained in Remote Sensing Database.}
#'
#' \item{$rasterdb(name)}{get RasterDB by name.}
#'
#' \item{$create_rasterdb(name, proj4=NULL, resolution=NULL, storage_type=NULL, create_new=TRUE)}{creates new empty RasterDB.
#'
#' name: name of RasterDB layer
#'
#' proj4: (optional) CRS as PROJ4
#'
#' resolution: (optional) one number or vector of two numbers of pixel size in projection units, e.g. 0.5 -> xres=0.5, yres=0.5  or e.g. c(10, 5) -> xres=10, yres=5
#'
#' storage_type: (optional) storage_type RasterUnit (default) or TileStorage
#'
#' create_new: (optional) TRUE: delete old raster layer of same name, FALSE: if raster layer already exists do not create it
#'
#' returns: success message}
#'
#' \item{$pointclouds}{get names of PointClouds contained in Remote Sensing Database.}
#'
#' \item{$pointcloud(name)}{get PointCloud by name.}
#'
#' \item{$pointdbs}{get names of PointDBs contained in Remote Sensing Database.}
#'
#' \item{$pointdb(name)}{get PointDB by name.}
#'
#' \item{$vecordbs}{get names of VectorDBs contained in Remote Sensing Database.}
#'
#' \item{$vectordb(name)}{get VectorDB by name.}
#'
#' \item{$roi_groups}{get names of ROI-groups.
#'
#' returns: data.frame of name and description}
#'
#' \item{$roi_group(name)}{get ROI-group by name.
#'
#' returns: data.frame of ROI-name and polygon}
#'
#' \item{$roi(group_name, roi_name)}{get one ROI.
#'
#' returns: polygon}
#'
#' \item{$poi_groups}{get names of POI-groups.
#'
#' returns: data.frame of name and description}
#'
#' \item{$poi_group(name)}{get POI-group by name.
#'
#' returns: data.frame of POI-name and position}
#'
#' \item{$poi(group_name, poi_name)}{get one POI.
#'
#' returns: position}
#'
#' \item{$tasks}{Get details of tasks that can be executed by 'submit_task'.
#'
#' returns: data.frame with nested data.frames of task details}
#'
#' \item{$tasks_rasterb}{Get details of type 'task_rasterb' tasks that can be executed by 'submit_task'.
#'
#' returns: data.frame with task details}
#'
#' \item{$tasks_pointdb}{Get details of type 'task_pointdb' tasks that can be executed by 'submit_task'.
#'
#' returns: data.frame with task details}
#'
#' \item{$tasks_pointcloud}{Get details of type 'task_pointcloud' tasks that can be executed by 'submit_task'.
#'
#' returns: data.frame with task details}
#'
#' \item{$tasks_voxeldb}{Get details of type 'task_voxeldb' tasks that can be executed by 'submit_task'.
#'
#' returns: data.frame with task details}
#'
#' \item{$tasks_vectordb}{Get details of type 'task_vectordb' tasks that can be executed by 'submit_task'.
#'
#' returns: data.frame with task details}
#'
#' \item{$submit_task(task, wait = TRUE)}{Submit a remote task to RSDB server.
#'
#' task: remote task parameters. List of parameters or character containing parameters as JSON text.
#'
#' wait: wait until task finished. Then return result. Default: TRUE.
#'
#' returns: RemoteTask object}
#'
#' \item{$web()}{Open web interface in browser.}
#' }
#'
#' @section ROI:
#' Region of interest (ROI) is a named polygon.
#'
#' Matrix of one row per polygon vertex and two columns with x- and y-coordinates describe the polygon.
#'
#' First and last vertex need to be same (a closed polygon).
#'
#' example:
#' \preformatted{
#' # get one ROI
#' roi <- remotesensing$roi(group_name="kili_A", roi_name="cof3_A")
#'
#' # get name of ROI
#' roi$name
#'
#' # get matrix of points
#' roi$polygon[[1]]
#' }
#'
#' @section POI:
#' Point of interest (POI) is a named point.
#'
#' example:
#' \preformatted{
#' # get one POI
#' poi <- remotesensing$poi(group_name="kili", poi_name="cof3")
#'
#' # get name of ROI
#' poi$name
#'
#' # get x-coodinate of POI
#' poi$x
#'
#' # get y-coodinate of POI
#' poi$y
#' }
#'
#' @examples
#' # open remote sensing database
#' library(RSDB)
#' # remotesensing <- RemoteSensing$new("http://localhost:8081", "user:password") # local
#' remotesensing <- RemoteSensing$new("http://example.com:8081", "user:password") # remote server
#'
#' # get names of RasterDBs
#' remotesensing$rasterdbs
#'
#' # get one rasterdb
#' rasterdb <- remotesensing$rasterdb("kili_campaign1")
#'
#' # get names of PointDBs
#' remotesensing$pointdbs
#'
#' # get one pointdb
#' pointdb <- remotesensing$pointdb("kili")
#'
#' # get names of ROI groups
#' remotesensing$roi_groups
#'
#' # get one ROI group
#' rois <- remotesensing$roi_group("kili_A")
#'
#' # get one ROI
#' roi <- remotesensing$roi(group_name="kili_A", roi_name="cof3_A")
#'
#' # get names of POI groups
#' remotesensing$poi_groups
#'
#' # get one POI group
#' pois <- remotesensing$poi_group("kili")
#'
#' # get one POI
#' poi <- remotesensing$poi(group_name="kili", poi_name="cof3")
#'
#' # create extent around POI of 10 meter edge length
#' ext <- extent_diameter(poi$x, poi$y, 10)
#'
#' # get RasterStack of all bands at ext
#' r <- rasterdb$raster(ext)
#'
#' # get data.frame of LiDAR points at ext
#' df1 <- pointdb$query(ext)
#'
#' #get data.frame of LiDAR points at polygon ROI
#' df2 <- pointdb$query_polygon(roi$polygon[[1]])
#'
#' # open web interface in browser
#' remotesensing$web()
#'
RemoteSensing <- R6::R6Class("RemoteSensing",
                          public = RemoteSensing_public,
                          active = RemoteSensing_active,
                          private = RemoteSensing_private,
                          lock_class = TRUE,
                          lock_objects = TRUE,
                          portable = FALSE,
                          class = TRUE,
                          cloneable = FALSE
)
