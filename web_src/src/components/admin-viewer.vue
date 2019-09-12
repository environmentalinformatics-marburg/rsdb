<template>
  <div class="main" :class="selectedBackgroundClass">
    <div id="olmap-viewer" style="width: 100%; height: 100%;" />
    <div class="no_layer" v-show="meta === undefined">
      <b>no layer</b>
    </div>
    <div class="busy" style="margin-left: 100px;" v-show="transactionRunCount > 0">
					<img src="images/busy.svg" /> Processing ... {{transactionRunCount > 1 ? '(' + transactionRunCount + ' queued)' : ''}}
					<b>{{transactionRunCount > 1 ? 'Please wait until processing finished.' : ''}}</b>
		</div>

    <!--<div id="top" style="display: inline-block; white-space: nowrap;">
      <v-toolbar>
            Mouse<br>Modus
            <v-btn-toggle v-model="mouseModus" mandatory>
              <v-btn flat value="move" title="move map by mouse (press and hold left mouse button)">
                move<v-icon>pan_tool</v-icon>
              </v-btn>
              <v-btn flat value="select" title="select an extent on map (press and hold left mouse button)">
                select<v-icon>settings_overscan</v-icon>
              </v-btn>
            </v-btn-toggle>
      </v-toolbar>
      <div v-show="!selectShow">
      {{rasterdb}}<v-btn flat icon color="indigo" @click="selectShow = true" title="open box"><v-icon>add</v-icon></v-btn>
      </div>
      <admin-viewer-select :currentRasterdb="rasterdb" :currentTimestamp="timestamp" :currentProduct="product" :meta="meta" @selected-rasterdb="selectedRasterdb = $event" @selected-timestamp="selectedTimestamp = $event" @selected-product="selectedProduct = $event" @selected-background="selectedBackground = $event" @close="selectShow = false" v-show="selectShow"/>
      <div v-show="!toolsShow">
      Tools<v-btn flat icon color="indigo" @click="toolsShow = true" title="open tools"><v-icon>add</v-icon></v-btn>
      </div>
      <admin-viewer-tools @close="toolsShow = false" v-show="toolsShow" @tool-raster-export-show="toolRasterExportShow = true;" />
    </div>-->

    <div class="innergrid-container">
      <v-list dense class="innergrid-item-nav">
         <v-toolbar>
            Mouse<br>Modus
            <v-btn-toggle v-model="mouseModus" mandatory style="border: 1px solid #8d8d88;">
              <v-btn flat value="move" title="move map by mouse (press and hold left mouse button)">
                <v-icon>open_with</v-icon>move
              </v-btn>
              <v-btn flat value="select" title="select an extent on map (press and hold left mouse button)">
                <v-icon>settings_overscan</v-icon>select
              </v-btn>
            </v-btn-toggle>

            <v-dialog v-model="settingsDialog" width="500">
              <template v-slot:activator="{ on }">
                <v-btn fab title="change viewer settings" v-on="on"><v-icon>settings_applications</v-icon></v-btn>
              </template>
              <v-card>
                <v-card-title class="headline grey lighten-2" primary-title>
                  Viewer Settings              
                </v-card-title>
                <v-card-text style="min-height: 500px;">
                  <admin-viewer-settings        
                  @selected-background="selectedBackground = $event"  
                  @selected-format="selectedFormat = $event"       
                  />
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions>
                  <v-spacer></v-spacer>
                  <v-btn color="primary" flat @click="settingsDialog = false">close</v-btn>
                </v-card-actions>
              </v-card>
            </v-dialog>
      </v-toolbar>
      <admin-viewer-select 
        :currentLayerWMS_opacity="layerWMS_opacity" 
        :currentRasterdb="rasterdb" 
        :currentTimestamp="timestamp" 
        :currentProduct="product" 
        :meta="meta" 
        @selected-rasterdb="selectedRasterdb = $event" 
        @selected-timestamp="selectedTimestamp = $event" 
        @selected-product="selectedProduct = $event" 
        @selected-layerwms-opacity="layerWMS_opacity = $event" 
        @selected-gamma="selectedGamma = $event" 
        @sync-bands="syncBands = $event" 
        @selected-mapping="selectedOneBandMapping = $event" 
        @close="selectShow = false" 
        v-show="selectShow"
      />
      <admin-viewer-tools @tool-raster-export-show="toolRasterExportShow = true;" @tool-point-export-show="toolPointExportShow = true;" @tool-point-raster-export-show="toolPointRasterExportShow = true;" :selectedExtent="selectedExtent" :meta="meta" :epsgCode="epsgCode" @selected-vectordb="selectedVectordb = $event" />
      </v-list>
      <!--<div id="olmap-viewer" innergrid-item-main />-->
    </div>




    <admin-viewer-raster-export v-if="meta !== undefined" 
      v-show="toolRasterExportShow" 
      @close="toolRasterExportShow = false" 
      :meta="meta" 
      :selectedExtent="selectedExtent"
      :preselectedTimestamp="selectedTimestamp" 
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

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'
import RingLoader from 'vue-spinner/src/RingLoader.vue'

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

