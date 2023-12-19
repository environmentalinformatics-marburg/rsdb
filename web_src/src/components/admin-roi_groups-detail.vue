<template>
    <div class="main">
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center;">
            <h3 class="headline mb-0">{{meta === undefined || meta.title === undefined || meta.title === '' ? roi_group : meta.title}}</h3>
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''">
                <span style="user-select: none;">id: </span>
                <b>{{roi_group}}</b>
            </span>
        </div>
        <div v-if="metaError">
            <b>ERROR</b> 
            {{metaErrorMessage}}
        </div>
        <div  v-if="meta != undefined">
            <v-divider class="meta-divider"></v-divider> 
            <h3 class="subheading mb-0"> 
                Info
            </h3> 
            <div class="meta-content">
                <box-info :meta="meta" />
                <table>
                    <tr>
                        <td><b>EPSG:</b></td>
                        <td><span v-if="meta.epsg.length === 0" style="color: grey;">(none)</span><span v-else>{{meta.epsg}}</span></td>                        
                    </tr>
                     <tr>
                        <td><b>Proj4:</b></td>
                        <td><span v-if="meta.proj4.length === 0" style="color: grey;">(none)</span><span v-else>{{meta.proj4}}</span></td>                        
                    </tr>                    
                </table>
                </div>

            <v-divider class="meta-divider" />  
            <h3 class="subheading mb-0"> 
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
            </table>
            </div>

            <template v-if="meta.messages !== undefined">
                <v-divider class="meta-divider" />  
                <h3 class="subheading mb-0"> 
                    ROI creation messages ({{ meta.messages.length }})
                </h3>                        
                <div class="meta-content">
                <div v-for="(m, i) in meta.messages" :key="i">{{ m }}</div>
                </div>
            </template>
  
            <v-divider class="meta-divider" />                            
            <v-card-title>
                {{meta.rois.length}} ROIs
                <v-spacer></v-spacer>
                <v-spacer></v-spacer>
                <v-text-field v-model="search" append-icon="search" label="Search" single-line hide-details />
            </v-card-title>
            <v-data-table :headers="headers" :items="meta.rois" hide-actions :search="search" :custom-filter="filterFunc">
                <template slot="items" slot-scope="props">
                    <td>{{props.item.name}}</td>
                    <td>{{props.item.characteristic}}</td>
                    <td style="text-align: right;">{{props.item.point_count}}</td>
                    <td style="text-align: right;">{{props.item.center[0].toFixed(4)}}, {{props.item.center[1].toFixed(4)}}</td>
                    <td style="text-align: right;">{{props.item.area === undefined ? '-' : props.item.area.toFixed(4)}}</td>
                </template>
            </v-data-table>
        </div>
    </div>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'
import boxInfo from './box-info.vue'

export default {
    name: 'admin-roi_group-detail',
    components: {
        PulseLoader,
        'box-info': boxInfo,         
    },
    props: ['roi_group'],
    data() {
        return {
            meta: undefined,
            metaError: false,
            metaErrorMessage: undefined,
            busy: false,
            busyMessage: undefined,

            headers: [
                {text: "Name", value: "name"},
                {text: "Characteristic", value: "characteristic"},
                {text: "Point count", value: "point_count", align: "right"},        
                {text: "Center", value: "center", align: "right"},
                {text: "Area", value: "area", align: "right"},
            ],
            search: undefined,            
        }
    },
    methods: {
        refresh() {
            var self = this;
            var url = this.$store.getters.apiUrl('roi_groups/' + self.roi_group);
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url)
                .then(function(response) {
                    self.meta = response.data.roi_group;
                    self.busy = false;
                    self.busyMessage = undefined;
                })
                .catch(function(error) {
                    console.log(error);
                    self.metaError = true;
                    self.metaErrorMessage = "ERROR getting meta: " + error;
                    self.meta = undefined;
                    self.busy = false;
                    self.busyMessage = undefined;
                });
                /*.finally(function()  {
                    self.busy = false;
                    self.busyMessage = undefined;
                });*/
        },
        optArray(a) {
            if(a === undefined) {
                return [];
            }
            if(Array.isArray(a)) {
                return a;
            }
            return [a];
        },
        filterFunc(items, search) {
            return items.filter(function(item) {return item.name.search(new RegExp(search, "i")) >= 0;});
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
        roi_group() {
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

</style>
