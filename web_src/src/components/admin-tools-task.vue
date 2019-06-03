<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">

      <multiselect v-if="data !== undefined" 
      v-model="selectedCategory" 
      :options="data" 
      track-by="category" 
      :allowEmpty="false" 
      :searchable="false" 
      :show-labels="false" 
      placeholder="select category">
        <template slot="singleLabel" slot-scope="props">{{props.option.category}}</template>
        <template slot="option" slot-scope="props">{{props.option.category}}</template>
      </multiselect>

      <multiselect v-if="selectedCategory !== undefined" 
      v-model="selectedRemoteTaskEntry" 
      :options="selectedCategory.remote_task_entries" 
      track-by="name" 
      :allowEmpty="false" 
      :searchable="false" 
      :show-labels="false" 
      placeholder="select task">
        <template slot="singleLabel" slot-scope="props">{{props.option.name}}</template>
        <template slot="option" slot-scope="props">{{props.option.name}}</template>
      </multiselect>


      <div v-for="remote_task_category in data" :key="remote_task_category.category">
      <br>
      <b>{{remote_task_category.category}}</b>
      <table>
        <thead>
          <tr>
            <th>Category</th>
            <th>Name</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="remote_task_entry in remote_task_category.remote_task_entries" :key="remote_task_entry.name">
            <td>{{remote_task_category.category}}</td>
            <td>{{remote_task_entry.name}}</td>
          </tr>
        </tbody>
      </table>
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
</div>



</template>

<script>

import RingLoader from 'vue-spinner/src/RingLoader.vue'
import axios from 'axios'
import Multiselect from 'vue-multiselect'

export default {
  name: 'admin-tools-task',
  components: {
    RingLoader,
    Multiselect,
  },
  data() {
    return {
      data: undefined,
      mode: 'init',
      message: '',
      messageActive: false,
      selectedCategory: undefined,
      selectedRemoteTaskEntry: undefined,
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
    },
    interpretError(error) {
      if (error.response) {
        return error.response.data ? error.response.data : error.response;       
      } else if (error.request) {
        return "network error";
      } else {
        return error.message ? error.message : error;
      }
    }
  },
  computed: {
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
</style>
