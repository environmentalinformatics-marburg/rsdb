# Remote Sensing Database (RSDB)
Manage remote sensing raster, point-cloud and vector data online. 

* **[RSDB homepage](https://environmentalinformatics-marburg.github.io/rsdb)**

* **[RSDB paper](https://doi.org/10.1111/ecog.05266)**

* **[Wiki](https://github.com/environmentalinformatics-marburg/rsdb/wiki)** with technical documentation, [point cloud indices details](https://github.com/environmentalinformatics-marburg/rsdb/wiki/Point-cloud-indices) and [troubleshooting](https://github.com/environmentalinformatics-marburg/rsdb/wiki/Troubleshooting)

* **[Example](https://github.com/environmentalinformatics-marburg/rsdb-data/blob/master/README.md)** with datasets, web interface and [R-script](https://github.com/environmentalinformatics-marburg/rsdb-data/blob/master/processing.md)  based processing

* **[Issues](https://github.com/environmentalinformatics-marburg/rsdb/issues)** are managed on GitHub

---------------------------------------

A ready to use RSDB distribution is located at:

[https://github.com/environmentalinformatics-marburg/rsdb-data](https://github.com/environmentalinformatics-marburg/rsdb-data)

---------------------------------------

### Install R Package

```R
if(!require("remotes")) install.packages("remotes")
remotes::install_github("environmentalinformatics-marburg/rsdb/r-package")
library(RSDB)
??RSDB
```

The RSDB R-package connects to a running RSDB server.
