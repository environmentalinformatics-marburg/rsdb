#' create extent by position and radius
#'
#' Creates an \link{extent} that covers the square over the circle at point x,y with radius r.
#'
#' Extents are used to query data from \link{RasterDB} and from \link{PointDB}.
#'
#' @param x first coordinate
#' @param y second coordinate
#' @param r radius
#' @return extent
#' @author woellauer
#' @seealso \link{extent}
#' @export
extent_radius <- function(x, y, r) {
  return(extent(x-r,x+r,y-r,y+r))
}

#' create extent by position and edge length.
#'
#' Creates an \link{extent} that covers the square with center point x,y and with diameter (edge length) d.
#'
#' Extents are used to query data from \link{RasterDB} and from \link{PointDB}.
#'
#' @param x first coordinate
#' @param y second coordinate
#' @param d diameter (edge length)
#' @return extent
#' @author woellauer
#' @seealso \link{extent}
#' @export
extent_diameter <- function(x, y, d) {
  xmin <- x - d/2
  ymin <- y - d/2
  xmax <- xmin + d
  ymax <- ymin + d
  return(extent(xmin, xmax, ymin, ymax))
}

#' Convert ROI polygon to rectangular extent
#'
#' @param roi a roi returned by database
#' @return extent
#' @author woellauer
#' @seealso \link{extent}
#' @export
roi_to_extent <- function(roi) {
  poly <- roi$polygon[[1]]
  x <- poly[,1]
  y <- poly[,2]
  ext <- extent(x=min(x), xmax=max(x), ymin=min(y), ymax=max(y))
  return(ext)
}

createPolygonParameter <- function(polygon) { # polygon is matrix one row per point an two columns (x and y values)
  if(is(polygon, "Polygons")) {
    polygon <- convert_Polygons_to_matrix(polygon)
  }
  if(is(polygon, "Polygon")) {
    polygon <- convert_Polygon_to_matrix(polygon)
  }
  stopifnot(is.matrix(polygon))
  stopifnot(ncol(polygon) == 2)
  stopifnot(nrow(polygon) >= 3)
  pRowsText <- apply(polygon, MARGIN=1, function(row){paste(row, collapse="_")})
  pText <- paste(pRowsText, collapse="__")
  return(pText)
}

toCharacter <- function(value) {
  return(format(value, scientific = FALSE, nsmall=3, trim=TRUE, drop0trailing=TRUE))
}

extToText <- function(ext) {
  if(!is(ext, "Extent")) {
    stop("parameter ext needs to be of type 'Extent'")
  }
  extText <- paste(ext@xmin, ext@xmax, ext@ymin, ext@ymax, sep=",")
  return(extText)
}

#' Interactively visualise RasterLayer in 3d.
#'
#' Visualises a RasterLayer (Matrix) as interactive 3d surface with cell values as elevation.
#'
#' @param rasterLayer RasterLayer received by e.g. pointdb$query_raster(...)
#' @author woellauer
#' @export
visualise_raster <- function(rasterLayer) {
  stopifnot(is(rasterLayer, "RasterLayer"))
  y <- values(rasterLayer) * 1
  x <- 1 * (1:nrow(rasterLayer))   # spacing (S to N)
  z <- 1 * (1:ncol(rasterLayer))   # spacing (E to W)
  ylim <- range(y, na.rm=TRUE)
  ylen <- ylim[2] - ylim[1] + 1
  colorlut <- terrain.colors(ylen) # height color lookup table
  col <- colorlut[ y - ylim[1] + 1 ] # assign colors to heights for each point
  rgl::rgl.surface(x=x, y=y, z=z, color = col, back = "lines")
}

create_query_url <- function(api_url, method, param_list=NULL) {
  method_url <- paste(api_url,method,sep="/")
  if(is.null(param_list)) {
    return(method_url)
  } else {
    pl <- param_list[param_list!=""]
    pe <- lapply(pl, function(e) { return(URLencode(e, reserved=TRUE))})
    param_pair_list <- paste(names(pe), pe, sep="=")
    param_text <- paste(param_pair_list,collapse="&")
    query_url <- paste(method_url,param_text,sep="?")
    return(query_url)
  }
}

query_json <- function(api_url, method, param_list=NULL, curlHandle=RCurl::getCurlHandle()) {
  query_url <- create_query_url(api_url, method, param_list)
  cat(paste("QUERY ", query_url), '\n')
  h  <- RCurl::basicTextGatherer()
  rawData <- RCurl::getBinaryURL(query_url, failonerror = FALSE, .opts = list(headerfunction = h$update), curl = RCurl::dupCurlHandle(curlHandle))
  header <- RCurl::parseHTTPHeader(h$value())
  if(header[["status"]] != "200") {
    text <- readChar(rawData,nchars=length(rawData),useBytes=TRUE)
    stop(text)
  }
  l <- jsonlite::fromJSON(rawToChar(rawData))
  return(l)
}

