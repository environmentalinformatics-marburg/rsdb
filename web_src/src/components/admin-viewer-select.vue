<template>
<div>

   <div v-if="rasterdbs !== undefined">
    <v-icon style="font-size: 1em;">collections</v-icon><b>Raster Layer</b>
    <div v-if="selectedRasterdb !== undefined && selectedRasterdb !== null" style="font-size: 0.8em; color: grey;">ID: {{selectedRasterdb.name}}</div>
    <multiselect v-model="selectedRasterdb" :options="rasterdbs" :searchable="true" :show-labels="false" placeholder="pick a layer" :allowEmpty="true">
      <template slot="singleLabel" slot-scope="{option}">
        <div><b>{{option.title}}</b></div>
      </template>
      <template slot="option" slot-scope="{option}">
        {{option.title}}
      </template>
    </multiselect>
  </div>

  <div v-if="meta !== undefined && meta.time_slices.length > 0" >
      <b>Time slice</b>
      <div style="display: flex;">
        <button @click="movePrevSelectedTimeSlice" :disabled="!selectedTimeSliceIndexHasPrev" :class="{hidden: !selectedTimeSliceIndexHasPrev}">◀</button>
        <multiselect v-if="meta.time_slices.length > 1" v-model="selectedTimeSlice" :options="meta.time_slices" :searchable="true" :show-labels="false" placeholder="pick a time slice" :allowEmpty="false" trackBy="id">
          <template slot="singleLabel" slot-scope="{option}">
            {{option.name}}
          </template>
          <template slot="option" slot-scope="{option}">
            {{option.name}}
          </template>
        </multiselect>
        <button @click="moveNextSelectedTimeSlice" :disabled="!selectedTimeSliceIndexHasNext" :class="{hidden: !selectedTimeSliceIndexHasNext}">▶</button>
      </div>
      <div v-if="meta.time_slices.length === 1">
        {{meta.time_slices[0].name}}
      </div>
  </div>

    <div v-if="styles.length > 0">
      <b>Raster Visualisation</b>
      <div style="display: flex;">
        <button @click="movePrevSelectedProduct" :disabled="!selectedProductIndexHasPrev" :class="{hidden: !selectedProductIndexHasPrev}">◀</button>
        <multiselect v-if="styles.length > 1" v-model="selectedProduct" :options="styles" label="title" :searchable="true" :show-labels="false" placeholder="pick a band" :allowEmpty="false" trackBy="name">
          <template slot="singleLabel" slot-scope="{option}">
            {{option.name}} - {{option.title}}
          </template>
          <template slot="option" slot-scope="{option}">
            {{option.name}} - {{option.title}}
          </template>
        </multiselect>
        <button @click="moveNextSelectedProduct" :disabled="!selectedProductIndexHasNext" :class="{hidden: !selectedProductIndexHasNext}">▶</button>
      </div>
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
      selectedTimeSlice: undefined,
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
    refreshSelectedTimeSlice() {
      if(this.selectedRasterdb === undefined || this.selectedRasterdb === null || this.meta === undefined || this.meta.name !== this.selectedRasterdb.name || this.meta.time_slices.length === 0) {
        this.selectedTimeSlice = undefined;
        return;
      }
      if(this.currentTimestamp === undefined) {
        if(this.meta.time_slices.length > 0) {
          this.selectedTimeSlice = this.meta.time_slices[0];
          return;
        } else {
          this.selectedTimeSlice = null;
          return;
        }
      }
      for (const timeSlice of this.meta.time_slices) {
          if(timeSlice.id == this.currentTimestamp) { // number to string compare
            this.selectedTimeSlice = timeSlice;
            return;
          }
      }
      this.selectedTimeSlice = undefined;     
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
      if(this.selectedProduct !== undefined && this.selectedProduct !== null && this.selectedProduct.name === 'custom') {
        var customProduct = {name: this.customProductText, title: 'custom'};
        this.$emit('selected-product', customProduct);
      } else {
        this.$emit('selected-product', this.selectedProduct);
      }
    },
    movePrevSelectedTimeSlice() {
      if(this.selectedTimeSliceIndex !== undefined && this.selectedTimeSliceIndexHasPrev) {
        return this.selectedTimeSlice = this.meta.time_slices[this.selectedTimeSliceIndex - 1];
      }
    },
    moveNextSelectedTimeSlice() {
      if(this.selectedTimeSliceIndex !== undefined && this.selectedTimeSliceIndexHasNext) {
        return this.selectedTimeSlice = this.meta.time_slices[this.selectedTimeSliceIndex + 1];
      }
    },
    movePrevSelectedProduct() {
      if(this.selectedProductIndex !== undefined && this.selectedProductIndexHasPrev) {
        return this.selectedProduct = this.styles[this.selectedProductIndex - 1];
      }
    },
    moveNextSelectedProduct() {
      if(this.selectedProductIndex !== undefined && this.selectedProductIndexHasNext) {
        return this.selectedProduct = this.styles[this.selectedProductIndex + 1];
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
    },
    selectedTimeSliceIndex() {
      if(this.selectedTimeSlice === undefined || this.selectedTimeSlice === null || this.meta === undefined || this.meta.time_slices === undefined || this.meta.time_slices.length < 1) {
        return undefined;
      }
      var i = this.meta.time_slices.findIndex(timeSlice => timeSlice.id === this.selectedTimeSlice.id);
      return (i >= 0) ? i : undefined;
    },
    timeSliceIndexMax() {
      if(this.meta === undefined || this.meta.time_slices === undefined) {
        return undefined;
      }
      return this.meta.time_slices.length - 1;
    },
    selectedTimeSliceIndexHasPrev() {
      if(this.selectedTimeSliceIndex === undefined) {
        return false;
      }
      return this.selectedTimeSliceIndex > 0;
    },
    selectedTimeSliceIndexHasNext() {
      if(this.selectedTimeSliceIndex === undefined) {
        return false;
      }
      return this.selectedTimeSliceIndex < this.timeSliceIndexMax;
    },
    selectedProductIndex() {
      if(this.selectedProduct === undefined || this.selectedProduct === null || this.styles.length < 1) {
        return undefined;
      }
      var i = this.styles.findIndex(style => style.name === this.selectedProduct.name);
      return (i >= 0) ? i : undefined;
    },
    productIndexMax() {
      if(this.styles.length < 1) {
        return undefined;
      }
      return this.styles.length - 1;
    },
    selectedProductIndexHasPrev() {
      if(this.selectedProductIndex === undefined) {
        return false;
      }
      return this.selectedProductIndex > 0;
    },
    selectedProductIndexHasNext() {
      if(this.selectedProductIndex === undefined) {
        return false;
      }
      return this.selectedProductIndex < this.productIndexMax;
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
      this.refreshSelectedTimeSlice();
      this.refreshSelectedProduct();
    },        
    meta() {
      this.refreshSelectedTimeSlice();
      this.refreshSelectedProduct();
    },
    currentTimestamp() {
      this.refreshSelectedTimeSlice();
    },
    selectedTimeSlice() {
      this.$emit('selected-time-slice', this.selectedTimeSlice);
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
    this.refreshSelectedTimeSlice();
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

.hidden {
  visibility: hidden;
}

</style>
