<template>
    <span>
        <v-dialog v-model="dialog" lazy width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set Associated layers of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text style="min-height: 400px;">                  
                    PointDB-layer: This RasterDB layer is visualisation of that PointDB.
                    <multiselect v-model="selectedPointdb" :options="pointdbs" placeholder="select pointdb-layer" :allow-empty="true"/>
                    <br> 
                    PointCloud-layer: This RasterDB layer is visualisation of that PointCloud.
                    <multiselect v-model="selectedPointcloud" :options="pointclouds" placeholder="select pointcloud-layer" :allow-empty="true"/>     
                    <br>
                    VoxelDB-layer: This RasterDB layer is visualisation of that VoxelDB.
                    <multiselect v-model="selectedVoxeldb" :options="voxeldbs" placeholder="select voxeldb-layer" :allow-empty="true"/>     
                    <br> 
                    POI-groups: Groups of named geo-points that can be used for processing.
                    <multiselect v-model="selectedPoi_groups" :options="poi_groups" placeholder="select poi-groups" :allow-empty="true" :multiple="true"/>
                    <br> 
                    ROI-groups: Groups of named polygons that can be used for processing.
                    <multiselect v-model="selectedRoi_groups" :options="roi_groups" placeholder="select roi-groups" :allow-empty="true" :multiple="true"/>                                 
                </v-card-text>                
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="set">Set</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
        </v-snackbar>
    </span>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

/*function optArray(a) {
    if(a === undefined) {
        return [];
    }
    if(Array.isArray(a)) {
        return a;
    }
    return [a];
}*/

export default {
    name: 'admin-rasterdb-dialog-set-associated',
    props: ['meta'],

    components: {
        Multiselect,
    },

    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            selectedPointdb: undefined,
            selectedPointcloud: undefined,
            selectedVoxeldb: undefined,
            selectedPoi_groups: [],     
            selectedRoi_groups: [],   
        }
    },
    computed: {
        ...mapGetters({
            pointdbs: 'pointdbs/names',
            pointclouds: 'pointclouds/names',
            voxeldbs: 'voxeldbs/names',
            poi_groups: 'poi_groups/names',
            roi_groups: 'roi_groups/names',            
        }),        
    }, 
    methods: {
        refresh() {
            this.selectedPointdb = this.meta.associated.PointDB;
            this.selectedPointcloud = this.meta.associated.pointcloud;
            this.selectedVoxeldb = this.meta.associated.voxeldb;   
            this.selectedPoi_groups = this.meta.associated.poi_groups;
            this.selectedRoi_groups = this.meta.associated.roi_groups;            
            this.$store.dispatch('pointdbs/refresh');
            this.$store.dispatch('pointclouds/refresh');
            this.$store.dispatch('poi_groups/refresh');
            this.$store.dispatch('roi_groups/refresh');            
        },

        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + '/set');
            var associated = {};
            if(this.selectedPointdb !== undefined) {
               associated.PointDB = this.selectedPointdb;
            }
            if(this.selectedPointcloud !== undefined) {
               associated.pointcloud = this.selectedPointcloud;
            }
            if(this.selectedVoxeldb !== undefined) {
               associated.voxeldb = this.selectedVoxeldb;
            }
            associated.poi_groups = this.selectedPoi_groups;
            associated.roi_groups = this.selectedRoi_groups;            
            axios.post(url, {
                meta: {
                    associated: associated
                }
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
                self.dialog = false;
            }).catch(function(error) {
                 self.setError = true;
                self.setErrorMessage = "Error setting property";
                console.log(error);
                self.$emit('changed');
                self.dialog = false;
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
        }

    },
    watch: {
        meta() {
            this.refresh();
        },
        dialog() {
            if(this.dialog) {
                this.refresh();
            }
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style>

</style>
