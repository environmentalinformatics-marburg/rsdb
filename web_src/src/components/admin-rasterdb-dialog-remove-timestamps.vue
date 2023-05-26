<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy width="800px">
            <v-btn title="open dialog to edit / remove time slices" slot="activator">
                <v-icon left>folder_open</v-icon>Time slices
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Manage time slices of layer: &nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-data-table v-bind:headers="timeSliceTableHeaders" :items="timeSlices" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td><input type="checkbox" id="checkbox" v-model="props.item.remove" style="width: 40px;"/>remove time slice</td>
                        <td>{{props.item.id}}</td>
                        <td><input v-model="props.item.name" placeholder="name" /></td>
                        <td :class="{remove: props.item.remove}">{{props.item.remove ? 'remove time slice' : '(keep time slice)'}}</td>
                    </template>
                </v-data-table>
                <v-card-text>
                    <b>Edit</b> time slice names by click on that name-entry.
                    <br>Raster data is fixed stored at time slices IDs. So, take care to not confuse them at multi time slices rename edits. 
                    <br><b>Remove</b> time slices by checkbox selection, all data of that time slices will be removed.
                </v-card-text>
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
                return {id: timeSlice.id, name: timeSlice.name};
            });
        },

        async execute() {
            var url = this.$store.getters.apiUrl('rasterdb/' + this.meta.name + '/set');
            try {
                var response = await axios.post(url, {
                    meta: {
                        time_slices: this.timeSlices,
                    } 
                });
                console.log(response);
                if(this.timeSlices.filter(b=>b.remove).length > 0) {
                    this.execute_time_slices_remove();
                } else {
                    this.$emit('changed');
                    this.dialog = false;
                }
            } catch(error) {
                this.setError = true;
                this.setErrorMessage = "Error setting property";
                console.log(error);
                this.$emit('changed');
                this.dialog = false;
            }                       
        },

        async execute_time_slices_remove() {
            var url = this.$store.getters.apiUrl('api/remote_tasks');
            var ts = this.timeSlices.filter(t=>t.remove).map(t=>t.id);
            try {
                var response = await axios.post(url, {
                    remote_task: {
                        task_rasterdb: "remove_timestamps",
                        rasterdb: this.meta.name,
                        timestamps: ts,
                    }
                });
                console.log(response);
                this.$emit('changed');
                this.dialog = false;
                var remote_task = response.data.remote_task;
                this.remote_task_id = remote_task.id;
            } catch(error) {
                console.log(error);
                console.log(this.errorToText(error));
                this.setError = true;
                this.setErrorMessage = "Error: " + this.errorToText(error);
                console.log(error);
                this.$emit('changed');
                this.dialog = false;
            }            
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
