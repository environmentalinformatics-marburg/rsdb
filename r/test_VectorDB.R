library(RSDB)

#httr::set_config(httr::verbose()) # http debug messages
httr::reset_config() # no http debug messages

remotesensing <- RSDB::RemoteSensing$new(url = "http://127.0.0.1:8081", userpwd = "user:pw1", ssl_verifypeer = FALSE)
vectordbs <- remotesensing$vectordbs
vectordb <- remotesensing$vectordb(name = vectordbs$name[1])

# active bindings
result_attributes <- vectordb$attributes
result_description <- vectordb$description
result_epsg <- vectordb$epsg
result_meta <- vectordb$meta
result_name <- vectordb$name
result_name_attribute <- vectordb$name_attribute
result_proj4 <- vectordb$proj4
result_title <- vectordb$title

# public functions
result_getVectors <- vectordb$getVectors()
result_getVectorsWGS84 <- vectordb$getVectorsWGS84()
