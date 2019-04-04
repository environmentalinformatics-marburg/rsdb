<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="generate sequence of timestamps" slot="activator">
                <v-icon left>folder_open</v-icon>generate timestamps
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">generate sequence of timestamps ({{bandCount}})</div>
                </v-card-title>
                <span style="color: grey;"><v-icon style="color: grey;" title="note">event_note</v-icon> Click <i>apply</i>-button to commit generated timestamps. (You may need to scroll down.)</span>
                <br>
                <br>
                <multiselect v-model="mode" :options="modes" track-by="id" label="title" :allowEmpty="false" :searchable="false" :show-labels="false" placeholder="select mode" />
                <br>
                first timestamp year (e.g. 2001) 
                <input v-model="first_year" placeholder="year" style="background-color: aliceblue;" /> 
                <span v-if="mode.id === 'month'">
                    month (e.g. 12) 
                    <input v-model="first_month" placeholder="month" style="background-color: aliceblue;" />
                </span>
                <br>
                <br>
                <hr>
                <div style="background-color: #e4ebf54d;">
                    <div style="color: red;">
                        {{inputError}}
                    </div>
                    <table v-if="inputError === undefined">
                        <tr>
                            <th align="center">band</th>
                            <th align="center">timestamp</th>
                        </tr>
                        <tr v-for="(timestamp, i) in timestamps" :key="timestamp">
                            <td align="center"><b>{{i+1}}</b></td>
                            <td align="center">{{timestamp}}</td>
                        </tr>
                    </table>
                </div>
                <hr>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="execute()" :disabled="timestamps.length !== bandCount">Apply</v-btn>
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

import Multiselect from 'vue-multiselect'

export default {
    name: 'admin-upload-generate-timestamps',
    props: ['specification'],        
    components: {
        Multiselect,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,

            bandCount: 0,
            modes: [{id: "month", title: "sequence of months"}, {id: "year", title: "sequence of years"}],
            mode: {id: "month", title: "sequence of months"},
            first_year: "2001",
            first_month: "12",
            inputError: undefined,
        }
    },
    methods: {
        
        refresh() {
            this.bandCount = this.specification.bands.length;
        },

        execute() {
            this.$emit('timestamp-sequence', this.timestamps);
            this.dialog = false;         
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
    computed: {
        timestamps() {
            var self = this;
            self.inputError = "unknown error";
            var ts = [];
            var year = parseInt(this.first_year);
            if(this.mode.id === 'year') {
                if(year>=1000 && year<=9999) {
                    year = year + 0;
                    for(var j=0; j<this.bandCount; j++) {
                        ts.push(year);
                        year++;
                    }
                    self.inputError = undefined;
                } else {
                    self.inputError = "not valid input";
                }
            } else if(this.mode.id === 'month') {
                var month = parseInt(this.first_month);
                if(year>=1000 && year<=9999 && month>=1 && month<=12) {
                    year = year + 0;
                    month = month + 0;
                    for(var i=0; i<this.bandCount; i++) {
                        var m = month <= 9 ? '0' + month : month;
                        var t = year + '-' + m;
                        ts.push(t);
                        if(month == 12) {
                            year++;
                            month = 1;
                        } else {
                            month++;
                        }
                    }
                    self.inputError = undefined;
                } else {
                    self.inputError = "not valid input";
                }
            }
            return ts;
        },
    },
    watch: {
        specification() {
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

hr {
    border-top: 1px solid #6e6e6e;
}


</style>
