<template>
    <div class="main">
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center; padding-right: 400px;">
            <h3 class="headline mb-0">
                {{meta === undefined || meta.title === undefined || meta.title === '' ? postgis : meta.title}}
                <!--<v-btn icon class="indigo--text" :href="$store.getters.apiUrl('web/app/#/?postgis=' + postgis)" target="_blank" title="open layer in viewer on new tab">
                    <v-icon>zoom_in</v-icon>
                </v-btn>-->
                <v-btn class="indigo--text" :href="$store.getters.apiUrl('web/app/#/?postgis=' + postgis)" target="_blank" title="open layer in viewer app on new tab">
                    <v-icon>launch</v-icon> Viewer app
                </v-btn>
            </h3>
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''"><span style="user-select: none;">id: </span><b>{{postgis}}</b></span>
        </div>
        <div v-if="metaError">
            <b>ERROR</b> 
            {{metaErrorMessage}}
        </div>
        <div v-if="meta != undefined">
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                <dialog-set-info 
                    @changed="refresh" 
                    v-if="modify"
                    :meta="meta" 
                    :url="$store.getters.apiUrl('postgis/layers/' + meta.name)" 
                />                
                Info
            </h3>
            <div class="meta-content">
                <img :key="meta.name" :src="$store.getters.apiUrl('postgis/layers/' + meta.name + '/image.png?width=400&height=600')" alt="Loading overview map ..." class="thumbnail" /> 
                <box-info :meta="meta" />
            </div>
            
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                Details
            </h3>
            <div class="meta-content">
                <table style="border-spacing: 4px;">
                    <tr>
                        <td><b>Fields:</b></td>
                        <td  style="padding-right: 400px;">
                            <span v-if="meta.fields.length > 0" style="display: flex; flex-wrap: wrap; gap: 10px;"><span v-for="name in meta.fields" :key="name"><span :class="isClass_field(name) ? 'class_field' : isName_field(name) ? 'name_field' : 'field'">{{name}}</span></span></span>
                            <span v-else>(none)</span>
                        </td>
                    </tr>                    
                    <tr><td><b>Name field:</b></td><td><span class="name_field">{{meta.name_field}}</span></td></tr>  
                    <tr>
                        <td><b>Class fields:</b></td>
                        <td>
                            <span v-if="meta.class_fields.length > 0" style="display: flex; flex-wrap: wrap; gap: 10px;"><span v-for="name in meta.class_fields" :key="name"><span class="class_field">{{name}}</span></span></span>
                            <span v-else>(none)</span>
                        </td>
                    </tr>                    
                    <tr><td><b>Geometry field:</b></td><td><span class="geo_field">{{meta.geometry_field}}</span></td></tr>
                    <tr v-if="meta.geometry_types !== undefined">
                        <td><b>Geometry type:</b></td>
                        <td style="display: flex; flex-wrap: wrap; gap: 10px;"><span v-for="name in meta.geometry_types" :key="name"><span>{{name}}</span></span></td>
                    </tr>
                    <tr  v-if="meta.item_count !== undefined"><td><b>Features:</b></td><td>{{meta.item_count}}</td></tr>  
                    <tr><td><b>EPSG:</b></td><td>{{meta.epsg}}</td></tr>
                    <tr><td><b>Extent:</b></td><td v-if="meta.extent !== undefined">{{meta.extent.xmin.toPrecision(8)}}<b>,</b> {{meta.extent.ymin.toPrecision(8)}} <b>-</b> {{meta.extent.xmax.toPrecision(8)}}<b>,</b> {{meta.extent.ymax.toPrecision(8)}}</td></tr>             
                </table>
            </div>

            <v-divider class="meta-divider"></v-divider>  
            <h3 class="subheading mb-0"> 
                <admin-postgis-dialog-set-acl :meta="meta" @changed="refresh" v-if="isAdmin" />
                Access control
            </h3>                        
            <div class="meta-content">
                <table>
                    <tr>
                        <td><b>access roles:</b></td>
                        <td>
                            <span v-for="role in meta.acl" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="meta.acl.length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>                
                    <tr>
                        <td><b>modify roles:</b></td>
                        <td>
                            <span v-for="role in meta.acl_mod" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="meta.acl_mod.length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>
                    <tr>
                        <td><b>owner roles:</b></td>
                        <td>
                            <span v-for="role in meta.acl_owner" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="meta.acl_owner.length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>
                </table>
            </div>

            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0"> 
                Accessibility
            </h3>
            <div class="meta-content">                
                <table>
                    <tr>
                        <th style="padding-right: 25px;">Service</th>
                        <th>Access</th>
                        <th>Description</th>
                    </tr>
                    <tr>
                        <td style="text-align: center;"><b>WFS</b></td>
                        <td style="text-align: center;">
                            <span class="url">{{wfsUrl}}</span>
                            <v-btn @click="onUrlCopy(wfsUrl)" title="Copy WFS URL of this PostGIS layer to clipboard." small><v-icon small>content_copy</v-icon>copy url</v-btn>
                        </td>
                        <td>Vector feature access to e.g. QGIS</td>
                    </tr>
                    <tr>
                        <td style="text-align: center;"><b>WMS</b></td>
                        <td style="text-align: center;">
                            <span class="url">{{wmsUrl}}</span>
                            <v-btn @click="onUrlCopy(wmsUrl)" title="Copy WMS URL of this PostGIS layer to clipboard." small><v-icon small>content_copy</v-icon>copy url</v-btn>
                        </td>
                        <td>Vector feature access as image visualization to e.g. QGIS</td>
                    </tr>
                    <tr>
                        <td style="text-align: center;"><b>GeoJSON</b></td>
                        <td style="text-align: center;"><a :href="geojsonUrl" :download="postgis + '.geojson'" target="_blank" title="Download GeoJSON file. Includes geometries and field values." style="padding-left: 10px;">Download GeoJSON</a></td>                        
                        <td>Export vector features as file to be opened by e.g. QGIS</td>
                    </tr>
                    <tr>
                        <td style="text-align: center;"><b>CSV</b></td>
                        <td style="text-align: center;">
                            <a :href="csvUrl" :download="postgis + '.csv'" target="_blank"  title="Download CSV file. Includes a table of field values." style="padding-left: 10px;">Download CSV</a>
                            <admin-postgis-data-table :meta="meta" />                            
                        </td>
                        <td>Export vector feature properties as file to be opened by e.g. spreadsheet application</td>
                    </tr>
                </table>
            </div>

            <div v-if="modify">
            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0"> 
                Administration
            </h3>
            <div class="meta-content">                
                <admin-postgis-structured-access :meta="meta" @changed="refresh(); $store.dispatch('poi_groups/refresh'); $store.dispatch('roi_groups/refresh');" v-if="modify" />
            </div>
        </div>

            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0" v-if="meta.style !== undefined"> 
                Style
            </h3>
            <div class="meta-content" v-if="meta.style !== undefined">
                <div class="legend">
                    <img :key="meta.name" :src="$store.getters.apiUrl('postgis/layers/' + meta.name + '/wms?REQUEST=GetLegendGraphic')" alt="Loading legend image ..." /> 
                </div>                
                <table>
                    <tr v-for="(value, key) in meta.style" :key="key">
                        <td><b>{{key}}</b></td>
                        <td v-if="key === 'values'">
                            <table>
                                <tr v-for="(value1, key1) in value" :key="key1">
                                    <td><b>{{key1}}</b></td>
                                    <td>{{JSON.stringify(value1, undefined, 1)}}</td>
                                </tr>
                            </table>
                        </td>
                        <td v-else>{{JSON.stringify(value, undefined, 1)}}</td>
                    </tr>
                </table>                
            </div>

            <v-divider class="meta-divider"></v-divider>
            <h3 class="subheading mb-0"  v-if="meta.invalid_geometry === undefined"> 
                Geometry valid.
            </h3>
            <h3 class="subheading mb-0" v-else>
                Geometry invalid: ({{meta.invalid_geometry.length}})
            </h3>                        
            <div class="meta-content" v-if="meta.invalid_geometry !== undefined">
                <table>
                    <tr v-for="r in meta.invalid_geometry" :key="r"><td>{{r}}</td></tr>
                </table>
            </div>            

        </div>
        
        <v-snackbar v-model="snackbarCopiedToClipboard" top :timeout="2000">
            URL copied to clipboard
            <v-btn color="pink" flat @click="snackbarCopiedToClipboard = false">Close</v-btn>
        </v-snackbar> 
    </div>
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

