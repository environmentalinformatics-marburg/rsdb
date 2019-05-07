<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="open dialog to edit band infos of this rasterdb layer" slot="activator">
                <v-icon left>folder_open</v-icon>manage bands
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Manage bands of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-data-table v-bind:headers="bandTableHeaders" :items="bands" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td>{{props.item.index}}</td>
                        <td><input v-model="props.item.title" placeholder="title" /></td>
                        <td><input v-model="props.item.vis_min" placeholder="vis_min" /></td>
                        <td><input v-model="props.item.vis_max" placeholder="vis_max" /></td>
                    </template>
                </v-data-table>
                <v-card-text>
                    Edit band titles by click on band title entry.
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
        <admin-task-console :id="remote_task_id" @done="$emit('changed');" @error="$emit('changed');" />
    </span>
</template>

<script>

import axios from 'axios'
import adminTaskConsole from './admin-task-console'

export default {
    name: 'admin-rasterdb-dialog-bands',
    props: ['meta'],        
    components: {
        'admin-task-console': adminTaskConsole,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            bandTableHeaders: [{ text: "index", align: 'left', value: "index"},
                               { text: "title", align: 'left', value: "title" },
                               { text: "vis_min", align: 'left', value: "vis_min"}, 
                               { text: "vis_max", align: 'left', value: "vis_max"},],
            bands: [],
            remote_task_id: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.bands = this.meta.bands.map(function(band) {
                return {index: band.index, title: band.title, vis_min: band.vis_min, vis_max: band.vis_max};
            });
        },

        execute() {
            var self = this;
            var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + '/set');
            axios.post(url, {
                meta: {
                    bands: self.bands,
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
