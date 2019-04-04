<template>
    <div class="main">
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center; padding-right: 400px;">
            <h3 class="headline mb-0">
                {{meta === undefined || meta.title === undefined || meta.title === '' ? pointcloud : meta.title}}
                <v-btn v-if="meta !== undefined && meta.associated.rasterdb !== undefined" icon class="indigo--text" :href="'#/viewer/' + meta.associated.rasterdb" target="_blank" title="open layer in viewer on new tab">
                    <v-icon>zoom_in</v-icon>
                </v-btn>
            </h3>
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''"><span style="user-select: none;">id: </span><b>{{pointcloud}}</b></span>
        </div>
        <div v-if="metaError">
            <b>ERROR</b> 
            {{metaErrorMessage}}
        </div>
        <div  v-if="meta != undefined">
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                <admin-pointcloud-dialog-set-info :meta="meta" @changed="refresh" v-if="modify" />
                Info
            </h3>
            <div class="meta-content">
                <a v-if="meta.associated.rasterdb !== undefined" :href="'#/viewer/' + meta.associated.rasterdb" target="_blank" title="open layer in viewer on new tab">
                    <img :key="meta.associated.rasterdb" :src="'../../rasterdb/' + meta.associated.rasterdb + '/raster.png?width=200'" alt="" class="thumbnail" />
                </a>
                <table style="padding-right: 420px;">
                    <tr>
                        <td><b>description:</b></td>
                        <td><span v-if="meta.description.length === 0" style="color: grey;">(none)</span><span v-else>{{meta.description}}</span></td>                        
                    </tr>
                    <tr>
                        <td><b>acquisition date:</b></td>
                        <td><span v-if="meta.acquisition_date === undefined" style="color: grey;">(none)</span><span v-else>{{meta.acquisition_date}}</span></td>                        
                    </tr>
                    <tr>
                        <td><b>corresponding contact:</b></td>
                        <td><span v-if="meta.corresponding_contact === undefined" style="color: grey;">(none)</span><span v-else>{{meta.corresponding_contact}}</span></td>                        
                    </tr>                                        
                    <tr>
                        <td><b>tags:</b></td>
                        <td>                      
                            <span v-for="tag in optArray(meta.tags)" :key="tag"><span class="meta-list">{{tag}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="optArray(meta.tags).length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>
                </table>
                </div>
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                <admin-pointcloud-dialog-set-projection :meta="meta" @changed="refresh" v-if="modify" />
                Projection
            </h3>
            <div class="meta-content">
                <table>
                    <tr>
                        <td><b>geo code:</b></td>
                        <td>
                            <span v-if="meta.code === undefined || meta.code.length === 0" style="color: grey;">(none)</span>
                            <span v-else>{{meta.code}}</span>
                        </td>
                    </tr>
                    <tr>
                        <td><b>proj4:</b></td>
                        <td>
                            <span v-if="meta.proj4 === undefined || meta.proj4.length === 0" style="color: grey;">(none)</span>
                            <span v-else>{{meta.proj4}}</span>
                        </td>
                    </tr>
                </table>          
            </div>

            <div v-if="isAdmin">
              <v-divider class="meta-divider"></v-divider>  
                <h3 class="subheading mb-0"> 
                    <admin-pointcloud-dialog-set-acl :meta="meta" @changed="refresh" />
                    Access control
                </h3>                        
                <div class="meta-content">
                <table>
                    <tr>
                        <td><b>access roles:</b></td>
                        <td>
                            <span v-for="role in meta.acl" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="meta.acl.length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>
                    
                    <tr>
                        <td><b>modify roles:</b></td>
                        <td>
                             <span v-for="role in meta.acl_mod" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="meta.acl_mod.length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>
                </table>
                </div>
            </div>

            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                Associated
            </h3>
            <div class="meta-content">
                <span v-if="Object.keys(meta.associated).length === 0" style="color: grey;">
                    (none)
                </span>
                <table>
                    <tr v-for="(names, type) in meta.associated" :key="type">
                        <td><b>{{type}}:</b></td>
                        <td><span v-for="name in optArray(names)" :key="name">
                            <span class="meta-list">{{name}}</span>
                            <v-btn v-if="type === 'PointDB'" icon class="indigo--text" :href="'#/layers/pointdbs/' + name" title="go to pointdb">
                                <v-icon>open_in_new</v-icon>
                            </v-btn>
                            <v-btn v-if="type === 'pointcloud'" icon class="indigo--text" :href="'#/layers/pointclouds/' + name" title="go to pointcloud">
                                <v-icon>open_in_new</v-icon>
                            </v-btn>
                            <v-btn v-if="type === 'rasterdb'" icon class="indigo--text" :href="'#/layers/rasterdbs/' + name" title="go to rasterdb">
                                <v-icon>open_in_new</v-icon>
                            </v-btn>
                            <v-btn v-if="type === 'poi_groups'" icon class="indigo--text" :href="'#/layers/poi_groups/' + name" title="go to POI group">
                                <v-icon>open_in_new</v-icon>
                            </v-btn>
                            <v-btn v-if="type === 'roi_groups'" icon class="indigo--text" :href="'#/layers/roi_groups/' + name" title="go to ROI group">
                                <v-icon>open_in_new</v-icon>
                            </v-btn>
                            &nbsp;&nbsp;&nbsp;
                        </span></td> 
                    </tr>
                </table>          
            </div>

            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                Details
            </h3>
            <div class="meta-content">
                <b></b> 
                <table>
                    <tr><td><b>point attributes:</b></td><td><span v-for="name in meta.attributes" :key="name"><span class="point-attributes">{{name}}</span>&nbsp;&nbsp;&nbsp;</span></td></tr>
                    <tr><td><b>point resolution:</b></td><td>{{1 / meta.cell_scale}}</td></tr>
                    <tr><td><b>tile size:</b></td><td>{{meta.cell_size}}</td></tr>
                </table>
            
            </div>

            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                Applications
            </h3>
            <div class="meta-content">
                <v-btn @click="app_lidar_indices"><v-icon>folder_open</v-icon>&nbsp;LiDAR indices processing</v-btn>
                <a :href="this.urlPrefix + '../../web/lidar_indices_description/lidar_indices_description.html'" target="_blank">Indices Description</a>
            </div>            
        </div>
    </div>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'
import adminPointcloudDialogSetInfo from './admin-pointcloud-dialog-set-info.vue'
import adminPointcloudDialogSetProjection from './admin-pointcloud-dialog-set-projection.vue'
import adminPointcloudDialogSetAcl from './admin-pointcloud-dialog-set-acl.vue'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'


export default {
    name: 'admin-pointcloud-detail',
    components: {
        'admin-pointcloud-dialog-set-info': adminPointcloudDialogSetInfo,
        'admin-pointcloud-dialog-set-projection': adminPointcloudDialogSetProjection,
        'admin-pointcloud-dialog-set-acl': adminPointcloudDialogSetAcl,
        PulseLoader,        
    },
    props: ['pointcloud'],
    data() {
        return {
            meta: undefined,
            metaError: false,
            metaErrorMessage: undefined,
            busy: false,
            busyMessage: undefined,
        }
    },
    methods: {
        refresh() { 
            var self = this;
            this.$store.dispatch('pointclouds/refresh');
            var url = '../../pointclouds/' + self.pointcloud;
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url)
                .then(function(response) {
                    self.meta = response.data.pointcloud;
                    self.busy = false;
                    self.busyMessage = undefined;
                })
                .catch(function(error) {
                    console.log(error);
                    self.metaError = true;
                    self.metaErrorMessage = "ERROR getting meta: " + error;
                    self.meta = undefined;
                    self.busy = false;
                    self.busyMessage = undefined;
                });
                /*.finally(function()  {
                    self.busy = false;
                    self.busyMessage = undefined;
                });*/
        },
        optArray(a) {
            if(a === undefined) {
                return [];
            }
            if(Array.isArray(a)) {
                return a;
            }
            return [a];
        },
        app_lidar_indices() {
            var url = this.urlPrefix + '../../web/lidar_indices/lidar_indices.html';
            window.open(url, '_blank');
        },
    },
    computed: {
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),
        modify() {
            return this.meta === undefined || this.meta.modify == undefined ? false :  this.meta.modify;
        },
        urlPrefix() {
            return this.$store.state.identity.urlPrefix;
        },
    },
    watch: {
        pointcloud() {
            this.refresh();
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style scoped>


.meta-divider {
    margin: 15px;
}

.tag-list {
    margin: 15px;
}

.main {
    position: relative;
}

.div-busy {
    position: absolute;
    color: rgb(129, 125, 125);
    background-color: rgb(243, 243, 243);
    font-size: 1.5em;
    width: 100%;
    height: 100%;
    z-index: 1;
}

.fade-enter-active {
  transition: opacity 2s;
}

.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}

.meta-content {
    padding-left: 100px;
}

.meta-list {
    background-color: rgb(243, 243, 243);
    padding: 1px;
    border-color: rgb(227, 227, 227);
    border-style: solid;
    border-width: 1px;
}

.point-attributes {
    background-color: rgb(227, 224, 222);
    padding: 1px;
    border-color: rgb(216, 214, 213);
    border-style: solid;
    border-width: 2px;
    color: #544141;
    border-radius: 5px;
}

.thumbnail {
    position: absolute;
    top: 0px;
    right: 0px;
    max-width: 420px;
    max-height: 600px;
    background-color: rgb(239, 239, 239);
    border-color: rgba(0, 0, 0, 0.1);
    border-style: solid;
    border-width: 1px;
}

</style>
