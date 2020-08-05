<template>
<div>
  <div class="raster-export-container">
    <h1>Export Raster Data <v-btn flat icon color="indigo" @click="$emit('close')"  title="close box" style="maring: 0px;"><v-icon>close</v-icon></v-btn></h1>
    <div style="display: inline-block; white-space: nowrap; padding-top: 20px;" v-if="selectedExtent === undefined">
      <h3><b style="color: red;">No extent selected.</b></h3>
      <br>
      <br>To Export raster data you need to first select an extent on the map.
      <br><v-btn @click="$emit('close')"  title="close box" style="maring: 0px;"><v-icon>close</v-icon>Close</v-btn> export dialog and select extent:
      <br>Set mouse-modus to 'select' and create extent by pressing left mouse button and moving mouse.
      <br>
      <br>
      <br><v-btn @click="$emit('select-full-extent')"  title="select all raster pixels in current layer" style="maring: 0px;"><v-icon>select_all</v-icon>select full extent</v-btn> <b>Note: </b> If the layer is large selecting full extent may not be optimal.
    </div>
    <div style="display: inline-block; white-space: nowrap; padding-top: 20px;" v-if="selectedExtent !== undefined">
      <h3>selected extent</h3>
      <div>
        <v-text-field v-model="user_xmin" label="xmin" box style="width: 150px;display: inline-block;" />
        ,
        <v-text-field v-model="user_ymin" label="ymin" box style="width: 150px;display: inline-block;" />
        -
        <v-text-field v-model="user_xmax" label="xmax" box style="width: 150px;display: inline-block;" />
        ,
        <v-text-field v-model="user_ymax" label="ymax" box style="width: 150px;display: inline-block;" />
        &nbsp;&nbsp;&nbsp;&nbsp;
        <b>size</b> (coordinate units) 
      {{user_xmax - user_xmin}} x {{user_ymax - user_ymin}} = {{(user_xmax - user_xmin) * (user_ymax - user_ymin)}}
      </div>
      
      <b>Scale</b>
      <br>
      <multiselect v-model="scaleDiv" :options="scaleDivs" :show-labels="false" :allowEmpty="false" placeholder="scale of source raster to output">
        <template slot="option" slot-scope="{option}">
           1:{{option}} -  
          <span v-if="option === 1"><b>original size</b></span>
          <span v-if="option > 1"><b>scaled down raster</b> - <i>smaller file size and faster download</i></span>
        </template>
        <template slot="singleLabel" slot-scope="{option}">
          1:{{option}} -  
          <span v-if="option === 1"><b>original size</b></span>
          <span v-if="option > 1"><b>scaled down raster</b> - <i>smaller file size and faster download</i></span>
        </template>
      </multiselect>
      <b>size</b> {{xpixels}} x {{ypixels}} = {{pixelCountText}}
      <br>
      <br>
      <b>Mode</b>
      <br>
      <multiselect v-model="mode" :options="modes" :show-labels="false" :allowEmpty="false" placeholder="output format" track-by="id" label="title" />
      <br>
      <div v-if="mode.id === 'direct_bands'">
        <b>Arrangement</b>
        <br>
        Raster file at one time slice with (multiple) bands.
        <br>
        <br>
        <b>File format</b>
        <br>
        <multiselect v-model="outputType" :options="outputTypes" :show-labels="false" :allowEmpty="false" placeholder="output format" />
        <br>
        <b>Time slice</b>
        <br>
        <multiselect v-model="selectedTimeSlice" :options="meta.time_slices" trackBy="id" label="name" :searchable="false" placeholder="(no time slice selected)" :closeOnSelect="true">
        </multiselect>        
      </div>

      <div v-if="meta !== undefined && mode.id === 'zip'">
        <b>Arrangement</b>
        <br>
        <multiselect v-model="zip_arrangement" :options="zip_arrangements" :show-labels="false" :allowEmpty="false" placeholder="output format" track-by="id" label="title" />
        <br>
        <b>File format of files contained in ZIP file</b>
        <br>
        GeoTIFF
        <br>
        <br>
        <b>Time slices</b>
        <br>
        <multiselect multiple v-model="selectedTimeSlices" :options="meta.time_slices" trackBy="id" label="name" :searchable="false" placeholder="(all time slices)" :closeOnSelect="false">
        </multiselect>
      </div>

      <br>
      <b>Bands</b>
      <br>
     
      <div v-if="meta !== undefined">
      <multiselect multiple v-model="selectedBands" :options="meta.bands" trackBy="index" label="index" :searchable="false" placeholder="(all bands)" :closeOnSelect="false">
        <template slot="option" slot-scope="{option}">
          {{option.index}} - {{option.title}}
        </template>
        <template slot="singleLabel" slot-scope="{option}">
          {{option.index}} - {{option.title}}
        </template>
      </multiselect>
      </div>

      <br>
      <div v-if="mode.id === 'direct_bands'">
        <a title="download as raster file" style="padding: 9px; background-color: #f5f5f5; 
        box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);" :href="downloadLink" :download="downloadFilename">
        <v-icon style="color: black;">cloud_download</v-icon> Download</a>
      </div>
      <div v-if="mode.id === 'zip'">
        <b>Compression</b>
        <br>
        <multiselect v-model="compression" :options="compressions" :show-labels="false" :allowEmpty="false" placeholder="output format" track-by="id" label="title" />
        <br>
        <v-btn @click="zip_download()"><v-icon>cloud_download</v-icon>Download</v-btn>
        <a ref="zip_download_link" hidden>hidden link</a>
      </div>
      <br>
      <br>
      <admin-viewer-raster-export-format-description v-if="mode.id === 'direct_bands'" :output-type="outputType" />
    </div>
  </div>
