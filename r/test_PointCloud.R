library(rPointDB)

#httr::set_config(httr::verbose()) # http debug messages
httr::reset_config() # no http debug messages

remotesensing <- rPointDB::RemoteSensing$new(url = "http://127.0.0.1:8081", userpwd = "user:pw1", ssl_verifypeer = FALSE)
plointclouds <- remotesensing$plointclouds
pointcloud <- remotesensing$pointcloud(name = plointclouds$name[1])

ext <- rPointDB::extent_radius(100,200, 10)

# active bindings
result_name <- pointcloud$name
result_meta <- pointcloud$meta
result_geocode <- pointcloud$geocode
result_proj4 <- pointcloud$proj4
result_result_extent <- pointcloud$extent
result_description <- pointcloud$description
result_raster_list <- pointcloud$raster_list
result_attribute_list <- pointcloud$attribute_list
result_index_list <- pointcloud$index_list

# public functions
result_points <- pointcloud$points(ext = ext)
result_raster <- pointcloud$raster(ext = ext)
result_volume <- pointcloud$volume(ext = ext)
result_indices <- pointcloud$indices(areas = list(a=ext, ä=ext), functions = list("count","max"), omit_empty_areas = FALSE)