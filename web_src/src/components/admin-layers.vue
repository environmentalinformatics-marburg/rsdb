<template>

<splitpanes class="rsdb-theme">
  <pane min-size="10" size="20" class="split-nav">
    <v-list dense class="split-nav-list">
      <v-list-tile v-for="item in items" :key="item.name" @click="navigation = item.name;" :class="{activeNavigation: navigation === item.name}" :to="'/layers' + '/' + item.name" replace>
        <v-list-tile-action>
          <v-icon>{{ item.icon }}</v-icon>
        </v-list-tile-action>
        <v-list-tile-content>
          <v-list-tile-title>{{ item.title }}</v-list-tile-title>
        </v-list-tile-content>
      </v-list-tile>
      <v-divider />
      <v-list-tile>
        <v-btn flat icon color="green" @click="refreshLayers()" title="refresh layers">
          <v-icon>refresh</v-icon>
        </v-btn>
        Layers
      </v-list-tile>

      <multiselect v-model="selectedTags" :options="layer_tags" :show-labels="false" placeholder="filter by tag" :multiple="true" v-if="layer_tags !== undefined" />

      <v-text-field
        v-model="searchText"
        label="search"
        clearable
        append-icon="search"        
      />

      <div v-if="filteredRasterdbs.length === 0 && filteredPointdbs.length === 0 && filteredPointclouds.length === 0 && filteredVoxeldbs.length === 0 && filteredPoi_groups.length === 0 && filteredRoi_groups.length === 0">
        no layers
      </div>

      <v-list-group v-if="filteredRasterdbs.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">collections</v-icon>&nbsp;<b>RasterDBs</b> ({{filteredRasterdbs.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>   
        <v-list-tile v-for="item in filteredRasterdbs" :key="item.name" @click="navigation = 'rasterdbs/' + item.name;" :class="{activeNavigation: navigation === 'rasterdbs/' + item.name}" :to="'/layers/rasterdbs/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>collections</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>        
      </v-list-group>

      <v-list-group v-if="filteredPointdbs.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">blur_on</v-icon>&nbsp;<b>PointDBs</b> ({{filteredPointdbs.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-for="item in filteredPointdbs" :key="item.name" @click="navigation = 'pointdbs/' + item.name;" :class="{activeNavigation: navigation === 'pointdbs/' + item.name}" :to="'/layers/pointdbs/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>blur_on</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list-group>

      <v-list-group v-if="filteredPointclouds.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">grain</v-icon>&nbsp;<b>Pointclouds</b> ({{filteredPointclouds.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-for="item in filteredPointclouds" :key="item.name" @click="navigation = 'pointclouds/' + item.name;" :class="{activeNavigation: navigation === 'pointclouds/' + item.name}" :to="'/layers/pointclouds/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>grain</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list-group>

      <v-list-group v-if="filteredVoxeldbs.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">view_module</v-icon>&nbsp;<b>Voxeldbs</b> ({{filteredVoxeldbs.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-for="item in filteredVoxeldbs" :key="item.name" @click="navigation = 'voxeldbs/' + item.name;" :class="{activeNavigation: navigation === 'voxeldbs/' + item.name}" :to="'/layers/voxeldbs/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>view_module</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list-group>      

      <v-list-group v-if="filteredVectordbs.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">category</v-icon>&nbsp;<b>Vectordbs</b> ({{filteredVectordbs.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-for="item in filteredVectordbs" :key="item.name" @click="navigation = 'vectordbs/' + item.name;" :class="{activeNavigation: navigation === 'vectordbs/' + item.name}" :to="'/layers/vectordbs/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>category</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list-group> 

      <v-list-group v-if="filteredPoi_groups.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">scatter_plot</v-icon>&nbsp;<b>POI groups</b> ({{filteredPoi_groups.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-for="item in filteredPoi_groups" :key="item.name" @click="navigation = 'poi_groups/' + item.name;" :class="{activeNavigation: navigation === 'poi_groups/' + item.name}" :to="'/layers/poi_groups/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>scatter_plot</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined && item.title !== '' ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list-group>

      <v-list-group v-if="filteredRoi_groups.length !== 0">
        <v-list-tile slot="activator">
            <v-list-tile-content>
              <v-list-tile-title><v-icon style="font-size: 1em;">widgets</v-icon>&nbsp;<b>ROI groups</b> ({{filteredRoi_groups.length}})</v-list-tile-title>
            </v-list-tile-content>
        </v-list-tile>
        <v-list-tile v-for="item in filteredRoi_groups" :key="item.name" @click="navigation = 'roi_groups/' + item.name;" :class="{activeNavigation: navigation === 'roi_groups/' + item.name}" :to="'/layers/roi_groups/' + item.name" replace>
          <v-list-tile-action>
            <v-icon>widgets</v-icon>
          </v-list-tile-action>
          <v-list-tile-content>
            <v-list-tile-title>{{ item.title !== undefined  && item.title !== '' ? item.title : item.name }}</v-list-tile-title>
          </v-list-tile-content>
        </v-list-tile>
      </v-list-group>


      <v-list-tile v-if="rasterdbsMode === 'init' || rasterdbsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading rasterdbs...
      </v-list-tile>
      <v-list-tile v-if="rasterdbsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load rasterdbs.
      </v-list-tile>

      <v-list-tile v-if="pointdbsMode === 'init' || pointdbsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading pointdbs...
      </v-list-tile>
      <v-list-tile v-if="pointdbsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load pointdbs.
      </v-list-tile>

      <v-list-tile v-if="pointcloudsMode === 'init' || pointcloudsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading pointclouds...
      </v-list-tile>
      <v-list-tile v-if="pointcloudsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load pointclouds.
      </v-list-tile>

      <v-list-tile v-if="voxeldbsMode === 'init' || voxeldbsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading voxeldbs...
      </v-list-tile>
      <v-list-tile v-if="voxeldbsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load voxeldbs.
      </v-list-tile>      

      <v-list-tile v-if="vectordbsMode === 'init' || vectordbsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading vectordbs...
      </v-list-tile>
      <v-list-tile v-if="vectordbsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load vectordbs.
      </v-list-tile>

      <v-list-tile v-if="poi_groupsMode === 'init' || poi_groupsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading poi groups...
      </v-list-tile>
      <v-list-tile v-if="poi_groupsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load poi groups.
      </v-list-tile> 

      <v-list-tile v-if="roi_groupsMode === 'init' || roi_groupsMode === 'load'">
        <ring-loader color="#000000" size="20px" />
        loading roi groups...
      </v-list-tile>
      <v-list-tile v-if="roi_groupsMode === 'error'">
        <v-icon>error</v-icon>
        Could not load roi groups.
      </v-list-tile>      
    </v-list>
  </pane>

  <pane min-size="10" class="split-main">
    <router-view />
  </pane>

  <v-snackbar v-model="rasterdbsError" :top="true">
    {{rasterdbsErrorMessage}}
    <v-btn flat class="pink--text" @click.native="rasterdbsError = false;">Close</v-btn>
  </v-snackbar>
</splitpanes>

</template>

<script>

import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'
import RingLoader from 'vue-spinner/src/RingLoader.vue'

var layerComparator = function() {
  var compare = new Intl.Collator(undefined, {numeric: true}).compare;
  return function(a, b) {
    return compare(a.title === undefined || a.title === '' ? a.name : a.title, b.title === undefined || b.title === '' ? b.name : b.title);
  };
}();

export default {
  name: 'admin-splitlayers',
  components: {
    Splitpanes,
    Pane,
    RingLoader,
    Multiselect,    
  },
  data() {
    return {
      navigation: "overview",
      selectedTags: [],
      searchText: undefined,
    }
  },
  methods: {
    refreshLayers() {
      this.$store.dispatch('rasterdbs/refresh');
      this.$store.dispatch('pointdbs/refresh');
      this.$store.dispatch('pointclouds/refresh');
      this.$store.dispatch('voxeldbs/refresh');
      this.$store.dispatch('vectordbs/refresh');
      this.$store.dispatch('poi_groups/refresh');
      this.$store.dispatch('roi_groups/refresh');
      this.$store.dispatch('layer_tags/refresh');
    },
  },
  computed: {
    items() {
      var elements = [];
      elements.push({ name: 'overview', title: 'overview', icon: 'apps' });
      return elements;
    },

    layer_tags() {
      return this.$store.state.layer_tags.data;
    },

    filterFun() {
      var f1 = this.tagFilterFun;
      var f2 = this.searchFilterFun;
      return function(e) {
        return f1(e) && f2(e);
      };
    },

    tagFilterFun() {
      if(this.selectedTags.length === 0) {
        return function() {
          return true;
        }
      }
      
      if(this.selectedTags.length === 1) {
        var selectedTag = this.selectedTags[0];
        return function(e) {
          var tags = e.tags;
          return tags === undefined ? false : tags.indexOf(selectedTag) >= 0;
        }
      }

      var selectedTags = this.selectedTags;
      return function(e) {
        var tags = e.tags;
        return tags === undefined ? false : selectedTags.every(function(selectedTag) {
          return tags.indexOf(selectedTag) >= 0;
        })
      }
    },

    searchFilterFun() {
				var filter = this.searchText;
				if (filter === undefined || filter === null || filter.length === 0) {
					return function () { return true; };
				}
				filter = filter.trim().toLowerCase();
				if (filter == '') {
					return function () { return true; };
				}
				return function (f) { 
          return f.name.toLowerCase().indexOf(filter) >= 0 || (f.title !== undefined && f.title.toLowerCase().indexOf(filter) >= 0); 
        };			
		},
    
    filteredRasterdbs() {
      return this.rasterdbs === undefined ? [] : this.rasterdbs.filter(this.filterFun).sort(layerComparator);
    },
    rasterdbs() {
      return this.$store.state.rasterdbs.data;
    },
    rasterdbsMode() {
      return this.$store.state.rasterdbs.mode;
    },
    rasterdbsError: {
      get() {
        return this.$store.state.rasterdbs.mode === 'error' && this.$store.state.rasterdbs.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('rasterdbs/closeMessage');
        }
      },
    },
    rasterdbsErrorMessage() {
      return this.$store.state.rasterdbs.message;
    },
    
    filteredPointdbs() {
      return this.pointdbs === undefined ? [] : this.pointdbs.filter(this.filterFun).sort(layerComparator);
    },
    pointdbs() {
      return this.$store.state.pointdbs.data;
    },
    pointdbsMode() {
      return this.$store.state.pointdbs.mode;
    },
    pointdbsError: {
      get() {
        return this.$store.state.pointdbs.mode === 'error' && this.$store.state.pointdbs.messageActive; 
      }, 
      set(v) {
        if(v == false) {
          this.$store.commit('pointdbs/closeMessage');
        }
      },
    },
    pointdbsErrorMessage() {
      return this.$store.state.pointdbs.message;
    },

    filteredPointclouds() {
      return this.pointclouds === undefined ? [] : this.pointclouds.filter(this.filterFun).sort(layerComparator);
    },    
    pointclouds() {
      return this.$store.state.pointclouds.data;
    },
    pointcloudsMode() {
      return this.$store.state.pointclouds.mode;
    },
    pointcloudsError: {
      get() {
        return this.$store.state.pointclouds.mode === 'error' && this.$store.state.pointclouds.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('pointclouds/closeMessage');
        }
      },
    },
    pointcloudsErrorMessage() {
      return this.$store.state.pointclouds.message;
    },

    filteredVoxeldbs() {
      return this.voxeldbs === undefined ? [] : this.voxeldbs.filter(this.filterFun).sort(layerComparator);
    },    
    voxeldbs() {
      return this.$store.state.voxeldbs.data;
    },
    voxeldbsMode() {
      return this.$store.state.voxeldbs.mode;
    },
    voxeldbsError: {
      get() {
        return this.$store.state.voxeldbs.mode === 'error' && this.$store.state.voxeldbs.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('voxeldbs/closeMessage');
        }
      },
    },
    voxeldbsErrorMessage() {
      return this.$store.state.voxeldbs.message;
    },

    filteredVectordbs() {
      return this.vectordbs === undefined ? [] : this.vectordbs.filter(this.filterFun).sort(layerComparator);
    },    
    vectordbs() {
      return this.$store.state.vectordbs.data;
    },
    vectordbsMode() {
      return this.$store.state.vectordbs.mode;
    },
    vectordbsError: {
      get() {
        return this.$store.state.vectordbs.mode === 'error' && this.$store.state.vectordbs.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('vectordbs/closeMessage');
        }
      },
    },
    vectordbsErrorMessage() {
      return this.$store.state.vectordbs.message;
    },    

    filteredPoi_groups() {
      return this.poi_groups === undefined ? [] : this.poi_groups.filter(this.filterFun).sort(layerComparator);
    },
    poi_groups() {
      return this.$store.state.poi_groups.data;
    },
    poi_groupsMode() {
      return this.$store.state.poi_groups.mode;
    },
    poi_groupsError: {
      get() {
        return this.$store.state.poi_groups.mode === 'error' && this.$store.state.poi_groups.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('poi_groups/closeMessage');
        }
      },
    },
    poi_groupsErrorMessage() {
      return this.$store.state.poi_groups.message;
    },

    filteredRoi_groups() {
      return this.roi_groups === undefined ? [] : this.roi_groups.filter(this.filterFun).sort(layerComparator);
    },
    roi_groups() {
      return this.$store.state.roi_groups.data;
    },
    roi_groupsMode() {
      return this.$store.state.roi_groups.mode;
    },
    roi_groupsError: {
      get() {
        return this.$store.state.roi_groups.mode === 'error' && this.$store.state.roi_groups.messageActive; 
      },
      set(v) {
        if(v == false) {
          this.$store.commit('roi_groups/closeMessage');
        }
      },
    },
    roi_groupsErrorMessage() {
      return this.$store.state.roi_groups.message;
    },
  },
  mounted() {
    this.$store.dispatch('rasterdbs/init');
    this.$store.dispatch('pointdbs/init');
    this.$store.dispatch('pointclouds/init');
    this.$store.dispatch('voxeldbs/init');
    this.$store.dispatch('vectordbs/init');
    this.$store.dispatch('poi_groups/init');
    this.$store.dispatch('roi_groups/init');
    this.$store.dispatch('layer_tags/init');
  },
}

</script>

<style scoped>

.split-nav {
  overflow-y: auto;
}

.split-nav-list {
  background-color: #0000001c;
}

.split-main {
  overflow-y: auto;
  padding: 15px;
}

.activeNavigation {
  background-color: #009cff33;
}

</style>

<style>

.splitpanes.rsdb-theme .splitpanes__pane {
  background-color: #f2f2f2;
}
.splitpanes.rsdb-theme .splitpanes__splitter {
  background-color: rgba(0, 0, 0, 0.2);
  box-sizing: border-box;
  position: relative;
  flex-shrink: 0;
}
.splitpanes.rsdb-theme .splitpanes__splitter:hover {
  background-color: rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
  position: relative;
  flex-shrink: 0;
}
.splitpanes.rsdb-theme .splitpanes__splitter:before, .splitpanes.rsdb-theme .splitpanes__splitter:after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  transition: background-color 0.3s;
}
.splitpanes.rsdb-theme .splitpanes__splitter:hover:before, .splitpanes.rsdb-theme .splitpanes__splitter:hover:after {
  background-color: rgba(0, 0, 0, 0.8);
}
.splitpanes.rsdb-theme .splitpanes__splitter:first-child {
  cursor: auto;
}
.rsdb-theme.splitpanes .splitpanes .splitpanes__splitter {
  z-index: 1;
}
.rsdb-theme.splitpanes--vertical > .splitpanes__splitter, .rsdb-theme .splitpanes--vertical > .splitpanes__splitter {
  width: 7px;
  border-left: 1px solid rgba(0, 0, 0, 0.05);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  margin-left: -1px;
}
.rsdb-theme.splitpanes--vertical > .splitpanes__splitter:before, .rsdb-theme .splitpanes--vertical > .splitpanes__splitter:before, .rsdb-theme.splitpanes--vertical > .splitpanes__splitter:after, .rsdb-theme .splitpanes--vertical > .splitpanes__splitter:after {
  transform: translateY(-50%);
  width: 1px;
  height: 30px;
}
.rsdb-theme.splitpanes--vertical > .splitpanes__splitter:before, .rsdb-theme .splitpanes--vertical > .splitpanes__splitter:before {
  margin-left: -2px;
}
.rsdb-theme.splitpanes--vertical > .splitpanes__splitter:after, .rsdb-theme .splitpanes--vertical > .splitpanes__splitter:after {
  margin-left: 1px;
}
.rsdb-theme.splitpanes--horizontal > .splitpanes__splitter, .rsdb-theme .splitpanes--horizontal > .splitpanes__splitter {
  height: 7px;
  border-top: 1px solid #eee;
  margin-top: -1px;
}
.rsdb-theme.splitpanes--horizontal > .splitpanes__splitter:before, .rsdb-theme .splitpanes--horizontal > .splitpanes__splitter:before, .rsdb-theme.splitpanes--horizontal > .splitpanes__splitter:after, .rsdb-theme .splitpanes--horizontal > .splitpanes__splitter:after {
  transform: translateX(-50%);
  width: 30px;
  height: 1px;
}
.rsdb-theme.splitpanes--horizontal > .splitpanes__splitter:before, .rsdb-theme .splitpanes--horizontal > .splitpanes__splitter:before {
  margin-top: -2px;
}
.rsdb-theme.splitpanes--horizontal > .splitpanes__splitter:after, .rsdb-theme .splitpanes--horizontal > .splitpanes__splitter:after {
  margin-top: 1px;
}
</style>
