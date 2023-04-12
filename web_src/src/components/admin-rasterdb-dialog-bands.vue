<template>
    <span style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute min-width="800px">
            <v-btn title="open dialog to edit / remove bands" slot="activator">
                <v-icon left>folder_open</v-icon>bands
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Manage bands of layer: &nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-data-table v-bind:headers="bandTableHeaders" :items="bands" class="meta-content" hide-actions>
                    <template slot="items" slot-scope="props">
                        <td><input type="checkbox" id="checkbox" v-model="props.item.remove" style="width: 40px;"/><span style="color: grey;"><span :class="{remove: props.item.remove}">remove band</span></span></td>
                        <td>{{props.item.index}} <span style="font-size: 0.8em; color: grey;">{{props.item.datatype}}</span></td>
                        <td><input v-model="props.item.title" placeholder="[title]" /></td>
                        <td><input v-model="props.item.wavelength" placeholder="[wavelength]" /></td>
                        <td><input v-model="props.item.fwhm" placeholder="[fwhm]" /></td>
                        <td :class="toVisualisationClass(props.item.visualisation)"><input v-model="props.item.visualisation" placeholder="[visualisation]" /></td>
                        <td><input v-model="props.item.vis_min" placeholder="[vis_min]" /></td>
                        <td><input v-model="props.item.vis_max" placeholder="[vis_max]" /></td>
                        <td style="color: grey;"><span :class="{remove: props.item.remove}">{{props.item.remove ? 'remove band' : '(keep band)'}}</span></td>
                    </template>
                </v-data-table>
                <v-card-text>
                    <b>Edit</b> band properties by click on that entry. 
                    <br><b>Remove</b> bands by checkbox selection. All data of that bands will be removed.
                    <br><b>Visualisation column</b> contains an optional hint for band visualisation. Possible values: <b>red</b> or <b>green</b> or <b>blue</b>.
                </v-card-text>
                <br>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="execute()">Apply</v-btn>
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
            bandTableHeaders: [{ text: "Removal", align: 'left', sortable: false}, 
                               { text: "Index", align: 'left', value: "index"},
                               { text: "Title", align: 'left', value: "title" },
                               { text: "Wavelength", align: 'left', value: "wavelength"},
                               { text: "FWHM", align: 'left', value: "fwhm"},
                               { text: "Visualisation", align: 'left', value: "visualisation" },
                               { text: "Vis. min", align: 'left', value: "vis_min"}, 
                               { text: "Vis. max", align: 'left', value: "vis_max"},
                               { text: "Action", align: 'left', value: "remove"}],
            bands: [],
            remote_task_id: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.bands = this.meta.bands.map(function(band) {
                return {
                    index: band.index, 
                    title: band.title,
                    wavelength: band.wavelength, 
                    fwhm: band.fwhm,  
                    visualisation:band.visualisation, 
                    vis_min: band.vis_min, 
                    vis_max: band.vis_max,
                    datatype: band.datatype,
                };
            });
        },

        async execute() {
            var url = this.$store.getters.apiUrl('rasterdb/' + this.meta.name + '/set');
            try {
                var response = await axios.post(url, {
                    meta: {
                        bands: this.bands,
                    } 
                });
                console.log(response);
                if(this.bands.filter(b=>b.remove).length > 0) {
                    this.execute_bands_remove();
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

        async execute_bands_remove() {
            var url = this.$store.getters.apiUrl('api/remote_tasks');
            var bs = this.bands.filter(b=>b.remove).map(b=>b.index);
            try {
                var response = await axios.post(url, {
                    remote_task: {
                        task_rasterdb: "remove_bands",
                        rasterdb: this.meta.name,
                        bands: bs,
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

        toVisualisationClass(visualisation) {
            if(visualisation === 'red') {
                return 'visualisation-red';
            }
            if(visualisation === 'green') {
                return 'visualisation-green';
            }
            if(visualisation === 'blue') {
                return 'visualisation-blue';
            }
            return 'visualisation-unspecified';
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
.visualisation-red {
    color: #d51717e5
}

.visualisation-green {
    color: #24a824e5;
}

.visualisation-blue {
    color: #0f4ee1;
}

.visualisation-unspecified {
    color: grey;
}

</style>
