<template>
  <div style="position: relative;">
  <splitpanes class="rsdb-theme" @resize="onSplitpanesResize">
    <pane min-size="10" size="20" class="split-nav">
      <v-list dense class="split-nav-list">
         <v-toolbar>
            Mouse<br>Modus
            <v-btn-toggle v-model="mouseModusButtonState" mandatory style="border: 1px solid #8d8d88;">
              <v-btn flat value="move" title="move map by mouse (press and hold left mouse button)">
                <v-icon>open_with</v-icon>move
              </v-btn>
              <v-btn flat value="select" title="select an extent on map (press and hold left mouse button)">
                <v-icon>settings_overscan</v-icon>select
              </v-btn>
            </v-btn-toggle>

            <div style="padding-right: 10px;"></div>
            <v-btn fab title="change viewer settings" @click="onSettingsDialog"><v-icon>settings_applications</v-icon></v-btn>
      </v-toolbar>
      <admin-viewer-select 
        :currentLayerWMS_opacity="layerWMS_opacity" 
        :currentRasterdb="rasterdb" 
        :currentTimestamp="timestamp" 
        :currentProduct="product" 
        :meta="meta" 
        @selected-rasterdb="selectedRasterdb = $event" 
        @selected-time-slice="selectedTimeSlice = $event" 
        @selected-product="selectedProduct = $event" 
        @selected-layerwms-opacity="layerWMS_opacity = $event"        
        @close="selectShow = false" 
        v-show="selectShow"
      />
      <admin-viewer-tools 
        @tool-raster-export-show="toolRasterExportShow = true;" 
        @tool-point-export-show="toolPointExportShow = true;" 
        @tool-point-raster-export-show="toolPointRasterExportShow = true;" 
        :selectedExtent="selectedExtent" 
        :meta="meta" 
        :epsgCode="epsgCode"
        :currentVectordb="vectordb"
        :currentTimestamp="timestamp" 
        @selected-vectordb="selectedVectordb = $event" 
      />
      </v-list>                        
    </pane>

    <pane min-size="10" id="split-main" class="split-main" :class="selectedBackgroundClass">
      <div id="olmap-viewer" style="height: 100%;" />
      <div class="busy" style="margin-left: 100px;" v-show="transactionRunCount > 0">
            <img src="images/busy.svg" /> Processing ... {{transactionRunCount > 1 ? '(' + transactionRunCount + ' queued)' : ''}}
            <b>{{transactionRunCount > 1 ? 'Please wait until processing finished.' : ''}}</b>
      </div>
    </pane>
  </splitpanes>

  <admin-viewer-settings v-show="settingsDialog" ref="settingsDialog" 
        @close="settingsDialog = false"       
        @selected-background="selectedBackground = $event"  
        @selected-format="selectedFormat = $event"
        @selected-gamma="selectedGamma = $event" 
        @sync-bands="syncBands = $event" 
        @selected-mapping="selectedOneBandMapping = $event"
        @show-labels="showLabels = $event"
        @value-range-min="valueRangeMin = $event"   
        @value-range-max="valueRangeMax = $event"
        style="position: absolute; top: 0px; left: 0px;"        
      />


    <admin-viewer-raster-export v-if="meta !== undefined" 
      v-show="toolRasterExportShow" 
      @close="toolRasterExportShow = false" 
      :meta="meta" 
      :selectedExtent="selectedExtent"
      :preselectedTimeSlice="selectedTimeSlice" 
      @select-full-extent="selectFullExtent()"
    />

    <admin-viewer-point-export v-if="meta !== undefined" 
      v-show="toolPointExportShow" 
      @close="toolPointExportShow = false" 
      :pointdb="meta.associated.PointDB" 
      :pointcloud="meta.associated.pointcloud" 
      :selectedExtent="selectedExtent"
    />

    <admin-viewer-point-raster-export v-if="meta !== undefined" 
      v-show="toolPointRasterExportShow" 
      @close="toolPointRasterExportShow = false" 
      :pointdb="meta.associated.PointDB" 
      :pointcloud="meta.associated.pointcloud" 
      :selectedExtent="selectedExtent"
    />

    <v-dialog :value="featureDetailsShow" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Attributes</v-card-title>
        <v-card-text>
        <table v-for="feature in selectedFeatures" :key="feature.getId()">
          <tr v-for="(a, i) in table.attributes" :key="i"><td><b>{{a}}</b></td><td>{{featureToTableEntry(feature.getId())[i]}}</td></tr>
        </table>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="featureDetailsShow = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    

    <div v-show="Object.keys(messages).length > 0" class="message_box">
      <div v-for="(text, id) in messages" :key="id">
        <ring-loader color="#000000" size="25px" style="display: inline-block;"/>
        {{text}}
      </div>
    </div>
    <v-snackbar value="true"  v-for="(text, id) in notifications" :key="id">
      {{text}}<v-btn flat color="pink" @click.native="removeNotification(id)">Close</v-btn>
    </v-snackbar>  
  </div>
