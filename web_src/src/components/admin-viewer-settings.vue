<template>
<vue-draggable-resizable :x="boxX" :y="boxY">
  <div class="box">
    <div class="box-header">
      Viewer Settings
      <button class="close-button" @click="$emit('close');">x</button>
    </div>
  <b>Background</b>
  <multiselect v-model="selectedBackground" :options="backgroundOptions" :searchable="true" :show-labels="false" placeholder="pick a background" :allowEmpty="false">
    <template slot="singleLabel" slot-scope="{option}">
      {{option.title}}
    </template>
    <template slot="option" slot-scope="{option}">
      {{option.title}}
    </template>
  </multiselect>

  <b>Connection</b> <button @click="connection_test">conntection test</button>
  <multiselect v-model="selectedFormat" :options="formatOptions" :searchable="true" :show-labels="false" placeholder="pick a format" :allowEmpty="false">
    <template slot="singleLabel" slot-scope="{option}">
      {{option.title}}
    </template>
    <template slot="option" slot-scope="{option}">
      {{option.description}}
    </template>
  </multiselect>

  <div>
    <br>
    <br>
    <hr>
    <b>Raster Visualisation</b>
    <div>
      <b>Gamma</b> &nbsp;&nbsp;&nbsp;<v-checkbox hide-details v-model="syncBands" style="display: inline-block;"/>sync bands
      <multiselect v-model="selectedGamma" :options="gammas" :show-labels="false" :allowEmpty="false" placeholder="gamma correction" />
    </div>
    
    <!--<div v-show="selectedProduct !== undefined && selectedProduct.name !== 'color'">-->
    <div>
      <b>Single Band Mapping</b>
      <multiselect v-model="selectedOneBandMapping" :options="oneBandMappings" :show-labels="false" :allowEmpty="false" placeholder="value to pixel mapping of one band" />
    </div>
  </div>
  </div>
</vue-draggable-resizable>

</template>

<script>

import axios from 'axios'
import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'
import VueDraggableResizable from 'vue-draggable-resizable'

export default {
  name: 'admin-viewer-settings',

  props: [],

  components: {
    Multiselect,
    VueDraggableResizable
  },

  data() {
    return {
      boxX: 0,
      boxY: 0,

      backgroundOptions: [
        {name: "checkerboard", title: "checkerboard"},
        {name: "osm", title: "OpenStreetMap"},
        {name: "OpenTopoMap", title: "OpenTopoMap"},
        {name: "StamenTerrain", title: "Stamen Terrain"},        
        {name: "black", title: "black"},
        {name: "grey", title: "grey"},
        {name: "white", title: "white"},
      ],
      selectedBackground: undefined,     

      formatOptions: [
        {name: "image/png:0", title: "uncompressed ", description: "uncompressed - connection > 200 MBit/s"},
        {name: "image/png", title: "compressed", description: "compressed (default)"},
        {name: "image/jpeg", title: "lossy compressed (no transparency)", description: "lossy compressed (no transparency) - connection < 50 MBit/s"},
      ],
      selectedFormat: undefined,

      gammas: ["auto", "0.1", "0.2", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0"],
      selectedGamma: "auto",
      syncBands: false,
      oneBandMappings: ["grey", "color"],
      selectedOneBandMapping: "grey",
    }
  },
  methods: {
    async connection_test() {
      console.log("connection test start");
      try {
        var tstart = performance.now();
        await axios.get(this.$store.getters.apiUrl('api/connection_test'), {responseType: 'arraybuffer'});
        var tend = performance.now();
        var tduration = tend - tstart;
        var mbps = ((10 * 1000) / tduration).toFixed(0);
        console.log("connection test end " + tduration + "    " + mbps +" MB/s");
      } catch {
        console.log("connection test error");
      }
    }   
  },
  computed: {
  },
  watch: {
    selectedBackground() {
      this.$emit('selected-background', this.selectedBackground);
    },
    selectedFormat() {
      this.$emit('selected-format', this.selectedFormat);
    },
    selectedGamma() {
    this.$emit('selected-gamma', this.selectedGamma);      
    },
    syncBands() {
      this.$emit('sync-bands', this.syncBands);      
    },
    selectedOneBandMapping: {
      immediate: true,
      handler() {
        this.$emit('selected-mapping', this.selectedOneBandMapping);
      }
    },  
  },
  mounted() {
    this.selectedBackground = this.backgroundOptions[2];
    this.selectedFormat = this.formatOptions[1];
  },  
}

</script>

<style scoped>

.box {
  position: relative;
  background-color: rgba(213, 213, 213, 0.91);
  padding: 10px;
  border: solid #0000006e 1px;
  border-radius: 5px;
  min-width: 500px;
}

.box-header {
  text-align: center;
  font-weight: bold;
}

.close-button {
  position: absolute;
  top: 2px;
  right: 2px;
  color: black;
  color: #3e3e3e;
  background-color: #ccc;
  border: solid #0000006e 1px;
  border-radius: 5px;
  padding-left: 5px;
  padding-right: 5px;
}

</style>
