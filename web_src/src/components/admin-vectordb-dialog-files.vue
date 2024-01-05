<template>
    <span style="display: inline-block;">
        <v-dialog v-model="dialog" lazy width="800px">
            <v-btn title="Upload new files or remove existing." slot="activator">
                <v-icon left>folder_open</v-icon>Files
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Upload / Remove Files</div>
                </v-card-title>
                <v-card-text v-if="!refreshing">
                    <uploader :options="options" @file-success="fileFinished" ref="uploader">
                    <uploader-unsupport></uploader-unsupport>
                    <uploader-drop>
                        <p><b>Upload</b> (possibly multiple files at once) - Drop files here or</p>
                        <uploader-btn><v-icon>cloud_upload</v-icon>select files</uploader-btn>
                    </uploader-drop>
                    <uploader-list></uploader-list>
                    </uploader>
                    <br>
                    <hr>
                    <b>Files in layer:</b>
                    <br>
                    <br>
                    <table v-if="meta.data_filenames.length > 0">
                        <tr v-for="filename in meta.data_filenames" :key="filename">
                            <td :class="filename === meta.data_filename ? 'data-filename-primary' : 'data-filename-other'">{{filename}}</td>
                            <td v-if="filename === meta.data_filename"> &lt;--</td>
                            <td v-else></td>
                            <td v-if="filename === meta.data_filename"><b>(anchor)</b></td>
                            <td v-else><button class="button-set-anchor" @click="setAnchorFile(filename)">set anchor</button></td>
                            <td><button class="button-delete" @click="deleteFile(filename)">delete file</button></td>
                        </tr>
                    </table>
                    <div v-if="meta.data_filenames.length === 0">
                        no data files
                    </div>
                    <hr>
                    <br>
                    <i>"anchor" should point to the vector-file that should be visualised.
                        <br>For shapefile format anchor needs to be set to the ".shp"-file.
                        <br>".gpkg"-files (GeoPackage) are also supported.
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
    name: 'admin-vectordb-dialog-files',
    props: ['meta'],
    components: {
        'admin-task-console': adminTaskConsole,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            refreshing: false,
            remote_task_id: undefined,
            need_refresh_catalog_entry: false,

             options: {
                // https://github.com/simple-uploader/Uploader/tree/develop/samples/Node.js
                target: undefined,
                testChunks: false,
                chunkSize: 10*1024*1024,
                simultaneousUploads: 1,
            },
        }
    },
    methods: {
        
        refresh() {
            this.refreshing = true; // workaround for not refreshing uploader
            //console.log("admin-vectordb-dialog-files refresh");
            this.$nextTick(() => { // workaround for not refreshing uploader
                const options_target = this.$store.getters.apiUrl('vectordbs/' + this.meta.name + "/files");
                //this.options.target = options_target; // not updated in uploader
                this.options = { // copy set
                    // https://github.com/simple-uploader/Uploader/tree/develop/samples/Node.js
                    target: options_target,
                    testChunks: false,
                    chunkSize: 10*1024*1024,
                    simultaneousUploads: 1,
                };
                //console.log("admin-vectordb-dialog-files refresh done");
                this.refreshing = false; // workaround for not refreshing uploader
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
            return error.message + " - " + JSON.stringify(error.response.data);
        },

        fileFinished() {
            this.need_refresh_catalog_entry = true;
            console.log("fileFinished");
            this.$emit("changed");
        },

        deleteFile(filename) {
            var self = this;
            this.need_refresh_catalog_entry = true;
            var url = this.$store.getters.apiUrl('vectordbs/' + self.meta.name + "/files/" + filename);
            axios.delete(url
            ).then(function(response) {
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

        setAnchorFile(filename) {
            var self = this;
            this.need_refresh_catalog_entry = true;
            var url = this.$store.getters.apiUrl('vectordbs/' + self.meta.name);
            axios.post(url,{
                data_filename: filename,
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
        }

    },
    computed: {
    },
    watch: {
        meta() {
            //console.log("admin-vectordb-dialog-files watch meta");
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
