<template>
    <span>
        <v-dialog v-model="dialog" lazy width="800px">
            <v-btn title="show vector data properties as table" slot="activator">
                <v-icon>storage</v-icon> open data table
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Data Table</div>
                </v-card-title>

                <v-data-table :headers="headers" :items="data">
                <template v-slot:items="props">
                    <td v-for="(a, i) in attributes.length" :key="i">{{props.item[i]}}</td>
                </template>
                </v-data-table>                

                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false">Close</v-btn>
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
    name: 'admin-vectordb-dialog-data-table',
    props: ['meta'],
    components: {
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,

            attributes: ["a", "b", "c"],
            data: [],
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

        refresh() {
            var self = this;
            var url = this.$store.getters.apiUrl('vectordbs/' + self.meta.name + '/table.json');
            axios.get(url, {})
            .then(function(response) {
                console.log(response);
                self.attributes = response.data.attributes;
                self.data = response.data.data;
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error: " + self.errorToText(error);
            });   
        }

    },
    computed: {
        headers() {
            return this.attributes.map(function(a){return {text: a, value: 1,};});
        },
    },
    watch: {
        meta() {
            if(this.dialog) {
                this.refresh();
            }
        },
        dialog() {
            if(this.dialog) {
                this.refresh();
            }
        }
    },
    mounted() {
        if(this.dialog) {
            this.refresh();
        }
    },
}

</script>

<style scoped>

</style>
