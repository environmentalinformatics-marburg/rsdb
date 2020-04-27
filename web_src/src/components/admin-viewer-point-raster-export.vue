<template>
<div>
  <div class="point-export-container">
    <h1>Export Raster of processed point-cloud data<v-btn flat icon color="indigo" @click="$emit('close')"  title="close box" style="maring: 0px;"><v-icon>close</v-icon></v-btn></h1>
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

      <br>
      <b>Point-cloud to raster processing type</b>
      <br>
      <span v-if="raster_processing_types != undefined">
        <multiselect v-model="raster_processing_type" :options="raster_processing_types" :show-labels="false" :allowEmpty="false" placeholder="point-cloud to raster processing type" track-by="name" label="label"/>
      </span>
      <span v-if="metaMessage != undefined">
        {{metaMessage}}
      </span>

      <br>
      <b>File-Format</b>
      <br>
      <multiselect v-model="outputType" :options="outputTypes" :show-labels="false" :allowEmpty="false" placeholder="output format" track-by="name" label="title"/>

      <br>
      <a title="download points as file" style="padding: 9px; background-color: #f5f5f5; 
      box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);" :href="downloadLink" :download="downloadFilename">
      <v-icon style="color: black;">cloud_download</v-icon> Download</a>
      <br>
      <br>
      <br>
      <admin-viewer-point-raster-export-format-description :output-type="outputType" />
    </div>
  </div>
</div>  
</template>

<script>

import axios from 'axios'

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

import adminViewerPointRasterExportFormatDescription from './admin-viewer-point-raster-export-format-description.vue'

