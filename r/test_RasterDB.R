library(rPointDB)

#httr::set_config(httr::verbose()) # http debug messages
httr::reset_config() # no http debug messages

remotesensing <- rPointDB::RemoteSensing$new(url = "http://127.0.0.1:8081", userpwd = "user:pw1", ssl_verifypeer = FALSE)
rasterdbs <- remotesensing$rasterdbs
rasterdb <- remotesensing$rasterdb(name = rasterdbs$name[1])

ext <- rPointDB::extent_radius(100,200, 10)

# active bindings
result_name <- rasterdb$name
result_bands <- rasterdb$bands
result_timestamps <- rasterdb$timestamps
result_pixel_size <- rasterdb$pixel_size
result_extent <- rasterdb$extent
result_geo_code <- rasterdb$geo_code
result_proj4 <- rasterdb$proj4
result_description <- rasterdb$description

# public functions
result_raster <- rasterdb$raster(ext = ext)
result_insert_RasterStack <- rasterdb$insert_RasterStack(result_raster)
result_rebuild_pyramid <- rasterdb$rebuild_pyramid()
