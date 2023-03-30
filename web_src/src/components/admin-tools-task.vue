<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">
      <span><b>Category</b></span>
      <multiselect v-if="data !== undefined" 
      v-model="selectedCategory" 
      :options="data" 
      track-by="category" 
      :allowEmpty="false" 
      :searchable="false" 
      :show-labels="false" 
      placeholder="select category" 
      style="display: inline-block; max-width: 200px;">
        <template slot="singleLabel" slot-scope="props">{{categoryToTitle(props.option.category)}}</template>
        <template slot="option" slot-scope="props">{{categoryToTitle(props.option.category)}}</template>
      </multiselect>
      <span><b>Task</b></span>
      <multiselect v-if="selectedCategory !== undefined" 
      v-model="selectedRemoteTaskEntry" 
      :options="selectedCategory.remote_task_entries" 
      track-by="name" 
      :allowEmpty="false" 
      :searchable="false" 
      :show-labels="false" 
      placeholder="select task"  
      style="display: inline-block; max-width: 300px;">
        <template slot="singleLabel" slot-scope="props">{{props.option.name}}</template>
        <template slot="option" slot-scope="props">{{props.option.name}}</template>
      </multiselect>

      <v-btn flat icon color="green" @click="refreshLayers()" title="refresh layers">
          <v-icon>refresh</v-icon>
        </v-btn>

      <div v-if="selectedRemoteTaskEntry !== undefined">
        <div v-if="selectedRemoteTaskEntry.description !== undefined">
          <br>
          <b>Description: </b>{{selectedRemoteTaskEntry.description}}
        </div>
        <div>
        <br>
        <div>
          <hr>
          <b>Task Parameters</b>
          <table>
          <tr>
          <th>Parameter</th>
          <th>Value</th>
          <th>Description</th>
          <th>Format</th>
          <th>Example</th>
          </tr>
          <tr v-for="param in selectedRemoteTaskEntry.params" :key="param.name">
            <td :class="[param.required ? 'param-required' : 'param-optional', param.required && (param.value === undefined || param.value === '') ? 'param-required-missing' : '']">{{param.name}}{{param.required ? '*' : ''}}</td>
            <td v-if="param.type === 'rasterdb'">
              <multiselect v-model="param.value" :options="rasterdbs" :searchable="true" :show-labels="false" placeholder="pick a rasterdb" :allowEmpty="false" style="min-width: 600px;">
                <template slot="singleLabel" slot-scope="{option}">
                  {{option}}
                </template>
                <template slot="option" slot-scope="{option}">
                  {{option}}
                </template>
              </multiselect>
            </td>
            <td v-else-if="param.type === 'pointdb'">
              <multiselect v-model="param.value" :options="pointdbs" :searchable="true" :show-labels="false" placeholder="pick a pointdb" :allowEmpty="false" style="min-width: 600px;">
                <template slot="singleLabel" slot-scope="{option}">
                  {{option}}
                </template>
                <template slot="option" slot-scope="{option}">
                  {{option}}
                </template>
              </multiselect>
            </td>
            <td v-else-if="param.type === 'pointcloud'">
              <multiselect v-model="param.value" :options="pointclouds" :searchable="true" :show-labels="false" placeholder="pick a pointcloud" :allowEmpty="false" style="min-width: 600px;">
                <template slot="singleLabel" slot-scope="{option}">
                  {{option}}
                </template>
                <template slot="option" slot-scope="{option}">
                  {{option}}
                </template>
              </multiselect>
            </td>
            <td v-else-if="param.type === 'vectordb'">
              <multiselect v-model="param.value" :options="vectordbs" :searchable="true" :show-labels="false" placeholder="pick a vectordb" :allowEmpty="false" style="min-width: 600px;">
                <template slot="singleLabel" slot-scope="{option}">
                  {{option}}
                </template>
                <template slot="option" slot-scope="{option}">
                  {{option}}
                </template>
              </multiselect>
            </td>
            <td v-else-if="param.type === 'voxeldb'">
              <multiselect v-model="param.value" :options="voxeldbs" :searchable="true" :show-labels="false" placeholder="pick a voxeldb" :allowEmpty="false" style="min-width: 600px;">
                <template slot="singleLabel" slot-scope="{option}">
                  {{option}}
                </template>
                <template slot="option" slot-scope="{option}">
                  {{option}}
                </template>
              </multiselect>
            </td>            
            <td v-else-if="param.type === 'string'">
              <input v-model="param.value" placeholder="(empty)" :class="[param.required && (param.value === undefined || param.value === '') ? 'param-required-missing' : '']" />
            </td>
            <td v-else-if="param.type === 'layer_id'">
              <input v-model="param.value" placeholder="(empty)" :class="[param.required && (param.value === undefined || param.value === '') ? 'param-required-missing' : '']" />
            </td>            
            <td v-else-if="param.type === 'string_array'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isStringArray(param.value) ? 'param-required-missing' : '']" />
            </td>
            <td v-else-if="param.type === 'number'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isNumber(param.value) ? 'param-required-missing' : '']" />
            </td> 
            <td v-else-if="param.type === 'number_array'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isNumberArray(param.value) ? 'param-required-missing' : '']" />
            </td>
            <td v-else-if="param.type === 'number_rect'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isNumberRect(param.value) ? 'param-required-missing' : '']" />
            </td>                                       
            <td v-else-if="param.type === 'integer'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isInteger(param.value) ? 'param-required-missing' : '']" />
            </td>
            <td v-else-if="param.type === 'integer_array'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isIntegerArray(param.value) ? 'param-required-missing' : '']" />
            </td>             
            <td v-else-if="param.type === 'boolean'">
              <input v-model="param.value" placeholder="(empty)" :class="[isParamRequiredOrNotEmpty(param) && !isBoolean(param.value) ? 'param-required-missing' : '']" />
            </td>                 
            <td v-else>[{{param.type}}] unknown parameter type</td>
            <td class="param-desc">{{param.desc}}</td>
            <td class="param-format">{{param.format}}</td>
            <td class="param-example" v-if="param.example !== undefined && param.example !== ''">{{param.example}}</td>
          </tr>
          </table>
          <span style="font-size: 0.8em; color: #6c6c6c;"><br><b>*</b> marked parameters are required. Red marked parameter values are not valid.</span>
          <hr>
        </div>
          <br>
          <b>resulting task</b>
          <div style="background-color: white;">
          {{taskJSON}}
          </div>
        </div>
        <br>
        <div>
          <v-btn @click="submit_task" :disabled="!isValidTask"><v-icon>input</v-icon> submit</v-btn>
          <span v-show="!isValidTask" style="color: #ff3f01e6;">some missing required parameters</span>
        </div>
      </div>
  
     <div v-if="mode === 'init' || mode === 'load'">
      <ring-loader color="#000000" size="20px" style="display: inline-block;" />
      loading...
    </div>
    <div v-if="mode === 'error'">
      <v-icon>error</v-icon>
      {{message}}
    </div>
    </div>
    <admin-task-console :id="remote_task_id" />
    <v-snackbar v-model="setError" :top="true" timeout="10000">
      {{setErrorMessage}}
      <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
    </v-snackbar>