</template>

<script>

import { mapState } from 'vuex'

import RingLoader from 'vue-spinner/src/RingLoader.vue'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'

import adminViewerSelect from './admin-viewer-select.vue'
import adminViewerTools from './admin-viewer-tools.vue'
import adminViewerRasterExport from './admin-viewer-raster-export.vue'
import adminViewerPointExport from './admin-viewer-point-export.vue'
import adminViewerPointRasterExport from './admin-viewer-point-raster-export.vue'
import adminViewerSettings from './admin-viewer-settings.vue'

import Vue from 'vue'

import 'ol/ol.css'

import ol_Map from 'ol/Map';
import * as ol_layer from 'ol/layer';
import * as ol_source from 'ol/source';
import ol_View from 'ol/View';
import * as ol_control from 'ol/control';
import ol_control_MousePosition from 'ol/control/MousePosition';
import * as ol_interaction from 'ol/interaction';
import * as ol_style from 'ol/style';
import {toStringXY} from 'ol/coordinate';
import GeoJSON from 'ol/format/GeoJSON';
import Projection from 'ol/proj/Projection';

import {register as ol_proj_proj4_register}  from 'ol/proj/proj4';
import proj4 from 'proj4';

import axios from 'axios';

export default {
  name: 'admin-viewer',

  props: ['rasterdb', 'timestamp', 'product', 'vectordb'],

  components: {
        'admin-viewer-select': adminViewerSelect,
        'admin-viewer-tools': adminViewerTools,
        'admin-viewer-raster-export': adminViewerRasterExport,
        'admin-viewer-point-export': adminViewerPointExport,
        'admin-viewer-point-raster-export': adminViewerPointRasterExport,
        'admin-viewer-settings': adminViewerSettings,
        Splitpanes,
        Pane,        
        RingLoader,
  },

  data() {
    return {
      messages: {},
      messagesIndex: 0,
      notifications: {},
      notificationsIndex: 0,

      olmap: undefined,
      mapAliasToProj4: {},
      mapProj4ToAlias: {},
      viewOverlay: undefined,
      olmousePosition: undefined,
      layerBackgroundOSM: undefined,
      layerBackgroundOpenTopoMap: undefined,
      layerBackgroundStamenTerrain: undefined,
      selectedFormat: undefined,
      vectorSource: undefined,
      vectorLayer: undefined,
      vectorLabelLayer: undefined,
      sourceWMS: undefined,
      layerWMS: undefined,
      layerWMS_opacity: 1,
      validProjection: true,

      meta: undefined,

      selectShow: true,
      selectedRasterdb: undefined,
      selectedTimeSlice: undefined,
      selectedProduct: undefined,
      selectedBackground: undefined,
      selectedGamma: "auto",
      syncBands: false,
      selectedOneBandMapping: undefined,
      valueRangeMin: NaN,
      valueRangeMax: NaN,

      toolsShow: true,
      toolRasterExportShow: false,
      toolPointExportShow: false,
      toolPointRasterExportShow: false,
      selectedVectordb: undefined,
      showLabels: true,

      settingsDialog: false,

      mouseModusButtonState: 'move',
      mouseModusKeySelect: false,
      interactionExtent: undefined,
      selectedExtent: undefined,

      globalEventListeners: {},

      table:{attibutes: [], data: []},
      featureDetailsShow: false,
      selectedFeatures: [],
    }
  },
  methods: {
    addnotification(text) {
      var id = this.notificationsIndex++;
      Vue.set(this.notifications, id, text);
      return id;
    },
    removeNotification(id) {
      Vue.delete(this.notifications, id);
    },
    addMessage(text) {
      var id = this.messagesIndex++;
      Vue.set(this.messages, id, text);
      return id;
    },
    removeMessage(id) {
      Vue.delete(this.messages, id);
    },

    async refreshMeta() {
      //console.log("refreshMeta");
      this.meta = undefined;
      if(this.selectedRasterdb === undefined || this.selectedRasterdb === null) {
        this.setVectorView();
        return;
      }
      var messageID = this.addMessage("loading meta data of layer ...");
      try {
        var url = this.$store.getters.apiUrl('rasterdb/' + this.selectedRasterdb.name + '/meta.json');
        var response = await axios.get(url);
        this.meta = response.data;
        this.removeMessage(messageID);
      } catch(e) {
        this.addnotification('ERROR loading meta data of layer ' + e);
        this.removeMessage(messageID);
      }     
    },

    getNoProjection(extent) {
      return new Projection({
          code: 'no-projection',
          units: 'pixels',
          extent: extent,
          axisOrientation: 'enu',
          global: false,
          metersPerUnit: 1,
          worldExtent: extent,
          getPointResolution: function(resolution) {
            return resolution;
          },
      });
    },

    refreshWMS(resetExtent) {
      var self = this;

      if(this.meta === undefined) {
        this.sourceWMS = undefined;
        this.layerWMS.setSource(undefined);
        this.refreshLayers();
        return;
      }

      var imageLoadFunctionWMS = function(image, src) {
					var cnt = self.$store.state.identity.transactionCount;
					image.wmsReqestCount = cnt;
          image.getImage().src = src + "&cnt=" + cnt;
          self.$store.commit('identity/incrementTransactionCount');
      };

      var extent = this.meta.ref.extent;

      var wmsProj4 = '';
      if(this.meta.ref.proj4 !== undefined && this.meta.ref.proj4 !== '') {
        wmsProj4 = this.meta.ref.proj4.trim();
      }

      var sourceWMSProjection = undefined;
      if(wmsProj4 !== '') {
        var wmsProjectionAlias = this.getProjectionAlias(wmsProj4);
        sourceWMSProjection = wmsProjectionAlias;
      }

      if(sourceWMSProjection === undefined) {
        sourceWMSProjection = this.getNoProjection(extent);
      }      

      var wmsUrl = this.$store.getters.apiUrl('rasterdb_wms');

      this.sourceWMS = new ol_source.ImageWMS({
        url: wmsUrl,
        params: this.wmsParams,
        imageLoadFunction: imageLoadFunctionWMS,
        projection: sourceWMSProjection,
        ratio: 1,
      });

      this.sourceWMS.on("imageloadstart", function () {
        self.$store.commit('identity/incrementTransactionRunCount');
      });

      this.sourceWMS.on("imageloadend", function () {
        self.$store.commit('identity/decrementTransactionRunCount');
      });

      this.sourceWMS.on("imageloaderror", function (e) {
        self.$store.commit('identity/decrementTransactionRunCount');
        var cnt = self.$store.state.identity.transactionCount - 1;
        if(e.image.wmsReqestCount === cnt) {
          self.addnotification('ERROR loading image');
        }
      });

      this.layerWMS.setSource(this.sourceWMS);
      if(resetExtent) {
        this.layerWMS.setExtent(extent);
      }

      this.setWmsView();
    },

    setWmsView() {
      if(this.meta === undefined) {
        this.refreshLayers();
        return;
      }
      if(this.meta.ref === undefined) {
        this.refreshLayers();
        return;
      }
      if(this.meta.ref.pixel_size === undefined) {
        this.refreshLayers();
        return;
      }
      if(this.meta.ref.extent === undefined) {
        this.refreshLayers();
        return;
      }

      var extent = this.meta.ref.extent;
      var center = [(extent[0] + extent[2]) / 2, (extent[1] + extent[3]) / 2];

      var wmsProj4 = '';
      if(this.meta.ref.proj4 !== undefined && this.meta.ref.proj4 !== '') {
        wmsProj4 = this.meta.ref.proj4.trim();
      }

      var sourceWMSProjection = undefined;
      if(wmsProj4 !== '') {
        var wmsProjectionAlias = this.getProjectionAlias(wmsProj4);
        sourceWMSProjection = wmsProjectionAlias;
      }

      if(sourceWMSProjection === undefined) {
        sourceWMSProjection = this.getNoProjection(extent);
        this.validProjection = false;
      } else {
        this.validProjection = true;
      }     

      /*console.log("setWmsView " + wmsProj4 +" -> " + sourceWMSProjection);
      var testingTransform = proj_transform(center, sourceWMSProjection, 'EPSG:4326');
      console.log(testingTransform);*/

      this.olmousePosition.setProjection(sourceWMSProjection);
      var min_pixel_size = this.meta.ref.pixel_size.x <= this.meta.ref.pixel_size.y ? this.meta.ref.pixel_size.x : this.meta.ref.pixel_size.y;
      var extent_size_x = extent[2] - extent[0];
      var extent_size_y = extent[3] - extent[1];
      var max_extent_size = extent_size_x >= extent_size_y ? extent_size_x : extent_size_y;

      var view = new ol_View({
        center: center,
        projection: sourceWMSProjection,
        extent: extent,
        constrainOnlyCenter: true,
        minResolution: min_pixel_size / 4,
        maxResolution: max_extent_size / 256,
      });
      
      this.olmap.setView(view);

      view.fit(extent);

      this.olmap.updateSize();
      this.refreshLayers();
    },

    setWebmercatorView() {
      var viewProj4 = '+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs';
      var viewProjectionAlias = this.getProjectionAlias(viewProj4);
      this.olmousePosition.setProjection(viewProjectionAlias);

      var view = new ol_View({
        center: [0, 0],
        projection: viewProjectionAlias,
        zoom: 2,
      });
      this.validProjection = true;

      this.olmap.setView(view);

      //view.fit(extent);

      this.olmap.updateSize();
      this.refreshLayers();
    },

    setVectorView() {
      if(this.meta !== undefined) {
        this.refreshLayers();
        return;
      }
      if(this.vectorSource === undefined) {
        this.setWebmercatorView();
        return;
      }
      var viewProj4 = '+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs';
      var viewProjectionAlias = this.getProjectionAlias(viewProj4);
      this.olmousePosition.setProjection(viewProjectionAlias);

      var view = new ol_View({
        center: [0, 0],
        projection: viewProjectionAlias,
        zoom: 2,
      });
      this.validProjection = true;

      this.olmap.setView(view);

      var extent = this.vectorSource.getExtent();
      view.fit(extent);

      this.olmap.updateSize();
      this.refreshLayers();
    },    


    refreshRoute() {
      var path = '/viewer';
      var query = {};

      if(this.selectedRasterdb === undefined) {
        if(this.rasterdb !== undefined) {
          path += '/' + this.rasterdb;
        }
      } else if(this.selectedRasterdb !== null) {
        path += '/' + this.selectedRasterdb.name;
      }

      if(this.selectedTimeSlice === undefined) {
        if(this.timestamp !== undefined) {
          query.timestamp = this.timestamp;
        }
      } else if(this.selectedTimeSlice !== null) {
        query.timestamp = this.selectedTimeSlice.id;
      }

      if(this.selectedProduct === undefined) {
        if(this.product !== undefined) {
          query.product = this.product;
        }
      } else if(this.selectedProduct !== null) {
        if(this.selectedProduct.title === 'custom') {
          query.product = 'custom';
        } else {
          query.product = this.selectedProduct.name;
        }
      }

      if(this.selectedVectordb === undefined) {
        if(this.vectordb !== undefined) {
          query.vectordb = this.vectordb;
        }
      } else if(this.selectedVectordb !== null) {
        query.vectordb = this.selectedVectordb.name;
      }
      
      this.$router.push({path: path, query: query});
    },
    refreshLayers() {
      var layers = this.olmap.getLayers();
      layers.clear();
      if(this.selectedBackground !== undefined && this.validProjection) {
        switch(this.selectedBackground.name) {
          case "osm":
            layers.push(this.layerBackgroundOSM);
            break;
          case "OpenTopoMap":
            layers.push(this.layerBackgroundOpenTopoMap);
            break;
          case "StamenTerrain":
            layers.push(this.layerBackgroundStamenTerrain);
            break;
        }
      }
      if(this.sourceWMS !== undefined) {
        layers.push(this.layerWMS);
      }
      if(this.vectorSource !== undefined) {
        layers.push(this.vectorLayer);
        if(this.showLabels) {
          layers.push(this.vectorLabelLayer);
        }
      }
    },
    selectFullExtent() {
      this.interactionExtent.setExtent(this.meta.ref.extent);
    },
    refreshVectordbLayer() {
      //console.log("refreshVectordbLayer");
      if(this.selectedVectordb !== undefined && this.selectedVectordb !== null && this.epsgCode !== undefined) {
        this.loadGeojson();
        this.loadTable();
      } else {
        this.refreshVectorSource(undefined);
      }
    },
    async loadGeojson() {
      var messageID = this.addMessage("loading vector data of layer ...");
      var url = this.$store.getters.apiUrl('vectordbs/' + this.selectedVectordb.name + '/geometry.json');
      try {
        var response = await axios.get(url, {
          params: {
            epsg: this.epsgCode,
            just_name_attribute: true,
          }
        });
        var geojson = response.data;
        this.removeMessage(messageID);
        this.refreshVectorSource(geojson);
      } catch(e) {
        this.addnotification('ERROR vector data of layer' + e);
        this.removeMessage(messageID);
      }
    },
    async loadTable() {
      var messageID = this.addMessage("loading table data of layer ...");
      var url = this.$store.getters.apiUrl('vectordbs/' + this.selectedVectordb.name + '/table.json');
      try {
        var response = await axios.get(url);
        this.table = response.data;
        this.removeMessage(messageID);
      } catch {
        this.addnotification('ERROR table data of layer');
        this.removeMessage(messageID);
      }
    },
    refreshVectorSource(geojson) {
      if(geojson === undefined) {
        this.vectorSource = undefined;
        this.vectorLayer.setSource(undefined);
        this.vectorLabelLayer.setSource(undefined);
        this.setVectorView();
        return;        
      }
      var features = (new GeoJSON()).readFeatures(geojson);
      //console.log(features[0]);

      this.vectorSource = new ol_source.Vector({
        features: features,
      });

      this.vectorLayer.setSource(this.vectorSource);
      this.vectorLabelLayer.setSource(this.vectorSource);
      //console.log("refreshVectorSource done");
      this.setVectorView();
    },
    onSettingsDialog() {
      this.settingsDialog = !this.settingsDialog;
      var rect = document.getElementById("split-main").getBoundingClientRect();
      //console.log(rect.right);
      //console.log(this.$refs.settingsDialog);
      this.$refs.settingsDialog.boxX = rect.left; // just inital x position
      //this.$refs.settingsDialog.boxX = 0;
      //this.$refs.settingsDialog.boxY = 0;
    },
    showFeaturesDialog(e) {
      var feature = this.vectorSource.getClosestFeatureToCoordinate(e.coordinate);
      if(feature !== null) {
        console.log("clicked " + feature.getId()+"  " + e.coordinate);      
        this.selectedFeatures = [feature];
        this.featureDetailsShow = true;
      }
    },
    featureToTableEntry(id) {
      if(id === undefined) {
        return [];
      }
      var row = this.table.data[id];
      if(row == undefined) {
        return [];
      }
      return row;
    },
    getProjectionAlias(proj4Text) {
      var alias = this.mapProj4ToAlias[proj4Text];
      if(alias === undefined) {
        var i = 1;
        alias = 'WMS_VIEW_PROJECTION_' + i;
        while(this.mapAliasToProj4[alias] !== undefined) {
          i++;
          alias = 'WMS_VIEW_PROJECTION_' + i;
        }
        this.mapAliasToProj4[alias] = proj4Text;
        this.mapProj4ToAlias[proj4Text] = alias;
        proj4.defs(alias, proj4Text);
        ol_proj_proj4_register(proj4);
      }
      return alias;
    },
    onSplitpanesResize() {
      this.olmap.updateSize();
    }
  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
    mouseModus() {
      var m = this.mouseModusButtonState;
      if(this.mouseModusKeySelect) {
        m = 'select';
      }
      return m;
    },
    session() {
      var i = this.$store.state.identity.data;
      return i === undefined ? undefined : i.session;
    },
    transactionRunCount() {
      return this.$store.state.identity.transactionRunCount;
    },
    selectedBackgroundClass() {
      if(this.selectedBackground === undefined) {
        return '';
      }
      switch(this.selectedBackground.name) {
        case 'checkerboard':
          return 'background-checkerboard';
        case 'black':
          return 'background-black';
        case 'grey':
          return 'background-grey';          
        case 'white':
          return 'background-white';
        default:
          return '';
      }
    },
    wmsParams() {
      var styleParamter = 'color';
      if(this.selectedProduct == undefined || this.selectedProduct == null) {
        if(this.meta !== undefined && this.meta !== null && this.meta.bands !== undefined && this.meta.bands.length == 1) {
          styleParamter = 'band' + this.meta.bands[0].index;
        } else {
          styleParamter = 'color';
        }
      } else {
        styleParamter = this.selectedProduct.name;
      }



      if(this.selectedGamma !== 'auto') {
        styleParamter += '@gamma' + this.selectedGamma;
      }

      if(this.selectedOneBandMapping !== 'undefined' && this.selectedOneBandMapping !== 'grey') {
        styleParamter += "@pal_" + this.selectedOneBandMapping;
      }

      if (this.syncBands) {
				styleParamter += "@sync_bands";
      }
      
      if(!isNaN(this.valueRangeMin)) {
        styleParamter += "@min" + this.valueRangeMin;
      }

      if(!isNaN(this.valueRangeMax)) {
        styleParamter += "@max" + this.valueRangeMax;
      }

      var p = {
        LAYERS: this.rasterdb,
      };

      if(this.selectedTimeSlice !== undefined && this.selectedTimeSlice !== null) {
        p.TIME = this.selectedTimeSlice.id; 
      }

      if(styleParamter !== '') {
        p.STYLES = styleParamter; 
      }

      if(this.session !== undefined) {
        p.session = this.session;
      }

      //p.FORMAT = "image/png";
      //p.FORMAT = "image/png:0";      
      //p.FORMAT = "image/jpeg";
      if(this.selectedFormat !== undefined) {
        p.FORMAT = this.selectedFormat.name;
      }

      return p;
    },
    epsgCode() {
      if(this.meta === undefined || this.meta.ref === undefined) {
        return 3857; // WGS 84 / Pseudo-Mercator
      }
      var code = this.meta.ref.code;
      if(code === undefined) {
        return undefined;
      }
      if(!code.startsWith("EPSG:")) {
        return undefined;
      }
      var i = Number.parseInt(code.substring(5));
      return Number.isInteger(i) ? i : undefined;
    }
  },
  watch: {
    /*$route(to) {
      console.log(to);
    },*/
    selectedRasterdb() {
      this.refreshMeta();
      this.refreshRoute();
    },
    meta() {
      this.refreshWMS(true);
      this.interactionExtent.setExtent();
      var projectionTitle = 'unknown projection';
      if(this.meta !== undefined && this.meta.ref !== undefined) {
        if(this.meta.ref.projectionTitle !== undefined) {
          projectionTitle = this.meta.ref.projectionTitle;
        } else if(this.meta.ref.proj4 !== undefined && this.meta.ref.proj4 !== '') {
          projectionTitle = this.meta.ref.proj4;
        }
      }
      document.getElementById('foot-start-1').innerHTML = projectionTitle;
    },
    selectedTimeSlice() {
      //this.refreshWMS(false);
      this.refreshRoute();
    },
    selectedProduct() {
      this.refreshRoute();
    },          
    selectedVectordb() {
      this.refreshVectordbLayer();
      this.refreshRoute();
    },          
    mouseModusButtonState() {
      this.mouseModusKeySelect = false; // reset key state at button modus switch
    },
   
    wmsParams() {
      if(this.sourceWMS !== undefined) {
        this.sourceWMS.updateParams(this.wmsParams);
      }
    },

    session() {
      this.refreshWMS();
    },
    
    selectedBackground() {
      //console.log(this.selectedBackground.name);
      this.refreshLayers();
    },

    validProjection() {
      this.refreshLayers();
    },

    mouseModus() {
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-move');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-move-on');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-select');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(this.mouseModus) {
        case 'select':
          this.interactionExtent.setActive(true); 
          this.viewOverlay.classList.add('olmap-viewer-mouseModus-select');
          break;
        default:
          this.interactionExtent.setActive(false);
          this.interactionExtent.createOrUpdatePointerFeature_([NaN, NaN]);
          this.viewOverlay.classList.add('olmap-viewer-mouseModus-move');
          break;
      }
    },
    layerWMS_opacity() {
      if(this.layerWMS !== undefined) {
        this.layerWMS.setOpacity(this.layerWMS_opacity);
      }
    },
    epsgCode() {
      this.refreshVectordbLayer();
    },
    showLabels() {
      this.refreshLayers();
    },  
  },
  mounted() {
    var self = this;

    document.getElementById('foot-start-1').innerHTML = '?';
    document.getElementById('foot-end-1').innerHTML = '';

    this.globalEventListeners.keydown = e => {
      if (e.keyCode == 17) { // ctrl-key
        this.mouseModusKeySelect = true;
      }
    };
    
    this.globalEventListeners.keyup = e => {
      if (e.keyCode == 17) { // ctrl-key
        this.mouseModusKeySelect = false;
      }
    };

    document.addEventListener('keydown', this.globalEventListeners.keydown);
    document.addEventListener('keyup', this.globalEventListeners.keyup); 

    this.$store.dispatch('identity/init');

    this.layerWMS = new ol_layer.Image({
    });

    this.layerWMS.setOpacity(this.layerWMS_opacity);

    this.layerBackgroundOSM = new ol_layer.Tile({
      source: new ol_source.OSM(),
    });

    this.layerBackgroundOpenTopoMap = new ol_layer.Tile({
      title: 'OpenTopoMap',
      type: 'base',
      visible: true,
      source: new ol_source.XYZ({
        url: 'https://{a-c}.tile.opentopomap.org/{z}/{x}/{y}.png',
        attributions: 'Kartendaten: © <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>-Mitwirkende, SRTM | Kartendarstellung: © <a href="http://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)',
        wrapX: false,
      })
		});

    this.layerBackgroundStamenTerrain = new ol_layer.Tile({
      source: new ol_source.Stamen({
        layer: 'terrain',
        wrapX: false,
      })
    });

    var vectorFill = new ol_style.Fill({
      //color: 'rgba(255,255,255,0.15)'
      color: 'rgba(255,255,255,0.0)'
    });
    var vectorStroke = new ol_style.Stroke({
      color: 'rgb(95, 112, 245)',
      width: 2,
      /*lineDash: [1, 5],*/
    });
    var vectorPointStroke = new ol_style.Stroke({
      color: 'rgb(200, 20, 35)',
      width: 2,
      /*lineDash: [1, 5],*/
    });
    function vectorStyleFun(/*feature*/) {
      return new ol_style.Style({
        /*image: new ol_style.Circle({
          fill: vectorFill,
          stroke: vectorStroke,
          radius: 5
        }),*/
        image: new ol_style.RegularShape({
            stroke: vectorPointStroke,
            points: 4,
            radius: 10,
            radius2: 0,
            angle: Math.PI / 4
        }),
        fill: vectorFill,
        stroke: vectorStroke,
      });
    }
    this.vectorLayer = new ol_layer.VectorImage({
      style: vectorStyleFun,
      //declutter: true,
    });

    /*var labelBackgroundStroke = new ol_style.Stroke({
      //color: 'rgba(95, 112, 245, 0.3)',
      color: 'rgba(255,255,255,0.5)',
      width: 2,
    });*/
    var labelBackgroundFill = new ol_style.Fill({
      color: 'rgba(255,255,255,0.6)',
    });
    function vectorLabelStyleFun(feature) {
      var text = new ol_style.Text({
        font: '15px sans-serif',
        text: feature.getProperties().name,
        overflow: true,
        //backgroundStroke: labelBackgroundStroke,
        backgroundFill: labelBackgroundFill,
        padding: [2, 2, 2, 2],
      });
      return new ol_style.Style({
        text: text,
      });
    }
     this.vectorLabelLayer = new ol_layer.VectorImage({
      style: vectorLabelStyleFun,
      declutter: true,
    });



    var interactionExtentBoxStyle = [
      new ol_style.Style({
        fill: new ol_style.Fill({
          color: [255, 255, 255, 0.5]
        })
      }),
      new ol_style.Style({
        stroke: new ol_style.Stroke({
          color: [50, 50, 50],
          width: 1,
          lineDash: [10],
        })
      }),
		];

    this.interactionExtent = new ol_interaction.Extent({
      boxStyle: interactionExtentBoxStyle,
    });

    this.interactionExtent.on('extentchanged', function (e) {
      if (e.extent === null) {
        self.selectedExtent = undefined;
      } else {
        self.selectedExtent = e.extent;
      }
    });
    
    this.interactionExtent.setActive(false);

    this.olmousePosition = new ol_control_MousePosition({
      projection: 'EPSG:4326', 
      target: 'foot-end-1', 
      coordinateFormat: function(coordinate) {return toStringXY(coordinate, 2);},
    });

    this.olmap = new ol_Map({
        target: 'olmap-viewer',
        controls: ol_control.defaults({ attributionOptions: { collapsible: false } }).extend([
          new ol_control.ScaleLine({}),
          this.olmousePosition,          
        ]),
        interactions: ol_interaction.defaults().extend([
          this.interactionExtent,
        ]),
      });

    this.viewOverlay = document.querySelector(".ol-overlaycontainer-stopevent");

    this.olmap.on('click', e => {
      switch(this.mouseModus) {
        case 'move':
          this.showFeaturesDialog(e);
          break;
      }
    });
    
    this.olmap.on('movestart', () => {
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-move');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-move-on');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-select');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(this.mouseModus) {
        case 'select':
          this.viewOverlay.classList.add('olmap-viewer-mouseModus-select-on');
          break;
        default:
          this.viewOverlay.classList.add('olmap-viewer-mouseModus-move-on');
          break;
      }
    });

    this.olmap.on('moveend', () => {
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-move');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-move-on');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-select');
      this.viewOverlay.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(this.mouseModus) {
        case 'select':
          this.viewOverlay.classList.add('olmap-viewer-mouseModus-select');
          break;
        default:
          this.viewOverlay.classList.add('olmap-viewer-mouseModus-move');
          break;
      }
    });

    //this.refreshMeta();
    this.setWebmercatorView();
  },
  destroyed() {
    document.getElementById('foot-start-1').innerHTML = '?';
    document.getElementById('foot-end-1').innerHTML = '?';
    document.removeEventListener('keydown', this.globalEventListeners.keydown);
		document.removeEventListener('keyup', this.globalEventListeners.keyup);
    //console.log("destroyed");
  },
}

