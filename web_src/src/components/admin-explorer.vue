<template>
  <div>
  <div class="main">
    <div id="olmap-explorer" style="width: 100%; height: 100%;" />
    <div class="innergrid-container" :class="[filter_focus ? 'innergrid-item-nav-fill' : 'innergrid-item-nav-compact']">
      <v-list class="innergrid-item-nav">

        <v-list-tile>
          <multiselect 
            v-model="selectedTags" 
            :options="layer_tags" 
            :show-labels="false" 
            placeholder="filter by tag" 
            :multiple="true" 
            v-if="layer_tags !== undefined" 
            @open="filter_focus = true;" 
            @close="filter_focus = false;"
          />
        </v-list-tile>

        <v-list-tile>
          <v-text-field
            v-model="searchText"
            label="search"
            clearable
            append-icon="search"
            style="display: inline-block;"
          /> 
        </v-list-tile>
        <v-list-tile>
         <v-btn style="display: inline-block;" @click="zoomToEntries()" title="zoom to all (filtered) layers"><v-icon>control_camera</v-icon>zoom fit</v-btn>
         <v-btn style="display: inline-block;" @click="selectAllLayers()" title="select all (filtered) layers, even layers that are not visible on the map"><v-icon>select_all</v-icon>select all</v-btn>
        </v-list-tile>
      </v-list>
    </div>

    <div v-show="Object.keys(messages).length > 0" class="message_box">
      <div v-for="(text, id) in messages" :key="id">
        <ring-loader color="#000000" size="25px" style="display: inline-block;"/>
        {{text}}
      </div>
    </div>

    

    <v-snackbar value="true"  v-for="(text, id) in notifications" :key="id">
      {{text}}<v-btn flat color="pink" @click.native="removeNotification(id)">Close</v-btn>
    </v-snackbar>
    <div class="hoverEntries">
      <b>hovered layers</b>
      <div v-for="e in hoverEntries" :key="e.name">{{e.title === undefined ? e.name : e.title}}</div>
    </div>
    <admin-explorer-selected :entries="selectedEntries" :selectedTags="selectedTags" :searchText="searchText" />
  </div>
  </div>
</template>

<script>

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'
import RingLoader from 'vue-spinner/src/RingLoader.vue'

import adminExplorerSelected from './admin-explorer-selected.vue'

import Vue from 'vue'

import 'ol/ol.css'

import ol_Map from 'ol/Map';
import * as ol_layer from 'ol/layer';
import * as ol_source from 'ol/source';
import ol_View from 'ol/View';
import * as ol_proj from 'ol/proj';
import * as ol_geom from 'ol/geom';
import ol_Feature from 'ol/Feature';
import * as ol_control from 'ol/control'; 
import ol_control_MousePosition from 'ol/control/MousePosition';
import * as ol_style from 'ol/style';
import {toStringXY} from 'ol/coordinate';

import axios from 'axios';

var srcProj = ol_proj.get('EPSG:4326'); // WGS84
var dstProj = ol_proj.get('EPSG:3857'); // Web Mercator