import {register as ol_proj_proj4_register}  from 'ol/proj/proj4';
import proj4 from 'proj4';

import axios from 'axios';

export default {
  name: 'admin-viewer',

  props: ['rasterdb', 'timestamp', 'product'],

  components: {
        'admin-viewer-select': adminViewerSelect,
        'admin-viewer-tools': adminViewerTools,
        'admin-viewer-raster-export': adminViewerRasterExport,
        'admin-viewer-point-export': adminViewerPointExport,
        'admin-viewer-point-raster-export': adminViewerPointRasterExport,
        'admin-viewer-settings': adminViewerSettings,
        RingLoader,
        Multiselect,
  },

  data() {
    return {
      messages: {},
      messagesIndex: 0,
      notifications: {},
      notificationsIndex: 0,

      olmap: undefined,
      olmousePosition: undefined,
      layerBackgroundOSM: undefined,
      layerBackgroundOpenTopoMap: undefined,
      layerBackgroundStamenTerrain: undefined,
      selectedFormat: undefined,
      vectorLayer: undefined,
      sourceWMS: undefined,
      layerWMS: undefined,
      layerWMS_opacity: 1,

      meta: undefined,
      mapAliasToProj4: {},
      mapProj4ToAlias: {},

      selectShow: true,
      selectedRasterdb: undefined,
      selectedTimestamp: undefined,
      selectedProduct: undefined,
      selectedBackground: undefined,
      selectedGamma: "auto",
      syncBands: false,
      selectedOneBandMapping: undefined,

      toolsShow: true,
      toolRasterExportShow: false,
      toolPointExportShow: false,
      toolPointRasterExportShow: false,
      selectedVectordb: undefined,

      settingsDialog: false,

      mouseModus: undefined,
      interactionExtent: undefined,
      selectedExtent: undefined,

      globalEventListeners: {},
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
    refreshMeta() {
      if(this.rasterdb === undefined) {
        this.meta = undefined;
        return;
      }
      var self = this;
      var messageID = this.addMessage("loading meta data of layer ...");
      axios.get(this.urlPrefix + '../../rasterdb/' + this.rasterdb + '/meta.json')
      .then(function(response) {
        self.meta = response.data;
        self.removeMessage(messageID);
      })
      .catch(function(e) {
        self.meta = undefined;
        self.addnotification('ERROR loading meta data of layer ' + e);
        self.removeMessage(messageID);
      });
      /*.finally(function () {
        self.removeMessage(messageID);
      });*/      
    },
    refreshWMS() {
      var self = this;

      if(this.meta === undefined) {
        this.layerWMS.setSource(undefined);
        return;
      }

      var imageLoadFunctionWMS = function(image, src) {
					var cnt = self.$store.state.identity.transactionCount;
					image.wmsReqestCount = cnt;
          image.getImage().src = src + "&cnt=" + cnt;
          self.$store.commit('identity/incrementTransactionCount');
      };
      var view_proj4 = '+proj=utm +zone=37 +south +datum=WGS84 +units=m +no_defs ';
      if(this.meta.ref.proj4 !== undefined && this.meta.ref.proj4 !== '') {
        view_proj4 = this.meta.ref.proj4.trim();
      }
      //console.log(view_proj4);

      var alias = this.mapProj4ToAlias[view_proj4];
      if(alias === undefined) {
        var i = 1;
        alias = 'WMS_VIEW_PROJECTION_' + i;
        while(this.mapAliasToProj4[alias] !== undefined) {
          i++;
          alias = 'WMS_VIEW_PROJECTION_' + i;
        }
        this.mapAliasToProj4[alias] = view_proj4;
        this.mapProj4ToAlias[view_proj4] = alias;
      }

      proj4.defs(alias, view_proj4);
      ol_proj_proj4_register(proj4);

      this.olmousePosition.setProjection(alias);

      var wmsUrl = this.$store.getters.apiUrl('rasterdb_wms');

      this.sourceWMS = new ol_source.ImageWMS({
        url: wmsUrl,
        params: this.wmsParams,
        imageLoadFunction: imageLoadFunctionWMS,
        projection: alias,
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
      
      var extent = this.meta.ref.extent;
      this.layerWMS.setExtent(extent);

      var min_pixel_size = this.meta.ref.pixel_size.x <= this.meta.ref.pixel_size.y ? this.meta.ref.pixel_size.x : this.meta.ref.pixel_size.y;
      var extent_size_x = extent[2] - extent[0];
      var extent_size_y = extent[3] - extent[1];
      var max_extent_size = extent_size_x >= extent_size_y ? extent_size_x : extent_size_y;

      var view = new ol_View({
        center: [(extent[0] + extent[2]) / 2, (extent[1] + extent[3]) / 2],
        projection: alias,
        extent: extent,
        minResolution: min_pixel_size / 4,
        maxResolution: max_extent_size / 256,
      });
      
      this.olmap.setView(view);

      view.fit(extent);


    },
    refreshRoute() {
      var rasterdb = this.selectedRasterdb === undefined ? this.rasterdb : this.selectedRasterdb.name;
      var timestamp = this.selectedTimestamp === undefined ? this.timestamp : this.selectedTimestamp.timestamp;
      var product = this.selectedProduct === undefined ? this.product : this.selectedProduct.name;

      this.$router.push('/viewer/' + rasterdb);
      this.$router.push({path: '/viewer/' + rasterdb, query: {timestamp: timestamp, product: product}});
    },
    refreshLayers() {
      var layers = this.olmap.getLayers();
      layers.clear();
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
      layers.push(this.layerWMS);
      layers.push(this.vectorLayer);
    },
    selectFullExtent() {
      //this.selectedExtent = this.meta.ref.extent;
      this.interactionExtent.setExtent(this.meta.ref.extent);
    },
    refreshVectordbLayer() {
      //console.log("refreshVectordbLayer");
      if(this.selectedVectordb !== undefined && this.epsgCode !== undefined) {
        this.loadGeojson();
      } else {
        this.refreshVectorSource(undefined);
      }
    },
    loadGeojson() {
      var self = this;
      var messageID = this.addMessage("loading vector data of layer ...");
      var url = this.$store.getters.apiUrl('vectordbs/' + this.selectedVectordb.name + '/geometry.json');
      axios.get(url, {
        params: {
          epsg: this.epsgCode,
        }
      })
      .then(function(response) {
        var geojson = response.data;
        self.removeMessage(messageID);
        self.refreshVectorSource(geojson);
      })
      .catch(function(e) {
        self.addnotification('ERROR vector data of layer' + e);
        self.removeMessage(messageID);
      });
    },
    refreshVectorSource(geojson) {
      var features = geojson === undefined ? [] : (new GeoJSON()).readFeatures(geojson);
      //console.log(features[0]);

      var vectorSource = new ol_source.Vector({
        features: features,
      });

      this.vectorLayer.setSource(vectorSource);
      //console.log("refreshVectorSource done");
    },
  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
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
        case 'white':
          return 'background-white';
        default:
          return '';
      }
    },
    wmsParams() {
      var styleParamter = this.product;

      if(this.selectedGamma !== 'auto') {
        styleParamter += ' gamma' + this.selectedGamma;
      }

      if(this.selectedOneBandMapping !== 'undefined' && this.selectedOneBandMapping !== 'grey') {
        styleParamter += " pal_" + this.selectedOneBandMapping;
      }

      if (this.syncBands) {
				styleParamter += " sync_bands";
			}

      var p = {
        LAYERS: this.rasterdb,
      };

      if(this.timestamp != undefined) {
        p.TIME = this.timestamp; 
      }

      if(styleParamter != undefined) {
        p.STYLES = styleParamter; 
      }

      if(this.session != undefined) {
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
        return undefined;
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
    rasterdb() {
      this.refreshRoute();
      this.refreshMeta();
    },
    wmsParams() {
      if(this.sourceWMS !== undefined) {
        this.sourceWMS.updateParams(this.wmsParams);
      }
    },
    timestamp() {
      this.refreshRoute();
      this.refreshWMS();
    },
    product() {
      this.refreshRoute();
    },
    meta() {
      this.refreshWMS();
      this.interactionExtent.setExtent();
      var title = this.meta.ref.projectionTitle;
      if(title === undefined) {
        title = this.meta.ref.proj4;
      }
      document.getElementById('foot-start-1').innerHTML = title === undefined ? 'unknown projection' : title;
    },
    session() {
      this.refreshWMS();
    },
    selectedRasterdb() {
      //console.log("selected");
      //console.log(this.selectedRasterdb);
      //console.log(this.selectedRasterdb.name);
      
      this.refreshRoute();
    },
    selectedTimestamp() {
      this.refreshRoute();
    },
    selectedProduct() {
      this.refreshRoute();
    },
    selectedBackground() {
      //console.log(this.selectedBackground.name);
      this.refreshLayers();
    },
    mouseModus() {
      var viewCanvas = document.querySelector("#olmap-viewer canvas");
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(this.mouseModus) {
        case 'select':
          this.interactionExtent.setActive(true); 
          viewCanvas.classList.add('olmap-viewer-mouseModus-select');
          break;
        default:
          this.interactionExtent.setActive(false);
          this.interactionExtent.createOrUpdatePointerFeature_([NaN, NaN]);
          viewCanvas.classList.add('olmap-viewer-mouseModus-move');
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
    selectedVectordb() {
      this.refreshVectordbLayer();
    }   
  },
  mounted() {
    var self = this;

    document.getElementById('foot-start-1').innerHTML = '?';
    document.getElementById('foot-end-1').innerHTML = '';

    this.globalEventListeners.keydown = e => {
      if (e.keyCode == 17) { // ctrl-key
        switch(this.mouseModus) {
          case 'select':
            break;
          default:
            var viewCanvas = document.querySelector("#olmap-viewer canvas");
            viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
            viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
            viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
            viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
            this.interactionExtent.setActive(true);
            viewCanvas.classList.add('olmap-viewer-mouseModus-select');
            break;
        }
      }
    };
    
    this.globalEventListeners.keyup = e => {
      if (e.keyCode == 17) { // ctrl-key
        switch(this.mouseModus) {
          case 'select':
            break;
          default:
          var viewCanvas = document.querySelector("#olmap-viewer canvas");
          viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
          viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
          viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
          viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
            this.interactionExtent.setActive(false);
            this.interactionExtent.createOrUpdatePointerFeature_([NaN, NaN]);
            viewCanvas.classList.add('olmap-viewer-mouseModus-move');
            break;
        }
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

    var vector_fill = new ol_style.Fill({
      //color: 'rgba(255,255,255,0.15)'
      color: 'rgba(255,255,255,0.0)'
    });
    var vector_stroke = new ol_style.Stroke({
      color: 'rgb(95, 112, 245)',
      width: 2,
      /*lineDash: [1, 5],*/
    });
    var vector_styles = [
      new ol_style.Style({
        image: new ol_style.Circle({
          fill: vector_fill,
          stroke: vector_stroke,
          radius: 5,
        }),
        fill: vector_fill,
        stroke: vector_stroke,
      })
    ];

    this.vectorLayer = new ol_layer.Vector({
      style: vector_styles,
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

    this.olmousePosition = new ol_control_MousePosition({projection: 'EPSG:4326', target: 'foot-end-1', coordinateFormat: function(coordinate) {return toStringXY(coordinate, 2);}});

    this.olmap = new ol_Map({
        target: 'olmap-viewer',
        controls: ol_control.defaults({ attributionOptions: { collapsible: false } }).extend([
          new ol_control.ScaleLine(),
          this.olmousePosition,          
        ]),
        interactions: ol_interaction.defaults().extend([
          this.interactionExtent,
        ]),
      });

    var viewCanvas = document.querySelector("#olmap-viewer canvas");

    viewCanvas.addEventListener('mousedown', e => {
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(self.mouseModus) {
        case 'select':
          viewCanvas.classList.add('olmap-viewer-mouseModus-select-on');
          break;
        default:
          if(e.ctrlKey) {
            this.interactionExtent.setActive(true); 
            viewCanvas.classList.add('olmap-viewer-mouseModus-select-on');
          } else {
            this.interactionExtent.setActive(false);
            this.interactionExtent.createOrUpdatePointerFeature_([NaN, NaN]); 
            viewCanvas.classList.add('olmap-viewer-mouseModus-move-on');
          }
          break;
      }
    });

    viewCanvas.addEventListener('mouseup', e => {
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(self.mouseModus) {
        case 'select':
          viewCanvas.classList.add('olmap-viewer-mouseModus-select');
          break;
        default:          
          if(e.ctrlKey) {
            this.interactionExtent.setActive(true); 
            viewCanvas.classList.add('olmap-viewer-mouseModus-select-on');
          } else {
            this.interactionExtent.setActive(false);
            this.interactionExtent.createOrUpdatePointerFeature_([NaN, NaN]); 
            viewCanvas.classList.add('olmap-viewer-mouseModus-move-on');
          }
          break;
      }
    });
    
    this.olmap.on('movestart', function() {
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(self.mouseModus) {
        case 'select':
          viewCanvas.classList.add('olmap-viewer-mouseModus-select-on');
          break;
        default:
          viewCanvas.classList.add('olmap-viewer-mouseModus-move-on');
          break;
      }
    });

    this.olmap.on('moveend', function() {
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-move-on');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select');
      viewCanvas.classList.remove('olmap-viewer-mouseModus-select-on');
      switch(self.mouseModus) {
        case 'select':
          viewCanvas.classList.add('olmap-viewer-mouseModus-select');
          break;
        default:
          viewCanvas.classList.add('olmap-viewer-mouseModus-move');
          break;
      }
    });

    this.refreshMeta();
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

.innergrid-container {
  display: grid;
  grid-template-columns: max-content auto;
  grid-template-rows: auto;
  position: absolute;
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  bottom: 0px;
  pointer-events: none;
}

.innergrid-item-nav {
  background-color: #e4e5e5;
  padding-right: 0px;
  overflow-y: auto;
  width: 310px;
  border-right-color: #0000001a;
  border-right-width: 1px;
  border-right-style: solid;
  pointer-events: auto;
}

.innergrid-item-main {
  pointer-events: auto;
}

.main {
  position: relative;
}

.background-checkerboard {
  /*background: repeating-linear-gradient( 68grad, #868686, #909090 10px, #9b9999 10px, #8c8c8c 20px );*/

  background: linear-gradient(45deg, rgba(0,0,0,0.0980392) 25%, rgba(0,0,0,0.05) 25%, rgba(0,0,0,0.05) 75%, rgba(0,0,0,0.0980392) 75%, rgba(0,0,0,0.0980392) 0), linear-gradient(45deg, rgba(0,0,0,0.0980392) 25%, rgba(0,0,0,0.05) 25%, rgba(0,0,0,0.05) 75%, rgba(0,0,0,0.0980392) 75%, rgba(0,0,0,0.0980392) 0), rgb(255, 255, 255);
  background-position: 0 0, 10px 10px;
  background-size: 20px 20px;
}

.background-black {
  background-color: black;
}

.background-white {
  background-color: white;
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

</style>
