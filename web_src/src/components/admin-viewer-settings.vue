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
      {{option.title}}
    </template>
  </multiselect>
  
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
        {name: "image/png:0", title: "lossless-uncompressed - net > 200 MBit/s"},
        {name: "image/png", title: "lossless-compressed (default)"},
        {name: "image/jpeg", title: "lossy (no transparency) - net < 50 MBit/s"},
      ],
      selectedFormat: undefined,
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
  },
  mounted() {
    this.selectedBackground = this.backgroundOptions[2];
    this.selectedFormat = this.formatOptions[1];
  },
}

</script>

<style scoped>

</style>
