<template>
<div class="innergrid-container">
  <div class="innergrid-item-main">
    <v-layout>
      <v-flex xs12 sm6 offset-sm3>
        <v-card>
          <h1 class="headline mb-0">Remote Sensing Database</h1>
          <v-divider></v-divider>
          <v-card-title>
            <span class="component"><a href="#/layers"><div><img src="images/layers.png" /></div><div class="component-text"><b>Layers</b></div></a></span>
            <span class="component"><a href="#/explorer"><div><img src="images/catalog2.png" /></div><div class="component-text"><b>Map Explorer</b></div></a></span>
            <span class="component"><a href="#/viewer"><div><img src="images/rasterdb_image.png" /></div><div class="component-text"><b>Layer Viewer</b></div></a></span>
            <!--<span class="component"><a href="#/upload"><div><img src="images/upload.png" /></div><div class="component-text">File Upload</div></a></span>-->
            <!--<span class="component"><a :href="old_interface_url" target="_blank"><div><img src="images/old_interface.png" /></div><div class="component-text">old web interface</div></a></span>-->
          </v-card-title>
          <v-divider style="padding-bottom: 100px;"></v-divider>
          <v-divider></v-divider>
          <v-card-title>
            <span class="component"><a :href="indices_processing_url" target="_blank"><div><img src="images/lidar_indices_processing.png" /></div><div class="component-text">LiDAR indices processing</div></a></span>
            <span class="component"><a :href="indices_documentation_url" target="_blank"><div><img src="images/lidar_indices_documentation.png" /></div><div class="component-text">LiDAR indices documentation</div></a></span>
            <span class="component"><a href="https://environmentalinformatics-marburg.github.io/rsdb" target="_blank"><div><img src="images/rsdb_documentation.png" /></div><div class="component-text">RSDB documentation</div></a></span>
            <span class="component"><a href="https://github.com/environmentalinformatics-marburg/rsdb/wiki" target="_blank"><div><img src="images/rsdb_wiki.png" /></div><div class="component-text">RSDB wiki</div></a></span>

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
  computed: {
    old_interface_url() {
      return this.$store.getters.apiUrl('web');
    },
    indices_processing_url() {
      return this.$store.getters.apiUrl('web/lidar_indices/lidar_indices.html');
    },
    indices_documentation_url() {
      return this.$store.getters.apiUrl('web/lidar_indices_description/lidar_indices_description.html');
    },
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