export default {
  name: 'admin-explorer',
  components: {
      RingLoader,
      Multiselect,
      'admin-explorer-selected': adminExplorerSelected,
  },
  data() {
    return {
      messages: {},
      messagesIndex: 0,
      notifications: {},
      notificationsIndex: 0,

      olmap: undefined,
      catalogLayer: undefined,
      catalog: [],
      hoverEntries: [],
      selectedEntries: [],
      selectedTags: [],

      searchText: '',

      filter_focus: false,
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
    refreshCatalog() {
      var self = this;
      var messageID = this.addMessage("loading catalog of layers ...");
      axios.get('../../api/catalog.json')
      .then(function(response) {
        self.catalog = response.data;
        self.removeMessage(messageID);
      })
      .catch(function() {
        self.addnotification('ERROR loading catalog');
        self.removeMessage(messageID);
      });
      /*.finally(function () {
        self.removeMessage(messageID);
      });*/
    },
    refreshCatalogSource(c) {
      var features = [];

      c.forEach(function (entry) {
            var polygon = entry.polygon;
            if (polygon !== undefined) {
              var geometry = new ol_geom.Polygon([polygon]);
              geometry.transform(srcProj, dstProj);
              features.push(new ol_Feature({
                geometry: geometry,
                entry: entry,
              }));
            }
      });

      var catalogSource = new ol_source.Vector({
        features: features,
      });

      this.catalogLayer.setSource(catalogSource);
      },

      zoomToEntries() {
        console.log("zoomToEntries");

        var xmin = +180;
        var ymin = +90;
        var xmax = -180;
        var ymax = -90;

        this.filteredCatalog.forEach(function (entry) {
              var polygon = entry.polygon;
              if (polygon !== undefined) {
                polygon.forEach(function(c){
                  var x = c[0];
                  var y = c[1];
                  if(x < xmin) xmin = x;
                  if(y < ymin) ymin = y;
                  if(xmax < x) xmax = x;
                  if(ymax < y) ymax = y;
                });
              }
        });

        var ext1 = [xmin, ymin, xmax, ymax];
        console.log(ext1);
        var min = ol_proj.fromLonLat([xmin, ymin]);
        var max = ol_proj.fromLonLat([xmax, ymax]);
        var ext = [min[0], min[1], max[0], max[1]];
        console.log(ext);
        this.olmap.getView().fit(ext);

      },

      selectAllLayers() {
        var self = this;
        self.selectedEntries = this.filteredCatalog;
        self.$emit('admin-explorer-selected-show');
      },
  },
  computed: {
    layer_tags() {
      return this.$store.state.layer_tags.data;
    },
    filteredCatalog() {
      return this.catalog.filter(this.filterFun);
    },
    filterFun() {
      var f1 = this.tagFilterFun;
      var f2 = this.searchFilterFun;
      return function(e) {
        return f1(e) && f2(e);
      };
    },
    tagFilterFun() {
      if(this.selectedTags.length === 0) {
        return function() {
          return true;
        }
      }
      
      if(this.selectedTags.length === 1) {
        var selectedTag = this.selectedTags[0];
        return function(e) {
          var tags = e.tags;
          return tags === undefined ? false : tags.indexOf(selectedTag) >= 0;
        }
      }

      var selectedTags = this.selectedTags;
      return function(e) {
        var tags = e.tags;
        return tags === undefined ? false : selectedTags.every(function(selectedTag) {
          return tags.indexOf(selectedTag) >= 0;
        })
      }
    },
    searchFilterFun() {
				var filter = this.searchText;
				if (filter === undefined || filter === null || filter.length === 0) {
					return function () { return true; };
				}
				filter = filter.trim().toLowerCase();
				if (filter == '') {
					return function () { return true; };
				}
				return function (f) { 
          return f.name.toLowerCase().indexOf(filter) >= 0 || (f.title !== undefined && f.title.toLowerCase().indexOf(filter) >= 0); 
        };			
			},
  },
  watch: {
    filteredCatalog() {
      this.refreshCatalogSource(this.filteredCatalog);
    },
  },
  mounted() {
    var self = this;

    document.getElementById('foot-start-1').innerHTML = 'WGS 84/Pseudo-Mercator';
    document.getElementById('foot-end-1').innerHTML = '';

    this.$store.dispatch('layer_tags/init');

    var fill = new ol_style.Fill({
      //color: 'rgba(255,255,255,0.15)'
      color: 'rgba(255,255,255,0.0)'
    });
    var stroke = new ol_style.Stroke({
      color: 'rgb(95, 112, 245)',
      width: 2,
      /*lineDash: [1, 5],*/
    });
    var styles = [
      new ol_style.Style({
        image: new ol_style.Circle({
          fill: fill,
          stroke: stroke,
          radius: 5
        }),
        fill: fill,
        stroke: stroke
      })
    ];

    this.catalogLayer = new ol_layer.Vector({
      style: styles,
    });

    this.olmap = new ol_Map({
        target: 'olmap-explorer',
        layers: [
          new ol_layer.Tile({
            source: new ol_source.OSM(),
          }),
          this.catalogLayer,
        ],
        view: new ol_View({
          center: ol_proj.fromLonLat([11, 20.82]),
          zoom: 3,
        }),
        controls: ol_control.defaults({ attributionOptions: { collapsible: false } }).extend([
          new ol_control.ScaleLine(),
          new ol_control_MousePosition({projection: 'EPSG:4326', target: 'foot-end-1', coordinateFormat: function(coordinate) {return toStringXY(coordinate, 4);}}),
        ]),
      });

    this.refreshCatalog();

    this.olmap.on('pointermove', function (e) {
      var entries = [];      
      var features = self.catalogLayer.getSource().getFeaturesAtCoordinate(e.coordinate);
      features.forEach(function (feature) {
        entries.push(feature.get("entry"));
      });
      self.hoverEntries = entries;
    });

    this.olmap.on('click', function (e) {
      console.log("click");
      var entries = [];      
      var features = self.catalogLayer.getSource().getFeaturesAtCoordinate(e.coordinate);
      features.forEach(function (feature) {
        entries.push(feature.get("entry"));
      });
      self.selectedEntries = entries;
      self.$emit('admin-explorer-selected-show');
    });
  },
  destroyed() {
    document.getElementById('foot-start-1').innerHTML = '?';
    document.getElementById('foot-end-1').innerHTML = '?';
  },
}



</script>

<style scoped>

.main {
  height: 100%;
  position: relative;
}


.innergrid-container {
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  bottom: 0px;
  display: grid;
  grid-template-columns: max-content auto;
  pointer-events: none;
}

.innergrid-item-nav {
  background-color: rgba(222, 222, 222, 0.77);
  padding: 0px;
  overflow-y: auto;
  width: 300px;
  border-right-color: #0000001a;
  border-right-width: 1px;
  border-right-style: solid;
  border-bottom-color: #0000001a;
  border-bottom-width: 1px;
  border-bottom-style: solid;
  pointer-events: auto;
}

.innergrid-item-nav-compact {
  grid-template-rows: minmax(1px, max-content);
}

.innergrid-item-nav-fill {
  grid-template-rows: minmax(1px, 1fr);
}

.message_box {
  position: absolute;
  top: 20px;
  left: 350px;
  background-color: rgb(243, 243, 243);
  padding: 1px;
  border-color: rgb(137, 137, 137);
  border-style: solid;
  border-width: 1px;
  font-size: 1.5em;
}

.hoverEntries {
  position: absolute;
  top: 20px;
  right: 50px;
  background-color: #dee8fb;
  opacity: 0.7;
  pointer-events: none;  
}

.selectedEntries {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #fff;
  width: 100%;
  height: 100%;  
}

</style>

<style>

#olmap-explorer .ol-zoom {
    top: .5em;
    right: .5em;
    left: unset;
}

#olmap-explorer .ol-attribution.ol-unselectable.ol-control.ol-uncollapsible {
  bottom: 0px;
}

</style>
