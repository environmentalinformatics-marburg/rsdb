<template>
<div>

  <b>Background</b>
  <multiselect v-model="selectedBackground" :options="backgroundOptions" :searchable="true" :show-labels="false" placeholder="pick a background" :allowEmpty="false">
    <template slot="singleLabel" slot-scope="{option}">
      {{option.title}}
    </template>
    <template slot="option" slot-scope="{option}">
      {{option.title}}
    </template>
  </multiselect>

  <b>Connection</b>
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

</template>

<script>

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
  name: 'admin-viewer-settings',

  props: [],

  components: {
    Multiselect,
  },

  data() {
    return {
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

</style>
