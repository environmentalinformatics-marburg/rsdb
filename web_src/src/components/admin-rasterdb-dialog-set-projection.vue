<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set projection of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text>
                    <span v-if="newCode != undefined && newCode != null && !newCode.startsWith('EPSG:')" style="color: red; ">
                        <v-icon color="red">warning</v-icon>
                        Projection code SHOULD start with <b>EPSG:</b>
                    </span>
                    <v-text-field label="projection code" v-model="newCode"></v-text-field>
                    Code may be an
                    <a href="https://epsg.io" target="_blank" rel="noopener noreferrer">EPSG</a> identifier (e.g. 'EPSG:4326')
                </v-card-text>
                <v-card-text>
                    <v-text-field label="PROJ.4" v-model="newProj4"></v-text-field>
                    <a href="http://proj4.org/" target="_blank" rel="noopener noreferrer">PROJ.4</a> description (e.g. '+proj=longlat +datum=WGS84 +no_defs ')
                </v-card-text>
                <v-card-text>
                    Ensure same projection for projection code and proj4 !
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
    name: 'admin-rasterdb-dialog-set-projection',
    props: ['meta'],
    data() {
        return {
            dialog: false,
            newCode: "",
            newProj4: "",
            setError: false,
            setErrorMessage: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.newCode = this.meta.ref.code;
            this.newProj4 = this.meta.ref.proj4;
        },

        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + '/set');
            axios.post(url, {
                meta: {
                    code: self.newCode,
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
