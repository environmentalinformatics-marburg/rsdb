<template>
<div class="innergrid-container">
  <div class="innergrid-item-main">
    <v-layout>
      <v-flex xs12 sm6 offset-sm3>
        <v-card>
          <h1 class="headline mb-0">Remote Sensing Database</h1>
          <v-divider></v-divider>
          <v-card-title>
            <span class="component"><a href="#/layers"><div><img src="images/layers.png" /></div><div class="component-text">Layers</div></a></span>
            <span class="component"><a href="#/explorer"><div><img src="images/catalog2.png" /></div><div class="component-text">Map Explorer</div></a></span>
            <span class="component"><a href="#/viewer"><div><img src="images/rasterdb_image.png" /></div><div class="component-text">Layer Viewer</div></a></span>
            <span class="component"><a href="#/upload"><div><img src="images/upload.png" /></div><div class="component-text">File Upload</div></a></span>
          </v-card-title>
        </v-card>
      </v-flex>
    </v-layout>
  </div>
</div>
</template>

<script>

export default {
  name: 'admin-overview',
  data() {
    return {
    }
  },
  methods: {
  },
  mounted() {
  },
}



</script>

<style scoped>

.innergrid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
  background-image: url("../images/background.png");
  background-repeat: repeat;  
}

.innergrid-item-main {
  padding: 0;
  overflow-y: auto;
}

h1 {
  font-family: serif;
  font-weight: bold;
  font-variant: small-caps;
  color: rgba(0, 0, 0, 0.7);
  text-align: center;
}

.component {
  padding: 10px;
}

.component-text {
  text-align: center;
}

</style>
