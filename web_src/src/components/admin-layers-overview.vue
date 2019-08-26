<template>
  <div>
      <h3 class="headline mb-0" style="text-align: center;"> Layers overview </h3>
      <v-divider class="meta-divider"></v-divider>       
      <v-container fluid grid-list-lg>
        <v-layout row wrap>

          <v-flex xs12>
            <v-card>
              <v-card-title primary-title><h3 class="headline mb-0"><v-icon style="font-size: 1em;">collections</v-icon>&nbsp;RasterDB</h3> </v-card-title>
              <v-card-text>
                Layers of this type store spatial <b>pixel data</b> as rasters that are organised in points in time and in bands.
              </v-card-text>
            </v-card>
          </v-flex>

          <v-flex xs12>
            <v-card>
              <v-card-title primary-title><h3 class="headline mb-0"><v-icon style="font-size: 1em;">blur_on</v-icon>&nbsp;PointDB</h3> </v-card-title>
              <v-card-text>
                Layers of this type store spatial point cloud points. Especially suitable for <b>LiDAR data</b>.
              </v-card-text>
            </v-card>
          </v-flex>

          <v-flex xs12>
            <v-card>
              <v-card-title primary-title><h3 class="headline mb-0"><v-icon style="font-size: 1em;">grain</v-icon>&nbsp;Pointcloud</h3> </v-card-title>
              <v-card-text>
                Layers of this type store spatial <b>point cloud points</b>. Points may contain a variety of attribues such as RGB color values.
              </v-card-text>
            </v-card>
          </v-flex>

          <v-flex xs12>
            <v-card>
              <v-card-title primary-title><h3 class="headline mb-0"><v-icon style="font-size: 1em;">category</v-icon>&nbsp;VectorDB</h3> 
              <admin-vectordb-create @created_vectordb="$router.push({path: '/layers/vectordbs/' + $event});" />
              </v-card-title>
              <v-card-text>
                Layers of this type store spatial <b>general vector data</b>.                
              </v-card-text>
            </v-card>
          </v-flex>

          <v-flex xs12>
            <v-card>
              <v-card-title primary-title><h3 class="headline mb-0"><v-icon style="font-size: 1em;">scatter_plot</v-icon>&nbsp;POI group</h3> </v-card-title>
              <v-card-text>
                Groups of this type store vector data: spatial <b>Points</b> with name.
              </v-card-text>
            </v-card>
          </v-flex>

          <v-flex xs12>
            <v-card>
              <v-card-title primary-title><h3 class="headline mb-0"><v-icon style="font-size: 1em;">widgets</v-icon>&nbsp;ROI group</h3> </v-card-title>
              <v-card-text>
                Groups of this type store vector data: <b>Polygons</b> with name and spatial vertex points.
              </v-card-text>
            </v-card>
          </v-flex>

        </v-layout>
      </v-container>
 
  </div>
</template>

<script>

import { mapGetters } from 'vuex'

import adminVectordbCreate from './admin-vectordb-create.vue'

export default {
  name: 'admin-layers-overview',
  components: {
    'admin-vectordb-create': adminVectordbCreate,
  },
  data() {
    return { }    
  },
  methods: {
  },
  computed: {
    ...mapGetters({
            isAdmin: 'identity/isAdmin',
    }),
   rasterdbs() {
      return this.$store.state.rasterdbs.data;
    },
  },
  mounted() {
    this.$store.dispatch('rasterdbs/init');
  },
}

</script>

<style scoped>

.meta-divider {
  margin: 15px;
}

</style>
