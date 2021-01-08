<template>
    <div class="main">
        <transition name="fade">
        <div v-if="busy" class="div-busy">
            <pulse-loader />{{busyMessage}}
        </div>
        </transition>
        <div style="text-align: center;">
            <h3 class="headline mb-0">{{meta === undefined || meta.title === undefined || meta.title === '' ? poi_group : meta.title}}</h3>
            <span v-if="meta !== undefined && meta.title !== undefined && meta.title !== ''"><span style="user-select: none;">id: </span><b>{{poi_group}}</b></span>
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
                <table>
                    <tr>
                        <td><b>description:</b></td>
                        <td><span v-if="meta.description.length === 0" style="color: grey;">(none)</span><span v-else>{{meta.description}}</span></td>                        
                    </tr>
                    <tr>
                        <td><b>tags:</b></td>
                        <td>                      
                            <span v-for="tag in optArray(meta.tags)" :key="tag"><span class="meta-list">{{tag}}</span>&nbsp;&nbsp;&nbsp;</span>
                            <span v-if="optArray(meta.tags).length === 0" style="color: grey;">(none)</span>
                        </td>
                    </tr>
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

            <v-divider class="meta-divider" />   
            <v-card-title>
                {{meta.pois.length}} POIs
                <v-spacer></v-spacer>
                <v-spacer></v-spacer>
                <v-text-field v-model="search" append-icon="search" label="Search" single-line hide-details />
            </v-card-title>
            <v-data-table :headers="headers" :items="meta.pois" hide-actions  :search="search" :custom-filter="filterFunc" :custom-sort="sortFunc">
              <template slot="items" slot-scope="props">
                <td>{{props.item.name}}</td>
                <td>{{props.item.x}}, {{props.item.y}}</td>
              </template>
          </v-data-table>
        </div>
    </div>
</template>

<script>

import { mapState, mapGetters } from 'vuex'
import axios from 'axios'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'



export default {
    name: 'admin-poi_group-detail',
    components: {
        PulseLoader,        
    },
    props: ['poi_group'],
    data() {
        return {
            meta: undefined,
            metaError: false,
            metaErrorMessage: undefined,
            busy: false,
            busyMessage: undefined,

            headers: [
                {text: "name", value: "name"},
                {text: "position", value: "position"},
      
            ],
            search: undefined,
        }
    },
    methods: {
        refresh() {
            var self = this;
            var url = this.urlPrefix + '../../poi_groups/' + self.poi_group;
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url)
                .then(function(response) {
                    self.meta = response.data.poi_group;
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
        cmpFunc(index, isDescending) {
            var compare = new Intl.Collator(undefined, {numeric: true}).compare;
            console.log(index);
            switch(index) {
                case 'position': {
                    return function(a, b) {
                        var c = (a.x - b.x);
                        var d = c === 0 ? (a.y - b.y) : c;
                        return isDescending ? -d : d;
                    };
                }
                default: {
                    return function(a, b) {
                    var c = compare(a.name, b.name);
                        return isDescending ? -c : c;
                    };
                }
            }
        } ,       
        sortFunc(items, index, isDescending) {            
            return items.sort(this.cmpFunc(index, isDescending));
        },
    },
    computed: {
        ...mapState({
            urlPrefix: state => state.identity.urlPrefix,
        }),
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),
        modify() {
            return this.meta === undefined || this.meta.modify == undefined ? false :  this.meta.modify;
        },
    },
    watch: {
        poi_group() {
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
