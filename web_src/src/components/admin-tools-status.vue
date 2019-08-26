<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">
      <v-btn :disabled="autoRefresh" flat icon color="green" @click="refresh()" title="refresh status" style="display: inline-block;">
        <v-icon>refresh</v-icon>
      </v-btn>
      <v-switch v-model="autoRefresh" label="auto refresh" style="display: inline-block;" />
      <table class="table-status">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Status</th>
            <th>Message</th>
            <th>Runtime</th>
            <th>Cancel</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="remote_task in data" :key="remote_task.id">
            <td>{{remote_task.id}}</td>
            <td>{{remote_task.name}}</td>
            <td>
              <v-icon v-if="remote_task.status === 'READY'" color="yellow">directions_walk</v-icon>
              <v-icon v-if="remote_task.status === 'RUNNING'" color="black">directions_run</v-icon>
              <v-icon v-if="remote_task.status === 'DONE'" color="green">done</v-icon>
              <v-icon v-if="remote_task.status === 'ERROR'" color="red">error</v-icon>
              {{remote_task.status}}
            </td>
            <td>{{remote_task.message}}</td>
            <td>{{(remote_task.runtime / 1000).toFixed(3)}}s</td>
            <td v-if="(remote_task.status === 'READY' || remote_task.status === 'RUNNING') && !remote_task.canceled && remote_task.cancelable"><v-btn icon @click="cancel(remote_task.id)"><v-icon>cancel</v-icon></v-btn></td>
            <td v-if="!remote_task.canceled && !remote_task.cancelable">---</td>
            <td v-if="remote_task.canceled">cancel requested</td>
          </tr>
        </tbody>
      </table>
  
     <transition name="fade">
      <div v-if="mode === 'init' || mode === 'load'">
        <ring-loader color="#000000" size="20px" style="display: inline-block;" />
        loading...
      </div>
     </transition>
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
      autoRefresh: false,
      autoRefreshTimer: undefined,
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
    },
    async cancel(id) {
      try {
        console.log(id);
        var url = this.$store.getters.apiUrl('api/remote_tasks/' + id + '/cancel');
        await axios.post(url);
        this.refresh();
      } catch(error) {
        this.mode = 'error';
        this.message = this.interpretError(error);
        this.messageActive = true;
      } finally {
        //this.refresh();
      }      
    },
    updateAutoRefresh() {
      if(this.autoRefreshTimer !== undefined) {
        window.clearInterval(this.autoRefreshTimer);
        this.autoRefreshTimer = undefined;
      }
      if(this.autoRefresh) {
        this.autoRefreshTimer = window.setInterval(() => {this.refresh();}, 1000)
      }
    },
  },
  computed: {
  },
  watch: {
    autoRefresh() {
      this.updateAutoRefresh();
    },
  },
  mounted() {
    this.refresh();
    this.updateAutoRefresh();
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

.table-status th {
  font-weight: bold;
  color: #4c4d62;
  font-size: 1.2em;
  background-color: #e6e6e6;
}

.table-status td:nth-child(1) {
  color: grey;
  background-color: #d9e3f6;
}

.table-status td:nth-child(2) {
  font-weight: bold;
}

.table-status td:nth-child(3) {
  font-size: 0.8em;
  background-color: #b4b16f2e;
}

.table-status td:nth-child(4) {
  font-size: 1em;
  background-color: #ffffffa6;
}

.table-status td:nth-child(5) {
  font-size: 1em;
  background-color: #b4b16f2e;
  font-family: monospace;
  text-align: right;
}

.table-status td {
  padding-left: 10px;
  padding-right: 10px;
}

.table-status tr:hover {
  background-color: #0000003f;
}

.fade-enter-active {
  transition-property: opacity;
  transition-duration: 2s;
}

.fade-leave-active {
  transition-property: opacity;
  transition-duration: 0s;
}

.fade-enter-active {
  transition-delay: 0s;
}

.fade-enter, .fade-leave-active {
  opacity: 0;
}



</style>
