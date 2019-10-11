<template>
<div>

   <div v-if="rasterdbs !== undefined">
    <v-icon style="font-size: 1em;">collections</v-icon><b>Raster Layer</b>
    <multiselect v-model="selectedRasterdb" :options="rasterdbs" :searchable="true" :show-labels="false" placeholder="pick a layer" :allowEmpty="true">
      <template slot="singleLabel" slot-scope="{option}">
        {{option.title}}
      </template>
      <template slot="option" slot-scope="{option}">
        {{option.title}}
      </template>
    </multiselect>
  </div>

  <div v-if="meta !== undefined && meta.timestamps.length > 0 && (meta.timestamps.length > 1 || meta.timestamps[0].timestamp !== 0)" >
      <b>Time</b>
      <multiselect v-if="meta.timestamps.length > 1" v-model="selectedTimestamp" :options="meta.timestamps" :searchable="true" :show-labels="false" placeholder="pick a time" :allowEmpty="false">
        <template slot="singleLabel" slot-scope="{option}">
          {{option.datetime}}
        </template>
        <template slot="option" slot-scope="{option}">
          {{option.datetime}}
        </template>
      </multiselect>
      <div v-if="meta.timestamps.length === 1">
        {{meta.timestamps[0].datetime}}
      </div>
  </div>

    <div v-if="meta !== undefined && meta.wms.styles.length > 0">
      <b>Raster Visualisation</b>
      <multiselect v-if="meta.wms.styles.length > 1" v-model="selectedProduct" :options="meta.wms.styles" label="title" :searchable="true" :show-labels="false" placeholder="pick a band" :allowEmpty="false">
        <template slot="singleLabel" slot-scope="{option}">
          {{option.name}} - {{option.title}}
        </template>
        <template slot="option" slot-scope="{option}">
          {{option.name}} - {{option.title}}
        </template>
      </multiselect>
      <div v-if="meta.wms.styles.length === 1">
        {{meta.wms.styles[0].name}} - {{meta.wms.styles[0].title}}
      </div>
      <v-slider v-model="selectedLayerWMS_opacity" label="opacity" min="0" max="1" step="0" style="padding-right: 15px; margin-top: 0px;" :hide-details="true" />
    </div>    
</div>

</template>

<script>

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
  name: 'admin-viewer-select',

  props: ['currentRasterdb', 'currentTimestamp', 'currentProduct', 'meta', 'currentLayerWMS_opacity'],

  components: {
    Multiselect,
  },

  data() {
    return {
      selectedRasterdb: undefined,
      selectedProduct: undefined,
      selectedTimestamp: undefined,
      selectedLayerWMS_opacity: 1,      
    }
  },
  methods: {
      refreshSelectedRasterdb() {
      if(this.rasterdbs === undefined) {
        this.selectedRasterdb = undefined;
        return;
      }
      if(this.currentRasterdb === undefined) {
        this.selectedRasterdb = null;
        return;
      }
      for (const rasterdb of this.rasterdbs) {
          if(rasterdb.name === this.currentRasterdb) {
            this.selectedRasterdb = rasterdb;
            return;
          }
      }
      this.selectedRasterdb = undefined;
    },
    refreshSelectedTimestamp() {
      if(this.selectedRasterdb === undefined || this.selectedRasterdb === null || this.meta === undefined || this.meta.name !== this.selectedRasterdb.name || this.meta.timestamps.length === 0) {
        this.selectedTimestamp = undefined;
        return;
      }
      if(this.currentTimestamp === undefined) {
        this.selectedTimestamp = null;
        return;
      }
      for (const timestamp of this.meta.timestamps) {
          if(timestamp.timestamp === this.currentTimestamp) {
            this.selectedTimestamp = timestamp;
            return;
          }
      }
      this.selectedTimestamp = undefined;     
    },
    refreshSelectedProduct() {
      if(this.selectedRasterdb === undefined || this.selectedRasterdb === null || this.meta === undefined || this.meta.name !== this.selectedRasterdb.name || this.meta.wms.styles.length === 0) {
        this.selectedProduct = undefined;
        return;
      }
      if(this.currentProduct === undefined) {
        this.selectedProduct = null;
        return;
      }
      for (const product of this.meta.wms.styles) {
        if(product.name === this.currentProduct) {
          this.selectedProduct = product;
          return;
        }
      }
      this.selectedProduct = undefined;
    },
  },
  computed: {
    rasterdbs() {
      var r = this.$store.state.rasterdbs.data;
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.title.localeCompare(b.title);});
    },
  },
  watch: {
    rasterdbs() {
      this.refreshSelectedRasterdb();
    },
    currentRasterdb() {
      this.refreshSelectedRasterdb();
    },
    selectedRasterdb() {
      this.$emit('selected-rasterdb', this.selectedRasterdb);
      this.refreshSelectedTimestamp();
      this.refreshSelectedProduct();
    },        
    meta() {
      this.refreshSelectedTimestamp();
      this.refreshSelectedProduct();
    },
    currentTimestamp() {
      this.refreshSelectedTimestamp();
    },
    selectedTimestamp() {
      this.$emit('selected-timestamp', this.selectedTimestamp);
    },
    currentProduct() {
      this.refreshSelectedProduct();
    },
    selectedProduct() {
      this.$emit('selected-product', this.selectedProduct);
    },
    currentLayerWMS_opacity() {
      this.selectedLayerWMS_opacity = this.currentLayerWMS_opacity;
    },
    selectedLayerWMS_opacity() {
      this.$emit('selected-layerwms-opacity', this.selectedLayerWMS_opacity);      
    },    
  },
  mounted() {
    this.$store.dispatch('rasterdbs/init');
    this.refreshSelectedRasterdb();
    this.refreshSelectedTimestamp();
    this.refreshSelectedProduct();
  },
}

</script>

<style scoped>

</style>
