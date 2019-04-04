<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="create new VectorDB layer" slot="activator">
                <v-icon>add</v-icon> create layer
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">create new VectorDB layer</div>
                </v-card-title>

                <v-card-text>
                    <v-text-field v-model="name" label="name" />
                </v-card-text>

                <v-card-text>
                    Space, not latin chars and other special chars are not allowed.
                    <br>You may replace space chars by underscore or hyphen.
                    <br>Allowed chars: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-
                </v-card-text>

                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="execute()">Execute</v-btn>
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
    name: 'admin-vectordb-create',
    props: [],        
    components: {
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            name: "",
        }
    },
    methods: {
        
        execute() {
            var self = this;
            var url = '../../vectordbs';
            axios.post(url, {
                create_vectordb: {
                    name: self.name,
                }
            }).then(function(response) {
                console.log(response);
                self.dialog = false;
                self.$store.dispatch('vectordbs/refresh');
                self.$emit('created_vectordb', self.name);
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error: " + self.errorToText(error);
                console.log(error);                
                self.dialog = false;
                self.$store.dispatch('vectordbs/refresh');
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
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>