#' @export
post_raw_get_json <- function(raw, api_url, method, param_list=NULL, curl=RCurl::getCurlHandle()) {
  query_url <- create_query_url(api_url, method, param_list)
  cat(paste("POST ", query_url), "\n")
  h  <- RCurl::basicTextGatherer()
  rawData <- RCurl::getBinaryURL(query_url, failonerror = FALSE, .opts = list(headerfunction = h$update), curl = curl, customrequest = "POST", infilesize = length(raw), upload = TRUE, readfunction = raw)
  header <- RCurl::parseHTTPHeader(h$value())
  if(header[["status"]] != "200") {
    text <- readChar(rawData,nchars=length(rawData),useBytes=TRUE)
    stop(text)
  }
  l <- jsonlite::fromJSON(rawToChar(rawData))
  return(l)
}

#' @export
post_json_get_json <- function(data, api_url, method, param_list=NULL, curl=RCurl::getCurlHandle()) {
  raw <- charToRaw(jsonlite::toJSON(data, auto_unbox=TRUE))
  query_url <- create_query_url(api_url, method, param_list)
  cat(paste("POST ", query_url), "\n")
  h  <- RCurl::basicTextGatherer()
  rawData <- RCurl::getBinaryURL(query_url, failonerror = FALSE, .opts = list(headerfunction = h$update), curl = curl, customrequest = "POST", infilesize = length(raw), upload = TRUE, readfunction = raw)
  header <- RCurl::parseHTTPHeader(h$value())
  if(header[["status"]] != "200") {
    text <- readChar(rawData,nchars=length(rawData),useBytes=TRUE)
    stop(text)
  }
  l <- jsonlite::fromJSON(rawToChar(rawData))
  return(l)
}

#' @export
post_json_get_rdat <- function(data, api_url, method, param_list=NULL, curl=RCurl::getCurlHandle()) {
  raw <- charToRaw(jsonlite::toJSON(data, auto_unbox=TRUE))
  query_url <- create_query_url(api_url, method, param_list)
  cat(paste("POST ", query_url), "\n")
  h  <- RCurl::basicTextGatherer()
  rawData <- RCurl::getBinaryURL(query_url, failonerror = FALSE, .opts = list(headerfunction = h$update), curl = curl, customrequest = "POST", infilesize = length(raw), upload = TRUE, readfunction = raw)
  header <- RCurl::parseHTTPHeader(h$value())
  if(header[["status"]] != "200") {
    text <- readChar(rawData,nchars=length(rawData),useBytes=TRUE)
    stop(text)
  }
  con <- rawConnection(rawData)
  data <- read_RDAT(con)
  close(con)
  return(data)
}

isUnauthorized <- function(url) { # checks if url needs auth then true else false, if url not exists returns false
  responseHeader <- RCurl::url.exists(url, .header = TRUE)
  if(is.na(responseHeader["status"])) {
    return(FALSE) # no response
  } else {
    return(responseHeader["status"] == 401)
  }
}

#' RasterStack to Speclib conversion
#'
#' Convert RasterStack (received from RasterDB) to \link{speclib} for usage of functionality in \link{hsdar-package}.
#'
#' For each contained RasterLayer attributes wavelength and fwhm need to be present (e.g. RasterStack created by \link{RasterDB}).
#'
#' Speclib can be converted back to Raster(Brick) by \link{HyperSpecRaster} ( e.g. r <- HyperSpecRaster(sp) ).
#'
#' @seealso \code{\link{speclib}} \link{hsdar-package}
#' @author woellauer
#' @export
as.speclib <- function(rasterStack, na=NULL) {
  stopifnot(is(rasterStack, "RasterStack"))
  m <- values(rasterStack)
  if(!is.null(na)) {
    m[is.na(m)] <- na   # set NA to na (e.g. zero), needed for derivative.speclib
  }
  wavelengths <- vapply(rasterStack@layers, function(layer) layer@wavelength, numeric(1))
  fwhms <- vapply(rasterStack@layers, function(layer) layer@fwhm, numeric(1))
  meta <- rastermeta(rasterStack, crs=crs(rasterStack))
  sp <- speclib(spectra=m, wavelength=wavelengths, fwhm=fwhms, rastermeta=meta)
  return(sp)
}