</script>

<style scoped>

.main {
  position: relative;
}

.background-checkerboard {
  /*background: repeating-linear-gradient( 68grad, #868686, #909090 10px, #9b9999 10px, #8c8c8c 20px );*/

  background: linear-gradient(45deg, rgba(0,0,0,0.0980392) 25%, rgba(0,0,0,0.05) 25%, rgba(0,0,0,0.05) 75%, rgba(0,0,0,0.0980392) 75%, rgba(0,0,0,0.0980392) 0), linear-gradient(45deg, rgba(0,0,0,0.0980392) 25%, rgba(0,0,0,0.05) 25%, rgba(0,0,0,0.05) 75%, rgba(0,0,0,0.0980392) 75%, rgba(0,0,0,0.0980392) 0), rgb(255, 255, 255) !important;
  background-position: 0 0, 10px 10px !important;
  background-size: 20px 20px !important;
}

.background-black {
  background-color: black !important;
}

.background-grey {
  background-color: grey !important;
}

.background-white {
  background-color: white !important;
}

#top {
  position: absolute;
  top: 4px;
  left: 0px;
  /*width: 100%;*/
  background-color:rgba(243, 243, 243, 0.94);

  border-style: solid;
  border-width: 2px;
  border-color: grey;
}

.message_box {
  position: absolute;
  top: 200px;
  left: 600px;
  background-color: rgb(243, 243, 243);
  padding: 1px;
  border-color: rgb(137, 137, 137);
  border-style: solid;
  border-width: 1px;
  font-size: 1.5em;
}

