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
            <td v-else-if="param.type === 'string'">
              <input v-model="param.value" placeholder="(empty)" :class="[param.required && (param.value === undefined || param.value === '') ? 'param-required-missing' : '']" />
            </td>
            <td v-else>[{{param.type}}] unknown parameter type</td>
            <td class="param-desc">{{param.desc}}</td>
          </tr>
          </table>
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
          <v-btn @click="submit_task"><v-icon>input</v-icon> submit</v-btn>
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
    <v-snackbar v-model="setError" :top="true">
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
    refresh() {
      var self = this;
        this.mode = 'load';
        var url = this.$store.getters.apiUrl('api/remote_task_entries');
        axios.get(url)
            .then(function(response) {
              self.data = response.data.remote_task_categories;
              self.mode = 'ready';
            })
            .catch(function(error) {
              self.mode = 'error';
              self.message = self.interpretError(error);
              self.messageActive = true;
            });
      this.$store.dispatch('rasterdbs/init');      
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
        case "task_rasterdb":
          return "RasterDB";
        case "task_pointdb":
          return "PointDB";
        case "task_pointcloud":
          return "PointCloud";
        case "task_vectordb":
          return "VectorDB";
        default:
          return c;
      }
    },
    submit_task() {
      var self = this;
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
  },
  computed: {
    taskJSON() {
      if(this.selectedCategory === undefined || this.selectedRemoteTaskEntry === undefined) {
        return undefined;
      }
      var t = {};
      t[this.selectedCategory.category] = this.selectedRemoteTaskEntry.name;
      this.selectedRemoteTaskEntry.params.forEach(param => {
        if(param.value !== undefined) {
          t[param.name] = param.value;
        }
      });
      return t;
    },
    rasterdbs() {
      if(this.$store.state.rasterdbs.data === undefined) {
        return [];
      }
      var r = this.$store.state.rasterdbs.data.map(e => e.name);
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

.param-desc {
  font-style: italic;
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
