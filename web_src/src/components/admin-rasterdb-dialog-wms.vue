<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute>
            <v-btn title="Show URL for WMS based access to this RasterDB layer." slot="activator">
                <v-icon left>folder_open</v-icon>WMS access
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">WMS access of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                WMS URL:
                <v-btn @click="onUrlCopy" title="copy WMS URL of this RasterDB layer to clipboard"><v-icon>content_copy</v-icon> Copy to clipboard</v-btn>
                <div class="wms-url">{{wmsUrl}}</div>

                <h2>WMS client <a href="https://qgis.org/" target="_blank">Qgis 3</a>:</h2>
                At <i>Browser</i>-Box rigth click on <i>WMS/WMTS</i>-list-entry,
                click the shown <i>New Connection...</i>-Button,
                the <i>Create a New WMS/WMTS Connection</i>-dialog opens,
                <br>at <i>Connection Details</i> type a name in the <i>Name</i>-field (e.g. the layer name),
                and in the <i>URL</i>-field type the shown WMS URL for this layer,
                <br>if the RSDB runs with login, at <i>Authentication</i> select the <i>Basic</i>-tab,
                type your account details in the <i>User name</i>-field and the <i>Password</i>-field,
                Qgis does support BASIC-Authentication, but not DIGEST-Authentication,
                <br>click the <i>OK</i>-Button.
                <br>
                <br>The new layer is shown as list-entry in the sub-list of the <i>WMS/WMTS</i>-list-entry,
                double click on the sub-list-entry,
                a list of bands and visualisations is shown,
                <br>(optional) if the layer contains multiple points in time, exapnd the list by click on the arrow symbol on the left,
                <br>double click on a list-entry to view in Qgis,
                the layer is added to the Qgis-Layers in the <i>Layers</i>-Box.
                <br>
                <br>
                <h2>WMS client <a href="http://openlayers.org/" target="_blank">OpenLayers</a>:</h2>
                <br>
                <b>WMS GetCapabilities request:</b>
                <br>
                <i>{{wmsUrl}}?REQUEST=GetCapabilities</i>
                <br>
                or
                <br>
                <i>{{wmsUrl}}</i>
                <br>
                <br>
                <b>WMS GetMap request:</b>
                <!--<br>To correctly handle OpenLayers requests the query parameter "modus=openlayers" needs to be added as compatiblity hint.
                (OpenLayers sends requests with coordinates in swapped order for some projections.) 
                <br>-->
                <br>                
                <i>{{wmsUrl}}?REQUEST=GetMap&modus=openlayers</i>
                <br>
                <i>&WIDTH=[WIDTH]</i>
                <br>
                <i>&HEIGHT=[HEIGHT]</i>
                <br>
                <i>&BBOX=[XMIN],[YMIN],[XMAX],[YMAX]</i>
                <br>
                <i>&LAYERS=[LAYER_NAME]</i>
                <br>
                <br>
                Parameters <i>REQUEST</i>, <i>WIDTH</i>, <i>HEIGHT</i>, <i>BBOX</i> are set by Openlayers.
                <br>
                Parameter <i>LAYERS</i> is set by user.
                <br>
                <br>
                <b>LAYERS:</b> (optional)
                <br>Name of visualisation that is listed as layer in the GetCapabilities document.

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
    name: 'admin-rasterdb-dialog-wms',
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
            copyTextToClipboard(this.wmsUrl);
            this.snackbarCopiedToClipboard = true;
        }

    },
    computed: {
        identity() {
            return this.$store.state.identity.data;
        },        
        wmsUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/rasterdb/' + this.meta.name + '/wms');
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

.wms-url {
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
    background: #0000002e;
    color: #334b60;
    padding: 1px;
}

</style>