.no_layer {
  position: absolute; 
  top: 0px;
  left: 0px;
  width: 100%; 
  height: 100%;
  display: flex;           /* establish flex container */
  flex-direction: column;  /* make main axis vertical */
  justify-content: center; /* center items vertically, in this case */
  align-items: center;     /* center items horizontally, in this case */
  background-color: rgba(221, 221, 221, 0.58);
}

.busy {
  position: absolute;
  top: 100px;
  left: 400px;
	background-color: rgba(216, 214, 209, 0.64);
	border-radius: 4px;
	padding: 2px;
	text-align: center;
	font-family: sans-serif;
	font-size: 1rem;
	pointer-events: none;
	display: inline-block;	
}
</style>

<style>

#olmap-viewer .ol-attribution.ol-unselectable.ol-control.ol-uncollapsible {
  bottom: 0px;
}

#olmap-viewer .ol-zoom {
    top: .5em;
    right: .5em;
    left: unset;
}

#olmap-viewer .ol-scale-line {
    left: 305px;
}

.olmap-viewer-mouseModus-move {
  cursor: grab;
}

.olmap-viewer-mouseModus-move-on {
  cursor: grabbing;  
}

.olmap-viewer-mouseModus-select {
  cursor: crosshair;
}

.olmap-viewer-mouseModus-select-on {
  cursor: move;
}

.v-toolbar__content {
  padding-left: 5px;
  padding-right: 0px;
  padding-top: 0px;
  padding-bottom: 0px;
}

.split-nav {
  overflow-y: auto;
  background-color: black;
}

.split-nav-list {
  background-color: black;
}

.split-main {

}

</style>
