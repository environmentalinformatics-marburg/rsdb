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

    <b>Connection</b> <button @click="connection_test" class="button-connection-test">|conntection test|</button> {{connectionSpeedMessage}}
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
      <b>Raster visualisation</b>

      <div>
        <b>Value range</b> &nbsp;&nbsp;&nbsp;
        <v-checkbox hide-details v-model="autoValueRange" style="display: inline-block;"/><span :class="{disabled: !autoValueRange}">auto</span> &nbsp;&nbsp;&nbsp;
        <span :class="{disabled: autoValueRange}">
          min: <input type="text" id="name" name="name" maxlength="8" size="10" class="text-input" :disabled="autoValueRange" placeholder="minimum" v-model="valueRangeMinText" :class="{'text-input-invalid': !autoValueRange && isNaN(valueRangeMin)}"/>
          max: <input type="text" id="name" name="name" maxlength="8" size="10" class="text-input" :disabled="autoValueRange" placeholder="maximum" v-model="valueRangeMaxText" :class="{'text-input-invalid': !autoValueRange && isNaN(valueRangeMax)}"/>
        </span> 
      </div>

      <div>
        <b>Gamma</b> &nbsp;&nbsp;&nbsp;<v-checkbox hide-details v-model="syncBands" style="display: inline-block;"/>sync bands
        <multiselect v-model="selectedGamma" :options="gammas" :show-labels="false" :allowEmpty="false" placeholder="gamma correction" />
      </div>
      
      <!--<div v-show="selectedProduct !== undefined && selectedProduct.name !== 'color'">-->
      <div>
        <b>Single band mapping</b>
        <multiselect v-model="selectedOneBandMapping" :options="oneBandMappings" :show-labels="false" :allowEmpty="false" placeholder="value to pixel mapping of one band" />
      </div>





    </div>

    <div>
      <br>
      <br>
      <hr>
      <b>Vector overlay</b>
      <div>
        <v-checkbox hide-details v-model="showLabels" style="display: inline-block;"/>Show vector feature labels
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

      connectionSpeedMessage: "(not measured)",

      gammas: ["auto", "0.1", "0.2", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0"],
      selectedGamma: "auto",
      syncBands: false,
      oneBandMappings: ["grey", "inferno", "viridis", "jet", "cividis"],
      selectedOneBandMapping: "grey",

      showLabels: true,

      autoValueRange: true,
      valueRangeMinText: "0",
      valueRangeMaxText: "255",
    }
  },
  methods: {
    async connection_test() {
      console.log("connection test start");
      this.connectionSpeedMessage = "measuring...";
      try {
        var tstart = performance.now();
        await axios.get(this.$store.getters.apiUrl('api/connection_test'), {responseType: 'arraybuffer'});
        var tend = performance.now();
        var tduration = tend - tstart;
        var mBps = ((20 * 1000) / tduration).toFixed(0);
        var mbps = ((20 * 1000 * 8) / tduration).toFixed(0);
        this.connectionSpeedMessage = "measured " + mbps +" Mbit/s (" + mBps + " MByte/s)";
      } catch {
        console.log("connection test error");
        this.connectionSpeedMessage = "measuring error";
      }
    },
    isNumber(v) {
      return v !== undefined && v !== '' && !isNaN(v);
    },
    parseNumber(v) {
      return (+v);
    },    
  },
  computed: {
    valueRangeMin() {
      return this.autoValueRange ? NaN : this.isNumber(this.valueRangeMinText) ? this.parseNumber(this.valueRangeMinText) : NaN;
    },
    valueRangeMax() {
      return this.autoValueRange ? NaN : this.isNumber(this.valueRangeMaxText) ? this.parseNumber(this.valueRangeMaxText) : NaN;
    },
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
    showLabels() {
      this.$emit('show-labels', this.showLabels);      
    },
    selectedOneBandMapping: {
      immediate: true,
      handler() {
        this.$emit('selected-mapping', this.selectedOneBandMapping);
      }
    },
    valueRangeMin() {
      this.$emit('value-range-min', this.valueRangeMin);      
    },
    valueRangeMax() {
      this.$emit('value-range-max', this.valueRangeMax);      
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
  pointer-events: auto;
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

.button-connection-test {
  background-color: #847c7375;
  border-style: solid;
  border-color: #00000038;
  border-width: 1px;
  margin: 5px;
  border-radius: 5px;
}

.text-input {
  border-style: solid;
}

.text-input-invalid {
  color: red;
}

.disabled {
  color: grey;
}

</style>
