library(RSDB)

#httr::set_config(httr::verbose()) # http debug messages
httr::reset_config() # no http debug messages

remotesensing <- RSDB::RemoteSensing$new(url = "http://127.0.0.1:8081", userpwd = "user:pw1", ssl_verifypeer = FALSE)

# active bindings
result_roi_groups <- remotesensing$roi_groups
result_poi_groups <- remotesensing$poi_groups
result_pointdbs <- remotesensing$pointdbs
result_rasterdbs <- remotesensing$rasterdbs
result_plointclouds <- remotesensing$plointclouds
result_vectordbs <- remotesensing$vectordbs

# public functions
result_lidar <- remotesensing$lidar(layer = result_pointdbs[1])
result_pointdb <- remotesensing$pointdb(name = result_pointdbs[1])
result_rasterdb <- remotesensing$rasterdb(name = result_rasterdbs$name[1])
result_lidar_layers <- remotesensing$lidar_layers()
#remotesensing$web() # open in web browser
result_poi_group <- remotesensing$poi_group(group_name = result_poi_groups$name[1])
result_poi <- remotesensing$poi(group_name = result_poi_groups$name[1], poi_name = result_poi_group$name[1])
result_roi_group <- remotesensing$roi_group(group_name = result_roi_groups$name[1])
result_roi <- remotesensing$roi(group_name = result_roi_groups$name[1], roi_name = result_roi_group$name[1])
result_create_rasterdb <- remotesensing$create_rasterdb(name = "test_raster")
result_pointcloud <- remotesensing$pointcloud(name = result_plointclouds$name[1])
result_vectordb <- remotesensing$vectordb(name = result_vectordbs$name[1])
