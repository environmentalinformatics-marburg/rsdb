<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">
    <v-layout>
      <v-flex xs12 sm6 offset-sm3>
        <v-card>
          <h3 class="headline mb-0">Public URLs
          <v-btn flat icon color="green" @click="refresh()" title="reload public entries">
            <v-icon>refresh</v-icon>
          </v-btn>
          {{refreshMessage}}
          </h3>
          <v-divider></v-divider>
          <table v-if="public_access !== undefined" class="access-table">
            <thead>
              <tr>
                <th>Id</th>
                <th>Type</th>
                <th>Properties</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
            <tr v-for="(entry, id) in public_access" :key="id">
              <td>{{id}}</td>
              <td>{{entry.type}}</td>
              <td><span v-for="(v, k) in entry" :key="k"><span v-if="k !== 'type'"><b>{{k}}:</b> {{v}}</span></span></td>
              <td>
                <v-btn @click="remove(id);" icon small color="grey lighten-3">
                  <v-icon title="Remove entry.">delete_forever</v-icon>
                </v-btn>
              </td>
            </tr>
            </tbody>
          </table>
          <v-divider></v-divider>
          <dialog-add-public ref="add_public" @changed="refresh"/>
          <v-btn @click="$refs.add_public.show = true;" title="Insert a new public URL."><v-icon>add</v-icon> Add public URL</v-btn>
        </v-card>
      </v-flex>
    </v-layout>
    <v-snackbar v-model="public_accessError" :top="true">
      {{public_accessErrorMessage}}
      <v-btn flat class="pink--text" @click.native="public_accessError = false">Close</v-btn>
    </v-snackbar>
  </div>
  </div>
</template>

<script>

import { mapState } from 'vuex'
import axios from 'axios'

import dialogAddPublic from './dialog-add-public.vue'

export default {
  name: 'admin-public',

  components: {
    dialogAddPublic,    
  },

  data() {
    return {
      public_access: undefined,
      public_accessError: false,
      public_accessErrorMessage: undefined,
      refreshMessage: 'init...',
    }
  },
  methods: {
    refresh() {
      this.$store.dispatch('rasterdbs/refresh');
      this.refreshMessage = "Reloading public_access...";
      var self = this;
      axios.get(this.urlPrefix + '../../api/public_access')
        .then(function(response) {
          self.public_access = response.data.public;
          self.refreshMessage = "";
        })
        .catch(function(error) {
          console.log(error);
          self.public_accessError = true;
          self.public_accessErrorMessage = "ERROR getting public_access: " + error;
          self.refreshMessage = "Error loading public_access.";
        });
    },
    async remove(id) {
      try {
        this.refreshMessage = "Sending remove public_access entry...";  
        let data = {actions: [{
          action: 'remove_public_access', 
          id,
        }]};
        var url = this.$store.getters.apiUrl('api/public_access');             
        await axios.post(url, data);
        this.refreshMessage = "";
        this.refresh();
      } catch(error) {
        this.public_accessError = true;
        this.public_accessErrorMessage = "ERROR removing public_access entry: " + error;
        this.refreshMessage = "Error removing public_access entry.";
        console.log(error);
        this.refresh();
      }
    },
  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
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

<style scoped> 
.access-table th, .access-table td{
  padding: 15px;
}
</style>
