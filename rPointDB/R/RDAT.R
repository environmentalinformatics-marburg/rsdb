readText <- function(con) {
  byte_count <- readBin(con, "int", size=1, signed=FALSE)
  text <- readChar(con, byte_count, useBytes=TRUE)
  return(text)
}

readTextVec_raw <- function(con, len) {
  v <- c()
  if(len > 0) {
    for(i in 1:len) {
      text <- readText(con)
      v[i] <- text
    }
  }
  return(v)
}

query_RDAT <- function(api_url, method, param_list=NULL, curlHandle=getCurlHandle()) {
  query_url <- create_query_url(api_url, method, param_list)
  cat(paste("QUERY ", query_url))
  data <- query_url_RDAT(query_url, curlHandle)
  return(data)
}

query_url_RDAT <- function(url, curlHandle=getCurlHandle()) {
  h  <- RCurl::basicTextGatherer()
  rawData <- RCurl::getBinaryURL(url, failonerror = FALSE, .opts = list(headerfunction = h$update), curl = RCurl::dupCurlHandle(curlHandle))
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

#' @export
read_RDAT <- function(con) {
  signature <- readChar(con,4)
  if(signature!="RDAT") {
    stop("no RDAT file: ", signature)
  }
  dataType <- readText(con)
  data <- NULL
  switch(dataType,
         RASTER = {data <- read_RDAT_RASTER(con)},
         DATA_FRAME = {data <- read_RDAT_DATA_FRAME(con)},
         POINT_DATA_FRAME = {data <- read_RDAT_DATA_FRAME(con)},
         stop("unknown RDAT type: ", dataType)
  )
  return(data)
}

#' Read rDat-file
#'
#' Reads rPointDB-package transfer format into R object.
#'
#' If file contains raster data it is returned as RasterLayer or RasterStack.
#'
#' @author woellauer
#' @export
read.rdat <- function(filename) {
  con <- file(description=filename, open="rb", raw=TRUE)
  data <- read_RDAT(con)
  close(con)
  return(data)
}

read_RDAT_LIST <- function(con, withSignature=TRUE) {
  if(withSignature) {
    signature <- readChar(con,4)
    if(signature!="LIST") {
      stop("no LIST: ", signature)
    }
  }

  entry_count <- readBin(con, "int", size=1)
  entries <- list()
  if(entry_count > 0) {
    for(i in 1:entry_count) {
      name <- readText(con)
      type <- readBin(con, "int", size=1)
      type_size <- readBin(con, "int", size=1)
      value <- NULL
      switch(as.character(type),
             "1" = {value <- readBin(con, "int", size=type_size, endian="big")},  # TYPE_SIGNED_INT
             "2" = {value <- readBin(con, "int", size=type_size, signed=FALSE, endian="big")}, # TYPE_UNSIGNED_INT
             "3" = {value <- readBin(con, "double", size=type_size, endian="big")}, # TYPE_FLOAT
             "4" = {value <- readChar(con, type_size, useBytes=TRUE)}, # TYPE_STRING
             "5" = {value <- read_basic_object(con)}, # TYPE_BASIC_OBJECT
             "6" = {value <- readBin(con, "logical", size=type_size)},  # TYPE_LOGICAL
             stop("unknown RDAT_LIST element type: ", type)
      )
      entries[name] <- value
    }
  }
  #print(entries)
  return(entries)
}

read_basic_object <- function(con) {
  signature <- readChar(con,4)
  data <- NULL
  switch(signature,
         LIST = {data <- read_RDAT_LIST(con, FALSE)},
         stop("unknown RDAT signature: ", signature)
  )
  return(data)
}


read_RDAT_VECT <- function(con) {
  signature <- readChar(con,4)
  if(signature!="VECT") {
    stop("no VECT: ", signature)
  }
  type <- readBin(con, "int", size=1)
  type_size <- readBin(con, "int", size=1)
  len <- readBin(con, "int", size=4, endian="big")
  data <- read_RDAT_VECT_raw(con, type, type_size, len)
  return(data)
}

read_RDAT_VECT_raw <- function(con, type, type_size, len) {
  #cat(paste("read_RDAT_VECT_raw type=", type, " type_size=", type_size, " len=", len, "\n"))
  values <- NULL
  switch(as.character(type),
         "1" = {values <- readBin(con, "int", size=type_size, endian="big", n=len)},  # TYPE_SIGNED_INT
         "2" = {values <- readBin(con, "int", size=type_size, signed=FALSE, endian="big", n=len)}, # TYPE_UNSIGNED_INT
         "3" = {values <- readBin(con, "double", size=type_size, endian="big", n=len)}, # TYPE_FLOAT
         "4" = {values <- readTextVec_raw(con, len)}, # TYPE_STRING
         # 5 TYPE_BASIC_OBJECT
         "6" = {values <- readBin(con, "logical", size=type_size, n=len)},  # TYPE_LOGICAL
         stop("unknown RDAT_VECT element type: ", type)
  )
  return(values)
}

read_RDAT_MTRX <- function(con) {
  signature <- readChar(con,4)
  if(signature!="MTRX") {
    stop("no MTRX: ", signature)
  }
  type <- readBin(con, "int", size=1)
  type_size <- readBin(con, "int", size=1)
  nrow <- readBin(con, "int", size=4, endian="big")
  ncol <- readBin(con, "int", size=4, endian="big")
  data <- read_RDAT_MTRX_raw(con, type, type_size, nrow, ncol)
  return(data)
}

read_RDAT_MTRX_raw <- function(con, type, type_size, nrow, ncol) { #data as row matrix
  len <- nrow*ncol
  data <- read_RDAT_VECT_raw(con, type, type_size, len)
  m <- matrix(data, nrow=nrow, ncol=ncol, byrow=TRUE)
  return(m)
}

read_RDAT_DTFM <- function(con) {
  signature <- readChar(con,4)
  if(signature!="DTFM") {
    stop("no DTFM: ", signature)
  }
  nrow <- readBin(con, "int", size=4, endian="big")
  ncol <- readBin(con, "int", size=4, endian="big")
  dataList <- list()
  if(ncol > 0) {
    for(i in 1:ncol) {
      colName <- readText(con)
      colType <- readBin(con, "int", size=1)
      colType_size <- readBin(con, "int", size=1)
      colValues <- read_RDAT_VECT_raw(con, colType, colType_size, nrow)
      dataList[[colName]] <- colValues
    }
  }
  df <- as.data.frame(dataList)
  return(df)
}

read_RDAT_DATA_FRAME <- function(con) {
  meta <- read_RDAT_LIST(con)
  df <- read_RDAT_DTFM(con)
  for(name in names(meta)) {
    attr(df, name) <- meta[[name]]
  }
  return(df)
}
