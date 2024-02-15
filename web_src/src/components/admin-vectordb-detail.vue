<template>
    <div class="main">        
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center; padding-right: 400px;">
            <h3 class="headline mb-0">
                {{meta === undefined || meta.title === undefined || meta.title === '' ? vectordb : meta.title}}
                <!--<v-btn icon class="indigo--text" :href="'#/vectorviewer/' + vectordb" target="_blank" title="open layer in viewer on new tab">
                    <v-icon>zoom_in</v-icon>
                </v-btn>-->
                <v-btn icon class="indigo--text" :href="'#/viewer?vectordb=' + vectordb" target="_blank" title="open layer in viewer on new tab">
                    <v-icon>zoom_in</v-icon>
                </v-btn>
                <v-btn class="indigo--text" :href="$store.getters.apiUrl('web/app/#/?vectordb=' + vectordb)" target="_blank" title="open layer in viewer app on new tab">
                    <v-icon>launch</v-icon> Viewer app
                </v-btn>
            </h3> 
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''"><span style="user-select: none;">id: </span><b>{{vectordb}}</b></span>
            </div>
        <div v-if="metaError">
            <b>ERROR</b> 
            {{metaErrorMessage}}
        </div>
        <div  v-if="meta != undefined">

            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                <dialog-set-info 
                    @changed="refresh" 
                    v-if="modify"
                    :meta="meta" 
                    :url="$store.getters.apiUrl('vectordbs/' + meta.name)" 
                />                
                Info
            </h3>
            <div class="meta-content" style="poisition: relative;">
                <a :href="'#/vectorviewer/' + vectordb" target="_blank" title="open layer in viewer on new tab">
                    <img :key="meta.name" :src="$store.getters.apiUrl('vectordbs/' + meta.name + '/raster.png?width=400&height=600&datatag=' + meta.datatag)" alt="" class="thumbnail" /> 
                </a>
                <box-info :meta="meta" /> 
            </div>
            
            <v-divider class="meta-divider"></v-divider>

            <h3 class="subheading mb-0"> 
                Content <a v-show="meta.data_filenames.length > 0" :href="urlPrefix + '../../vectordbs/' + meta.name + '/package.zip'" :download="meta.name + '.zip'" title="download as ZIP-file" style="font-size: 0.7em"><v-icon color="blue">cloud_download</v-icon>(download)</a>
            </h3>
            <div class="meta-content">
            <table v-if="meta.data_filenames.length > 0">
                <!--<th>
                    <td>data files</td>
                </th>-->
                <tr v-for="filename in meta.data_filenames" :key="filename">
                    <td :class="filename === meta.data_filename ? 'data-filename-primary' : 'data-filename-other'">{{filename}}</td>
                    <td v-if="filename === meta.data_filename"> &lt;--</td>
                </tr>
            </table>
            <div v-if="meta.data_filenames.length === 0" style="color: grey;">
                <b>no files.</b> Click "Manage Files" to upload files.
            </div>
            </div>

        <v-divider class="meta-divider"></v-divider> 
        <h3 class="subheading mb-0"> 
            Details
        </h3>
        <div class="meta-content">
            <table>
                <tr><td><b>EPSG:</b></td><td>{{meta.details.epsg}}</td></tr>                
                <tr><td><b>Proj4:</b></td><td>{{meta.details.proj4}}</td></tr>
                <tr><td><b>attributes:</b></td><td style="display: flex; flex-wrap: wrap;"><span v-for="name in meta.details.attributes" :key="name"><span :class="name === meta.name_attribute ? 'name-attribute' : 'attributes'" style="margin-right: 10px;">{{name}}</span></span></td></tr>
                <tr><td><b>structured access:</b></td><td v-if="!meta.structured_access.poi && !meta.structured_access.roi">none</td><td v-if="meta.structured_access.poi">POI-group</td><td v-if="meta.structured_access.roi">ROI-group</td></tr>
            </table>
            <admin-vectordb-dialog-data-table :meta="meta" @changed="refresh" />
        </div>

        <v-divider class="meta-divider"></v-divider>  
        <h3 class="subheading mb-0"> 
            <admin-vectordb-dialog-set-acl :meta="meta" @changed="refresh" v-if="meta.owner" />
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
            <tr>
                <td><b>owner roles:</b></td>
                <td>
                    <span v-for="role in meta.acl_owner" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                    <span v-if="meta.acl_owner.length === 0" style="color: grey;">(none)</span>
                </td>
            </tr>
        </table>
        </div>

        <v-divider class="meta-divider"></v-divider> 
        <h3 class="subheading mb-0"> 
            Accessibility
        </h3>
        <div class="meta-content">
            <admin-vectordb-dialog-wfs :meta="meta" @changed="refresh" />
            <admin-vectordb-dialog-wms :meta="meta" @changed="refresh" />
        </div>         

        <div v-if="modify">
            <v-divider class="meta-divider"></v-divider>        
            <h3 class="subheading mb-0"> 
                Administration
            </h3>
            <div class="meta-content">
                <admin-vectordb-dialog-files :meta="meta" @changed="refresh" v-if="modify" />                
                <admin-vectordb-attributes :meta="meta" @changed="refresh" v-if="modify" />                
                <admin-vectordb-structured-access :meta="meta" @changed="refresh(); $store.dispatch('poi_groups/refresh'); $store.dispatch('roi_groups/refresh');" v-if="modify" />
                <a href="#/tools/task"> more at 'tools'-tab - 'task creation' (e.g. <b>layer renaming</b>)</a>
                <br>
                <br>
                <br>
                <admin-vectordb-delete :meta="meta" @changed="refresh" v-if="modify" />
            </div>
            </div>
        </div>

    </div>
