library(rPointDB)

#httr::set_config(httr::verbose()) # http debug messages
httr::reset_config() # no http debug messages

remotesensing <- rPointDB::RemoteSensing$new(url = "http://127.0.0.1:8081", userpwd = "user:pw1", ssl_verifypeer = FALSE)
pointdbs <- remotesensing$pointdbs
pointdb <- remotesensing$pointdb(name = pointdbs[1])

ext <- rPointDB::extent_radius(100,200, 10)
poly <- Polygon(matrix(c(1,1,1,2,2,2,2,1,1,1), ncol = 2, byrow = TRUE))

# active bindings
result_info <- pointdb$info
result_processing_functions <- pointdb$processing_functions
result_roi_groups <- pointdb$roi_groups
result_poi_groups <- pointdb$poi_groups
result_raster_processing_types <- pointdb$raster_processing_types

# public functions
result_process <- pointdb$process(areas = list(a=ext, ä=ext), functions = list("count","max"))
result_query <- pointdb$query(ext = ext)
result_query_polygon <- pointdb$query_polygon(polygon = poly)
result_query_raster <- pointdb$query_raster(ext = ext, type = "DTM")

