<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="Set name attibute" slot="activator">
                <v-icon left>folder_open</v-icon>Manage Attributes
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set name attribute</div>
                </v-card-title>
                <v-card-text>
                    <br>
                    <hr>
                    <b>Attributes:</b>
                    <br>
                    <br>
                    <table>
                        <tr v-for="attribute in meta.details.attributes" :key="attribute">
                            <td :class="attribute=== meta.name_attribute ? 'data-filename-primary' : 'data-filename-other'">{{attribute}}</td>
                            <td v-if="attribute === meta.name_attribute"> &lt;--</td>
                            <td v-else></td>
                            <td v-if="attribute === meta.name_attribute"><b>(name attribute)</b></td>
                            <td v-else><button class="button-set-anchor" @click="setNameAttribute(attribute)">set name attribute</button></td>
                        </tr>
                    </table>
                    <div v-if="meta.details.attributes.length === 0">
                        no attributes
                    </div>
                    <hr>
                    <br>
                    <i>"name attribute" denotes the attribut that should be used as name of a vector feature.
                    </i>
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false">Close</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
        </v-snackbar>
        <admin-task-console :id="remote_task_id" @done="$emit('changed');" @error="$emit('changed');" />
    </span>
</template>

<script>

import Vue from 'vue'
import axios from 'axios'
import uploader from 'vue-simple-uploader'
import adminTaskConsole from './admin-task-console'

Vue.use(uploader)

export default {
    name: 'admin-vectordb-attributes',
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
            need_refreh_catalog_entry: false,
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

        fileFinished() {
            this.need_refreh_catalog_entry = true;
            console.log("fileFinished");
            this.$emit("changed");
        },

        setNameAttribute(attribute) {
            var self = this;
            this.need_refreh_catalog_entry = true;
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

        refreshCatalogEntry() {
            var self = this;
            var url = this.$store.getters.apiUrl('api/insert_remote_task');
            axios.post(url, {
                remote_task: {
                    task_vectordb: "refreh_catalog_entry",
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
                if(this.need_refreh_catalog_entry) {
                    this.refreshCatalogEntry();
                    this.need_refreh_catalog_entry = false;
                }
            }
            if(this.$refs.uploader !== undefined) {
                var files = this.$refs.uploader.files.slice(0);
                for(var i in files) {                    
                    files[i].removeFile(files[i]);
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
