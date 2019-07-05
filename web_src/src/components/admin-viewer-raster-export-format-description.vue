<template>
  <v-expansion-panel>
    <v-expansion-panel-content>
      <div slot="header">format description</div>
      <hr>
      <h3 :class="{disabled: (outputType !== 'GeoTIFF - data')}">GeoTIFF - data</h3>
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
      <h3 :class="{disabled: (outputType !== 'GeoTIFF - tiled data (experimental)')}">GeoTIFF - tiled data (experimental)</h3>
      <br>
      TIFF raster file format with projection information and tiled data (experimental).
      <br>
      As this format is intended for GIS-software general image-processing<br> applications may not be able to read that files.
      <br>
      <br>
      For compatibility with TIFF readers (e.g. GDAL) all bands need to be of same data type, for output "biggest" data type is chosen. 
      <br>
      <br>
      Experimental tiled output.
      <br>
      <br>
      <hr>
      <h3 :class="{disabled: (outputType !== 'GeoTIFF - visualisation')}">GeoTIFF - visualisation</h3>
      <br>
      TIFF image file format with projection information (transparency as alpha channel, GeoTIFF)
      <br>
      Up to three bands are rendered:
      <ul>
        <li>one band: raser band is rendered to one-band greyscale image (8-bit).</li>
        <li>two bands: raster bands are rendered to three band red-green-blue image (3x8 bit) with interpolated green.</li>
        <li>three or more bands: Raster data of first three bands is rendered to three band red-green-blue image (3x8 bit).</li>
      </ul>
      <br>
      This format is intended for visualisation only.
      <br>
      Pixel values are mapped non-linearly to image range (0 - 255) 
      <br>to present hight detail in resulting image.
      <br>
      <br>
      <hr>
      <h3 :class="{disabled: (outputType !== 'PNG - visualisation')}">PNG - visualisation</h3>
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
      <h3 :class="{disabled: (outputType !== 'JPEG - visualisation')}">JPEG - visualisation</h3>
      <br>
      JPEG image file format (small file size with lossy image compression, no projection information, no transparency information)
      <br>
      Files can be displayed in web browsers.
      <br>
      <br>
      Output processing details are same as 'GeoTIFF - visualisation'.          
      <br>
      <br>          
      <hr>
      <h3 :class="{disabled: (outputType !== 'rDAT - data')}">rDAT - data</h3>
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
  name: 'admin-viewer-raster-export-format-description',

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
