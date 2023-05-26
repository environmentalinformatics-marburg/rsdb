<template>
    <span>
        <v-dialog v-model="dialog" lazy width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set projection of <i>VoxelDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text>
                    <span v-if="!isEpsg(newEpsg)" style="color: red; ">
                        <v-icon color="red">warning</v-icon>
                        EPSG needs to be a <b>number</b>.
                    </span>
                    <v-text-field label="EPSG" v-model="newEpsg"></v-text-field>
                    <a href="https://epsg.io" target="_blank" rel="noopener noreferrer">EPSG</a> is an identifying number (e.g. '4326').
                </v-card-text>
                <v-card-text>
                    <v-text-field label="PROJ.4" v-model="newProj4"></v-text-field>
                    <a href="http://proj4.org/" target="_blank" rel="noopener noreferrer">PROJ.4</a> description (e.g. '+proj=longlat +datum=WGS84 +no_defs ')
                </v-card-text>
                <v-card-text>
                    Ensure same projection for EPSG and proj4 !
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="set">Set</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
        </v-snackbar>
    </span>
</template>

<script>

import axios from 'axios'

export default {
    name: 'admin-voxeldb-dialog-set-projection',
    props: ['meta'],
    data() {
        return {
            dialog: false,
            newEpsg: "",
            newProj4: "",
            setError: false,
            setErrorMessage: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.newEpsg = this.meta.ref.epsg;
            this.newProj4 = this.meta.ref.proj4;
        },

        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('voxeldbs/' + self.meta.name);
            var epsg = this.isEpsg(self.newEpsg) ? this.parseEpsg(self.newEpsg) : 0;
            axios.post(url, {
                voxeldb: {
                    epsg: epsg,
                    proj4: self.newProj4,
                }
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
                self.dialog = false;
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error setting property: " + self.errorToText(error);
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
            return error.message + " - " + error.response.data;
        },

        isEpsg(v) {
            return v !== undefined && v !== '' && !isNaN(v);
        },
        parseEpsg(v) {
            return (+v);
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

<style>

</style>
