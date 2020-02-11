read_RDAT_RASTER <- function(con) {
  meta <- read_RDAT_LIST(con)
  type <- readBin(con, "int", size=1)
  type_size <- readBin(con, "int", size=1)
  nlayers <- readBin(con, "int", size=4, endian="big")
  nrows <- readBin(con, "int", size=4, endian="big")
  ncols <- readBin(con, "int", size=4, endian="big")
  len <- nrows*ncols
  if(nlayers==1) {
    r <- read_RDAT_RASTER_layer(con, type, type_size, len, nrows, ncols, meta)
    return(r)
  } else {
    #st <- stack()
    #for(i in 1:nlayers) {
      ##print(i)
      ##t0 <- Sys.time()
      #r <- read_RDAT_RASTER_layer(con, type, type_size, len, nrows, ncols, meta)
      ##t1 <- Sys.time()
      #st <- addLayer(st, r)
      ##t2 <- Sys.time()
      ##print(c(t1-t0, t2-t1))
    #}
    layers <- c(1:nlayers)
    rasterList <- lapply(layers, function(i){
      r <- read_RDAT_RASTER_layer(con, type, type_size, len, nrows, ncols, meta)
      return(r);
    })
    st <- stack(rasterList)
    for (name in names(meta)) {
      if("name"!=name) {
        attr(st, name) <- meta[[name]]
      }
    }
    return(st)
  }
}

read_RDAT_RASTER_layer <- function(con, type, type_size, len, nrows, ncols, meta) {
  layerMeta <- read_RDAT_LIST(con)
  data <- read_RDAT_VECT_raw(con, type, type_size, len)
  if(!is.null(meta$nodatavalue)) { #set NA values
    if(meta$nodatavalue==0) {
      #message("set 0 to NA")
      data[data==0] <- NA   # set zero to NA
    } else {
      warning("ignore unknown nodatavalue: "+meta$nodatavalue)
    }
  }
  r <- raster::raster(nrows=nrows, ncols=ncols, xmn=meta$xmn, ymn=meta$ymn, xmx=meta$xmx, ymx=meta$ymx, crs=meta$proj4)
  values(r) <- data
  if((is.null(layerMeta$flipped) || layerMeta$flipped != 1)) {
    r <- flip(r, direction='y') # mirror raster  !! flip removes attributes !!
  }
  for (name in names(meta)) {
    if("name"!=name) {
      attr(r, name) <- meta[[name]]
    }
  }
  names(r) <- layerMeta$name
  for (name in names(layerMeta)) {
    if("name"!=name) {
      attr(r, name) <- layerMeta[[name]]
    }
  }
  return(r)
}
