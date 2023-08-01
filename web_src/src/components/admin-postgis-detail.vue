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
    </div>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'

export default {
    name: 'admin-postgis-detail',
    components: {
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
        refresh(more_details) { 
            var self = this;
            this.$store.dispatch('postgis/refresh');
            var url = this.$store.getters.apiUrl('postgis/layers/' + self.postgis);
            var params = {};
            if(more_details) {
                params.storage_measures = true;
            }
            self.metaError = false;
            self.metaErrorMessage = undefined;
            self.busy = true;
            self.busyMessage = "loading ...";
            axios.get(url, {params: params})
                .then(function(response) {
                    self.meta = response.data.voxeldb;
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

.attributes {
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
