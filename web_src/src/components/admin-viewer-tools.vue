<template>
<div>
  <v-btn v-if="meta !== undefined" @click="$emit('tool-raster-export-show')" ><v-icon>cloud_download</v-icon>&nbsp;export</v-btn>
  <span v-show="meta !== undefined && meta.associated !== undefined && (meta.associated.PointDB !== undefined  || meta.associated.pointcloud !== undefined)" style="font-size: 0.8em;">
    ! just visualisation raster !
  </span>
  <div v-show="epsgCode !== undefined">
    <br>
    <hr>
    <v-icon style="font-size: 1em;">category</v-icon><b>Vector Layer</b>
    <div v-if="selectedVectordb !== undefined && selectedVectordb !== null" style="font-size: 0.8em; color: grey;">ID: {{selectedVectordb.name}}</div>
    <multiselect v-if="vectordbs !== undefined" v-model="selectedVectordb" :options="vectordbs" :searchable="true" :show-labels="false" placeholder="pick a vector layer" :allowEmpty="true">
      <template slot="singleLabel" slot-scope="{option}">
        {{option.title === undefined ? option.name : option.title}}
      </template>
      <template slot="option" slot-scope="{option}">
        {{option.title === undefined ? option.name : option.title}}
      </template>
    </multiselect>
    <div v-if="selectedVectordb !== undefined && selectedVectordb !== null">
      <div><a :href="urlPrefix + '../../vectordbs/' + selectedVectordb.name + '/package.zip'" :download="selectedVectordb.name + '.zip'" title="download as ZIP-file"><v-icon color="blue">cloud_download</v-icon>(download vector layer)</a></div>
    </div>
  </div>
  <div v-show="epsgCode === undefined" style="color: #db4c11; padding: 10px; background-color: #0000000d;" title="Set an EPSG-code for this raster-layer to enable feature 'vector overlay'!">
    ! missing EPSG-code !
  </div>

  <div v-if="meta !== undefined && meta.associated !== undefined && meta.associated.PointDB !== undefined">
    <br>
    <hr>
    <v-icon style="font-size: 1em;">blur_on</v-icon><b>Associated PointDB</b>
    <br>
    <v-btn @click="viewPointsPointdb" :disabled="selectedExtent === undefined"><v-icon>3d_rotation</v-icon>&nbsp;&nbsp;view points</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select position)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at center of selection)</span>
    <br>
    <v-btn @click="viewSurfacePointdb" :disabled="selectedExtent === undefined"><v-icon>360</v-icon>&nbsp;&nbsp;view surface</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select position)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at center of selection)</span>
    <br>
    <!--<v-btn @click="exportPointsPointdb" :disabled="selectedExtent === undefined"><v-icon>save_alt</v-icon>&nbsp;&nbsp;export points</v-btn>-->
    <v-btn @click="$emit('tool-point-export-show')" :disabled="selectedExtent === undefined"><v-icon>save_alt</v-icon>&nbsp;&nbsp;export points</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select extent)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at selection)</span>
    <br>
    <!--<v-btn @click="exportSurfacePointdb" :disabled="selectedExtent === undefined"><v-icon>get_app</v-icon>&nbsp;&nbsp;export surface</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select extent)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at selection)</span>
    <br>-->
    <v-btn @click="$emit('tool-point-raster-export-show')" :disabled="selectedExtent === undefined"><v-icon>get_app</v-icon>&nbsp;&nbsp;export surface</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select extent)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at selection)</span>
    <br>
  </div>

  <div v-if="meta !== undefined && meta.associated !== undefined && meta.associated.pointcloud !== undefined">
    <br>
    <hr>
    <v-icon style="font-size: 1em;">grain</v-icon><b>Associated PointCloud</b>
    <br>
    <v-btn @click="viewPointsPointcloud" :disabled="selectedExtent === undefined"><v-icon>3d_rotation</v-icon>&nbsp;&nbsp;view points</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select position)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at center of selection)</span>    
    <br>
    <v-btn @click="viewSurfacePointcloud" :disabled="selectedExtent === undefined"><v-icon>360</v-icon>&nbsp;&nbsp;view surface</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select position)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at center of selection)</span>
    <br>
    <v-btn @click="$emit('tool-point-export-show')" :disabled="selectedExtent === undefined"><v-icon>save_alt</v-icon>&nbsp;&nbsp;export points</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select extent)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at selection)</span>
    <br>
    <v-btn @click="$emit('tool-point-raster-export-show')" :disabled="selectedExtent === undefined"><v-icon>get_app</v-icon>&nbsp;&nbsp;export surface</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select extent)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at selection)</span>    
  </div>

  <div v-if="meta !== undefined && meta.associated !== undefined && meta.associated.voxeldb !== undefined">
    <br>
    <hr>
    <v-icon style="font-size: 1em;">grain</v-icon><b>Associated VoxelDB</b>
    <br>
    <v-btn @click="viewVoxelsVoxelDB" :disabled="selectedExtent === undefined"><v-icon>3d_rotation</v-icon>&nbsp;&nbsp;view voxels</v-btn>
    <span v-show="selectedExtent === undefined" style="font-size: 0.7em;">(select position)</span>
    <span v-show="selectedExtent !== undefined" style="font-size: 0.7em;">(at center of selection)</span>    
    <br>        
  </div>       

