RasterDB_public <- list( #      *********** public *********************************

  initialize = function(base_url, name, curlHandle, rsdbConnector) {
    private$rsdbConnector <- rsdbConnector

    private$base_url <- base_url
    private$name_ <- name
    private$curlHandle <- curlHandle
    private$url <- paste0(private$base_url, "/rasterdb/", private$name_)
    #if(!RCurl::url.exists(paste0(private$url, "/meta.json"), curl = RCurl::dupCurlHandle(private$curlHandle))) {
    #  stop("no connection to RasterDB: ",private$db_url)
    #}
    #private$meta <- query_json(private$url, "meta.json", curl = RCurl::dupCurlHandle(private$curlHandle))
    self$refresh_meta()
  },

  refresh_meta = function() {
    private$meta <- private$rsdbConnector$GET(paste0("/rasterdb/", private$name_, "/meta.json"))
  },

  raster = function(ext, time_slice=NULL, band=NULL, product=NULL, masking='center', timestamp=NULL) {
    spMask <- NULL
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
    extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
    param_list <- c(ext=extText)
    if(!is.null(time_slice)) {
      param_list <- c(param_list, time_slice=time_slice)
    }
    if(!is.null(band)) {
      bandText <- paste(as.integer(band), collapse=" ")
      param_list <- c(param_list, band=bandText)
    }
    if(!is.null(product)) {
      param_list <- c(param_list, product=product)
    }
    if(!is.null(timestamp)) {
      param_list <- c(param_list, timestamp=timestamp)
    }
    #r <- query_RDAT(private$url, "raster.rdat", param_list, curl = RCurl::dupCurlHandle(private$curlHandle))
    #return(r)
    path <- paste0("/rasterdb/", private$name_, "/raster.rdat")
    query <- as.list(param_list)
    rdat <- private$rsdbConnector$GET(path, query)
    if(masking == 'extent') {
      # nothing
    } else if(masking == 'center') {
      if(!is.null(spMask)) {
        warning('some extended meta data attributes of the raster are lost by applied masking')
        rdat <- raster::mask(rdat, spMask)
      }
    } else {
      stop("unknown masking: ", masking)
    }
    return(rdat)
  },

  insert_RasterLayer = function(r, time_slice=NULL, band=1, band_title=NULL, flush=TRUE, update_pyramid=TRUE, refresh_meta=TRUE, timestamp=NULL) {
    stopifnot(is(r, "RasterLayer"))
    ext <- r@extent
    extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
    data <- raster::getValues(r)
    if(typeof(data) == "integer" & -32768 <= min(data, na.rm = TRUE) & max(data, na.rm = TRUE) <= 32767) {
      data_type <- "int16"
      raw <- writeBin(object=data, con=raw(0), size=2, endian="little")
    } else {
      data_type <- "float32"
      if(typeof(data) != "numeric") {
        data <- as.numeric(data)
      }
      raw <- writeBin(object=data, con=raw(0), size=4, endian="little")
      #cat(length(data), length(raw), "\n")
    }
    path <- paste0("/rasterdb/", private$name_, "/insert_raster")
    query <- list(width=r@ncols, height=r@nrows, time_slice=time_slice, timestamp=timestamp, band=band, ext=extText, proj4=r@crs@projargs, flip_y=TRUE, band_title=band_title, flush=flush, update_pyramid=update_pyramid, data_type=data_type)
    result <- private$rsdbConnector$POST_raw(path, query, raw)
    if(refresh_meta) {
      self$refresh_meta()
    }
    return(result)
  },

  insert_RasterStack = function(r, time_slice=NULL, bands=NULL, flush=TRUE, update_pyramid=TRUE, refresh_meta=TRUE, timestamp=NULL) {
    stopifnot(is(r, "RasterStack"))
    len <- length(r@layers)
    if(is.null(bands)) {
      bands <- c(1:len)
    }
    stopifnot(len == length(bands))
    result <- lapply(c(1:len), function(i) {
      layer <- r@layers[[i]]
      band <- bands[[i]]
      if(i < len) {
        res <- self$insert_RasterLayer(r=layer, time_slice=time_slice, band=band, flush=FALSE, update_pyramid=FALSE, refresh_meta=FALSE, timestamp=timestamp)
      } else {
        res <- self$insert_RasterLayer(r=layer, time_slice=time_slice, band=band, flush=flush, update_pyramid=update_pyramid, refresh_meta=refresh_meta, timestamp=timestamp)
      }
      return(res)
    })
    return(result)
  },

  rebuild_pyramid = function() {
    self$update_pyramid()
  },

  update_pyramid = function() {
    #result <- query_json(private$url, "rebuild_pyramid", curl = RCurl::dupCurlHandle(private$curlHandle))
    path <- paste0("/rasterdb/", private$name_, "/rebuild_pyramid")
    result <- private$rsdbConnector$POST_json(path)
    return(result)
  },

  set_meta = function(meta) {
    path <- paste0("/rasterdb/", private$name_, "/set")
    json = list(meta = meta)
    result <- private$rsdbConnector$POST_json(path, data = json)
    self$refresh_meta()
    return(result)
  },

  set_band_meta = function(bands) {
    meta <- list(bands = bands)
    result <- self$set_meta(meta)
    return(result)
  }

) #***********************************************************************************

