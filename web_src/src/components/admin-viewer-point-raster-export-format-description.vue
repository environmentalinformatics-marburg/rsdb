<template>
  <v-expansion-panel>
    <v-expansion-panel-content>
      <div slot="header">format description</div>
      <hr>
      <h3 :class="{disabled: (outputType.name !== 'zip')}">ZIP (experimantal)</h3>
      <br>
      ZIP file containing tiff files (described in section GeoTIFF - data)   
      <br>
      <br>Contained tiff files are tiles of 1x1km size. All tiles covering the requested extent are returned. 
      <br>Tiles are cropped by extend, so parts of tiles outside of extend are empty.
      <br>     
      <hr>      
      <hr>
      <h3 :class="{disabled: (outputType.name !== 'tiff')}">GeoTIFF - data</h3>
      <br>
      TIFF raster file format with projection information (GeoTIFF)
      <br>
      As this format is intended for GIS-software general image-processing<br> applications may not be able to read that files.
      <br>
      <br>
      For compatibility with TIFF readers (e.g. GDAL) all bands need to be of same data type, for output "biggest" data type is chosen. 
      <br>
      <br>
      Tested with: <a href="http://gdal.org/">GDAL</a>, <a href="https://qgis.org/">QGIS</a>, <a href="https://cran.r-project.org/web/packages/raster/">R raster</a>
      <br>
      <br>
      <hr>
      <h3 :class="{disabled: (outputType.name !== 'png')}">PNG - visualisation</h3>
      <br>
      PNG image file format (transparency as alpha channel, no projection information)
      <br>
      Files can be displayed in web browsers.
      <br>
      <br>
      Output processing details are same as 'GeoTIFF - visualisation'.
      <br>
      <br>
      <hr>
      <h3 :class="{disabled: (outputType.name !== 'rDAT')}">rDAT - data</h3>
      <br>
      rDAT file format is transfer format of rPointDB package. 
      <br>
      Extensive meta data is preserved by this format.
      <br>e.g. band names, wavelength
      <br>
      <br>
      For compatibility with R rasters all bands need to be of same data type. For output "biggest" data type is chosen. 
      <br>
      <br>
      example: 
      <br><i>load file in <a href="https://www.r-project.org/">R</a> as RasterLayer (one band) or as RasterStack (multiple bands) and plot it.</i>
      <br>
      <br>
      <pre>
library(rPointDB)
r &lt;- read.rdat("c:/downloads/raster.rdat")
plot(r)
      </pre>
    </v-expansion-panel-content>
  </v-expansion-panel>
</template>

<script>

export default {
  name: 'admin-viewer-point-raster-export-format-description',

  props: ['outputType'],

  components: {
  },

  data() {
    return {};
  },
  methods: {
  },
  computed: {
  },
  watch: {
  },
  mounted() {
  },
}

</script>

<style scoped>

.disabled {
  color: grey;
}

</style>
