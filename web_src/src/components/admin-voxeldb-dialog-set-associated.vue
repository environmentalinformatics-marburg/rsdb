<template>
    <span>
        <v-dialog v-model="dialog" lazy width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set Associated layers of <i>VoxelDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text style="min-height: 400px;">  
                
                    RasterDB-layer: Raster-visualisation of this voxeldb.
                   <multiselect v-model="selectedRasterdb" :options="rasterdbs" placeholder="select rasterdb-layer" :allow-empty="true"/>
                   <br> 
                   POI-groups: Groups of named geo-points that can be used for processing this voxeldb.
                   <multiselect v-model="selectedPoi_groups" :options="poi_groups" placeholder="select poi-groups" :allow-empty="true" :multiple="true"/>
                   <br> 
                   ROI-groups: Groups of named polygons that can be used for processing this voxeldb.
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
    name: 'admin-voxeldb-dialog-set-associated',
    props: ['meta'],

    components: {
        Multiselect,
    },

    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            selectedRasterdb: undefined,
            selectedPoi_groups: [],     
            selectedRoi_groups: [],   
        }
    },
    computed: {
        ...mapGetters({
            rasterdbs: 'rasterdbs/names',
            poi_groups: 'poi_groups/names',
            roi_groups: 'roi_groups/names',
        }),        
    }, 
    methods: {
        refresh() {
            this.selectedRasterdb = this.meta.associated.rasterdb;
            this.selectedPoi_groups = this.meta.associated.poi_groups;
            this.selectedRoi_groups = this.meta.associated.roi_groups;
            this.$store.dispatch('rasterdbs/refresh');
            this.$store.dispatch('poi_groups/refresh');
            this.$store.dispatch('roi_groups/refresh');
        },

        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('voxeldbs/' + self.meta.name);
            var associated = {};
            if(this.selectedRasterdb !== undefined) {
               associated.rasterdb = this.selectedRasterdb;
            }
            associated.poi_groups = this.selectedPoi_groups;
            associated.roi_groups = this.selectedRoi_groups;
            axios.post(url, {
                voxeldb: {
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
