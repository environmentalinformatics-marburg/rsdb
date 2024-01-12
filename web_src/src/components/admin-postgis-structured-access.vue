<template>
    <span style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px" :persistent="blocked">
            <v-btn title="Set name attibute" slot="activator">
                <v-icon left>folder_open</v-icon>Structured Access
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set structured access</div>
                </v-card-title>
                <v-card-text>
                    <br>
                    <hr>
                    <b>Enable/disable access types:</b>
                    <br>
                    <br>
                    <table>
                        <tr>
                            <td><v-switch v-model="structured_access_poi" label="POI-group" /></td><td style="padding-left: 20px;"><i>view of named <b>points</b> (Points Of Interest)</i></td>
                        </tr>
                        <tr>
                            <td><v-switch v-model="structured_access_roi" label="ROI-group" /></td><td style="padding-left: 20px;"><i>view of named <b>polygons</b> (Regions Of Interest)</i></td>
                        </tr>
                    </table>
                    <!--<div v-if="meta.details.attributes.length === 0">
                        no attributes
                    </div>-->
                    <hr>
                    <br>
                    <!--<p><b>Name</b> of POI-group / ROI-group is identical to <b>vector layer name</b>.</p>
                    <p><b>Names</b> for POIs and ROIs are determined by vector layer "<b>name attribute</b>" (may be set by "manage attributes" dialog).</p>-->
                    <p>For <b>POIs</b> just <b>point vector features</b> are included in resulting POI collection. <i>Entries with other geometry types will be skipped.</i></p>
                    <p>For <b>ROIs</b> just <b>areal vector features</b> are included in resulting ROI collection: (multi)-polygons (with holes). <i>Entries with other geometry types will be skipped.</i></p>
                    <p>Projection is identical to source vector layer projection.</p>
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false" :disabled="blocked">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="apply" :disabled="blocked">Apply</v-btn>
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
    name: 'admin-postgis-structured-access',
    props: ['meta'],
    components: {
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            structured_access_poi: false,
            structured_access_roi: false,
            blocked: false,
        }
    },
    methods: {        
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

        refresh(){
            this.structured_access_poi = this.meta.structured_access.poi;
            this.structured_access_roi = this.meta.structured_access.roi;
        },

        async apply() {
            this.blocked = true;
            try {
                var url = this.$store.getters.apiUrl('postgis/layers/' + this.meta.name);
                var response = await axios.post(url,{
                    structured_access: {poi: this.structured_access_poi, roi: this.structured_access_roi},
                });
                console.log(response);
                this.dialog = false;
            } catch(error) {
                console.log(error);
                console.log(this.errorToText(error));
                this.setError = true;
                this.setErrorMessage = "Error: " + this.errorToText(error);
            } finally {
                this.blocked = false;
                this.$emit('changed');
            }
        }

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
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style>

</style>
