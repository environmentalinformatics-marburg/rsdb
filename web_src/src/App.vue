<template>
<v-app light>
  <div class="grid-container">
    <div class="grid-item-head">
      <v-tabs dark color="blue-grey lighten-1" slider-color="yellow">
        <v-tab v-for="tab in tabs" :key="tab.name" :to="'/' + (tab.target === undefined ? tab.name : tab.target)" ripple replace>
          <v-icon v-if="tab.icon !== undefined">{{tab.icon}}</v-icon>
          {{tab.title}}
        </v-tab>
      </v-tabs>
    </div>
    <!--<keep-alive include="admin-viewer,admin-explorer">-->
    <router-view class="grid-item-content" />
    <!--</keep-alive>-->
    <div class="grid-item-foot"><span id="foot-start-1">?</span><span id="foot-end-1">?</span></div>
  </div>
  <div v-if="isInternetExplorer" class="internet-explorer">
    <h3 style="color: rgba(197, 32, 32, 1);">Microsoft Internet Explorer is not usable for this site.</h3>
    Use a browser that conforms to current web standards:
    <br>Mozilla Firefox
    <br>Google Chrome
    <br>Microsoft Edge
  </div>
</v-app>
</template>

<script>

import { mapGetters } from 'vuex'

export default {
  name: 'App',
  data() {
    return {};   
  },
  methods: {
  },
  computed: {
    ...mapGetters({
      isAdmin: 'identity/isAdmin',
    }),
    tabs() {
      var tabs = [];
      tabs.push({name: 'overview', title: 'Overview', target: '', icon: 'home'});
      tabs.push({name: 'layers', title: 'Layers', icon: 'photo_album'});
      tabs.push({name: 'explorer', title: 'Explorer', icon: 'vpn_lock'});
      tabs.push({name: 'viewer', title: 'Viewer', icon: 'satellite'});
      tabs.push({name: 'vectorviewer', title: 'VectorViewer', icon: 'map'});
      /*tabs.push({name: 'upload', title: 'Upload', icon: 'cloud_upload'});*/
      tabs.push({name: 'files', title: 'Files', icon: 'attachment'});
      tabs.push({name: 'tools', title: 'Tools', icon: 'build'});
      return tabs;
    },
    isInternetExplorer() {
     return window.document.documentMode !== undefined;
   },
  },
  mounted() {
    this.$store.dispatch('init');
  }
}
</script>

<style>

html {
    height: 100vh;
    overflow-y: auto;
}

body { 
    height: 100vh;
    margin: 0;
}

.grid-container {
  height: 100vh;
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: max-content minmax(100px, 1fr) max-content;
}                

.grid-item-head {
  background-color: rgb(221, 221, 221);
  border-bottom-color: #5e666b;
  border-bottom-width: 1px;
  border-bottom-style: solid;
}

.grid-item-content {
    min-height: 100%;
    background-color: rgb(247, 249, 249); 
}

.grid-item-foot {
  background-color: rgb(172, 172, 172);
  text-align: right;
  border-top-color: #898989;
  border-top-width: 1px;
  border-top-style: solid;
  display: flex;
  justify-content: space-between;
}

.v-tabs__slider {
  height: 4px;
}

.internet-explorer {
    position: fixed;
    right: 10px;
    top: 10px;
    background-color: rgba(217, 217, 217, 0.64);
    font-size: 20px;
    pointer-events: none;
    padding: 10px;
    border-color: black;
    border-width: 1px;
    border-style: solid;
}


#foot-end-1 > .ol-mouse-position {
    top: unset;
    right: unset;
    position: unset;
    display: inline;
}

</style>