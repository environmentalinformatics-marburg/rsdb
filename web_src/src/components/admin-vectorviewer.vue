<template>
  <div>
  <div class="main">
    <div id="olmap-vectorviewer" style="width: 100%; height: 100%;" />
    <div class="innergrid-container" :class="[filter_focus ? 'innergrid-item-nav-fill' : 'innergrid-item-nav-compact']">
      <v-list class="innergrid-item-nav">

        <v-list-tile>
          <multiselect v-if="vectordbs !== undefined" 
          v-model="selectedVectordb" 
          :options="vectordbs" 
          track-by="name" 
          :allowEmpty="false" 
          :searchable="false" 
          :show-labels="false" 
          placeholder="select vectordb"
          @open="filter_focus = true;" 
          @close="filter_focus = false;">
            <template slot="singleLabel" slot-scope="props">{{props.option.title === undefined ? props.option.name : props.option.title}}</template>
            <template slot="option" slot-scope="props">{{props.option.title === undefined ? props.option.name : props.option.title}}</template>
          </multiselect>
        </v-list-tile>

        <v-list-tile v-if="meta != undefined">
        <v-btn @click="$refs.dialogVectorStyle.show = true;">Style</v-btn>
        <dialog-vector-style 
          ref="dialogVectorStyle" 
          :meta="meta"
          @changed="updateMeta();" 
        />
        </v-list-tile>

        <v-list-tile v-if="selectedVectordb !== undefined && selectedVectordb !== null">
          <input type="checkbox" id="labels" v-model="showLabels">
          <label for="labels">Show Labels</label>
        </v-list-tile>
        <a v-if="selectedVectordb !== undefined && selectedVectordb !== null" :href="urlPrefix + '../../vectordbs/' + selectedVectordb.name + '/package.zip'" :download="selectedVectordb.name + '.zip'" title="download as ZIP-file"><v-icon color="blue">cloud_download</v-icon>(download vector layer)</a>
      </v-list>
            
    </div>

    <v-dialog :value="featureDetailsShow" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Attributes</v-card-title>
        <v-card-text>
        <table v-for="feature in selectedFeatures" :key="feature.getId()">
          <tr v-for="(a, i) in table.attributes" :key="i"><td><b>{{a}}</b></td><td>{{table.data[feature.getId()][i]}}</td></tr>
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
  </div>
</template>

<script>

import { mapState } from 'vuex'

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'
import RingLoader from 'vue-spinner/src/RingLoader.vue'

import Vue from 'vue'

import 'ol/ol.css'

import ol_Map from 'ol/Map';
import * as ol_layer from 'ol/layer';
import * as ol_source from 'ol/source';
import ol_View from 'ol/View';
import * as ol_proj from 'ol/proj';
//import * as ol_geom from 'ol/geom';
//import ol_Feature from 'ol/Feature';
import * as ol_control from 'ol/control'; 
import ol_control_MousePosition from 'ol/control/MousePosition';
import * as ol_style from 'ol/style';
import {toStringXY} from 'ol/coordinate';
import GeoJSON from 'ol/format/GeoJSON';
//import * as ol_interaction from 'ol/interaction';
//import * as ol_geom from 'ol/geom';
//import * as ol_format from 'ol/format';

import axios from 'axios';

import dialogVectorStyle from './dialog-vector-style.vue'

//var srcProj = ol_proj.get('EPSG:4326'); // WGS84
//var dstProj = ol_proj.get('EPSG:3857'); // Web Mercator

var layerComparator = function() {
  var compare = new Intl.Collator().compare;
  return function(a, b) {
    return compare(a.title === undefined ? a.name : a.title, b.title === undefined ? b.name : b.title);
  };
}();

