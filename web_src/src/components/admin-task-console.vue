<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-card>
                <v-card-title>
                    <div class="headline">Task Console</div>
                </v-card-title>
                <v-icon v-if="remote_task.status === 'READY'" color="yellow">directions_walk</v-icon>
                <v-icon v-if="remote_task.status === 'RUNNING'" color="black">directions_run</v-icon>
                <v-icon v-if="remote_task.status === 'DONE'" color="green">done</v-icon>
                <v-icon v-if="remote_task.status === 'ERROR'" color="red">error</v-icon>
                {{remote_task.status}}
                <span style="padding-left: 50px;">
                <span v-if="(remote_task.status === 'READY' || remote_task.status === 'RUNNING') && !remote_task.canceled && remote_task.cancelable"><v-btn icon @click="cancel(remote_task.id)"><v-icon>cancel</v-icon> cancel</v-btn></span>
                <span v-if="!remote_task.canceled && !remote_task.cancelable">---</span>
                <span v-if="remote_task.canceled">cancel requested</span>
                </span>
                <br>
                id: {{id}}, time {{(remote_task.runtime/1000).toFixed()}} s
                <br>
                {{remote_task.message}}
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <span v-if="status === 'error'">error {{setErrorMessage}} <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Close</v-btn></span>
                </v-card-actions>
                <span style="color: grey;"><v-icon>event_note</v-icon> Click outside of this box to close it. 
                <br>Task continues to run and can be viewed on status view.</span>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false" >Close</v-btn>
        </v-snackbar>
    </span>
</template>

<script>

import { mapState } from 'vuex'
import axios from 'axios'

export default {
    name: 'admin-task-console',
    props: {'id': Number, 'closeOnDone': Boolean},
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            remote_task: {},
            status: 'init',
        }
    },
    methods: {
        
        refresh() {
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

        query_remote_task() {
            var self = this;
            axios.get(this.urlPrefix + '../../api/remote_tasks/' + this.id)
            .then(function(response) {
                console.log(response.data.remote_task);
                self.remote_task = response.data.remote_task;
                if(self.remote_task.active) {
                    self.status = 'running';
                    window.setTimeout(function() {self.query_remote_task();}, 1000);
                } else {
                    if(self.remote_task.status === 'DONE') {
                        self.status = "done";
                        if(self.closeOnDone) {
                            self.dialog = false;
                        }
                        self.$emit('done');
                    } else {
                        self.status = "error";
                        console.log("error");
                        self.$emit('error');
                    }
                    console.log(response);
                }
            }).catch(function(error) {
                self.setErrorMessage = self.errorToText(error);
                self.status = "error";
                if(self.dialog) {
                    window.setTimeout(function() {self.query_remote_task();}, 1000);
                }
            });            
        },

        async cancel(id) {
        try {
            console.log(id);
            var url = this.$store.getters.apiUrl('api/remote_tasks/' + id + '/cancel');
            await axios.post(url);
            this.refresh();
        } catch(error) {
            console.log(error);
        } finally {
            //this.refresh();
        }      
    },

    },
    computed: {
        ...mapState({
            urlPrefix: state => state.identity.urlPrefix,
        }),
    },
    watch: {
        meta() {
            this.refresh();
        },
        dialog() {
            if(this.dialog) {
                this.refresh();
            }
        },
        id() {
            this.setErrorMessage = undefined;
            if(this.id === undefined) {
                this.status = "init";
                this.dialog = false;
                this.remote_task = {};
            } else {
                this.status = "init";
                this.dialog = true;
                this.query_remote_task();
            }
        },
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style>

</style>
