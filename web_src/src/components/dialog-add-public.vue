<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Add public URL</v-card-title>
        <v-card-text>
          Public <b>Id</b>, user defined part of public URL.
          <br><i>Choose a random character sequence to limit access to users knowing that URL, click on the construction at the right.</i>
          <v-text-field
            v-model="id"
            label="Id"
            append-outer-icon="construction"
            @click:append-outer="setRandom"
          />   
          <b>Type</b>
          <multiselect 
            v-model="type" 
            :options="types" 
            placeholder="select one public URL type" 
            :searchable="false"
            :allowEmpty="false"
            :preselectFirst="true"
            :showLabels="false"
          />
          <hr>    
        </v-card-text>
        <v-card-text v-if="type === 'RasterDB_WMS'">
          Connected <b>RasterDB</b> layer
          <multiselect v-model="rasterdb" :options="rasterdbs" :searchable="true" :show-labels="false" placeholder="pick a rasterdb" :allowEmpty="false">
            <template slot="singleLabel" slot-scope="{option}">
              {{option}}
            </template>
            <template slot="option" slot-scope="{option}">
              {{option}}
            </template>
          </multiselect>
          <hr>          
        </v-card-text>
        <v-card-text>
          {{postMessage}}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="show = false">Cancel</v-btn>
          <v-btn color="green darken-1" flat="flat" @click.native="commit" :disabled="!valid">Add</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios';
import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
name: 'dialog-add-public',
props: {
},

components: {
  Multiselect,
},

data() {
    return {
        show: false,
        types: ['RasterDB_WMS'],   
        type: undefined,
        id: '',
        rasterdb: undefined,
    }
},
computed: {
  valid() {
    if(this.id === undefined || this.id === null || this.id.length === 0) {
      return false;
    }
    if(this.type === undefined || this.type === null) {
      return false;
    }
    if(this.type === 'RasterDB_WMS' && (this.rasterdb === undefined || this.rasterdb === null)) {
      return false;
    }
    return true;
  },  
  rasterdbs() {
    if(this.$store.state.rasterdbs.data === undefined) {
      return [];
    }
    var r = this.$store.state.rasterdbs.data.map(e => e.name);
    return r === undefined ? [] : r.slice().sort(function(a, b) { return a.localeCompare(b);});
  },
},    
methods: {
  async commit() {
    if(this.valid) {
      try {
        this.postMessage = "Sending add public_access...";
        let action = {
          action: 'add_public_access', 
          id: this.id, 
          type: this.type,
        };
        if(this.type === 'RasterDB_WMS') {
          action.rasterdb = this.rasterdb;
        }  
        let data = {actions: [action]};
        var url = this.$store.getters.apiUrl('api/public_access');             
        await axios.post(url, data);
        this.postMessage = undefined;
        this.$emit('changed');
        this.show = false;
      } catch(error) {
        this.postMessage = "Error add account.";
        console.log(error);
        this.$emit('changed');
      }
    }
  },

  getRandom(len) {
    var rnd = new Uint32Array(len);
    window.crypto.getRandomValues(rnd);
    var r = "";
    var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    var charsLen = chars.length;
    for(var i = 0; i < len; i++) {
      r += chars[rnd[i] % charsLen];
    }
    return r;
  },

  setRandom() {
    this.id = this.getRandom(12);
  },
  
  refresh() {
    this.$store.dispatch('rasterdbs/refresh');
  },  
},
watch: {
  show: {
    immediate: true,
    handler() {
      this.username = '';
      this.password = '';
      this.postMessage = undefined;
      this.selectedRoles = [];
      if(this.show) {
        this.setRandom();
        this.refresh();
      }
    },
  },
},
mounted() {
},
}

</script>

<style scoped>

</style>