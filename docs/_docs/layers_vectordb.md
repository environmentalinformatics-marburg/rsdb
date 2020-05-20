---
title: VectorDB layer (vector data)
---

Vectors are features of type **point**, **line** and **polygon** with associated **properties**.

## Upload

Supported Vector-file formats:

- [Shapefile](https://en.wikipedia.org/wiki/Shapefile) (at least: `filename.shp` `filename.shx` `filename.dbf`)
- [GeoPackage](http://www.geopackage.org/) (`filename.gpkg`)
- [GeoJSON](https://geojson.org/) (`filename.geojson`)

### 1. Create a new VectorDB layer
At the web interface - tab `LAYERS` - `overview` - Layers overview page:  
- Scroll to box `VectorDB` and click `CREATE`-button
- Type a name for the new VectorDB layer (for illustration here: `my_layer`)
- (optional) set roles to read or modify the layer
- When done, click `EXECUTE`-button

### 2. Upload vector files
At the web interface - tab `LAYERS` - `Vectordbs` - `my_layer` layer detail page:
- Click `FILES`-button
- Upload vector files (one for GeoPackage or GeoJSON, multiple for Shapefile)
- At the main file (e.g. filename.shp) click the `set anchor`-button
- When done, click `CLOSE`-button
- If vector data is correctly uploaded, at the layer detail page a visualisation of the vector data is shown.

### 3. (optional) Set name-attribute
At the web interface - tab `LAYERS` - `Vectordbs` - `my_layer` layer detail page:
- Click `ATTRIBUTES`-button
- Choose from the vector data attributes one attribute that should be used as name for vector features and click `set name attribute`-button
- When done, click `CLOSE`-button

### 4. (optional) Mark layer as POI-group and/or ROI-group
At the web interface - tab `LAYERS` - `Vectordbs` - `my_layer` layer detail page:
- Click `STRUCTURED ACCESS`-button
- Switch on/off POI-group and/or ROI-group
- When done, click `APPLY`-button: POI-group and/or ROI-group layers of same name will be created
- At tab `LAYERS` - `POI groups` or `ROI groups` - `my_layer` layer detail page: view POI/ROI can be inspected
