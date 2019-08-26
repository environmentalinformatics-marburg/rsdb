<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">
    <v-layout>
      <v-flex xs12 sm6 offset-sm3>
        <v-card>
          <h3 class="headline mb-0">Remote Sensing Database Administration <v-btn flat icon color="green" @click="refresh()" title="refresh layers">
          <v-icon>refresh</v-icon>
      </v-btn></h3>
          <v-divider></v-divider>
          <v-card-title v-if="identity != undefined">
            <div>
              <div><b>identity</b>: {{identity.user}}</div>
              <div><b>roles</b>: {{identity.roles.join(", ")}}</div>
              <div><b>connection</b>: {{identity.secure ? 'secure' : 'plain'}}</div>            
              <div><b>ip</b>: {{identity.ip}}</div>            
              <div><b>authentication</b>: {{identity.auth_method}}</div>                
              
              <div v-if="identity !== undefined && (identity.auth_method === 'DIGEST' || identity.auth_method === 'BASIC')">
                <br><a :href="logout_url"><v-icon>call_end</v-icon> logout</a> 
                <span v-if="identity.auth_method === 'DIGEST'" style="font-size: 0.8em">(HTTP {{identity.auth_method}} authentication)</span>
                <span v-if="identity.auth_method === 'BASIC'" style="font-size: 0.8em">(HTTPS {{identity.auth_method}} authentication)</span>
              </div>

              <div v-if="identity !== undefined && (identity.auth_method === 'jws')">
                <br><a :href="logout_jws_url"><v-icon>call_end</v-icon> logout</a> <span style="font-size: 0.8em">(JWS authentication)</span>
              </div>

              <div v-if="identity !== undefined && identity.http_port !== undefined && identity.auth_method !== 'DIGEST'">
                <br><a :href="http_url"><v-icon>http</v-icon><b>switch to <span style="font-size: 1.2em">HTTP (digest authentication)</span></b> {{http_url}}</a> 
                <br><span v-if="identity.auth_method === 'BASIC' || (identity.auth_method === 'jws' && identity.jws_protocol === 'https:')" style="font-size: 0.8em">(Your browser may not allow to downgrade connection from HTTPS to HTTP.)</span>
              </div>
              
              <div v-if="identity !== undefined && identity.https_port !== undefined && identity.auth_method !== 'BASIC'">
                <br><a :href="https_url"><v-icon>https</v-icon><b>switch to <span style="font-size: 1.2em">HTTPS (basic authentication)</span></b> {{https_url}}</a>
                <br>(You may need to add an exception in your browser to allow a self signed certificate.)
              </div>

              <div v-if="identity !== undefined && identity.jws_port !== undefined && identity.jws_protocol === 'http:' && identity.auth_method !== 'jws'">
                <br><a :href="jws_url"><v-icon>vpn_key</v-icon><b>switch to <span style="font-size: 1.2em">HTTP (JWS authentication)</span></b> {{jws_url}}</a>
              </div>

              <div v-if="identity !== undefined && identity.jws_port !== undefined && identity.jws_protocol === 'https:' && identity.auth_method !== 'jws'">
                <br><a :href="jws_url"><v-icon>vpn_key</v-icon><b>switch to <span style="font-size: 1.2em">HTTPS (JWS authentication)</span></b> {{jws_url}}</a>
                <br>(You may need to add an exception in your browser to allow a self signed certificate.)
              </div>

            </div>
          </v-card-title>
        </v-card>
      </v-flex>
    </v-layout>
     <div v-if="identityMode === 'init' || identityMode === 'load'">
      <ring-loader color="#000000" size="20px" style="display: inline-block;" />
      loading...
    </div>
    <div v-if="identityMode === 'error'">
      <v-icon>error</v-icon>
      {{identityMessage}}
    </div>
    </div>
</div>



</template>

<script>

import RingLoader from 'vue-spinner/src/RingLoader.vue'

export default {
  name: 'admin-tools-info',
  components: {
    RingLoader,
  },
  data() {
    return {
    }
  },
  methods: {
    refresh() {
      this.$store.dispatch('identity/refresh');
    },
  },
  computed: {

    identity() {
      return this.$store.state.identity.data;
    },
    identityMode() {
      return this.$store.state.identity.mode;
    },
    identityMessage() {
      return this.$store.state.identity.message;
    },

    logout_url() {
      var url = window.location.protocol + "//";
      url += "logout:logout@";
      url += window.location.host;
      var path = window.location.pathname;
      if(path.endsWith('/')) {
        path = path.substring(0, path.length - 1);
      }       
      url += path;
      return url;
    },

    http_url: function() {
      var port = this.identity.http_port;      
      var host = window.location.host;
      var pIndex = host.indexOf(':');
      if(pIndex >= 0) {
        host = host.slice(0, pIndex);
      }
      host = host + ':' + port;	
      return 'http://' + host + window.location.pathname;
    },

    https_url: function() {
      var port = this.identity.https_port;      
      var host = window.location.host;
      var pIndex = host.indexOf(':');
      if(pIndex >= 0) {
        host = host.slice(0, pIndex);
      }
      host = host + ':' + port;	
      return 'https://' + host + window.location.pathname;
    },
    
    jws_url() {
      //var protocol = window.location.protocol;
      var protocol = this.identity.jws_protocol;
      var port = this.identity.jws_port;      
      var host = window.location.host;
      var pIndex = host.indexOf(':');
      if(pIndex >= 0) {
        host = host.slice(0, pIndex);
      }
      host = host + ':' + port;	
      return protocol + '//' + host + window.location.pathname;
    },

    logout_jws_url() {
      var protocol = window.location.protocol;
      var host = window.location.host;
      var path = window.location.pathname;
      var search = '?jws=logout';	
      return protocol + '//' + host + path + search;
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
</style>
