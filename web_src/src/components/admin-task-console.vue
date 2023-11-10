<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px" :fullscreen="isFullscreen">
            <v-card style="padding: 4px;">
                <v-card-actions style="background-color: #0000001a;">
                    <div class="headline"><v-icon>terminal</v-icon> Task Console</div>
                    <v-spacer></v-spacer>
                    <v-btn :title="isFullscreen ? 'Reduce task console.' : 'Maximize task console.'" icon><v-icon @click="isFullscreen = !isFullscreen;">{{isFullscreen ? 'content_copy' : 'crop_square'}}</v-icon></v-btn> 
                    <v-btn title="Close task console. A running task will continue to run." icon><v-icon @click="dialog = false;">close</v-icon></v-btn>  
                </v-card-actions>
                <v-icon v-if="remote_task.status === 'READY'" color="yellow">directions_walk</v-icon>
                <v-icon v-if="remote_task.status === 'RUNNING'" color="black">directions_run</v-icon>
                <v-icon v-if="remote_task.status === 'DONE'" color="green">done</v-icon>
                <v-icon v-if="remote_task.status === 'ERROR'" color="red">error</v-icon>
                {{remote_task.status}}
                <span style="padding-left: 50px;">
                <span v-if="(remote_task.status === 'READY' || remote_task.status === 'RUNNING') && !remote_task.canceled && remote_task.cancelable"><v-btn icon @click="cancel(remote_task.id)" title="Request cancelling of current task."><v-icon>cancel</v-icon> cancel</v-btn></span>
                <span v-if="!remote_task.canceled && !remote_task.cancelable">---</span>
                <span v-if="remote_task.canceled">cancel requested</span>
                </span>
                <br>
                id: {{id}}, time {{(remote_task.runtime/1000).toFixed()}} s
                <br>
                <div style="color: rgb(199, 200, 225); background-color: #403e4b; padding-left: 10px; padding-right: 10px; padding-top: 3px; padding-bottom: 3px;">
                    {{remote_task.message}}
                </div>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <span v-if="status === 'error'">error {{setErrorMessage}} <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Close</v-btn></span>
                </v-card-actions>
                <span style="color: grey;"><v-icon>event_note</v-icon> Click outside of this box to close it. 
                <br>Task continues to run and can be viewed on status view.</span>
                <div v-if="remote_task.task">
                    <hr>
                    <b>Task</b>
                    <pre>
{{JSON.stringify(remote_task.task, null, 2)}}
                    </pre>
                </div>
                <div v-if="remote_task.identity || remote_task.start || remote_task.end">
                    <hr>
                    <span v-if="remote_task.identity">
                        <b>Identity:</b>
                        {{remote_task.identity}}
                    </span>

                    <span v-if="remote_task.start">
                        <b> Start:</b>
                        {{remote_task.start}}
                    </span>

                    <span v-if="remote_task.end">
                        <b> End:</b>
                        {{remote_task.end}}
                    </span>
                </div>                                               
                <hr>
                <b>Log Messages</b> <v-btn @click="onLogCopy" small title="Copy log messages to clipboard."><v-icon>content_copy</v-icon>Copy to clipboard</v-btn>
                <div style="color: rgb(199, 200, 225); background-color: #403e4b; padding-left: 10px; padding-right: 10px; padding-top: 3px; padding-bottom: 3px;">
                    <div v-for="(line, i) in log" :key="i">
                        {{line}}
                    </div>
                </div>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false" >Close</v-btn>
        </v-snackbar>
        <v-snackbar v-model="snackbarCopiedToClipboard" top :timeout="2000">
            Log messages copied to clipboard.
            <v-btn color="pink" flat @click="snackbarCopiedToClipboard = false">Close</v-btn>
        </v-snackbar>        
    </span>
</template>

<script>

//derived from http://stackoverflow.com/questions/400212/how-do-i-copy-to-the-clipboard-in-javascript
function copyTextToClipboard(text) {
	var textArea = document.createElement("textarea");
	textArea.style.position = 'fixed';
	textArea.style.top = 0;
	textArea.style.left = 0;
	textArea.style.width = '2em';
	textArea.style.height = '2em';
	textArea.style.padding = 0;
	textArea.style.border = 'none';
	textArea.style.outline = 'none';
	textArea.style.boxShadow = 'none';
	textArea.style.background = 'transparent';
	textArea.value = text;
	document.body.appendChild(textArea);
	textArea.select();
	try {
		var successful = document.execCommand('copy');
		var msg = successful ? 'successful' : 'unsuccessful';
		console.log('copying text command was ' + msg);
	} catch (e) {
		console.log('ERROR unable to copy: '+e);
	}
	document.body.removeChild(textArea);
}

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
            log: [],
            snackbarCopiedToClipboard: false,
            isFullscreen: false,
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
            this.queryLog(this.id);
            var self = this;
            axios.get(this.urlPrefix + '../../api/remote_tasks/' + this.id + '?task&identity')
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
        async queryLog(id) {
            try {
                console.log(id);
                var url = this.$store.getters.apiUrl('api/remote_tasks/' + id + '/log');
                var response = await axios.get(url);
                this.log = response.data.split('\n');
            } catch(error) {
                console.log(error);
            } finally {
                //this.refresh();
            }
        },
        onLogCopy() {
            if(this.log && this.log.length > 0) {
                const text = this.log.join('\n');
                copyTextToClipboard(text);
                this.snackbarCopiedToClipboard = true;
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
            } else {
                this.$emit('close');
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