</template>

<script>


import { mapState, mapGetters } from 'vuex'
import axios from 'axios'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'
import dialogSetInfo from './dialog-set-info.vue'
import boxInfo from './box-info.vue'
import adminVectordbDelete from './admin-vectordb-delete'
import adminVectordbDialogFiles from './admin-vectordb-dialog-files'
import adminVectordbAttributes from './admin-vectordb-attributes'
import adminVectordbStructuredAccess from './admin-vectordb-structured-access'
import adminVectordbDialogSetAcl from './admin-vectordb-dialog-set-acl.vue'
import adminVectordbDialogDataTable from './admin-vectordb-dialog-data-table.vue'
import adminVectordbDialogWfs from './admin-vectordb-dialog-wfs.vue'
import adminVectordbDialogWms from './admin-vectordb-dialog-wms.vue'

export default {
    name: 'admin-vectordb-detail', 
    components: {
        PulseLoader,
        'dialog-set-info':dialogSetInfo,
        'box-info': boxInfo,
        'admin-vectordb-delete': adminVectordbDelete,
        'admin-vectordb-dialog-files': adminVectordbDialogFiles,
        'admin-vectordb-attributes': adminVectordbAttributes,
        'admin-vectordb-structured-access': adminVectordbStructuredAccess,
        'admin-vectordb-dialog-set-acl': adminVectordbDialogSetAcl,
        'admin-vectordb-dialog-data-table': adminVectordbDialogDataTable,
        'admin-vectordb-dialog-wfs': adminVectordbDialogWfs,
        'admin-vectordb-dialog-wms': adminVectordbDialogWms,
    },
    props: ['vectordb'],
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
            this.$store.dispatch('vectordbs/refresh');
            var url = this.urlPrefix + '../../vectordbs/' + self.vectordb;
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url)
                .then(function(response) {
                    self.meta = response.data.vectordb;
                    self.busy = false;
                    self.busyMessage = undefined;
                })
                .catch(function(error) {
                    //console.log(JSON.stringify(error));
                    self.metaError = true;
                    self.metaErrorMessage = "getting meta data: " + self.errorToText(error);
                    self.meta = undefined;
                    self.busy = false;
                    self.busyMessage = undefined;
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
        optArray(a) {
            if(a === undefined) {
                return [];
            }
            if(Array.isArray(a)) {
                return a;
            }
            return [a];
        },
    },
    computed: {
        ...mapState({
            urlPrefix: state => state.identity.urlPrefix,
        }),
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),        
        modify() {
            return this.meta === undefined || this.meta.modify == undefined ? false :  this.meta.modify;
        },
    },
    mounted() {
        this.refresh();
    },
    watch: {
        vectordb() {
            this.refresh();
        }
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
    word-wrap: break-word;
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
    background-color: rgb(232, 232, 232);
    padding: 1px;
    margin-right: 5px;
    border-color: rgba(0, 0, 0, 0.062);
    border-style: solid;
    border-width: 1px;
}

.data-filename-primary {
    color: black;
}

.data-filename-other {
    color: grey;
}

.attributes {
    background-color: rgb(232, 232, 232);
    padding: 1px;
    margin-left: 2px;
    margin-right: 3px;
    border-color: rgba(0, 0, 0, 0.062);
    border-style: solid;
    border-width: 2px;
    color: #544141;
    border-radius: 5px;
}

.name-attribute {
    background-color: rgb(212, 212, 212);
    padding: 1px;
    margin-left: 2px;
    margin-right: 3px;
    border-color: rgba(0, 0, 0, 0.637);
    border-style: solid;
    border-width: 2px;
    color: #000000;
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
