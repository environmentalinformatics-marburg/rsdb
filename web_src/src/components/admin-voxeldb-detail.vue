<template>
    <div class="main">
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center; padding-right: 400px;">
            <h3 class="headline mb-0">
                {{meta === undefined || meta.title === undefined || meta.title === '' ? voxeldb : meta.title}}
                <v-btn v-if="meta !== undefined && meta.associated.rasterdb !== undefined" icon class="indigo--text" :href="'#/viewer/' + meta.associated.rasterdb" target="_blank" title="open layer in viewer on new tab">
                    <v-icon>zoom_in</v-icon>
                </v-btn>
            </h3>
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''"><span style="user-select: none;">id: </span><b>{{voxeldb}}</b></span>
        </div>
        <div v-if="metaError">
            <b>ERROR</b> 
            {{metaErrorMessage}}
        </div>
        <div  v-if="meta != undefined">
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                <admin-voxeldb-dialog-set-info :meta="meta" @changed="refresh" v-if="modify" />
                Info
            </h3>
            <div class="meta-content">
                <a v-if="meta.associated.rasterdb !== undefined" :href="'#/viewer/' + meta.associated.rasterdb" target="_blank" title="open layer in viewer on new tab">
                    <img :key="meta.associated.rasterdb" :src="$store.getters.apiUrl('rasterdb/' + meta.associated.rasterdb + '/raster.png?width=200')" alt="" class="thumbnail" />
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
                <admin-voxeldb-dialog-set-projection :meta="meta" @changed="refresh" v-if="modify" />
                Projection
            </h3>
            <div class="meta-content">
                <table>
                    <tr>
                        <td><b>EPSG:</b></td>
                        <td>
                            <span v-if="meta.ref.epsg === undefined || meta.ref.epsg.length === 0" style="color: grey;">(none)</span>
                            <span v-else>{{meta.ref.epsg}}</span>
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

            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0">
                <admin-voxeldb-dialog-set-associated :meta="meta" @changed="refresh" v-if="modify" /> 
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
                            <v-btn v-if="type === 'voxeldb'" icon class="indigo--text" :href="'#/layers/voxeldbs/' + name" title="go to voxeldb">
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

                <div style="padding-bottom: 10px;"><b>voxel attributes:</b><span v-for="name in meta.attributes" :key="name"><span class="point-attributes">{{name}}</span>&nbsp;&nbsp;&nbsp;</span></div>

                <table class="table-details" style="display: inline-block;">
                    <thead>
                        <tr>                            
                            <th>Voxel size <div class="header-unit">{{projection_units}}</div></th>                       
                            <th>Cell size <div class="header-unit">voxels</div></th>
                            <th>Cell size <div class="header-unit">{{projection_units}}</div></th>
                            <th>Origin <div class="header-unit">coordinates</div></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>{{voxelSizeText}}</td>
                            <td>{{cellSizeText}}</td>
                            <td>{{cellSizeProjectedText}}</td>
                            <td>{{+meta.ref.origin.x.toPrecision(8)}}<b>,</b> {{+meta.ref.origin.y.toPrecision(8)}}<b>,</b> {{+meta.ref.origin.z.toPrecision(8)}}</td>                              
                         </tr>                                            
                    </tbody>
                </table>

                <v-btn v-if="meta.storage_measures === undefined" @click="refresh(true)" style="vertical-align: top;"><v-icon>arrow_drop_down</v-icon>&nbsp;more</v-btn>

                <table v-if="meta.storage_measures !== undefined" class="table-details">
                    <thead>
                        <tr>
                            <th>Cell extent <div class="header-unit">cell coordinates</div></th>
                            <th>Cell range <div class="header-unit">cells</div></th>
                            <th>Cell range <div class="header-unit">voxels</div></th>
                            <th>Cell range <div class="header-unit">{{projection_units}}</div></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>{{meta.storage_measures.cell_range.xmin}}<b>,</b> {{meta.storage_measures.cell_range.ymin}}<b>,</b> {{meta.storage_measures.cell_range.zmin}} <b>-</b> {{meta.storage_measures.cell_range.xmax}}<b>,</b> {{meta.storage_measures.cell_range.ymax}}<b>,</b> {{meta.storage_measures.cell_range.zmax}}</td>
                            <td>{{cellRangeText}}</td>
                            <td>{{cellRangeVoxelText}}</td>
                            <td>{{cellRangeProjectedText}}</td>
                         </tr>                    
                    </tbody>
                </table>

                <table v-if="meta.storage_measures !== undefined" class="table-details">
                    <thead>
                        <tr>
                            <th>Cells <div class="header-unit">count</div></th>
                            <th>Cell size <div class="header-unit">bytes</div></th>                            
                            <th>Storage size <div class="header-unit">bytes</div></th>
                            <th>Storage internal free size <div class="header-unit">bytes</div></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td v-if="meta.storage_measures.cell_count !== undefined">{{meta.storage_measures.cell_count}}</td>
                            <td v-else>none</td>
                            <td v-if="meta.storage_measures.cell_size_stats !== undefined">min: {{meta.storage_measures.cell_size_stats.min}}&nbsp;&nbsp;&nbsp;  mean: {{meta.storage_measures.cell_size_stats.mean}}&nbsp;&nbsp;&nbsp;  max: {{meta.storage_measures.cell_size_stats.max}}</td>
                            <td v-else>none</td>                              
                            <td v-if="meta.storage_measures.storage_size !== undefined">{{meta.storage_measures.storage_size}}</td>
                            <td v-else>none</td>
                            <td v-if="meta.storage_measures.storage_internal_free_size !== undefined">{{meta.storage_measures.storage_internal_free_size}}</td>
                            <td v-else>none</td>
                        </tr>                    
                    </tbody>
                </table>            
            </div>

            <v-divider class="meta-divider"></v-divider>  
            <h3 class="subheading mb-0"> 
                <admin-voxeldb-dialog-set-acl :meta="meta" @changed="refresh" v-if="isAdmin" />
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

            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0"> 
                Actions
            </h3>            
            <div class="meta-content" v-if="modify">
                <admin-voxeldb-dialog-delete :meta="meta" @changed="refresh" />
            </div>

        </div>
    </div>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'
