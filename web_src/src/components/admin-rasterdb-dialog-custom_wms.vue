<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px" persistent>
            <v-btn title="open dialog to manage custom WMS entries" slot="activator">
                <v-icon left>folder_open</v-icon>Custom WMS
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Manage custom WMS entries of layer: &nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text>
                    <b>Add (<v-icon>add</v-icon>) / edit (<v-icon>edit</v-icon>) / remove (<v-icon>delete_forever</v-icon>)</b> custom WMS entries, copy WMS URL to clipboard (<v-icon>content_copy</v-icon>).
                </v-card-text>
                <hr />                
                <v-card-text>
                    <table>
                        <thead>
                        <tr>
                            <th></th>
                            <th></th>
                            <th>ID</th>
                            <th>URL</th>
                        </tr>
                        </thead>
                    <tbody>
                        <tr v-for="(c, key) in custom_wms" :key="key">
                            <td><v-btn flat icon title="Remove custom WMS entry." @click="removeEntry(key)"><v-icon>delete_forever</v-icon></v-btn></td>
                            <td>
                                <admin-rasterdb-dialog-custom_wms-edit :entry="c" :id="key" :ref="'entry_' + key" />
                            </td>
                            <td><b>{{key}}</b></td>
                            <td><v-btn flat icon @click="onUrlCopy(wmsUrl(key))" title="copy WMS URL of this custom WMS entry to clipboard"><v-icon>content_copy</v-icon></v-btn></td>
                            <td>{{wmsUrl(key)}}</td>
                        </tr>
                    </tbody>
                    </table>
                </v-card-text>
                <hr /> 
                <v-card-actions>
                    <v-menu
                        v-model="addEntryVisible"
                        bottom
                        origin="center center"
                        transition="scale-transition"
                        :close-on-content-click="false"
                    >
                        <template v-slot:activator="{ on }">
                            <v-btn class="indigo--text" v-on="on" slot="activator" title="Add a new custom WMS entry"><v-icon>add</v-icon> Add entry</v-btn>
                        </template>
                        <v-card>
                            <v-card-title>
                                <v-textarea v-model="new_entry" label="entry ID" auto-grow rows="1" title="Add a new custom WMS entry.">
                                    <template v-slot:append-outer>
                                        <v-btn class="indigo--text" :disabled="new_entry === undefined || new_entry === null || new_entry === '' || custom_wms[new_entry] !== undefined" @click="addEntry(new_entry, {}, true)" title="Add a new custom WMS entry.">
                                            <v-icon>add</v-icon> Add entry
                                        </v-btn>
                                    </template>
                                </v-textarea>
                            </v-card-title>
                        </v-card>
                    </v-menu>
                    <v-spacer></v-spacer>
                    <v-btn class="red--text darken-1" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" @click.native="execute()">Commit</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
        </v-snackbar>
        <v-snackbar v-model="snackbarCopiedToClipboard" top :timeout="2000">
            URL copied to clipboard
            <v-btn color="pink" flat @click="snackbarCopiedToClipboard = false">Close</v-btn>
        </v-snackbar>           
    </span>
</template>

<script>

import axios from 'axios'

import adminRasterdbDialogCustom_wmsEdit from './admin-rasterdb-dialog-custom_wms-edit'

export default {
    name: 'admin-rasterdb-dialog-custom_wms',
    props: ['meta'],        
    components: {
        'admin-rasterdb-dialog-custom_wms-edit': adminRasterdbDialogCustom_wmsEdit,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            custom_wms: {},
            addEntryVisible: false,
            new_entry: '',
            snackbarCopiedToClipboard: false,
        }
    },

    computed: {
        identity() {
            return this.$store.state.identity.data;
        },
    },

    methods: {
        
        refresh() {
            this.custom_wms = {};
            for (const prop in this.meta.custom_wms) {
                console.log(prop);
                this.addEntry(prop, this.meta.custom_wms[prop]);
            }
        },

        async execute() {
            var url = this.$store.getters.apiUrl('rasterdb/' + this.meta.name + '/set');
            try {
                var response = await axios.post(url, {
                    meta: {
                        custom_wms: this.custom_wms,
                    } 
                });
                console.log(response);
                this.$emit('changed');
                this.dialog = false;
            } catch(error) {
                this.setError = true;
                this.setErrorMessage = "Error setting custom_wms";
                console.log(error);
                //this.$emit('changed');
                //this.dialog = false;
            }                       
        },

        addEntry(id, c, edit) {
            let v = {};
            v.value_range = c.value_range !== undefined ? c.value_range : 'auto';
            v.value_range_static_min = c.value_range_static_min !== undefined ? c.value_range_static_min: '0';
            v.value_range_static_max = c.value_range_static_max !== undefined ? c.value_range_static_max: '255';
            v.gamma = c.gamma !== undefined ? c.gamma : 'auto';
            v.gamma_auto_sync = c.gamma_auto_sync !== undefined ? c.gamma_auto_sync : false;
            v.palette = c.palette !== undefined ? c.palette : 'grey';
            v.format = c.format !== undefined ? c.format : 'png:compressed';
            v.epsg = c.epsg !== undefined ? c.epsg : 0;
            this.$set(this.custom_wms, id, v);
            this.addEntryVisible = false;
            if(edit) {
                this.$nextTick(() => {
                    console.log(this.$refs);
                    console.log(this.$refs['entry_' + id]);
                    this.$refs['entry_' + id][0].visible = true;
                });
            }
        },

        removeEntry(id) {
            this.$delete(this.custom_wms, id);
        },

        wmsUrl(id) {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/rasterdb/' + this.meta.name + '/wms/' + id);
        },

        onUrlCopy(url) {
            copyTextToClipboard(url);
            this.snackbarCopiedToClipboard = true;
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

//derived from http://stackoverflow.com/questions/400212/how-do-i-copy-to-the-clipboard-in-javascript
function copyTextToClipboard(text) {
	var textArea = document.createElement("textarea");
	textArea.style.position = 'fixed';
	textArea.style.top = 0;
	textArea.style.left = 0;
	textArea.style.width = '2em';
	textArea.style.height = '2em';
	textArea.style.padding = 0;
	textArea.style.border = 'none';
	textArea.style.outline = 'none';
	textArea.style.boxShadow = 'none';
	textArea.style.background = 'transparent';
	textArea.value = text;
	document.body.appendChild(textArea);
	textArea.select();
	try {
		var successful = document.execCommand('copy');
		var msg = successful ? 'successful' : 'unsuccessful';
		console.log('copying text command was ' + msg);
	} catch (e) {
		console.log('ERROR unable to copy: '+e);
	}
	document.body.removeChild(textArea);
}

</script>

<style scoped>

</style>
