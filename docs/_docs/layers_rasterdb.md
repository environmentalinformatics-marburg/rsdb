---
title: RasterDB layer (raster data)
---

A raster is a grid of equal size **pixels** over several **bands** at multiple **timestamps**.  
Pixels have a x-size and y-size (resolution) which is constant over all bands and timestamps.  
Each band may have an individual value type, currently integer or float.

## Upload

Supported Raster-file formats:

- [GeoTIFF](https://www.ogc.org/standards/geotiff) (`filename.geotiff` or `filename.tiff` or `filename.tif`)
- [GDAL compatible](https://gdal.org/drivers/raster/index.html) (several file formats`)

### 1. Upload raster file
At the web interface - tab `LAYERS` - `overview` - Layers overview page:  
- Scroll to box `RasterDB` and click `UPLOAD`-button
- Choose a raster file and upload it

### 2. Selected import strategy
After raster file upload the view changes to a strategy selection view.  
Select strategy:
- `create new layer` The raster data is inserted in a new RasterDB layer
- `insert into existing layer with new (added) bands` The raster data is inserted in an existing RasterDB layer but in newly additionally created bands of that layer.
- `insert into existing layer with (preferably) into existing bands` The raster data is inserted in an existing RasterDB layer in existing bands of that layer.

If not new layer select an exising raster layer id.

When done, click `INSPECT`-button.

### 3. Import specification
After click at `INSPECT`-button, a form with partly prefilled properties is shown.

Add and/or correct properties for the imported raster data.

When done, scroll down and click `IMPORT`-button.

### 4. Import finished
Wait until 'Import done.' is shown.

Two links are shown to inspect the impoerted layer:
- Click on link at 'view meta data' to open the layer details page
- Click on link at 'view layer' to open the interactive map of that layer.