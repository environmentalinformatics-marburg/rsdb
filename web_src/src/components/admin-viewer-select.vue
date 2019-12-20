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

    <div v-if="styles.length > 0">
      <b>Raster Visualisation</b>
      <multiselect v-if="styles.length > 1" v-model="selectedProduct" :options="styles" label="title" :searchable="true" :show-labels="false" placeholder="pick a band" :allowEmpty="false">
        <template slot="singleLabel" slot-scope="{option}">
          {{option.name}} - {{option.title}}
        </template>
        <template slot="option" slot-scope="{option}">
          {{option.name}} - {{option.title}}
        </template>
      </multiselect>
      <div v-if="selectedProduct !== undefined && selectedProduct !== null &&selectedProduct.name === 'custom'">
        custom: 
        <input type="text" id="name" name="name" class="text-input" placeholder="type text, e.g. [b1,b2,b3]" v-model="customProductText" title="e.g. for RGB visualisation with bands 1,3,5 type: [b1,b3,b5]" />
      </div>      
      <div v-if="styles.length === 1">
        {{styles[0].name}} - {{styles[0].title}}
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
      customProductText: undefined,      
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
      if(this.selectedRasterdb === undefined || this.selectedRasterdb === null || this.meta === undefined || this.meta.name !== this.selectedRasterdb.name || this.styles.length === 0) {
        this.selectedProduct = undefined;
        return;
      }
      if(this.currentProduct === undefined) {
        this.selectedProduct = null;
        return;
      }
      for (const product of this.styles) {
        if(product.name === this.currentProduct) {
          this.selectedProduct = product;
          return;
        }
      }
      this.selectedProduct = undefined;
    },
    emitSelectedProduct() {
      if(this.selectedProduct !== undefined && this.selectedProduct !== null &&this.selectedProduct.name === 'custom') {
        var customProduct = {name: this.customProductText, title: 'custom'};
        this.$emit('selected-product', customProduct);
      } else {
        this.$emit('selected-product', this.selectedProduct);
      }
    },
  },
  computed: {
    rasterdbs() {
      var r = this.$store.state.rasterdbs.data;
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.title.localeCompare(b.title);});
    },
    styles() {
      if(this.meta === undefined) {
        return [];
      }
      return this.meta.wms.styles.concat({name: 'custom', title: 'user defined'});
    }
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
      this.emitSelectedProduct();
    },
    customProductText() {
      this.emitSelectedProduct();
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

.text-input {
  border-style: solid;
  border-color: #00000054;
  border-width: 1px;
}

</style>
