<template>
    <div class="main">        
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center; padding-right: 400px;">
            <h3 class="headline mb-0">
                {{meta === undefined || meta.title === undefined || meta.title === '' ? rasterdb : meta.title}}
                <v-btn icon class="indigo--text" :href="'#/viewer/' + rasterdb" target="_blank" title="open layer in viewer on new tab">
                    <v-icon>zoom_in</v-icon>
                </v-btn>
                <v-btn class="indigo--text" :href="$store.getters.apiUrl('web/app/#/?rasterdb=' + rasterdb)" target="_blank" title="open layer in viewer app on new tab">
                    <v-icon>launch</v-icon> Viewer app
                </v-btn>
            </h3> 
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''"><span style="user-select: none;">Identifier: </span><b>{{rasterdb}}</b></span>
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
                    :url="$store.getters.apiUrl('rasterdb/' + meta.name + '/set')" 
                    data_name="meta"
                />
                Info
            </h3>
            <div class="meta-content" style="poisition: relative;">
                <a :href="'#/viewer/' + rasterdb" target="_blank" title="open layer in viewer on new tab">
                    <img :key="meta.name" :src="$store.getters.apiUrl('rasterdb/' + meta.name + '/raster.png?width=200')" alt="" class="thumbnail" /> 
                </a>
                <box-info :meta="meta" />               
            </div>
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                <admin-rasterdb-dialog-set-projection :meta="meta" @changed="refresh" v-if="modify" />
                Projection
            </h3>
            <div class="meta-content">
                <table>
                    <tr>
                        <td><b>geo code:</b></td>
                        <td>
                            <span v-if="meta.ref.code === undefined || meta.ref.code.length === 0" style="color: grey;">(none)</span>
                            <span v-else>{{meta.ref.code}}</span>
                        </td>
                    </tr>
                    <tr>
                        <td><b>proj4:</b></td>
                        <td>
                            <span v-if="meta.ref.proj4 === undefined || meta.ref.proj4.length === 0" style="color: grey;">(none)</span>
                            <span v-else>{{meta.ref.proj4}}</span>
                        </td>
                    </tr>
                </table>          
            </div>

            <div>
                <v-divider class="meta-divider"></v-divider> 
                <h3 class="subheading mb-0">
                    <admin-rasterdb-dialog-set-associated :meta="meta" @changed="refresh" v-if="modify" />  
                    Associated
                </h3>
                <div class="meta-content">
                    <!--<span v-if="Object.keys(meta.associated).length === 0" style="color: grey;">
                        (none)
                    </span>-->
                    <table>
                        <tr v-for="(names, type) in meta.associated" :key="type">
                            <td><b>{{type}}:</b></td>
                            <td><span v-for="name in optArray(names)" :key="name">
                                <span class="meta-list">{{name}}</span>
                                <v-btn v-if="type === 'PointDB'" icon class="indigo--text" :href="'#/layers/pointdbs/' + name" title="go to pointdb layer info">
                                    <v-icon>open_in_new</v-icon>
                                </v-btn>
                                <v-btn v-if="type === 'pointcloud'" icon class="indigo--text" :href="'#/layers/pointclouds/' + name" title="go to pointcloud layer info">
                                    <v-icon>open_in_new</v-icon>
                                </v-btn>
                                <v-btn v-if="type === 'voxeldb'" icon class="indigo--text" :href="'#/layers/voxeldbs/' + name" title="go to voxeldb">
                                    <v-icon>open_in_new</v-icon>
                                </v-btn>                                
                                <v-btn v-if="type === 'poi_groups'" icon class="indigo--text" :href="'#/layers/poi_groups/' + name" title="go to POI group layer info">
                                    <v-icon>open_in_new</v-icon>
                                </v-btn>
                                <v-btn v-if="type === 'roi_groups'" icon class="indigo--text" :href="'#/layers/roi_groups/' + name" title="go to ROI group layer info">
                                    <v-icon>open_in_new</v-icon>
                                </v-btn>
                                &nbsp;&nbsp;&nbsp;
                            </span></td> 
                        </tr>
                    </table>          
                </div>
            </div>

            <div>
                <v-divider class="meta-divider"></v-divider> 
                <h3 class="subheading mb-0">
                    <admin-rasterdb-dialog-attachment-files :meta="meta" @changed="refresh" /> Attachments  <a :href="urlPrefix + '../../rasterdb/' + meta.name + '/attachments.zip'" :download="meta.name + '_attachments.zip'"  target="_blank" title="Download attachments as ZIP-file." style="font-size: 0.7em" v-if="meta.attachment_filenames.length > 0"><v-icon color="blue">cloud_download</v-icon>(download)</a>
                </h3>
                <div class="meta-content" v-if="meta.attachment_filenames.length > 0">
                    <table>
                        <tr v-for="filename in meta.attachment_filenames" :key="filename">
                            <td><a :href="urlPrefix + '../../rasterdb/' + meta.name + '/attachments/' + filename" target="_blank">{{filename}}</a></td>
                        </tr>
                    </table> 
                </div>
            </div>            

            <div v-if="meta.bands.length > 0">
                <v-divider class="meta-divider"></v-divider>
                <h3 class="subheading mb-0"> 
                    Bands ({{meta.bands.length}})
                </h3>
                <v-data-table v-bind:headers="bandsTableHeaders" :items="meta.bands" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td>{{ props.item.index }}</td>
                        <td>{{ props.item.title }}</td>
                        <td>{{ props.item.wavelength }}</td>
                        <td>{{ props.item.fwhm }}</td>
                        <td :class="toVisualisationClass(props.item.visualisation)">{{ props.item.visualisation}}</td>
                        <td>{{ props.item.datatype}}</td>
                    </template>
                </v-data-table>
            </div>

            <div v-if="meta.time_slices.length > 0">
                <v-divider class="meta-divider"></v-divider>
                <h3 class="subheading mb-0"> 
                    Time slices ({{meta.time_slices.length}})
                </h3>
                <v-data-table v-bind:headers="timeSliceTableHeaders" :items="meta.time_slices" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td>{{ props.item.id }}</td>
                        <td>{{ props.item.name }}</td>
                    </template>
                </v-data-table>
            </div>

            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0"> 
                    Details
            </h3>
            
            <div class="meta-content" v-if="meta.ref !== undefined">
                <table class="table-details" style="display: inline-block;">
                    <thead>
                        <tr>                            
                            <th>Pixel size <div class="header-unit">{{projection_units}}</div></th>
                            <th>Extent <div class="header-unit">pixel</div></th>
                            <th>Extent <div class="header-unit">{{projection_units}}</div></th>
                            <th>Extent <div class="header-unit">coordinates</div></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>{{pixelSizeText}}</td>
                            <td v-if="meta.ref.internal_rasterdb_extent !== undefined">{{meta.ref.internal_rasterdb_extent[2] - meta.ref.internal_rasterdb_extent[0] + 1}} <b>x</b> {{meta.ref.internal_rasterdb_extent[3] - meta.ref.internal_rasterdb_extent[1] + 1}}</td>
                            <td v-else>none</td>
                            <td v-if="meta.ref.internal_rasterdb_extent !== undefined && meta.ref.pixel_size !== undefined">{{((meta.ref.internal_rasterdb_extent[2] - meta.ref.internal_rasterdb_extent[0] + 1) * meta.ref.pixel_size.x).toPrecision(8)}} <b>x</b> {{((meta.ref.internal_rasterdb_extent[3] - meta.ref.internal_rasterdb_extent[1] + 1) * meta.ref.pixel_size.y).toPrecision(8)}}</td>
                            <td v-else>none</td> 
                            <td v-if="meta.ref.extent !== undefined">{{meta.ref.extent[0].toPrecision(8)}}<b>,</b> {{meta.ref.extent[1].toPrecision(8)}} <b>-</b> {{meta.ref.extent[2].toPrecision(8)}}<b>,</b> {{meta.ref.extent[3].toPrecision(8)}}</td>
                            <td v-else>none</td>                            
                        </tr>                    
                    </tbody>
                </table>

                <v-btn v-if="meta.tile_count === undefined" @click="refresh(true)" style="vertical-align: top;"><v-icon>arrow_drop_down</v-icon>&nbsp;more</v-btn>
                
                <div v-if="meta.tile_count !== undefined">
                    <table class="table-details" style="display: inline-block;">
                        <thead>
                            <tr>
                                <th>Origin <div class="header-unit">coordinates</div></th>                                  
                                <th>Tiles <div class="header-unit">count</div></th>
                                <th>Tile size <div class="header-unit">pixel</div></th>                             
                                <th>Internal extent <div class="header-unit">pixel</div></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td v-if="meta.ref.internal_rasterdb_offset !== undefined">{{meta.ref.internal_rasterdb_offset.x.toPrecision(8)}}<b>,</b> {{meta.ref.internal_rasterdb_offset.y.toPrecision(8)}}</td>
                                <td v-else>none</td>                                
                                <td v-if="meta.tile_count !== undefined">{{meta.tile_count}}</td>
                                <td v-else>none</td>  
                                <td v-if="meta.tile_pixel_len !== undefined">{{meta.tile_pixel_len}} <b>x</b> {{meta.tile_pixel_len}}</td> 
                                <td v-else>none</td>                               
                                <td v-if="meta.ref.internal_rasterdb_extent !== undefined">{{meta.ref.internal_rasterdb_extent[0]}}<b>,</b> {{meta.ref.internal_rasterdb_extent[1]}} <b>-</b> {{meta.ref.internal_rasterdb_extent[2]}}<b>,</b> {{meta.ref.internal_rasterdb_extent[3]}}</td>
                                <td v-else>none</td>                                                              
                            </tr>                    
                        </tbody>
                    </table>

                    <table class="table-details">
                        <thead>
                            <tr>
                                <th>Storage size <div class="header-unit">bytes</div></th>
                                <th>Storage internal free size <div class="header-unit">bytes</div></th>
                                <th>Tile size <div class="header-unit">bytes</div></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td v-if="meta.storage_size !== undefined">{{meta.storage_size}}</td>
                                <td v-else>none</td>
                                 <td v-if="meta.storage_internal_free_size !== undefined">{{meta.storage_internal_free_size}}</td>
                                <td v-else>none</td>  
                                 <td v-if="meta.tile_size_stats !== undefined">min: {{meta.tile_size_stats.min}}&nbsp;&nbsp;&nbsp;  mean: {{meta.tile_size_stats.mean}}&nbsp;&nbsp;&nbsp;  max: {{meta.tile_size_stats.max}}</td>
                                <td v-else>none</td>                                                             
                            </tr>                    
                        </tbody>
                    </table>
                </div>

            </div>

            <v-divider class="meta-divider"></v-divider>  
            <h3 class="subheading mb-0"> 
                <admin-rasterdb-dialog-set-acl :meta="meta" @changed="refresh" v-if="meta.owner" />
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
                <admin-rasterdb-dialog-wms :meta="meta" @changed="refresh" />
                <!--<a :href="this.urlPrefix + '../../web/wms_access/wms_access.html'" target="_blank">WMS access info (obsolete)</a>-->
                <admin-rasterdb-dialog-wcs :meta="meta" @changed="refresh" />
            </div> 

            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0"> 
                Administration
            </h3>            
            <div class="meta-content" v-if="modify">                
                <admin-rasterdb-bands :meta="meta" @changed="refresh" />
                <admin-rasterdb-dialog-remove-timestamps :meta="meta" @changed="refresh" />
                <admin-rasterdb-dialog-custom_wms :meta="meta" @changed="refresh" />
                <admin-rasterdb-dialog-custom_wcs :meta="meta" @changed="refresh" />
                <br><br><a href="#/tools/task">more available as <i>tasks</i> at 'tools'-tab - 'task creation' (e.g. <b>layer renaming</b>)</a>
                <br><br>
                <admin-rasterdb-dialog-delete :meta="meta" @changed="refresh" />
            </div>
 
        </div>
    </div>
