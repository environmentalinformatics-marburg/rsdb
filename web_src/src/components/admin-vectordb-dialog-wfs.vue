<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy>
            <v-btn title="Show URL for WFS based access to this VectorDB layer." slot="activator">
                <v-icon left>folder_open</v-icon>WFS access
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">WFS access of <i>VectorDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                WFS URL:
                <v-btn @click="onUrlCopy" title="copy WFS URL of this VectorDB layer to clipboard"><v-icon>content_copy</v-icon> Copy to clipboard</v-btn>
                <div class="wfs-url">{{wfsUrl}}</div>

                <br>
                Web Feature Service (WFS) <a href="https://www.ogc.org/standards/wfs" target="_blank">specification</a>, 
                <a href="https://en.wikipedia.org/wiki/Web_Feature_Service" target="_blank"> Wikipedia entry</a>
                <br>
                <br>

                <h2>WFS client <a href="https://qgis.org/" target="_blank">Qgis 3</a>:</h2>
                At Qgis window <i>Browser</i>-Box rigth click on <i>WFS</i>-list-entry,
                click the shown <i>New Connection...</i>-Button,
                the <i>Create a New WFS Connection</i>-dialog opens,
                <br>at <i>Connection Details</i> type a name in the <i>Name</i>-field (e.g. the layer name),
                and in the <i>URL</i>-field type or paste the above WFS URL for this layer,
                <br>if the RSDB server runs with login, at <i>Authentication</i> select the <i>Basic</i>-tab,
                type your account details in the <i>User name</i>-field and the <i>Password</i>-field,
                Qgis does support BASIC-Authentication, but not DIGEST-Authentication,
                <br>click the <i>OK</i>-Button.
                <br>
                <br>The new layer is shown as list-entry in the sub-list of the <i>WFS</i>-list-entry,
                double click on the sub-list-entry,
                a list-entry of the layer name is shown,
                <br>double click on this list-entry to view in Qgis,
                the layer is added to the Qgis-Layers in the <i>Layers</i>-Box.
                <br>
                <br>
                <b>Note</b>: Qgis does cache received network data. If you changed layer (meta-)data and if it is not reflected in Qgis, do the following:
                <br>Remove the relevant entries in the Qgis Layers-box.
                <br>At Main-menu <i>Setting</i>, click <i>Options..."</i>-entry. The <i>Options</i> dialog-box opens. Select Page <i>Network</i> on the left. 
                <br>At <i>Cache Settings</i>-section, <i>Content</i>-tab click the <i>trash can</i>-symbol-button.
                <br>Cache is now cleared. Right click on the WFS-entry at the Browser-box, click <i>refresh</i>-button to load updated data.

                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Close</v-btn>
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

export default {
    name: 'admin-vectordb-dialog-wfs',
    props: ['meta'],        
    components: {
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            snackbarCopiedToClipboard: false,
        }
    },
    methods: {
        
        refresh() {
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

        onUrlCopy() {
            copyTextToClipboard(this.wfsUrl);
            this.snackbarCopiedToClipboard = true;
        }

    },
    computed: {
        identity() {
            return this.$store.state.identity.data;
        },        
        wfsUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/vectordbs/' + this.meta.name + '/wfs');
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

.wfs-url {
    background-color: #000000c4;
    color: #e6e6e6;
    padding: 5px;
    font-weight: bold;
    border-style: solid;
    border-radius: 8px;
    border-color: #686868;
    font-family: "Courier New", Courier, monospace;
    font-size: 1em;
    margin: 10px;    
}

i {
    background: #0034ff0a;
    color: #616d29;
    padding: 1px;
    font-weight: bold;
}

</style>