</div>

</template>

<script>

import RingLoader from 'vue-spinner/src/RingLoader.vue'
import axios from 'axios'
import Multiselect from 'vue-multiselect'

import adminTaskConsole from './admin-task-console'

export default {
  name: 'admin-tools-task',
  components: {
    RingLoader,
    Multiselect,
    'admin-task-console': adminTaskConsole,
  },
  data() {
    return {
      data: undefined,
      mode: 'init',
      message: '',
      messageActive: false,
      selectedCategory: undefined,
      selectedRemoteTaskEntry: undefined,
      remote_task_id: undefined,
      setError: false,
      setErrorMessage: undefined,
    }
  },
  methods: {
    async refresh() {
      this.$store.dispatch('rasterdbs/init'); 
      this.$store.dispatch('pointdbs/init');      
      this.$store.dispatch('pointclouds/init'); 
      this.$store.dispatch('vectordbs/init');
      this.$store.dispatch('voxeldbs/init');
      this.mode = 'load';
      var url = this.$store.getters.apiUrl('api/remote_task_entries');
      try {
        var response = await axios.get(url);
        this.data = response.data.remote_task_categories;
        this.mode = 'ready';
      } catch(error) {
        this.mode = 'error';
        this.message = this.interpretError(error);
        this.messageActive = true;
      } 
    },
    interpretError(error) {
      if (error.response) {
        return error.response.data ? error.response.data : error.response;       
      } else if (error.request) {
        return "network error";
      } else {
        return error.message ? error.message : error;
      }
    },
    categoryToTitle(c) {
      switch(c) {
        case "task_rsdb":
          return "RSDB";        
        case "task_rasterdb":
          return "RasterDB";
        case "task_pointdb":
          return "PointDB";
        case "task_pointcloud":
          return "PointCloud";
        case "task_vectordb":
          return "VectorDB";
        case "task_voxeldb":
          return "VoxelDB";          
        default:
          return c;
      }
    },
    submit_task() {
      var self = this;
      self.remote_task_id = undefined;
      var url = this.$store.getters.apiUrl('api/remote_tasks');
      axios.post(url, {
          remote_task: self.taskJSON,
      }).then(function(response) {
          console.log(response);
          var remote_task = response.data.remote_task;
          self.remote_task_id = remote_task.id;
      }).catch(function(error) {
          console.log(error);
          console.log(self.errorToText(error));
          self.setError = true;
          self.setErrorMessage = "Error: " + self.errorToText(error);
          console.log(error);
      }); 
    },
    errorToText(error) {
      if(error === undefined) {
          return "unknown error";
      }
      if(error.message === undefined) {
          return error;
      }
      if(error.response === undefined || error.response.data === undefined) {
          return error.message;
      }
      return error.message + " - " + error.response.data;
    },
    isParamRequiredOrNotEmpty(param) {
      return param.required || !this.isEmpty(param.value);
    },
    isEmpty(value) {
      return value === undefined || value.trim() === '';
    },
    isNumber(v) {
      return v !== undefined && v !== '' && !isNaN(v);
    },
    isInteger(v) {
      return v !== undefined && v !== '' && !isNaN(v) && Number.isInteger(+v);
    },
    isBoolean(v) {
      return v === 'true' || v == 'false';
    },
    isStringArray(v) {
      return this.parseStringArray(v).length > 0;
    },
    isNumberArray(v) {
      return this.parseNumberArray(v).length > 0;
    },
    isNumberRect(v) {
      return this.parseNumberArray(v).length === 4;
    },
    parseNumber(v) {
      return (+v);
    },    
    parseInteger(v) {
      return Number.parseInt(v);
    },
    isIntegerArray(v) {
      return this.parseIntegerArray(v).length > 0;
    },
    parseBoolean(v) {
      return v === 'true' ? true : v == 'false' ? false : undefined;
    },
    parseStringArray(v) {
      if(v === undefined || v === '') {
        return [];
      }
      return v.split(',').map(s => s.trim()).filter(s => s !== '');
    },
    parseNumberArray(v) {
      var a = this.parseStringArray(v);
      if(!a.every(e => this.isNumber(e))) {
        return [];
      }
      return a.map(e => this.parseNumber(e));
    },
    parseNumberRect(v) {
      return this.parseNumberArray(v); 
    },
    parseIntegerArray(v) {
      var a = this.parseStringArray(v);
      if(!a.every(e => this.isInteger(e))) {
        return [];
      }
      return a.map(e => this.parseInteger(e));
    },
    refreshLayers() {
      this.$store.dispatch('rasterdbs/refresh');
      this.$store.dispatch('pointdbs/refresh');
      this.$store.dispatch('pointclouds/refresh');
      this.$store.dispatch('vectordbs/refresh');
      this.$store.dispatch('voxeldbs/refresh');
      this.$store.dispatch('poi_groups/refresh');
      this.$store.dispatch('roi_groups/refresh');
      this.$store.dispatch('layer_tags/refresh');
    },  
  },
  computed: {
    taskJSON() {
      if(this.selectedCategory === undefined || this.selectedRemoteTaskEntry === undefined) {
        return undefined;
      }
      var t = {};
      t[this.selectedCategory.category] = this.selectedRemoteTaskEntry.name;
      this.selectedRemoteTaskEntry.params.forEach(param => {
        if(!this.isEmpty(param.value)) {
          switch(param.type) {
            case 'string_array': {
              if(this.isStringArray(param.value)) {
                t[param.name] = this.parseStringArray(param.value);
              }
              break;
            }
            case 'number': {
              if(this.isNumber(param.value)) {
                t[param.name] = this.parseNumber(param.value);
              }
              break;
            }
            case 'number_array': {
              if(this.isNumberArray(param.value)) {
                t[param.name] = this.parseNumberArray(param.value);
              }
              break;
            }
            case 'number_rect': {
              if(this.isNumberRect(param.value)) {
                t[param.name] = this.parseNumberRect(param.value);
              }
              break;
            }              
            case 'integer': {
              if(this.isInteger(param.value)) {
                t[param.name] = this.parseInteger(param.value);
              }
              break;
            }
            case 'integer_array': {
              if(this.isIntegerArray(param.value)) {
                t[param.name] = this.parseIntegerArray(param.value);
              }
              break;
            }
            case 'boolean': {
              if(this.isBoolean(param.value)) {
                t[param.name] = this.parseBoolean(param.value);
              }
              break;
            }            
            default: {
               t[param.name] = param.value;
             }
          }
        }
      });
      return t;
    },
    isValidTask() {
      return this.selectedRemoteTaskEntry.params.filter(e => e.required).every(e => this.taskJSON[e.name] !== undefined); 
    },
    rasterdbs() {
      if(this.$store.state.rasterdbs.data === undefined) {
        return [];
      }
      var r = this.$store.state.rasterdbs.data.map(e => e.name);
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.localeCompare(b);});
    },
    pointdbs() {
      if(this.$store.state.pointdbs.data === undefined) {
        return [];
      }
      var r = this.$store.state.pointdbs.data.map(e => e.name);
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.localeCompare(b);});
    },
    pointclouds() {
      if(this.$store.state.pointclouds.data === undefined) {
        return [];
      }
      var r = this.$store.state.pointclouds.data.map(e => e.name);
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.localeCompare(b);});
    },
    vectordbs() {
      if(this.$store.state.vectordbs.data === undefined) {
        return [];
      }
      var r = this.$store.state.vectordbs.data.map(e => e.name);
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.localeCompare(b);});
    },
    voxeldbs() {
      if(this.$store.state.voxeldbs.data === undefined) {
        return [];
      }
      var r = this.$store.state.voxeldbs.data.map(e => e.name);
      return r === undefined ? [] : r.slice().sort(function(a, b) { return a.localeCompare(b);});
    },    
  },
  watch: {
    data() {
      if(this.selectedCategory === undefined && this.data !== undefined && this.data.length > 0) {
        this.selectedCategory = this.data[0];
      }
    },
    selectedCategory() {
      if(this.selectedCategory !== undefined && this.selectedCategory.remote_task_entries.length > 0) {
        this.selectedRemoteTaskEntry = this.selectedCategory.remote_task_entries[0];
      } else {
        this.selectedRemoteTaskEntry = undefined;
      }
    },
  },
  mounted() {
    this.refresh();
  },
}



</script>

<style scoped>
.innergrid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
}

.innergrid-item-main {
  padding: 0;
  overflow-y: auto;
}

input {
  background-color: white;
  border-style: solid;
  border-width: 1px;
  border-color: #00000059;
  padding: 2px;
  margin: 2px;
}

hr {
  color: #0f0f3e3d;
  margin: 10px;
}

table {
  border-collapse: collapse;
}

th, td {
  border-bottom: 1px solid rgba(0, 0, 0, 0.055);
}

.param-desc {
  font-style: italic;
  padding-left: 10px;
  padding-right: 10px;
  white-space: pre-wrap;
}

.param-format {
  color: #0f385b8a;
  background-color: #62161603;
  padding-right: 10px;
}

.param-example {
  background-color: #7777770f;
  border-style: solid;
  border-width: 1px;
  border-color: #5b69750d;
}

.param-required {
  font-weight: bold;
}

.param-optional {
  font-weight: 600;
  color: #5d5d5d;
}

.param-required-missing {
  color: red;
}

</style>
