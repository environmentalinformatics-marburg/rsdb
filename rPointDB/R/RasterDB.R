RasterDB_public <- list( #      *********** public *********************************

  initialize = function(base_url, name, curlHandle) {
    private$base_url <- base_url
    private$name_ <- name
    private$curlHandle <- curlHandle
    private$url <- paste0(private$base_url, "/rasterdb/", private$name_)
    if(!RCurl::url.exists(paste0(private$url, "/meta.json"), curl = RCurl::dupCurlHandle(private$curlHandle))) {
      stop("no connection to RasterDB: ",private$db_url)
    }
    private$meta <- query_json(private$url, "meta.json", curl = RCurl::dupCurlHandle(private$curlHandle))
  },

  raster = function(ext, band=NULL, timestamp=NULL, product=NULL) {
    extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
    param_list <- c(ext=extText, timestamp=timestamp)
    if(!is.null(band)) {
      bandText <- paste(as.integer(band), collapse=" ")
      param_list <- c(param_list, band=bandText)
    }
    if(!is.null(timestamp)) {
      param_list <- c(param_list, timestamp=timestamp)
    }
    if(!is.null(product)) {
      param_list <- c(param_list, product=product)
    }
    r <- query_RDAT(private$url, "raster.rdat", param_list, curl = RCurl::dupCurlHandle(private$curlHandle))
    return(r)
  },

  insert_RasterLayer = function(r, band=1, timestamp=0) {
    stopifnot(is(r, "RasterLayer"))
    ext <- r@extent
    extText <- paste(ext@xmin, ext@ymin, ext@xmax, ext@ymax, sep=" ")
    param_list <- c(width=r@ncols, height=r@nrows, timestamp=timestamp, band=band, ext=extText, proj4=r@crs@projargs, flip_y=TRUE)
    data <- r@data@values
    raw <- writeBin(object=data, con=raw(0), size=2, endian="little")
    result <- post_raw_get_json(raw=raw, api_url=private$url, method="insert_raster", param_list=param_list, curl=RCurl::dupCurlHandle(private$curlHandle)) #raw not send if with curlHandle
    return(result)
  },

  insert_RasterStack = function(r, bands=NULL, timestamp=0) {
    stopifnot(is(r, "RasterStack"))
    len <- length(r@layers)
    if(is.null(bands)) {
      bands <- c(1:len)
    }
    stopifnot(len == length(bands))
    result <- lapply(c(1:len), function(i) {
      layer <- r@layers[[i]]
      band <- bands[[i]]
      res <- self$insert_RasterLayer(layer, band, timestamp)
      return(res)
    })
    return(result)
  },

  rebuild_pyramid = function() {
    result <- query_json(private$url, "rebuild_pyramid", curl = RCurl::dupCurlHandle(private$curlHandle))
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

  timestamps = function() {
    return(private$meta$timestamps)
  },

  pixel_size = function() {
    return(private$meta$ref$pixel_size)
  },

  extent = function() {
    return(raster::extent(private$meta$ref$extent))
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
  meta = NULL

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
#' raster_stack <- rasterdb$raster(ext, band=NULL, timestamp=NULL, product=NULL)
#' rasterdb$insert_RasterLayer(r, band=1, timestamp=0)
#' rasterdb$insert_RasterStack(r, bands=NULL, timestamp=0)
#' rasterdb$rebuild_pyramid()
#'
#' rasterdb$name
#' rasterdb$bands
#' rasterdb$timestamps
#' rasterdb$pixel_size
#' rasterdb$extent
#' rasterdb$geo_code
#' rasterdb$proj4
#' rasterdb$description
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
#' \item{$raster(ext, band=NULL, timestamp=NULL, product=NULL)}{Request raster data from opened RasterDB.
#' If neither band nor product are specified all bands are returned. Either band or product can be specified.
#'
#' band: vector or single number of requested band numbers
#'
#' timestamp: request raster of time. If not specified newest data is returned.
#' Timestamp format is part of ISO 8601. Valid syntax is: YYYY-MM-DDThh:mm for exact timestamp (e.g. "2017-12-31T23:59")
#' or shortened versions (YYYY, YYYY-MM, YYYY-MM-DD, YYYY-MM-DDThh) (e.g. 2017-12).
#' When shortened version is used oldest data in layer within that timerange is returned.
#'
#' product: character vector of requested product specification. See section product.
#'
#' returns: RasterLayer or RasterStack}
#'
#' \item{$insert_RasterLayer(r, band=1, timestamp=0)}{Insert raster data into RasterDB.
#'
#' returns: success message}
#'
#' \item{$insert_RasterStack(r, bands=NULL, timestamp=0)}{Insert raster data into RasterDB.
#'
#' returns: success message}
#'
#' \item{$rebuild_pyramid()}{(administrative method) Rebuild RasterDB internal pyramid of scaled rasters.
#'
#' Note: This method processes all data in database and it may take a long time. Only use it if really needed.
#'
#' returns: success message}
#'
#' \item{$name}{name of RasterDB}
#'
#' \item{$bands}{data.frame of band information (band index, wavelength, fwhm (wavelength bandwidth), title)}
#'
#' \item{$timestamps}{data.frame of timestamps}
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
#' }
#'
#' @section Product:
#'
#' Parameter product is a texuel specification of raster processing request.
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
#' library(rPointDB)
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