import { mapState, mapGetters } from 'vuex'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'

import axios from 'axios'

import dialogSetInfo from './dialog-set-info.vue'
import boxInfo from './box-info.vue'
import adminPostgisDialogSetAcl from './admin-postgis-dialog-set-acl.vue'
import adminPostgisStructuredAccess from './admin-postgis-structured-access'
import adminPostgisDataTable from './admin-postgis-data-table'


export default {
    name: 'admin-postgis-detail',
    components: {
        PulseLoader, 
        'dialog-set-info': dialogSetInfo,
        'box-info': boxInfo,
        'admin-postgis-dialog-set-acl': adminPostgisDialogSetAcl,
        'admin-postgis-structured-access': adminPostgisStructuredAccess,
        'admin-postgis-data-table': adminPostgisDataTable,                
    },
    props: ['postgis'],
    data() {
        return {
            meta: undefined,
            metaError: false,
            metaErrorMessage: undefined,
            busy: false,
            busyMessage: undefined,
            snackbarCopiedToClipboard: false,
        }
    },
    methods: {
        async refresh() {
            const url = this.$store.getters.apiUrl('postgis/layers/' + this.postgis);
            this.metaError = false;
            this.metaErrorMessage = undefined;
            this.busy = true;
            this.busyMessage = "loading ...";
            try {
                let response = await axios.get(url);
                this.meta = response.data;
                this.busy = false;
                this.busyMessage = undefined;
            } catch(error) {
                console.log(error);
                this.metaError = true;
                this.metaErrorMessage = "ERROR getting meta: " + error;
                this.meta = undefined;
                this.busy = false;
                this.busyMessage = undefined;
            }
        },

        isClass_field(fld) {
            if(this.meta === undefined || this.meta.class_fields === undefined) {
                return false;
            }
            return this.meta.class_fields.includes(fld);
        },

        isName_field(fld) {
            if(this.meta === undefined || this.meta.name_field === undefined || this.meta.name_field === '') {
                return false;
            }
            return this.meta.name_field === fld;
        },

        onUrlCopy(url) {
            copyTextToClipboard(url);
            this.snackbarCopiedToClipboard = true;
        }
    },
    computed: {
        ...mapState({
            identity: state => state.identity.data,
        }),        
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),
        modify() {
            return this.meta === undefined || this.meta.modify == undefined ? false :  this.meta.modify;
        },       
        wfsUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/postgis/layers/' + this.meta.name + '/wfs');
        },
        wmsUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/postgis/layers/' + this.meta.name + '/wms');
        },        
        csvUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/postgis/layers/' + this.meta.name + '/table.csv');
        },
        geojsonUrl() {
            return this.identity === undefined || this.meta === undefined ? '[unknown]' : (this.identity.url_base + '/postgis/layers/' + this.meta.name + '/geometry.geojson');
        },
    },
    watch: {
        postgis() {
            this.refresh();
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style scoped>


.meta-divider {
    margin: 15px;
}

.tag-list {
    margin: 15px;
}

.main {
    position: relative;
}

.div-busy {
    position: absolute;
    color: rgb(129, 125, 125);
    background-color: rgb(243, 243, 243);
    font-size: 1.5em;
    width: 100%;
    height: 100%;
    z-index: 1;
}

.fade-enter-active {
  transition: opacity 2s;
}

.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}

.meta-content {
    padding-left: 100px;
}

.meta-list {
    background-color: rgb(232, 232, 232);
    padding: 1px;
    margin-right: 5px;
    border-color: rgba(0, 0, 0, 0.062);
    border-style: solid;
    border-width: 1px;
}

.field {
    background-color: rgb(232, 232, 232);
    padding: 1px;
    margin-left: 2px;
    margin-right: 3px;
    border-color: rgba(0, 0, 0, 0.062);
    border-style: solid;
    border-width: 2px;
    color: #544141;
    border-radius: 5px;
}

.class_field {
    background-color: rgb(212, 212, 212);
    padding: 1px;
    margin-left: 2px;
    margin-right: 3px;
    border-color: rgba(0, 0, 0, 0.637);
    border-style: solid;
    border-width: 2px;
    color: #000000;
    border-radius: 5px;
}

.geo_field {
    background-color: rgb(212, 212, 212);
    padding: 1px;
    margin-left: 2px;
    margin-right: 3px;
    border-color: rgba(0, 0, 0, 0.637);
    border-style: solid;
    border-width: 2px;
    color: #0004ff;
    border-radius: 5px;
}

.name_field {
    background-color: rgb(212, 212, 212);
    padding: 1px;
    margin-left: 2px;
    margin-right: 3px;
    border-color: rgba(0, 0, 0, 0.637);
    border-style: solid;
    border-width: 2px;
    color: #496e02;
    border-radius: 5px;
}

.thumbnail {
    position: absolute;
    top: 0px;
    right: 0px;
    max-width: 403px;
    max-height: 603px;
    min-width: 100px;
    min-height: 100px;
    background-color: rgb(239, 239, 239);
    border-color: rgba(0, 0, 0, 0.1);
    border-style: solid;
    border-width: 1px;
}

.unit {
    color: #000000c9;
    background-color: rgb(240, 240, 240);
    padding: 2px;
    border-radius: 5px;
}

.table-details th {
    padding: 3px;
    background-color: #d7d7d7;
}

.table-details td {
    padding: 3px;
    background-color: rgb(233, 233, 233);
    text-align: center;
}

.header-unit {
    color: rgba(0, 0, 0, 0.68); 
    font-weight:normal;
}

.url {
    background-color: #0000000f;
    color: #13274f;
    padding: 5px;
    font-weight: bold;
    border-style: solid;
    border-radius: 8px;
    border-color: #6868681f;
    font-family: "Courier New", Courier, monospace;   
}

.legend img {
    background-color: rgb(239, 239, 239);
    border-color: rgba(0, 0, 0, 0.1);
    border-style: solid;
    border-width: 1px;
}

</style>
