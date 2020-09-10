<template>
    <span  style="display: inline-block;">
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="Show URL for WMS based access to this RasterDB layer." slot="activator">
                <v-icon left>folder_open</v-icon>WMS access
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">WMS access of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                WMS URL:
                <br>
                <v-btn @click="onUrlCopy"><v-icon>content_copy</v-icon> copy</v-btn><span class="wms-url">{{wmsUrl}}</span>
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
}

</style>
