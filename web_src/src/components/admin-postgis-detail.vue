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
                <img :key="meta.name" :src="$store.getters.apiUrl('postgis/layers/' + meta.name + '/image.png?width=400&height=600')" alt="" class="thumbnail" /> 
                <box-info :meta="meta" />
            </div>
            
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                Details
            </h3>
            <div class="meta-content">
                <table style="border-spacing: 4px;">
                    <tr><td><b>EPSG:</b></td><td>{{meta.epsg}}</td></tr>   
                    <tr><td><b>Extent:</b></td><td v-if="meta.extent !== undefined">{{meta.extent.xmin.toPrecision(8)}}<b>,</b> {{meta.extent.ymin.toPrecision(8)}} <b>-</b> {{meta.extent.xmax.toPrecision(8)}}<b>,</b> {{meta.extent.ymax.toPrecision(8)}}</td></tr>             
                    <tr  v-if="meta.item_count !== undefined"><td><b>Items:</b></td><td>{{meta.item_count}}</td></tr>  
                    <tr v-if="meta.geometry_types !== undefined">
                        <td><b>Types:</b></td>
                        <td style="display: flex; flex-wrap: wrap; gap: 10px;"><span v-for="name in meta.geometry_types" :key="name"><span>{{name}}</span></span></td>
                    </tr>
                    <tr><td><b>Geometry attribute:</b></td><td><span class="geo_attribute">{{meta.geometry_attribute}}</span></td></tr>   
                    <tr>
                        <td><b>Class attributes:</b></td>
                        <td>
                            <span v-if="meta.class_attributes.length > 0" style="display: flex; flex-wrap: wrap; gap: 10px;"><span v-for="name in meta.class_attributes" :key="name"><span class="class_attribute">{{name}}</span></span></span>
                            <span v-else>(none)</span>
                        </td>
                    </tr>                    
                    <tr>
                        <td><b>Attributes:</b></td>
                        <td>
                            <span v-if="meta.attributes.length > 0" style="display: flex; flex-wrap: wrap; gap: 10px;"><span v-for="name in meta.attributes" :key="name"><span :class="isClass_attribute(name) ? 'class_attribute' : 'attribute'">{{name}}</span></span></span>
                            <span v-else>(none)</span>
                        </td>
                    </tr>
                </table>
                <admin-vectordb-dialog-data-table :meta="meta" @changed="refresh" />
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

        </div>        
    </div>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'
import dialogSetInfo from './dialog-set-info.vue'
import boxInfo from './box-info.vue'
import adminPostgisDialogSetAcl from './admin-postgis-dialog-set-acl.vue'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'

export default {
    name: 'admin-postgis-detail',
    components: {
        'dialog-set-info': dialogSetInfo,
        'box-info': boxInfo,
        'admin-postgis-dialog-set-acl': adminPostgisDialogSetAcl,        
        PulseLoader,        
    },
    props: ['postgis'],
    data() {
        return {
            meta: undefined,
            metaError: false,
            metaErrorMessage: undefined,
            busy: false,
            busyMessage: undefined,
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

        isClass_attribute(attr) {
            if(this.meta === undefined || this.meta.class_attributes === undefined) {
                return false;
            }
            return this.meta.class_attributes.includes(attr);
        },
    },
    computed: {
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),
        modify() {
            return this.meta === undefined || this.meta.modify == undefined ? false :  this.meta.modify;
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

.attribute {
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

.class_attribute {
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

.geo_attribute {
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

.thumbnail {
    position: absolute;
    top: 0px;
    right: 0px;
    max-width: 420px;
    max-height: 600px;
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

</style>
