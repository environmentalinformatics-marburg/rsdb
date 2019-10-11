library(viridisLite)

col <- viridisLite::cividis(n = 256)

m <- grDevices::col2rgb(col)

paste0(m["red",], collapse = ',')
paste0(m["green",], collapse = ',')
paste0(m["blue",], collapse = ',')