import adminVoxeldbDialogSetInfo from './admin-voxeldb-dialog-set-info.vue'
import adminVoxeldbDialogSetProjection from './admin-voxeldb-dialog-set-projection.vue'
import adminVoxeldbDialogSetAssociated from './admin-voxeldb-dialog-set-associated.vue'
import adminVoxeldbDialogSetAcl from './admin-voxeldb-dialog-set-acl.vue'
import adminVoxeldbDialogDelete from './admin-voxeldb-dialog-delete'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'


export default {
    name: 'admin-voxeldb-detail',
    components: {
        'admin-voxeldb-dialog-set-info': adminVoxeldbDialogSetInfo,
        'admin-voxeldb-dialog-set-projection': adminVoxeldbDialogSetProjection,
        'admin-voxeldb-dialog-set-associated': adminVoxeldbDialogSetAssociated,
        'admin-voxeldb-dialog-set-acl': adminVoxeldbDialogSetAcl,
        'admin-voxeldb-dialog-delete': adminVoxeldbDialogDelete,
        PulseLoader,        
    },
    props: ['voxeldb'],
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
        refresh(more_details) { 
            var self = this;
            this.$store.dispatch('voxeldbs/refresh');
            var url = this.$store.getters.apiUrl('voxeldbs/' + self.voxeldb);
            var params = {};
            if(more_details) {
                params.storage_measures = true;
            }
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url, {params: params})
                .then(function(response) {
                    self.meta = response.data.voxeldb;
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
        projection_units() {
            if(this.meta === undefined || this.meta.ref === undefined || this.meta.ref.projection_unit === undefined || this.meta.ref.projection_unit.name === undefined) {
                return 'projection units';
            }
            return this.meta.ref.projection_unit.name;
        },
        voxelSizeText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.ref.voxel_size === undefined) {
                return 'not set';
            } else {
                let vs = this.meta.ref.voxel_size;
                return ((+vs.x.toPrecision(8)) + ' x ' + (+vs.y.toPrecision(8)) + ' x ' + (+vs.z.toPrecision(8)));
            }
        },
        cellSizeText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.cell_size === undefined) {
                return 'not set';
            } else {
                let cs = this.meta.cell_size;
                return ((+cs.x.toPrecision(8)) + ' x ' + (+cs.y.toPrecision(8)) + ' x ' + (+cs.z.toPrecision(8)));
            }
        },
        cellSizeProjectedText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.cell_size === undefined) {
                return 'not set';
            } else {
                let vs = this.meta.ref.voxel_size;
                let cs = this.meta.cell_size;
                return (+(cs.x * vs.x).toPrecision(8)) + ' x ' + (+(cs.y * vs.y).toPrecision(8)) + ' x ' + (+(cs.z * vs.z).toPrecision(8));
            }
        },
        cellRangeText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.ref.voxel_size === undefined) {
                return 'not set';
            } else {
                let r = this.meta.storage_measures.cell_range;
                return (r.xmax - r.xmin) + ' x ' + (r.ymax - r.ymin) + ' x ' + (r.zmax - r.zmin);
            }
        },
        cellRangeVoxelText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.ref.voxel_size === undefined) {
                return 'not set';
            } else {
                let r = this.meta.storage_measures.cell_range;
                let cs = this.meta.cell_size;
                return ((r.xmax - r.xmin) * cs.x) + ' x ' + ((r.ymax - r.ymin) * cs.y) + ' x ' + ((r.zmax - r.zmin) * cs.z);
            }
        },
        cellRangeProjectedText() {
            if (this.meta === undefined || this.meta.ref === undefined || this.meta.ref.voxel_size === undefined) {
                return 'not set';
            } else {
                let r = this.meta.storage_measures.cell_range;
                let cs = this.meta.cell_size;
                let vs = this.meta.ref.voxel_size;
                return ((r.xmax - r.xmin) * cs.x * vs.x) + ' x ' + ((r.ymax - r.ymin) * cs.y * vs.y) + ' x ' + ((r.zmax - r.zmin) * cs.z * vs.z);
            }
        },
    },
    watch: {
        voxeldb() {
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
    margin: 3px;
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

.unit {
    color: #000000c9;
    background-color: rgb(240, 240, 240);
    padding: 2px;
    border-radius: 5px;
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

.header-unit {
    color: rgba(0, 0, 0, 0.68); 
    font-weight:normal;
}

</style>
