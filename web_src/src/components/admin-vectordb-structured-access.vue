<template>
    <span style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px" :persistent="blocked">
            <v-btn title="Set name attibute" slot="activator">
                <v-icon left>folder_open</v-icon>Structured Access
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">set structured access</div>
                </v-card-title>
                <v-card-text>
                    <br>
                    <hr>
                    <b>enable/disable access types:</b>
                    <br>
                    <br>
                    <table>
                        <tr>
                            <td><v-switch v-model="structured_access_poi" label="POI-group" /></td><td style="padding-left: 20px;"><i>view of named <b>points</b> (Points Of Interest)</i></td>
                        </tr>
                        <tr>
                            <td><v-switch v-model="structured_access_roi" label="ROI-group" /></td><td style="padding-left: 20px;"><i>view of named <b>polygons</b> (Regions Of Interest)</i></td>
                        </tr>
                    </table>
                    <div v-if="meta.details.attributes.length === 0">
                        no attributes
                    </div>
                    <hr>
                    <br>
                    <p><b>Name</b> of POI-group / ROI-group is identical to <b>vector layer name</b>.</p>
                    <p><b>Names</b> for POIs and ROIs are determined by vector layer "<b>name attribute</b>" (may be set by "manage attributes" dialog).</p>
                    <p>For <b>POIs</b> just <b>point vector features</b> are included in resulting POI collection. (Other geometry types will be ignored.)</p>
                    <p>For <b>ROIs</b> just <b>polygon vector features</b> are included in resulting ROI collection. Multi-polygons and "holes" are not supported. (Other geometry types will be ignored.)</p>
                    <p>Projection is identical to source vector layer projection.</p>
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false" :disabled="blocked">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="apply" :disabled="blocked">Apply</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
        </v-snackbar>
        <admin-task-console :id="remote_task_id" closeOnDone @done="$emit('changed');" @error="$emit('changed');" />
    </span>
</template>

<script>

import Vue from 'vue'
import axios from 'axios'
import uploader from 'vue-simple-uploader'
import adminTaskConsole from './admin-task-console'

Vue.use(uploader)

export default {
    name: 'admin-vectordb-structured-access',
    props: ['meta'],
    components: {
        'admin-task-console': adminTaskConsole,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            remote_task_id: undefined,
            need_refresh_catalog_entry: false,
            structured_access_poi: false,
            structured_access_roi: false,
            blocked: false,
        }
    },
    methods: {        
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
            return error.message + " - " + JSON.stringify(error.response.data);
        },

        setNameAttribute(attribute) {
            var self = this;
            this.need_refresh_catalog_entry = true;
            var url = this.$store.getters.apiUrl('vectordbs/' + self.meta.name);
            axios.post(url,{
                name_attribute: attribute,
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error: " + self.errorToText(error);
                console.log(error);
                self.$emit('changed');
            });            
        },

        refresh(){
            this.structured_access_poi = this.meta.structured_access.poi;
            this.structured_access_roi = this.meta.structured_access.roi;
            this.need_refresh_catalog_entry = false;
        },

        refreshCatalogEntry() {
            var self = this;
            var url = this.$store.getters.apiUrl('api/remote_tasks');
            axios.post(url, {
                remote_task: {
                    task_vectordb: "refresh_catalog_entry",
                    vectordb: self.meta.name,
                }
            }).then(function(response) {
                console.log(response);
                var remote_task = response.data.remote_task;
                self.remote_task_id = remote_task.id;
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error: " + self.errorToText(error);
                console.log(error);
            });   
        },

        async apply() {
            this.blocked = true;
            try {
                var url = this.$store.getters.apiUrl('vectordbs/' + this.meta.name);
                var response = await axios.post(url,{
                    structured_access: {poi: this.structured_access_poi, roi: this.structured_access_roi},
                });
                console.log(response);
                this.dialog = false;
            } catch(error) {
                console.log(error);
                console.log(this.errorToText(error));
                this.setError = true;
                this.setErrorMessage = "Error: " + this.errorToText(error);
            } finally {
                this.blocked = false;
                this.$emit('changed');
            }
        }

    },
    computed: {
    },
    watch: {
        meta() {
            this.refresh();
        },
        dialog() {
            if(this.dialog) {
                this.refresh();
            } else {
                if(this.need_refresh_catalog_entry) {
                    this.refreshCatalogEntry();
                    this.need_refresh_catalog_entry = false;
                }
            }
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style>

.button-delete {
    background-color: #f009;
    border-radius: 8px;
    padding-left: 2px;
    padding-right: 2px;
    padding-top: 0px;
    padding-bottom: 0px;
    border-color: #590000;
    border-style: solid;
    border-width: 1px;
    box-shadow: 1px 1px 2px grey;
}

.button-set-anchor {
    background-color: #ffe10099;
    border-radius: 8px;
    padding-left: 2px;
    padding-right: 2px;
    padding-top: 0px;
    padding-bottom: 0px;
    border-color: #590000;
    border-style: solid;
    border-width: 1px;
    box-shadow: 1px 1px 2px grey;
}

</style>
