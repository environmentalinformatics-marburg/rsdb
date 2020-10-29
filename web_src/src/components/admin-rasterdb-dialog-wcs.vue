<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute>
            <v-btn title="Show URL for WCS based access to this RasterDB layer." slot="activator">
                <v-icon left>folder_open</v-icon>WCS access
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">WCS access of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                WCS URL:
                <v-btn @click="onUrlCopy" title="copy WCS URL of this RasterDB layer to clipboard"><v-icon>content_copy</v-icon> Copy to clipboard</v-btn>
                <div class="wcs-url">{{wcsUrl}}</div>

                <br>
                Web Coverage Service (WCS) <a href="http://www.opengeospatial.org/standards/wcs" target="_blank">specification</a>, 
                <a href="https://en.wikipedia.org/wiki/Web_Coverage_Service" target="_blank"> Wikipedia entry</a>
                <br>
                <br>

                <h2>WCS client <a href="https://qgis.org/" target="_blank">Qgis 3</a>:</h2>
                At Qgis window <i>Browser</i>-Box rigth click on <i>WCS</i>-list-entry,
                click the shown <i>New Connection...</i>-Button,
                the <i>Create a New WCS Connection</i>-dialog opens,
                <br>at <i>Connection Details</i> type a name in the <i>Name</i>-field (e.g. the layer name),
                and in the <i>URL</i>-field type or paste the above WCS URL for this layer,
                <br>if the RSDB server runs with login, at <i>Authentication</i> select the <i>Basic</i>-tab,
                type your account details in the <i>User name</i>-field and the <i>Password</i>-field,
                Qgis does support BASIC-Authentication, but not DIGEST-Authentication,
                <br>click the <i>OK</i>-Button.
                <br>
                <br>The new layer is shown as list-entry in the sub-list of the <i>WCS</i>-list-entry,
                double click on the sub-list-entry,
                a list-entry of the layer name is shown,
                <br>double click on thid list-entry to view in Qgis,
                the layer is added to the Qgis-Layers in the <i>Layers</i>-Box.
                <br>

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
    name: 'admin-rasterdb-dialog-wcs',
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
            copyTextToClipboard(this.wcsUrl);
            this.snackbarCopiedToClipboard = true;
        }

    },
    computed: {
        identity() {
            return this.$store.state.identity.data;
        },        
        wcsUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/rasterdb/' + this.meta.name + '/wcs');
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

.wcs-url {
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
