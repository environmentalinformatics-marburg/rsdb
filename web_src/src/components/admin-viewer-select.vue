<template>
<div>

   <div v-if="rasterdbs !== undefined">
    <v-icon style="font-size: 1em;">collections</v-icon><b>Raster Layer</b>
    <multiselect v-model="selectedRasterdb" :options="rasterdbs" :searchable="true" :show-labels="false" placeholder="pick a layer" :allowEmpty="false">
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
      //console.log("refreshSelectedRasterdb");
      if(this.rasterdbs === undefined || this.rasterdbs.length === 0) {
        this.selectedRasterdb = undefined;
      } else if(this.currentRasterdb === undefined) {
        this.selectedRasterdb = this.rasterdbs[0];
      } else {
        for (const rasterdb of this.rasterdbs) {
          if(rasterdb.name === this.currentRasterdb) {
            this.selectedRasterdb = rasterdb;
          }
        }
      }     
    },
    refreshSelectedTimestamp() {
      //console.log("refreshSelectedTimestamp");
      if(this.meta === undefined || this.meta.timestamps.length === 0) {
        this.selectedTimestamp = undefined;
      } else if(this.currentTimestamp === undefined) {
        this.selectedTimestamp = this.meta.timestamps[0];
      } else {
        var av = false;
        for (const timestamp of this.meta.timestamps) {
          if(timestamp.timestamp === this.currentTimestamp) {
            this.selectedTimestamp = timestamp;
            av = true;
            break;
          }
        }
        if(!av) {
          this.selectedTimestamp = this.meta.timestamps[0];
        }
      }     
    },
    refreshSelectedProduct() {
      //console.log("refreshSelectedProduct");
      if(this.meta === undefined || this.meta.wms.styles.length === 0) {
        this.selectedProduct = undefined;
      } else if(this.currentProduct === undefined) {
        this.selectedProduct = this.meta.wms.styles[0];
      } else {
        var selectedProduct = undefined;
        for (const product of this.meta.wms.styles) {
          if(product.name === this.currentProduct) {
            selectedProduct = product;
          }
        }
        this.selectedProduct = selectedProduct === undefined ? this.meta.wms.styles[0] : selectedProduct;
      }     
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
    meta() {
      this.refreshSelectedTimestamp();
      this.refreshSelectedProduct();
    },
    currentRasterdb() {
      this.refreshSelectedRasterdb();
    },
    currentTimestamp() {
      this.refreshSelectedTimestamp();
    },
    currentProduct() {
      this.refreshSelectedProduct();
    },
    selectedRasterdb() {
      this.$emit('selected-rasterdb', this.selectedRasterdb);
      this.refreshSelectedTimestamp();
      this.refreshSelectedProduct();
    },
    selectedTimestamp() {
      this.$emit('selected-timestamp', this.selectedTimestamp);
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