export default {
  name: 'admin-vectorviewer',
  props: ['vectordb'],
  components: {
      RingLoader,    
      Multiselect,
      'dialog-vector-style': dialogVectorStyle,
  },
  data() {
    return {
      messages: {},
      messagesIndex: 0,
      notifications: {},
      notificationsIndex: 0,

      olmap: undefined,
      vectorSource: undefined,
      vectorLayer: undefined,
      vectorLabelLayer: undefined,      
      filter_focus: false,
      vectorWmsLayer: undefined,

      meta: undefined,
      geojson: undefined,
      table:{attibutes: [], data: []},
      selectedVectordb: undefined,
      showLabels: true,
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
    loadGeojson() {
      var self = this;
      var messageID = this.addMessage("loading vector data of layer ...");
      var url = this.$store.getters.apiUrl('vectordbs/' + this.meta.name + '/geometry.json?epsg=3857');
      axios.get(url)
      .then(function(response) {
        self.geojson = response.data;
        self.removeMessage(messageID);
        self.refreshVectorSource();
      })
      .catch(function() {
        self.addnotification('ERROR vector data of layer');
        self.removeMessage(messageID);
      });
    },
    loadTable() {
      var self = this;
      var messageID = this.addMessage("loading table data of layer ...");
      var url = this.$store.getters.apiUrl('vectordbs/' + this.meta.name + '/table.json');
      axios.get(url)
      .then(function(response) {
        self.table = response.data;
        self.removeMessage(messageID);
      })
      .catch(function() {
        self.addnotification('ERROR table data of layer');
        self.removeMessage(messageID);
      });
    },
    refreshVectorSource() {
      var features = this.geojson === undefined ? [] : (new GeoJSON()).readFeatures(this.geojson);
      //console.log(features);

      this.vectorSource = new ol_source.Vector({
        features: features,
      });

      this.vectorLayer.setSource(this.vectorSource);
      this.vectorLabelLayer.setSource(this.vectorSource);

      if(features.length > 0) {
        var ext = this.vectorSource.getExtent();
        this.olmap.getView().fit(ext);
      }
      //console.log("refreshVectorSource done");
    },
    refreshRoute() {
      if(this.vectordbs != undefined) {
        let path = '/vectorviewer';
        if(this.selectedVectordb !== undefined) {
          path += '/' + this.selectedVectordb.name;
        }
        if(path !== this.$router.currentRoute.path) {
            //console.log("change route: " + this.$router.currentRoute.path +"  to  " + path);
            this.$router.push({path: path});
        }
      }
    },
    updateSelectedVectordb() {
      //console.log("updateSelectedVectordb");
      if(this.vectordbs !== undefined && this.vectordb !== undefined) {
        var foundVectordb = this.vectordbs.find(v => {
          return v.name === this.vectordb;
        });
        if(foundVectordb === undefined) {
          this.addnotification("vectordb not found: " + this.vectordb);
          this.selectedVectordb = undefined;
        } else {
          //console.log(this.vectordb);
          //console.log(this.selectedVectordb);
          //console.log(foundVectordb);
          this.selectedVectordb = foundVectordb;          
        }
      }
    },
    async updateMeta() {
      if(this.selectedVectordb !== undefined && this.selectedVectordb !== null) {
        var messageID = this.addMessage("loading table data of layer ...");
        var url = this.$store.getters.apiUrl('vectordbs/' + this.selectedVectordb.name);
        this.meta = undefined;
        try {
          let response = await axios.get(url);
          this.meta = response.data.vectordb;
          this.removeMessage(messageID);
        } catch {        
          this.addnotification('ERROR meta data of layer');
          this.removeMessage(messageID);
        }
      }
    },
  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
    vectordbs() {
      var dbs = this.$store.state.vectordbs.data;
      return dbs === undefined ? undefined : dbs.slice().sort(layerComparator);
    },
    vectordbsError: {
      get() {
        return this.$store.state.vectordbs.mode === 'error' && this.$store.state.vectordbs.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('vectordbs/closeMessage');
        }
      },
    },
    vectordbsErrorMessage() {
      return this.$store.state.vectordbs.message;
    },
    urlPrefix() {
      return this.$store.state.identity.urlPrefix;
    },        
  },
  watch: {
    vectordbs() {
      this.updateSelectedVectordb();
    },
    vectordb: {
      immediate: true,
      handler() {
        //console.log("watch " + this.vectordb);
        this.updateSelectedVectordb();
      }
    },    
    selectedVectordb: {
      immediate: true,
      handler() {
        this.updateMeta();      
        this.refreshRoute();
      }
    },    
    meta() {
      if(this.meta !== undefined) {
        this.loadGeojson();
        this.loadTable();
        let vectorWmsSource = new ol_source.ImageWMS({
          url: this.$store.getters.apiUrl('vectordbs/' + this.meta.name + '/wms'),
          ratio: 1,
          params: {
            rev: Date.now(),
          },
        });
        this.vectorWmsLayer.setSource(vectorWmsSource);
      } else {
        this.geojson = undefined;
        this.table = {attibutes: [], data: []};
        this.refreshVectorSource();
        this.vectorWmsLayer.setSource(undefined);
      }
    },
    showLabels() {
      this.vectorLabelLayer.setVisible(this.showLabels);
    },   
  },
  mounted() {
    var self = this;
    this.$store.dispatch('vectordbs/init');

    document.getElementById('foot-start-1').innerHTML = 'WGS 84/Pseudo-Mercator';
    document.getElementById('foot-end-1').innerHTML = '';

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
      //renderMode: 'image',
    });

    var vectorLabelStyle = new ol_style.Style({
      text: new ol_style.Text({
        font: '16px sans-serif',
        fill: new ol_style.Fill({
          color: 'rgba(0, 0, 0, 1)',
        }),
        stroke: new ol_style.Stroke({
          color: 'rgba(255, 255, 255, 0.8)',
          width: 3,
        }),
        overflow: true,
      }),
    });

     this.vectorLabelLayer = new ol_layer.VectorImage({
      style: feature => {
        vectorLabelStyle.getText().setText(feature.get(this.meta.name_attribute));
        return vectorLabelStyle;
      },     
      declutter: true,
      renderMode: 'image',
    });

    //var backgroundSourceUrl = this.$store.getters.apiUrl('api/proxy/{z}/{x}/{y}.png');
    //var backgroundSourceUrl = this.$store.getters.apiUrl('api/mbtiles/satellite/{z}/{x}/{-y}');

    var backgroundSource = new ol_source.OSM({
      //url: backgroundSourceUrl,
    });

    /*var backgroundSource = new ol_source.VectorTile({
      format: new ol_format.MVT(),
      url: backgroundSourceUrl,
    });*/

    this.vectorWmsLayer = new ol_layer.Image({
    });

    self.olmap = new ol_Map({
        target: 'olmap-vectorviewer',
        layers: [
          new ol_layer.Tile({
            source: backgroundSource,
          }),
          /*new ol_layer.VectorTile({
            source: backgroundSource,
          }),*/
          this.vectorWmsLayer,
          //this.vectorLayer,
          this.vectorLabelLayer,
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

    /*var selectSingleClick = new ol_interaction.Select();
      self.olmap.addInteraction(selectSingleClick); 
    selectSingleClick.on('select', function(e) {
        console.log(e.selected);
    });*/

    self.olmap.on('click', function(e) {
      var feature = self.vectorSource.getClosestFeatureToCoordinate(e.coordinate);
      if(feature !== null) {
        //console.log("clicked " + feature.getId()+"  ");
        self.selectedFeatures = [feature];
        self.featureDetailsShow = true;
      }
    });

    this.refreshVectorSource();
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

</style>

<style>

#olmap-vectorviewer .ol-zoom {
    top: .5em;
    right: .5em;
    left: unset;
}

#olmap-vectorviewer .ol-attribution.ol-unselectable.ol-control.ol-uncollapsible {
  bottom: 0px;
}

</style>