</div>

</template>

<script>

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
  name: 'admin-viewer-tools',

  props: ['meta', 'selectedExtent', 'epsgCode', 'currentVectordb', 'currentTimestamp'],

  components: {
    Multiselect,
  },

  data() {
    return {
      selectedVectordb: null,
    };
  },
  methods: {
    viewPointsPointdb() {
      console.log(this.selectedExtent);
      var ext = this.selectedExtent;
      var x = (ext[0] + ext[2]) / 2;
      var y = (ext[1] + ext[3]) / 2;
      var name = this.meta.associated.PointDB;
      var param = 'db=' + name + '&x=' + x +'&y=' + y;
      var url = this.urlPrefix + '../../web/pointcloud_view/pointcloud_view.html#/?' + param;
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },
    viewPointsPointcloud() {
      console.log(this.selectedExtent);
      var ext = this.selectedExtent;
      var x = (ext[0] + ext[2]) / 2;
      var y = (ext[1] + ext[3]) / 2;
      var name = this.meta.associated.pointcloud;
      //var param = 'pointcloud=' + name + '&x=' + x +'&y=' + y;
      //var url = this.urlPrefix + '../../web/pointcloud_view/pointcloud_view.html#/?' + param;      
      let params = {pointcloud: name, x: x, y: y};
      if(this.currentTimestamp !== undefined) {
        params.time_slice_id = this.currentTimestamp;
      }
      const urlParams = new URLSearchParams(params);
      var url = this.urlPrefix + '../../web/pointcloud_view/pointcloud_view.html#/?' + urlParams;
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },
    viewVoxelsVoxelDB() {
      console.log(this.selectedExtent);
      let ext = this.selectedExtent;
      let x = (ext[0] + ext[2]) / 2;
      let y = (ext[1] + ext[3]) / 2;
      let z = -2;
      let voxeldb = this.meta.associated.voxeldb;
      let params = {voxeldb: voxeldb, x: x, y: y, z: z};
      if(this.currentTimestamp !== undefined) {
        params.time_slice_id = this.currentTimestamp;
      }
      const urlParams = new URLSearchParams(params);
      const url = this.urlPrefix + '../../web/voxel_view/?' + urlParams;
      console.log(url);

      //var param = 'pointcloud=' + name + '&x=' + x +'&y=' + y;
      
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },    
    viewSurfacePointdb() {
      console.log(this.selectedExtent);
      var ext = this.selectedExtent;
      var x = (ext[0] + ext[2]) / 2;
      var y = (ext[1] + ext[3]) / 2;
      var name = this.meta.associated.PointDB;
      var param = 'db=' + name + '&x=' + x +'&y=' + y;
      var url = this.urlPrefix + '../../web/height_map_view/height_map_view.html?' + param;
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },
    viewSurfacePointcloud() {
      console.log(this.selectedExtent);
      var ext = this.selectedExtent;
      var x = (ext[0] + ext[2]) / 2;
      var y = (ext[1] + ext[3]) / 2;
      var name = this.meta.associated.pointcloud;
      var param = 'pointcloud=' + name + '&x=' + x +'&y=' + y;
      var url = this.urlPrefix + '../../web/height_map_view/height_map_view.html?' + param;
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },    
    exportPointsPointdb() {
      console.log(this.selectedExtent);
      var ext = this.selectedExtent;
      var name = this.meta.associated.PointDB;
      var param = 'db=' + name + '&modus=ext' + '&xmin=' + ext[0] +'&ymin=' + ext[1] + '&xmax=' + ext[2] +'&ymax=' + ext[3];
      var url = this.urlPrefix + '../../web/point_processing/point_processing.html#/?' + param;
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },
    exportSurfacePointdb() {
      console.log(this.selectedExtent);
      var ext = this.selectedExtent;
      var name = this.meta.associated.PointDB;
      var param = 'db=' + name + '&modus=ext' + '&xmin=' + ext[0] +'&ymin=' + ext[1] + '&xmax=' + ext[2] +'&ymax=' + ext[3];
      var url = this.urlPrefix + '../../web/raster_point_processing/raster_point_processing.html#/?' + param;
      console.log(this.urlPrefix);
      console.log(url);
      window.open(url, '_blank');
    },
    refreshSelectedVectordb() {
      if(this.vectordbs === undefined) {
        this.selectedVectordb = undefined;
        return;
      }
      if(this.currentVectordb === undefined) {
        this.selectedVectordb = null;
        return;
      }
      for (const vectordb of this.vectordbs) {
          if(vectordb.name === this.currentVectordb) {
            this.selectedVectordb = vectordb;
            return;
          }
      }
      this.selectedVectordb = undefined;
    },
  },
  computed: {
    vectordbs() {
      return this.$store.state.vectordbs.data;
    },
    urlPrefix() {
      return this.$store.state.identity.urlPrefix;
    },
  },
  watch: {
    vectordbs() {
      this.refreshSelectedVectordb();
    },
    currentVectordb() {
      this.refreshSelectedVectordb();
    },
    selectedVectordb() {
      this.$emit('selected-vectordb', this.selectedVectordb);     
    },
  },
  mounted() {
    this.$store.dispatch('vectordbs/init');
  },
}

</script>

<style scoped>



</style>