</div>  
</template>

<script>

import axios from 'axios'
import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

import adminViewerRasterExportFormatDescription from './admin-viewer-raster-export-format-description.vue'

export default {
  name: 'admin-viewer-raster-export',

  props: ['meta', 'selectedExtent', 'preselectedTimeSlice'],

  components: {
    Multiselect,
    'admin-viewer-raster-export-format-description': adminViewerRasterExportFormatDescription,
  },

  data() {
    return {
      user_xmin: undefined,
      user_ymin: undefined,
      user_xmax: undefined,
      user_ymax: undefined,
      outputTypes: ['GeoTIFF - data', 'GeoTIFF - tiled data (experimental)', 'rDAT - data', 'PNG - visualisation', 'JPEG - visualisation', 'GeoTIFF - visualisation'],
      outputType: 'GeoTIFF - data',
      modes: [{id: 'zip', title: 'zip file with raster files (divided into tiles for arbitrary large raster downloads)'},
              {id: 'direct_bands', title: 'raster file (for small raster downloads only)'}],
      mode: undefined,
      zip_arrangements: [{id: 'multiband', title: 'multiband: one tile per time slice (files with multiple bands)'},
                        {id: 'timeseries', title: 'timeseries: one tile per band (files with multiple bands containing time slices)'},
                        {id: 'separate_timestamp_band', title: 'separate: tiles for each time slice and each band (single band files)'},
                        {id: 'separate_band_timestamp', title: 'separate: tiles for each band and each time slice (single band files)'}],
      zip_arrangement: {id: 'multiband', title: 'multiband: one tile per time slice (files with multiple bands)'},
      compressions: [{id: '0', title: '0 - no compression (fastest)'},
                    {id: '1', title: '1 - compression (fast)'},
                    {id: '2', title: '2 - compression (fast)'},
                    {id: '3', title: '3 - compression (fast)'},
                    {id: '4', title: '4 - compression (fast)'},
                    {id: '5', title: '5 - compression (fast)'},
                    {id: '6', title: '6 - compression (compromise)'},
                    {id: '7', title: '7 - compression (slow)'},
                    {id: '8', title: '8 - compression (slower)'},
                     {id: '9', title: '9 - best compression (slowest)'}],
      compression: {id: '0', title: '0 - no compression (fastest)'},
      selectedBands: [],
      selectedTimeSlice: undefined,
      selectedTimeSlices: [],
      scaleDivs: [1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536],
      scaleDiv: 1,
    };
  },
  methods: {
    toQuery: function (parameters) {
      var query = "";
      for (var p in parameters) {
        if (query.length == 0) {
          query = "?";
        } else {
          query += "&";
        }
        query += p + "=" + parameters[p];
      }
      return query;
    },
    zip_download() {
      var self = this;
      var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + '/packages');
      var spec = {
        ext: [this.user_xmin, this.user_ymin, this.user_xmax, this.user_ymax],
        compression: self.compression.id,
        div: self.scaleDiv,
        arrangement: self.zip_arrangement.id,
      };
      if(this.selectedTimeSlices.length > 0) {
        spec.time_slice_ids = this.selectedTimeSlices.map(time_slice => time_slice.id);
      }
      if(self.selectedBands.length > 0) {
        spec.bands = self.selectedBands.map(band => band.index);
      }
      axios.post(url, {
        package: spec,
      }).then(function(response) {
        var json = response.data.package;
        console.log(json);
        console.log("start");
        var link = self.$refs.zip_download_link;
        console.log(link);
        var downloadUrl = self.urlPrefix + '../../rasterdb/' + self.meta.name + '/packages/' + json.zip_file_url;
        link.href = downloadUrl;
        link.download = json.zip_file_name;
        link.click();
        console.log(link);
        console.log("stop");        
      }).catch(function(error) {
        console.log(error);
      });
     
    },
  },
  computed: {
    xpixels() {
      return Math.floor(Math.floor((this.user_xmax - this.user_xmin)/this.meta.ref.pixel_size.x)/this.scaleDiv);
    },
    ypixels() {
      return Math.floor(Math.floor((this.user_ymax - this.user_ymin)/this.meta.ref.pixel_size.y)/this.scaleDiv);
    },
    pixelCountText() {
        var pixelCount = (this.xpixels * this.ypixels);
        if(pixelCount >= 100000000000) {
          return '' + (pixelCount / 1000000000000).toFixed(1) + ' terapixel';
        }
        if(pixelCount >= 100000000) {
          return '' + (pixelCount / 1000000000).toFixed(1) + ' gigapixel';
        }
        if(pixelCount >= 1000000) {
          return '' + (pixelCount / 1000000).toFixed(1) + ' megapixel';
        }
        if(pixelCount >= 100000) {
          return '' + (pixelCount / 1000000).toFixed(2) + ' megapixel';
        }
        return '' + pixelCount + ' pixel';
    },
    urlPrefix() {
      return this.$store.state.identity.urlPrefix;
    },
    downloadLink() {
      var url_rasterdb = this.$store.getters.apiUrl('rasterdb');
      var method = 'raster.tiff';
      var ext = [this.user_xmin, this.user_ymin, this.user_xmax, this.user_ymax].join(' ');
      var parameters = { ext: ext };

      if(this.selectedTimeSlice !== undefined && this.selectedTimeSlice !== null) {
        parameters.time_slice_id = this.selectedTimeSlice.id;
      }

      switch(this.outputType) {
        case 'GeoTIFF - data':
          method = 'raster.tiff';
          break;
        case 'GeoTIFF - tiled data (experimental)':
          method = 'raster.tiff';
          parameters.format = 'tiff:banded:tiled';
          break;          
        case 'GeoTIFF - visualisation':
          method = 'raster.tiff';
          parameters.visualisation = true;
          break;
        case 'PNG - visualisation':
          method = 'raster.png';
          parameters.visualisation = true;
          break;
        case 'rDAT - data':
          method = 'raster.rdat';
          break;
        case 'JPEG - visualisation':
          method = 'raster.jpg';
          break;      
        default:
          throw "unknown output type";
      }

      if(this.selectedBands.length > 0) {
        parameters.band = this.selectedBands.map(band => band.index).join(' ');
      }
      if(this.scaleDiv !== 1) {
        parameters.div = this.scaleDiv;
      }
      var url = url_rasterdb + '/' + this.meta.name + '/' + method + this.toQuery(parameters);
      return url;
    },
    downloadFilename() {
      var ext = '';
      switch(this.outputType) {
        case 'GeoTIFF - data':
          ext = '.tiff';
          break;
        case 'GeoTIFF - tiled data (experimental)':
          ext = '.tiff';
          break;          
        case 'GeoTIFF - visualisation':
          ext = '.tiff';
          break;
        case 'PNG - visualisation':
          ext = '.png';
          break;
        case 'rDAT - data':
          ext = '.rdat';
          break;
        case 'JPEG - visualisation':
          ext = '.jpg';
          break;      
        default:
          ext = '';
        }
        var name = this.meta.name;
        if(this.selectedTimeSlice !== undefined && this.selectedTimeSlice !== null) {
          name += '__' + this.selectedTimeSlice.name.split('-').join('_').split(':').join('_').split('/').join('_').split('\\').join('_').split('.').join('_');
        } 
      return name + ext;
    },
  },
  watch: {
    selectedExtent() {
      if(this.selectedExtent !== undefined) {
        this.user_xmin = this.selectedExtent[0];
        this.user_ymin = this.selectedExtent[1];
        this.user_xmax = this.selectedExtent[2];
        this.user_ymax = this.selectedExtent[3];
      }
    },
    preselectedTimeSlice() {
      if(this.preselectedTimeSlice != undefined && this.preselectedTimeSlice != null) {
        this.selectedTimeSlice = this.preselectedTimeSlice;
        this.selectedTimeSlices = [this.preselectedTimeSlice];
      } else {
         this.selectedTimeSlice = undefined;
        this.selectedTimeSlices = [];
      }
    },
    /*selectedTimestamps() {
      var ts = this.selectedTimestamps.slice().sort(function(a, b) {
        if (a.timestamp > b.timestamp) {
          return 1;
        }
        if (a.timestamp < b.timestamp) {
          return -1;
        }
        return 0;
      });
      this.selectedTimestamps = ts;
      console.log("selectedTimestamps changed");
    },*/
  },
  mounted() {
    this.mode = this.modes[0];
  },
}

</script>

<style scoped>

.raster-export-container {
    position: absolute;
    top: 0px;
    left: 0px;
    width: 100%;
    height: 100%;
    background-color: aliceblue;
    padding: 30px;
}

.disabled {
  color: grey;
}

</style>