</template>

<script>


import { mapGetters } from 'vuex'
import axios from 'axios'
import dialogSetInfo from './dialog-set-info.vue'
import boxInfo from './box-info.vue'
import adminRasterdbDialogSetProjection from './admin-rasterdb-dialog-set-projection.vue'
import adminRasterdbDialogSetAssociated from './admin-rasterdb-dialog-set-associated.vue'
import adminRasterdbDialogSetAcl from './admin-rasterdb-dialog-set-acl.vue'
import adminRasterdbDialogDelete from './admin-rasterdb-dialog-delete'
import adminRasterdbDialogRemoveTimestamps from './admin-rasterdb-dialog-remove-timestamps'
import adminRasterdbDialogCustom_wms from './admin-rasterdb-dialog-custom_wms'
import adminRasterdbDialogCustom_wcs from './admin-rasterdb-dialog-custom_wcs'
import adminRasterdbDialogBands from './admin-rasterdb-dialog-bands.vue'
import adminRasterdbDialogWms from './admin-rasterdb-dialog-wms.vue'
import adminRasterdbDialogWcs from './admin-rasterdb-dialog-wcs.vue'
import adminRasterdbDialogAttachmentFiles from './admin-rasterdb-dialog-attachment-files.vue'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'

export default {
    name: 'admin-rasterdb-detail', 
    components: {
        'dialog-set-info': dialogSetInfo,
        'box-info': boxInfo,
        'admin-rasterdb-dialog-set-projection': adminRasterdbDialogSetProjection,
        'admin-rasterdb-dialog-set-associated': adminRasterdbDialogSetAssociated,
        'admin-rasterdb-dialog-set-acl': adminRasterdbDialogSetAcl,
        'admin-rasterdb-dialog-delete': adminRasterdbDialogDelete,
        'admin-rasterdb-dialog-remove-timestamps': adminRasterdbDialogRemoveTimestamps,
        'admin-rasterdb-dialog-custom_wms': adminRasterdbDialogCustom_wms,
        'admin-rasterdb-dialog-custom_wcs': adminRasterdbDialogCustom_wcs,
        'admin-rasterdb-bands': adminRasterdbDialogBands,
        'admin-rasterdb-dialog-wms': adminRasterdbDialogWms,
        'admin-rasterdb-dialog-wcs': adminRasterdbDialogWcs,
        'admin-rasterdb-dialog-attachment-files': adminRasterdbDialogAttachmentFiles,
        PulseLoader,
    },
    props: ['rasterdb'],
    data() {
        return {
            meta: undefined,
            metaError: false,
            metaErrorMessage: undefined,
            busy: false,
            busyMessage: undefined,
            bandsTableHeaders: [{ text: "Id", align: 'right', value: "index", width: "55px" }, { text: "Name", align: 'left', value: "title" }, { text: "Wavelength", align: 'left', value: "wavelength", width: "10px" }, { text: "Fwhm", align: 'left', value: "fwhm", width: "10px" }, { text: "Visualisation", align: 'left', value: "visualisation", width: "100px" }, { text: "Type", align: 'left', value: "datatype", width: "20px" }],
            timeSliceTableHeaders: [{ text: "Id", align: 'right', value: "id", width: "55px" }, { text: "Name", align: 'left', value: "name" }],
            panel: [true, true],
        }
    },
    methods: {
        refresh(more_details) {
            var self = this;
            this.$store.dispatch('rasterdbs/refresh');
            var url = this.$store.getters.apiUrl('rasterdb/' + self.rasterdb + '/meta.json?attachment_filenames');
            var params = {};
            if(more_details) {
                params.tile_count = true;
                params.storage_size = true;
                params.storage_internal_free_size = true;
                params.tile_size_stats = true;                
            }
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url, {params: params})
                .then(function(response) {
                    self.meta = response.data;
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
                /*.finally(function()  {
                    self.busy = false;
                    self.busyMessage = undefined;
                });*/
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
        toVisualisationClass(visualisation) {
            if(visualisation === 'red') {
                return 'visualisation-red';
            }
            if(visualisation === 'green') {
                return 'visualisation-green';
            }
            if(visualisation === 'blue') {
                return 'visualisation-blue';
            }
            return 'visualisation-unspecified';
        },
    },
    computed: {
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),
        pixelSizeText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.ref.pixel_size === undefined) {
                return 'not set';
            } else {
                return this.meta.ref.pixel_size.x === this.meta.ref.pixel_size.y ? ((+this.meta.ref.pixel_size.x.toPrecision(8)) + ' x ' + (+this.meta.ref.pixel_size.x.toPrecision(8))) : (+this.meta.ref.pixel_size.x.toPrecision(8)) + ' x ' + (+this.meta.ref.pixel_size.y.toPrecision(8));
            }
        },
        modify() {
            return this.meta === undefined || this.meta.modify == undefined ? false :  this.meta.modify;
        },
        urlPrefix() {
            return this.$store.state.identity.urlPrefix;
        },
        projection_units() {
            return this.meta !== undefined 
                && this.meta.ref !== undefined
                && this.meta.ref.unit !== undefined
                && this.meta.ref.unit.name !== undefined ? this.meta.ref.unit.name :  'projection units';
        },
    },
    mounted() {
        this.refresh();
    },
    watch: {
        rasterdb() {
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

.table-details th {
    padding: 3px;
    background-color: #d7d7d7;
}

.table-details td {
    padding: 3px;
    background-color: rgb(233, 233, 233);
    text-align: center;
}

.unit {
    color: #000000c9;
    background-color: rgb(240, 240, 240);
    padding: 2px;
    border-radius: 5px;
}

.header-unit {
    color: rgba(0, 0, 0, 0.68); 
    font-weight:normal;
}


.visualisation-red {
    color: #d51717e5
}

.visualisation-green {
    color: #24a824e5;
}

.visualisation-blue {
    color: #0f4ee1;
}

.visualisation-unspecified {
    color: grey;
}

</style>
