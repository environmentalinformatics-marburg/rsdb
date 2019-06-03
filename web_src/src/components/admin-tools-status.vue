<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">
       <v-btn flat icon color="green" @click="refresh()" title="refresh status">
          <v-icon>refresh</v-icon>
        </v-btn>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Status</th>
            <th>Message</th>
            <th>Runtime</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="remote_task in data" :key="remote_task.id">
            <td>{{remote_task.id}}</td>
            <td>{{remote_task.name}}</td>
            <td><v-icon v-if="remote_task.status === 'DONE'">done</v-icon>({{remote_task.status}})</td>
            <td>{{remote_task.message}}</td>
            <td>{{(remote_task.runtime / 1000).toFixed(3)}}s</td>
          </tr>
        </tbody>
      </table>
  
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

export default {
  name: 'admin-tools-status',
  components: {
    RingLoader,
  },
  data() {
    return {
      data: undefined,
      mode: 'init',
      message: '',
      messageActive: false,
    }
  },
  methods: {
    refresh() {
      var self = this;
        this.mode = 'load';
        var url = this.$store.getters.apiUrl('api/remote_tasks');
        axios.get(url)
            .then(function(response) {
              self.data = response.data.remote_tasks;
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
