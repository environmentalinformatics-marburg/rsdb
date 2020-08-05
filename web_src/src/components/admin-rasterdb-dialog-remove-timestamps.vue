<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="open dialog to select and remove time slices of this rasterdb layer" slot="activator">
                <v-icon left>folder_open</v-icon>Time slices
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">remove time slices of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-data-table v-bind:headers="timeSliceTableHeaders" :items="timeSlices" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td><input type="checkbox" id="checkbox" v-model="props.item.remove" style="width: 40px;"/>remove time slice</td>
                        <td>{{props.item.id}}</td>
                        <td>{{props.item.name}}</td>
                        <td :class="{remove: props.item.remove}">{{props.item.remove ? 'remove time slice' : '(keep time slice)'}}</td>
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
            timeSliceTableHeaders: [{ text: "", align: 'left', sortable: false}, 
                                    { text: "Id", align: 'left', value: "id" }, 
                                    { text: "Name", align: 'left', value: "name" }, 
                                    { text: "action", align: 'left', value: "remove"}],
            timeSlices: [],
            remote_task_id: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.timeSlices = this.meta.time_slices.map(function(timeSlice) {
                return {id: timeSlice.id, name: timeSlice.name, remove: false};
            });
        },

        execute() {
            var self = this;
            var url = this.$store.getters.apiUrl('api/remote_tasks');
            var ts = this.timeSlices.filter(t=>t.remove).map(t=>t.id);
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