export default {
  name: 'admin-viewer-point-raster-export',

  props: ['pointdb', 'pointcloud', 'selectedExtent'],

  components: {
    Multiselect,
    'admin-viewer-point-raster-export-format-description': adminViewerPointRasterExportFormatDescription,
  },

  data() {
    return {
      user_xmin: undefined,
      user_ymin: undefined,
      user_xmax: undefined,
      user_ymax: undefined,
      outputTypes: [
        {name: 'zip', title: '.zip - containing tiled .tiff files (for arbitrarily large rasters)'},
        {name: 'tiff', title: '.tiff - GeoTIFF raster file (for small rasters only)'}, 
        {name: 'png', title: '.png - PNG image file (for small rasters only)'}, 
        {name: 'rDAT', title: '.rDAT - binary file (for small rasters only)'}, 
      ],
      outputType: undefined,
      valid_processing_types: ["basic_raster", "index_raster", "multi_raster"],
      raster_processing_type: undefined,
      meta: undefined,
      metaMessage: 'init',
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
    refreshMeta() {
      if(this.pointdb !== undefined) {
        this.meta = undefined;
        this.metaMessage = "loading meta data of layer ...";
        axios.get(this.urlPrefix + '../../pointdb/info.json' + '?db=' + this.pointdb + '&statistics=false') 
        .then(response => {
          this.meta = response.data;
          this.metaMessage = undefined;
        })
        .catch(e => {
          this.meta = undefined;
          this.metaMessage = 'ERROR loading meta data of layer ' + e;
        });        
      } else if(this.pointcloud !== undefined) {
        this.meta = undefined;
        this.metaMessage = "loading meta data of layer ...";
        axios.get(this.urlPrefix + '../../pointclouds/' + this.pointcloud) 
        .then(response => {
          this.meta = response.data;
          this.metaMessage = undefined;
        })
        .catch(e => {
          this.meta = undefined;
          this.metaMessage = 'ERROR loading meta data of layer ' + e;
        });
      } else { 
        this.meta = undefined;
        this.metaMessage = 'error in meta';
        return;
      }        
    },
  },
  computed: {
    xpixels() {
      return Math.floor(Math.floor((this.user_xmax - this.user_xmin)/1)/1);
    },
    ypixels() {
      return Math.floor(Math.floor((this.user_ymax - this.user_ymin)/1)/1);
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
      if(this.pointdb !== undefined && this.outputType !== undefined && this.raster_processing_type !== undefined) {
        var url_pointdb = this.urlPrefix + '../../pointdb';
        var method_pointdb = 'unknown';
        var ext_pointdb = [this.user_xmin, this.user_xmax, this.user_ymin, this.user_ymax].join(',');
        var parameters_pointdb = { db: this.pointdb, ext: ext_pointdb, type: this.raster_processing_type.name };

        switch(this.outputType.name) {
          case 'tiff':
            method_pointdb = 'raster.tiff';
            parameters_pointdb.format = 'tiff';
            break;
          case 'png':
            method_pointdb = 'raster.png';
            parameters_pointdb.format = 'png';
            break;          
          case 'rDAT':
            method_pointdb = 'raster.rDAT';
            parameters_pointdb.format = 'rDAT';
            break;          
          case 'zip':
            method_pointdb = 'raster.zip';
            parameters_pointdb.format = 'zip';
            break;        
          default:
            parameters_pointdb.format = 'unknown';
            throw "unknown output type " + this.outputType.name;
        }

        var link_pointdb = url_pointdb + '/' + method_pointdb + this.toQuery(parameters_pointdb);
        return link_pointdb;
      }
      if(this.pointcloud !== undefined && this.outputType !== undefined && this.raster_processing_type !== undefined) {
        var url_pointcloud = this.urlPrefix + '../../pointclouds';
        var method_pointcloud = 'unknown';
        var ext_pointcloud = [this.user_xmin, this.user_ymin, this.user_xmax, this.user_ymax].join(' ');
        var parameters_pointcloud = { ext: ext_pointcloud, type: this.raster_processing_type.name, fill: 10 };

        switch(this.outputType.name) {
          case 'tiff':
            method_pointcloud = 'raster.tiff';
            parameters_pointcloud.format = 'tiff';
            break;
          case 'png':
            method_pointcloud = 'raster.png';
            parameters_pointcloud.format = 'png';
            break;          
          case 'rDAT':
            method_pointcloud = 'raster.rDAT';
            parameters_pointcloud.format = 'rDAT';
            break;          
          case 'zip':
            method_pointcloud = 'raster.zip';
            parameters_pointcloud.format = 'zip';
            break;        
          default:
            parameters_pointcloud.format = 'unknown';
            throw "unknown output type " + this.outputType.name;
        }

        var link_pointcloud = url_pointcloud + '/' + this.pointcloud + '/' + method_pointcloud + this.toQuery(parameters_pointcloud);
        return link_pointcloud;
      }
      return "error";
    },
    downloadFilename() {
      var ext = '';
      switch(this.outputType.name) {
        case 'tiff':
          ext = '.tiff';
          break;
        case 'png':
          ext = '.png';
          break;
        case 'rDAT':
          ext = '.rdat';
          break;
        case 'zip':
          ext = '.zip';
          break;               
        default:
          throw "unknown output type " + this.outputType.name;
      }
      var name = this.pointdb !== undefined ? this.pointdb : this.pointcloud !== undefined ? this.pointcloud : 'raster';
      return name + ext;
    },
    raster_processing_types() {
        if(this.meta === undefined) {
          return undefined;
        }
        if(this.meta.raster_processing_types !== undefined) {
          return this.meta.raster_processing_types
          .filter(t => this.valid_processing_types.indexOf(t.data_type) >= 0)
          .map(t => {return {name: t.name, label : t.name + ' - ' + t.title};});
        }
        if(this.meta.pointcloud !== undefined) {
          return this.meta.pointcloud.raster_types
          .map(t => {return {name: t.name, label : t.name + ' - ' + t.description};});
        }
        return undefined;
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
    pointdb() {
      this.refreshMeta();
    },
    pointcloud() {
      this.refreshMeta();
    },
    raster_processing_types() {
      this.raster_processing_type = this.raster_processing_types !== undefined && this.raster_processing_types.length !== 0 ? this.raster_processing_types[0] : undefined;
    }
  },
  mounted() {
    this.outputType = this.outputTypes.find(o=>o.name === 'zip');
    this.refreshMeta();
  },
}

</script>

<style scoped>

.point-export-container {
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
