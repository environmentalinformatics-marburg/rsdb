<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="Delete this PointCloud layer. This can not be undone." slot="activator">
                <v-icon left>delete_forever</v-icon>delete entire layer
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">DELETE <i>PointCloud</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text>
                    Delete this PointCloud layer. <b>This can not be undone.</b>
                </v-card-text>
                <v-card-text>
                    Are you sure?
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="red--text darken-1" flat="flat" @click.native="delete_layer()">Delete</v-btn>
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
    name: 'admin-pointcloud-dialog-delete',
    props: ['meta'],
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
        }
    },
    methods: {
        
        refresh() {
        },

        delete_layer() {
            var self = this;
            var url = this.$store.getters.apiUrl('pointclouds/' + self.meta.name);
            axios.post(url, {
                pointcloud: {
                    delete_pointcloud: self.meta.name,
                }
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
                self.dialog = false;
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

<style>

</style>
