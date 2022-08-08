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
                <th>Public URL</th>
              </tr>
            </thead>
            <tbody>
            <tr v-for="(entry, id) in public_access" :key="id">
              <td style="max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{id}}</td>
              <td style="font-size: 0.8em;">{{entry.type}}</td>
              <td><span style="font-size: 0.8em;" v-for="(v, k) in entry" :key="k"><span v-if="k !== 'type'"><b>{{k}}:</b> {{v}}</span></span></td>
              <td>
                <v-btn @click="remove(id);" icon small color="grey lighten-3">
                  <v-icon title="Remove entry.">delete_forever</v-icon>
                </v-btn>
              </td>
              <td style="color: grey; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                <v-btn flat icon @click="onUrlCopy(publicUrl(id))" title="Copy public URL to clipboard."><v-icon>content_copy</v-icon></v-btn>
                <span style="font-size: 0.8em;">{{publicUrl(id)}}</span>
              </td>
            </tr>
            </tbody>
          </table>
          <v-divider></v-divider>
          <dialog-add-public ref="add_public" @changed="refresh"/>
          <v-btn @click="$refs.add_public.show = true;" title="Insert a new public URL."><v-icon>add</v-icon> Add public URL</v-btn>
          <v-divider style="padding-bottom: 50px;"></v-divider>
          <h4>Info</h4>
          <p>
            Public URLs allow access to user defined parts of RSDB without login. Public URLs can be accessed by anyone knowing the URL. Choosing a long random ID prevents guessing the correct public ID.
          </p>
          <p>
          The public URL is build from the "server base URL" and the "public URL entry ID" listed above. 
          <br>e.g server base url <b>http://127.0.0.1:8080</b> and ID <b>ABCD1</b> results in public URL <b>http://127.0.0.1:8080/ABCD1</b>
          </p>
          <h4>Type: RasterDB_WMS</h4>
          The public URL is the WMS URL usable e.g. in QGIS or some Web GIS.
          <br>At the RasterDB layer detail page <b>custom WMS IDs</b> with user defined properties can be added. Ths custom IDs can also be accessed with public URLs by adding that custom ID to the public URL.
          <br>e.g <br>server base url <b>http://127.0.0.1:8080</b> 
          <br>public URL ID <b>ABCD1</b> 
          <br>custom WMS ID <b>MY_WMS</b> on the set RasterDB layer results in
          <br><b>http://127.0.0.1:8080/ABCD1/MY_WMS</b>


        </v-card>
      </v-flex>
    </v-layout>
    <v-snackbar v-model="public_accessError" :top="true">
      {{public_accessErrorMessage}}
      <v-btn flat class="pink--text" @click.native="public_accessError = false">Close</v-btn>
    </v-snackbar>
    <v-snackbar v-model="snackbarCopiedToClipboard" top :timeout="2000">
        URL copied to clipboard
        <v-btn color="pink" flat @click="snackbarCopiedToClipboard = false">Close</v-btn>
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
      snackbarCopiedToClipboard: false,
    }
  },
  methods: {
    refresh() {
      this.$store.dispatch('identity/refresh');
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
    publicUrl(id) {
      return (this.identity === undefined ? '[unknown base URL]' : this.identity.url_base) + '/' + id;
    },
    onUrlCopy(url) {
      copyTextToClipboard(url);
      this.snackbarCopiedToClipboard = true;
    },
  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
    identity() {
      return this.$store.state.identity.data;
    },    
  },
  mounted() {
    this.refresh();
  },
}

//derived from http://stackoverflow.com/questions/400212/how-do-i-copy-to-the-clipboard-in-javascript
function copyTextToClipboard(text) {
	var textArea = document.createElement("textarea");
	textArea.style.position = 'fixed';
	textArea.style.top = 0;
	textArea.style.left = 0;
	textArea.style.width = '2em';
	textArea.style.height = '2em';
	textArea.style.padding = 0;
	textArea.style.border = 'none';
	textArea.style.outline = 'none';
	textArea.style.boxShadow = 'none';
	textArea.style.background = 'transparent';
	textArea.value = text;
	document.body.appendChild(textArea);
	textArea.select();
	try {
		var successful = document.execCommand('copy');
		var msg = successful ? 'successful' : 'unsuccessful';
		console.log('copying text command was ' + msg);
	} catch (e) {
		console.log('ERROR unable to copy: '+e);
	}
	document.body.removeChild(textArea);
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
