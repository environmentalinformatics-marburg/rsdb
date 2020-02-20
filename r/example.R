# open remote sensing database
library(RSDB)
remotesensing <- RemoteSensing$new("http://example.com:8081", 
                                   "user:password")

# get names of PointDBs
remotesensing$pointdbs

# get one pointdb
pointdb <- remotesensing$pointdb("kili")

# get names of ROI groups
remotesensing$roi_groups

# get one ROI group
rois <- remotesensing$roi_group("kili_A")

# get one ROI
roi <- remotesensing$roi(group_name="kili_A", 
                         roi_name="cof3_A")

#get data.frame of LiDAR points at polygon ROI
df2 <- pointdb$query_polygon(roi$polygon[[1]])