RasterDB_active <- list( #      *********** active *********************************

  name = function() {
    return(private$name_)
  },

  bands = function() {
    return(private$meta$bands)
  },

  time_slices = function() {
    return(private$meta$time_slices)
  },

  timestamps = function() {
    return(private$meta$timestamps)
  },

  pixel_size = function() {
    return(private$meta$ref$pixel_size)
  },

  extent = function() {
    m <- private$meta$ref$extent
    return(raster::extent(m[1], m[3], m[2], m[4]))
  },

  geo_code = function() {
    return(private$meta$ref$code)
  },

  proj4 = function() {
    return(private$meta$ref$proj4)
  },

  description = function() {
    return(private$meta$description)
  }

) #***********************************************************************************

RasterDB_private <- list( #      *********** private *******************************

  base_url = NULL,
  name_ = NULL,
  curlHandle = NULL,
  url = NULL,
  meta = NULL,
  rsdbConnector = NULL

) #***********************************************************************************


#' RasterDB class
#'
#' RasterDB class provides methods to access (hyperspectral) rasters of Remote Sensing Database.
#'
#' @keywords hyperspectral spectral raster database band pixel remote-sensing
#' @seealso \link{RemoteSensing} \link{as.speclib} \link{extent} \link{extent_diameter} \link{extent_radius} \link{RasterLayer-class} \link{RasterStack-class}
#' @author woellauer
#'
#' @usage
#' remotesensing <- RemoteSensing$new(url, userpwd=NULL)
#' rasterdb <- remotesensing$rasterdb(name)
#'
#' raster_stack <- rasterdb$raster(ext, time_slice=NULL, band=NULL, product=NULL, masking='center', timestamp=NULL)
#' rasterdb$insert_RasterLayer(r, time_slice=NULL, band=1, band_title=NULL, flush=TRUE, update_pyramid=TRUE, refresh_meta=TRUE, timestamp=NULL)
#' rasterdb$insert_RasterStack(r, time_slice=NULL, bands=NULL, flush=TRUE, update_pyramid=TRUE, refresh_meta=TRUE, timestamp=NULL)
#' rasterdb$refresh_meta()
#' rasterdb$update_pyramid()
#' rasterdb$set_meta(meta)
#' rasterdb$set_band_meta(bands)
#'
#' rasterdb$name
#' rasterdb$time_slices
#' rasterdb$bands
#' rasterdb$pixel_size
#' rasterdb$extent
#' rasterdb$geo_code
#' rasterdb$proj4
#' rasterdb$description
#' rasterdb$timestamps
#'
#' @format
#' RasterDB \link{R6Class} object.
#'
#' @section Details:
#'
#' In the Following methods of RasterDB are described. Use instance objects to call that methods (e.g. rasterdb$method(paramters) ).
#' Use instance of \link{RemoteSensing} class to create instances of RasteDB.
#'
#' \describe{
#'
#' \item{remotesensing$rasterdb(name)}{Open existing raster with given name in Remote Sensing Database. This is the only method that needs a \link{RemoteSensing} instance.
#'
#' name: Name of RasterDB.
#'
#' returns: RasterDB object}
#'
#' \item{$raster(ext, time_slice=NULL, band=NULL, product=NULL, masking='center', timestamp=NULL)}{Request raster data from opened RasterDB layer.
#' If neither band nor product are specified all bands are returned; either band or product can be specified.
#'
#' ext: object of one class:
#' \itemize{
#'  \item{\strong{raster::extent}  extent}
#'  \item{\strong{sf::st_bbox}  extent}
#'  \item{\strong{SpatialPolygons}  if masking=='center': polygon masked raster or if masking=='extent':  extent of polygon}
#'  \item{\strong{sfc_MULTIPOLYGON}  if masking=='center': polygon masked raster or if masking=='extent':  extent of polygon}
#'  \item{\strong{sfc_POLYGON}  if masking=='center': polygon masked raster or if masking=='extent':  extent of polygon}
#' }
#'
#' time_slice: (optional) Point in time, has to match an existing time_slice on this RasterDB layer.
#'
#' band: (optional) vector or single number of requested band numbers
#'
#' product: (optional) character vector of requested product specification. See section product.
#'
#' masking: (optional) if 'ext' parameter is an extent masking parameter is ignored.
#'
#' \itemize{
#'  \item{\strong{'center'} raster is masked by polygon (in 'ext' parameter) for pixels that center is within the polygon, see \link[raster]{mask}}
#'  \item{\strong{'extent'}  just extent (in 'ext' parameter) is used, no masking}
#' }
#'
#' timestamp: (optional) (obsolete, use time_slice) timestamp of requested raster data.
#' Timestamp format is in ISO 8601. Valid syntax is: YYYY-MM-DDThh:mm for exact timestamp (e.g. "2017-12-31T23:59")
#' or shortened versions (YYYY, YYYY-MM, YYYY-MM-DD, YYYY-MM-DDThh) (e.g. 2017-12).
#' When shortened version is used oldest data in layer within that timerange is returned.
#' Alternatively, when timestamp is an integer the timestamp is used as an internal timestamp-id to request data.
#'
#' returns: RasterLayer or RasterStack}
#'
#' \item{$insert_RasterLayer(r, time_slice=NULL, band=1, band_title=NULL, flush=TRUE, update_pyramid=TRUE, refresh_meta=TRUE, timestamp=NULL)}{Insert raster data from RasterLayer into RasterDB.
#'
#' If projection and/or resolution information is missing in the RasterDB layer then it is inserted form the given raster.
#'
#' r: inserted RasterLayer
#'
#' time_slice: (optional) Point in time, free text, but ISO 8601 preferred, e.g. 2021-02.
#'
#' band: (optional) integer band number, creates band if it does not exist
#'
#' band_title: (optional) title of band, only applied for newly created band
#'
#' flush: (optional) ensure that all data is persisted on disc when this call returns.
#'
#' update_pyramid: (optional) update pyramid with the new raster data.
#'
#' refresh_meta: (optional) load updated metadata in R.
#'
#' timestamp: (optional) (obsolete) integer representation of point in time, 0 represents missing timestamp. Or as specified in method 'raster, e.g. "2017-12-31T23:59".
#'
#' returns: success message}
#'
#' \item{insert_RasterStack(r, time_slice=NULL, bands=NULL, flush=TRUE, update_pyramid=TRUE, refresh_meta=TRUE, timestamp=NULL)}{Insert raster data from RasterStack into RasterDB.
#'
#' If projection and/or resolution information is missing in the RasterDB layer then it is inserted form the given raster.
#'
#' r: inserted RasterStack
#'
#' time_slice: (optional) Point in time, free text, but ISO 8601 preferred, e.g. 2021-02.
#'
#' bands: (optional) integer band numbers, creates bands if not existing
#'
#' flush: (optional) ensure that all data is persisted on disc when this call returns.
#'
#' update_pyramid: (optional) update pyramid with the new raster data.
#'
#' refresh_meta: (optional) load updated metadata in R.
#'
#' timestamp: (optional) (obsolete) integer representation of point in time, 0 represents missing timestamp. Or as specified in method 'raster, e.g. "2017-12-31T23:59".
#'
#' returns: success message}
#'
#' \item{$update_pyramid()}{(administrative method) Rebuild RasterDB internal pyramid of scaled rasters.
#'
#' Note: This method processes all data in database and it may take a long time. Only use it if really needed.
#'
#' returns: success message}
#'
#' \item{$refresh_meta()}{Reload meta data of this raster layer from RSDB server.}
#'
#' \item{$set_meta(meta)}{change meta data of this raster layer
#'
#'  meta: named list of items to change:: 'title': Tile of raster-layer, 'description', tags: 'list of raster-layer tags, acl: 'list read access roles', acl_mod: 'list modify access roles'}
#'
#' \item{$set_band_meta(bands)}{change band meta data of this raster layer
#'
#'  bands: data.frame with band numbers in column 'index' and several of following meta data columns: 'datatype' 'title' 'wavelength' 'fwhm' 'visualisation' 'vis_min' 'vis_max'}
#'
#' \item{$name}{ID of RasterDB}
#'
#' \item{$time_slices}{data.frame of existing time_slices}
#'
#' \item{$bands}{data.frame of band information (band index, wavelength, fwhm (wavelength bandwidth), title)}
#'
#' \item{$pixel_size}{size of pixels in projection coordinates}
#'
#' \item{$extent}{current full extent of this RasterDB.
#'
#' Note: Don't use this extent directly for queries on RasterDB as it may be very large.}
#'
#' \item{$geo_code}{geo code of projection. Typically this is an epsg code (e.g. "EPSG:32737")}
#'
#' \item{$proj4}{PROJ.4 of projection. (e.g. "+proj=utm +zone=37 +south +datum=WGS84 +units=m +no_defs ")}
#'
#' \item{$description}{description text)}
#'
#' \item{$timestamps}{data.frame of timestamps}
#'
#' }
#'
#' @section Product:
#'
#' Parameter product is a texual specification of raster processing request.
#'
#' Syntax:
#' \describe{
#' \item{full_spectrum}{all bands that are part of (hyperspectral)-spectrum}
#' \item{bxxx}{band with numer xxx}
#' \item{rxxx}{band with approximate wavelenght xxx nanometer}
#' \item{[a, b, c]}{multiple bands a b c}
#' \item{[bxxx:byyy]}{all bands from index xxx to yyy}
#' \item{[rxxx:ryyy]}{all bands with wavelenght from xxx to yyy nanometer}
#' \item{[rxxx:ryyy, c]}{(combined) all bands with wavelenght from xxx to yyy and band c}
#' \item{a + x}{add to pixels of band a band x (or number x)}
#' \item{a - x}{substract to pixels of band a band x (or number x)}
#' \item{a * x}{multiply to pixels of band a band x (or number x)}
#' \item{a / x}{divide to pixels of band a band x (or number x)}
#' \item{(a + x) / (b * y)}{(combined) formula}
#' \item{ndvi}{vegetation index NDVI (Normalized Difference Vegetation Index)}
#' \item{evi}{vegetation index EVI (Enhanced vegetation index)}
#' \item{evi2}{vegetation index EVI2 (Two-band EVI)}
#' \item{savi}{vegetation index SAVI (Soil-Adjusted Vegetation Index)}
#' \item{normalised_difference(g, [a, b, c])}{normalise bands a b c by band g (method normalised difference: \code{(g - a) / (g + a)} )}
#' \item{normalised_ratio(g, [a, b, c])}{normalise bands a b c by band g (method ratio: \code{a / g} )}
#' \item{pca([a, b, c]) or pca([a, b, c], 2)}{PCA transform bands a b c (and return first 2 components)}
#' \item{euclidean_distance([a, b, c])}{distance from zero over all bands (euclidean distance: sqrt(a^2 + b2^2 + c^2) )}
#' \item{black_point_compensation([a, b, c])}{substract minimum value of each band (of current extent) ==> mimimum values are tranformed to zero ( [a - mina, b - minb, c - minc] )}
#' \item{gap_filling([a, b, c]) or gap_filling([a, b, c], 10) }{interpolate NA values with source pixels of maximum distance to target pixel of second parameter}
#' }
#'
#' Examples:
#'
#' band of number 2: \code{b2}
#'
#' band of approximate wavelenght 650 nanometers: \code{r650}
#'
#' three bands: \code{[r450, r460, r470]}
#'
#' NDVI: \code{ndvi}
#'
#' NDVI by formula: \code{(r800 - r680) / (r800 + r680)}
#'
#' normalise bands of 600 to 700 nanometers by band at 800 nanometers: \code{normalised_difference(r800, [r600:r700])}
#'
#' @section RasterLayer / RasterStack Format:
#'
#' RasterLayer (one raster band) and RasterStack (multiple raster bands stored as multiple RasterLayer)
#' contain raster pixel values and additional information, that can be accessed by following commands:
#'
#' \strong{RasterStack} r:
#' \describe{
#' \item{names(r)}{character vector of band titles}
#' \item{r@source}{source layer name of database}
#' \item{r@timestamp}{timestamp of received data (if present)}
#' \item{r@product}{product formula of processing (if present)}
#' \item{crs(r) or r@crs}{coordinate reference system as PROJ4 text}
#' \item{extent(r) or r@extent}{extent of raster in projection coordinates}
#' \item{xres(r)}{x resolution per pixel in projection coordinates}
#' \item{yres(r)}{y resolution per pixel in projection coordinates}
#' \item{res(r)}{x y resolution per pixel in projection coordinates}
#' \item{nlayers(r) or length(r@layers)}{count of layers (bands)}
#' \item{ncol(r) or r@ncols}{width in pixels}
#' \item{nrow(r) or r@nrows}{height in pixels}
#' \item{ncell(r)}{count of pixels per layer (width * height)}
#' \item{length(r)}{total count of pixels (layer_count * width * height)}
#' \item{dim(r)}{get width height layer_count}
#' \item{unstack(r) or r@layers}{list of RasterLayer}
#' \item{r@layers[[1]]}{first RasterLayer}
#' \item{as.vector(r)}{pixel values of all layers as numeric vector (layer1 row by row, layer2 row by row, ...)}
#' \item{as.vector(as.array(r))}{pixel values of all layers as numeric vector (pixel index 1 (layer by layer), pixel index 2 (layer by layer), ... with pixel index row by row)}
#' \item{values(r) or as.matrix(r)}{pixel values as matrix with first: pixel index (row by row), second: layer}
#' \item{as.array(r)}{pixel values as array of three dimensions with first: pixel row, second: pixel column, third: layer}
#' }
#'
#' \strong{RasterLayer} r:
#' \describe{
#' \item{names(r) or r@data@names}{title of raster (band) e.g. for band 17 "b17"}
#' \item{r@source}{source layer name of database}
#' \item{r@timestamp}{timestamp of received data (if present)}
#' \item{r@product}{product formula of processing (if present)}
#' \item{r@index}{band number in RasterDB}
#' \item{crs(r) or r@crs}{coordinate reference system as PROJ4 text}
#' \item{extent(r) or r@extent}{extent of raster in projection coordinates}
#' \item{xres(r)}{x resolution per pixel in projection coordinates}
#' \item{yres(r)}{y resolution per pixel in projection coordinates}
#' \item{res(r)}{x y resolution per pixel in projection coordinates}
#' \item{r@wavelength}{wavelength in nanometer}
#' \item{r@fwhm}{wavelength bandwidth (full width at half maximum) nanometer}
#' \item{ncol(r) or r@ncols}{width in pixels}
#' \item{nrow(r) or r@nrows}{height in pixels}
#' \item{ncell(r) or length(r)}{count of pixels (width * height)}
#' \item{dim(r)}{get width height}
#' \item{values(r) or as.vector(r) or r@data@values}{pixel values as numeric vector (row by row)}
#' \item{as.matrix(r) or as.array(r)}{pixel values as matrix with first: pixel row, second: pixel column}
#' }
#'
#' @examples
#' # open remote sensing database
#' library(RSDB)
#' #remotesensing <- RemoteSensing$new("http://localhost:8081", "user:password") # local
#' remotesensing <- RemoteSensing$new("http://example.com:8081", "user:password") # remote server
#'
#' # open RasterDB
#' rasterdb <- remotesensing$rasterdb("kili_campaign1")
#'
#' # create extent
#' ext <- extent_dimateter(x=312062, y=9638537, d=10)
#'
#' # request raster all bands from RasterDB
#' r <- rasterdb$raster(ext)
#' plot(r)
#'
#' # request principal component analysis of bands up to wavelength 800 nm and return first 7 components.
#' r <- rasterdb$raster(ext, product="pca([r0:r800], 7)")
#' plot(r)
#'
#' @export
RasterDB <- R6::R6Class("RasterDB",
                          public = RasterDB_public,
                          active = RasterDB_active,
                          private = RasterDB_private,
                          lock_class = TRUE,
                          lock_objects = TRUE,
                          portable = FALSE,
                          class = TRUE,
                          cloneable = FALSE
)
