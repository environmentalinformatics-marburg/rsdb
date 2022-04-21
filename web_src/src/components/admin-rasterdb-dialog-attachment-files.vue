<template>
    <span style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="Upload new attachment files or remove existing.">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Upload / Remove Attachment Files</div>
                </v-card-title>
                <v-card-text>
                    <uploader :options="options" class="uploader-example" @file-success="fileFinished" ref="uploader">
                    <uploader-unsupport></uploader-unsupport>
                    <uploader-drop>
                        <p><b>Upload</b> (possibly multiple files at once) - Drop files here or</p>
                        <uploader-btn><v-icon>cloud_upload</v-icon>select files</uploader-btn>
                    </uploader-drop>
                    <uploader-list></uploader-list>
                    </uploader>
                    <br>
                    <hr>
                    <b>Files in layer attachments:</b>
                    <br>
                    <br>
                    <table v-if="meta.attachment_filenames.length > 0">
                        <tr v-for="filename in meta.attachment_filenames" :key="filename">
                            <td>{{filename}}</td>                           
                            <td><button class="button-delete" @click="deleteFile(filename)">delete file</button></td>
                        </tr>
                    </table>
                    <div v-if="meta.attachment_filenames.length === 0">
                        no data files
                    </div>
                    <hr>
                    <br>
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
    name: 'admin-rasterdb-dialog-files',
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
            this.options.target = this.$store.getters.apiUrl('rasterdb/' + this.meta.name + "/attachments");
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
            console.log("fileFinished");
            this.$emit("changed");
        },

        deleteFile(filename) {
            var self = this;
            var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + "/attachments/" + filename);
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

</style>