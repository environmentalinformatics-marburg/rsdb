<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="open dialog to select and remove timestamps of this rasterdb layer" slot="activator">
                <v-icon left>folder_open</v-icon>timestamps
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">remove timestamps of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-data-table v-bind:headers="timestampTableHeaders" :items="timestamps" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td><input type="checkbox" id="checkbox" v-model="props.item.remove" style="width: 40px;"/>remove timestamp</td>
                        <td>{{props.item.datetime}}</td>
                        <td>{{props.item.timestamp}}</td>
                        <td :class="{remove: props.item.remove}">{{props.item.remove ? 'remove timestamp' : '(keep timestamp)'}}</td>
                    </template>
                </v-data-table>
                <br>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="red--text darken-1" flat="flat" @click.native="execute()">Execute</v-btn>
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

import axios from 'axios'
import adminTaskConsole from './admin-task-console'

export default {
    name: 'admin-rasterdb-dialog-remove-timestamps',
    props: ['meta'],        
    components: {
        'admin-task-console': adminTaskConsole,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            timestampTableHeaders: [{ text: "", align: 'left', sortable: false}, 
                                    { text: "date", align: 'left', value: "datetime" }, 
                                    { text: "timestamp", align: 'left', value: "timestamp" }, 
                                    { text: "action", align: 'left', value: "remove"}],
            timestamps: [],
            remote_task_id: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.timestamps = this.meta.timestamps.map(function(timestamp) {
                return {timestamp: timestamp.timestamp, datetime: timestamp.datetime, remove: false};
            });
        },

        execute() {
            var self = this;
            var url = this.$store.getters.apiUrl('api/remote_tasks');
            var ts = this.timestamps.filter(t=>t.remove).map(t=>t.timestamp);
            axios.post(url, {
                remote_task: {
                    task_rasterdb: "remove_timestamps",
                    rasterdb: self.meta.name,
                    timestamps: ts,
                }
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
                self.dialog = false;
                var remote_task = response.data.remote_task;
                self.remote_task_id = remote_task.id;
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error: " + self.errorToText(error);
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
            return error.message + " - " + JSON.stringify(error.response.data);
        },

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

<style scoped>

.remove {
    color: #e43215;
}

</style>
