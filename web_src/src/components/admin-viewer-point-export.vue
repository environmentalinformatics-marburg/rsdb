<template>
<div>
  <div class="point-export-container">
    <h1>Export Point Data <v-btn flat icon color="indigo" @click="$emit('close')"  title="close box" style="maring: 0px;"><v-icon>close</v-icon></v-btn></h1>
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
      <b>File-Format</b>
      <br>
      <multiselect v-model="outputType" :options="outputTypes" :show-labels="false" :allowEmpty="false" placeholder="output format" />

      <br>
      <a title="download points as file" style="padding: 9px; background-color: #f5f5f5; 
      box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);" :href="downloadLink" :download="downloadFilename">
      <v-icon style="color: black;">cloud_download</v-icon> Download</a>
      <br>
      <br>
      <br>
      <admin-viewer-point-export-format-description :output-type="outputType" />
    </div>
  </div>
</div>  
</template>

<script>

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

import adminViewerPointExportFormatDescription from './admin-viewer-point-export-format-description.vue'

export default {
  name: 'admin-viewer-point-export',

  props: ['pointdb', 'pointcloud', 'selectedExtent'],

  components: {
    Multiselect,
    'admin-viewer-point-export-format-description': adminViewerPointExportFormatDescription,
  },

  data() {
    return {
      user_xmin: undefined,
      user_ymin: undefined,
      user_xmax: undefined,
      user_ymax: undefined,
      outputTypes: ['LAS', 'xyz', 'rDAT'],
      outputType: 'LAS',
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
      if(this.pointdb !== undefined) {
        var url_pointdb = this.urlPrefix + '../../pointdb';
        var method_pointdb = 'unknown';
        var ext_pointdb = [this.selectedExtent[0], this.selectedExtent[2], this.selectedExtent[1], this.selectedExtent[3]].join(',');
        var parameters_pointdb = { db: this.pointdb, ext: ext_pointdb };

        switch(this.outputType) {
          case 'LAS':
            method_pointdb = 'points.las';
            parameters_pointdb.format = 'las';
            break;
          case 'xyz':
            method_pointdb = 'points.xyz';
            parameters_pointdb.format = 'xyz';
            break;
          case 'rDAT':
            method_pointdb = 'points.rdat';
            parameters_pointdb.format = 'rdat';
            break;     
          default:
            parameters_pointdb.format = 'unknown';
            throw "unknown output type " + this.outputType;
        }

        var link_pointdb = url_pointdb + '/' + method_pointdb + this.toQuery(parameters_pointdb);
        return link_pointdb;
      }
      if(this.pointcloud !== undefined) {
        var url_pointcloud = this.urlPrefix + '../../pointclouds';
        var method_pointcloud = 'unknown';
        var ext_pointcloud = this.selectedExtent.join(' ');
        var parameters_pointcloud = { ext: ext_pointcloud };

        switch(this.outputType) {
          case 'LAS':
            method_pointcloud = 'points.las';
            break;
          case 'xyz':
            method_pointcloud = 'points.xyz';
            break;
          case 'rDAT':
            method_pointcloud = 'points.rdat';
            break;     
          default:
            throw "unknown output type " + this.outputType;
        }

        var link_pointcloud = url_pointcloud + '/' + this.pointcloud + '/' + method_pointcloud + this.toQuery(parameters_pointcloud);
        return link_pointcloud;
      }
      return "error";
    },
    downloadFilename() {
      var ext = '';
      switch(this.outputType) {
        case 'LAS':
          ext = '.las';
          break;
        case 'xyz':
          ext = '.xyz';
          break;
        case 'rDAT':
          ext = '.rdat';
          break;     
        default:
          throw "unknown output type " + this.outputType;
      }
      var name = 'this.meta.name';
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
  },
  mounted() {
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
