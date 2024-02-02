<template>
    <span style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="Open data table of field values." slot="activator">
                <v-icon left>folder_open</v-icon>View
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Data table</div>
                </v-card-title>
                <v-card-text>
                    <table v-if="data">
                        <tbody>
                            <tr v-for="(row, index) in data" :key="index">
                                <!--<td v-for="(value, index) in row" :key="index">{{value}}</td>-->
                                <td><pre>{{row.join('\t')}}</pre></td>
                            </tr>
                        </tbody>
                    </table>
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Close</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="error" :top="true">
            {{errorMessage}}
            <v-btn flat class="pink--text" @click.native="error = false">Close</v-btn>
        </v-snackbar>
    </span>
</template>

<script>

import axios from 'axios'

function parseCSV(str) { // source from: https://stackoverflow.com/questions/1293147/how-to-parse-csv-data
    const arr = [];
    let quote = false;  // 'true' means we're inside a quoted field

    // Iterate over each character, keep track of current row and column (of the returned array)
    for (let row = 0, col = 0, c = 0; c < str.length; c++) {
        let cc = str[c], nc = str[c+1];        // Current character, next character
        arr[row] = arr[row] || [];             // Create a new row if necessary
        arr[row][col] = arr[row][col] || '';   // Create a new column (start with empty string) if necessary

        // If the current character is a quotation mark, and we're inside a
        // quoted field, and the next character is also a quotation mark,
        // add a quotation mark to the current column and skip the next character
        if (cc == '"' && quote && nc == '"') { arr[row][col] += cc; ++c; continue; }

        // If it's just one quotation mark, begin/end quoted field
        if (cc == '"') { quote = !quote; continue; }

        // If it's a comma and we're not in a quoted field, move on to the next column
        if (cc == ',' && !quote) { ++col; continue; }

        // If it's a newline (CRLF) and we're not in a quoted field, skip the next character
        // and move on to the next row and move to column 0 of that new row
        if (cc == '\r' && nc == '\n' && !quote) { ++row; col = 0; ++c; continue; }

        // If it's a newline (LF or CR) and we're not in a quoted field,
        // move on to the next row and move to column 0 of that new row
        if (cc == '\n' && !quote) { ++row; col = 0; continue; }
        if (cc == '\r' && !quote) { ++row; col = 0; continue; }

        // Otherwise, append the current character to the current column
        arr[row][col] += cc;
    }
    return arr;
}

export default {
    name: 'admin-postgis-data-table',
    props: ['meta'],
    components: {
    },
    data() {
        return {
            dialog: false,
            error: false,
            errorMessage: undefined,
            data: undefined,
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

        async refresh(){
            try {
                this.error = false;
                this.errorMessage = undefined;
                const url = this.$store.getters.apiUrl('postgis/layers/' + this.meta.name + '/table.csv');
                let response = await axios.get(url, {});
                this.data = parseCSV(response.data);
            } catch(error) {
                console.log(error);
                console.log(this.errorToText(error));
                this.error = true;
                this.errorMessage = "Error: " + this.errorToText(error);
            }
        },
    },
    computed: {
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

<style>

</style>