#' data.frame of points in this package to LAS object of lidR package conversion.
#'
#' Convert data.frame of points (received from PointDB or PointCloud) to \link[lidR]{LAS} in lidR package.
#'
#' @seealso \link[lidR]{LAS}
#' @author woellauer
#' @export
as.LAS <- function(df) {
  stopifnot(is.data.frame(df))
  cn <- colnames(df)
  stopifnot("x" %in% cn && "y" %in% cn)
  dt <- data.table::data.table(X=df$x, Y=df$y)
  if("z" %in% cn) dt$Z <- df$z
  if("intensity" %in% cn) dt$Intensity <- df$intensity
  if("returnNumber" %in% cn) dt$ReturnNumber <- df$returnNumber
  if("returns" %in% cn) dt$NumberOfReturns <- df$returns
  if("scanAngleRank" %in% cn) dt$ScanAngle <- df$scanAngleRank
  if("classification" %in% cn) dt$Classification <- df$classification

  header <- list("X offset"=0, "Y offset"=0, "Z offset"=0, "X scale factor"=1, "Y scale factor"=1, "Z scale factor"=1)

  result <- lidR::LAS(data=dt, header=header, check=TRUE)
  return(result)
}

#' Convert polygon of class Polygon to coordinate matrix.
#'
#' (mostly for internal use) Preperation of polygon data for PointDB processing function.
#'
#' @seealso \link{PointDB} \link{convert_Polygons_to_matrix} \link{convert_SpatialPolygons_to_named_matrix_list} \link{convert_SpatialPolygonsDataFrame_to_named_matrix_list}
#' @author woellauer
#' @export
convert_Polygon_to_matrix <- function(plgn) {
  stopifnot(is(plgn, "Polygon"))
  if(plgn@hole) {
    stop("Polygon is hole. ", plgn)
  }
  return(plgn@coords)
}

#' Convert polygon of class Polygons to coordinate matrixs.
#'
#' Note: one object of class Polygons contains just one polygon.
#'
#' Only first ring of each polygon is converted.
#'
#' (mostly for internal use) Preperation of polygon data for PointDB processing function.
#'
#' @seealso \link{PointDB} \link{convert_Polygon_to_matrix} \link{convert_SpatialPolygons_to_named_matrix_list} \link{convert_SpatialPolygonsDataFrame_to_named_matrix_list}
#' @author woellauer
#' @export
convert_Polygons_to_matrix <- function(plgns) {
  stopifnot(is(plgns, "Polygons"))
  poly <- plgns@Polygons
  len <- length(poly)
  if(len < 1) {
    stop("no polygon in ID ", plgns@ID)
  }
  if(len > 1) {
    warning("One plygon needs to be build of one ring, but there are: ", len, " rings. ID: ", plgns@ID, ". Using first ring only.")
  }
  p <- poly[[1]]
  if(p@hole) {
    stop("Polygon is hole. ID ", plgns@ID)
  }
  return(p@coords)
}

#' Convert polygons of SpatialPolygons to named list of coordinate matrices.
#'
#' Only first ring of each polygon is converted.
#'
#' (mostly for internal use) Preperation of polygon data for PointDB processing function.
#'
#' @seealso \link{PointDB} \link{convert_Polygon_to_matrix} \link{convert_Polygons_to_matrix} \link{convert_SpatialPolygonsDataFrame_to_named_matrix_list}
#' @author woellauer
#' @export
convert_SpatialPolygons_to_named_matrix_list <- function(areas) {
  stopifnot(is(areas, "SpatialPolygons") || is(areas, "SpatialPolygonsDataFrame"))
  polys <- areas@polygons
  result <- lapply(polys, convert_Polygons_to_matrix)
  ids <- lapply(polys, function(poly) {return(poly@ID)})
  names(result) <- ids
  return(result)
}

#' Convert polygons of SpatialPolygonsDataFrame to named list of coordinate matrices.
#'
#' Only first ring of each polygon is converted.
#'
#' (mostly for internal use) Preperation of polygon data for PointDB processing function.
#'
#' @seealso \link{PointDB} \link{convert_Polygon_to_matrix} \link{convert_Polygons_to_matrix} \link{convert_SpatialPolygons_to_named_matrix_list}
#' @author woellauer
#' @export
convert_SpatialPolygonsDataFrame_to_named_matrix_list <- function(areas) {
  stopifnot(is(areas, "SpatialPolygons") || is(areas, "SpatialPolygonsDataFrame"))
  return(convert_SpatialPolygons_to_named_matrix_list(areas))
}
