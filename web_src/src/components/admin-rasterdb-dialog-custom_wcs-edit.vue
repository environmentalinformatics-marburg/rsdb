<template>
<v-dialog v-model="visible" lazy width="600px">
    <v-btn slot="activator" flat icon title="Change settings of custom WCS entry."><v-icon>edit</v-icon></v-btn>
    <v-card>
        <v-card-title>
            Custom WCS entry: <b> {{id}}</b>
        </v-card-title>
        <hr />   

        <v-card-text>
            <b>Coordinate reference system (CRS)</b>  (reprojection)
            <v-select v-model="crsSelect" :items="crss" :show-labels="false" :allowEmpty="false" solo hide-details />
            <span :class="{disabled: (crsSelect !== -1)}">    
                EPSG:<input type="text" id="name" name="name" maxlength="8" size="10" class="text-input" :disabled="crsSelect !== -1" placeholder="number" v-model="crsUserValue" :class="{'text-input-invalid': isNaN(crsUserValue) || crsUserValue < 0}"/>
            </span>
        </v-card-text>  

        <v-card-text>
            <b>Included bands</b>  (fewer bands for quick network transfer times)
            <v-select 
                v-model="selectedBands" 
                :items="layerBands" 
                :item-text="item => item.index + ' - ' + item.title" 
                item-value="index" 
                :show-labels="false" 
                :allowEmpty="false" 
                solo 
                hide-details
                multiple
                label="(all bands)" 
            />
        </v-card-text>         
    </v-card>
</v-dialog>
</template>

<script>


export default {
    name: 'admin-rasterdb-dialog-custom_wcs-edit',
    props: ['meta', 'entry', 'id'],        
    components: {
   
    },
    data() {
        return {
            visible: false,            
            crss: [
                {value: 0, text: '(no reprojection)'},
                {value: 3857, text: 'EPSG:3857 (Web Mercator)'},
                {value: -1, text: 'other (custom)'},
            ],
            crsSelect: 0,
            crsUserValue: 0,
            preselecting: false,
            selectedBands: [],
        }
    },
    computed: {
        layerBands() {
            if(this.meta === undefined) {
                return [];
            }
            if(this.meta.bands === undefined) {
                return [];
            }
            return this.meta.bands;
        },
    },    
    methods: {    
    },
    watch: {
        crsSelect() {
            if(!this.preselecting) {
                if(this.crsSelect > 0) {
                    this.crsUserValue = this.crsSelect;
                } else if(this.crsSelect === 0) {
                    this.crsUserValue = 0;                    
                } else {
                    this.crsUserValue = 3857; 
                }
            }
        },
        crsUserValue() {
            this.entry.epsg = this.crsUserValue;
        },
        selectedBands() {
            if(!this.preselecting) {
                if(this.selectedBands !== undefined && this.selectedBands !== null) {
                    this.entry.bands = this.selectedBands.slice().sort();
                } else {
                    this.entry.bands = [];                 
                }
            }
        },
        async visible() {
            if(this.visible) {
                console.log("update");
                console.log(this.entry.epsg);
                try {
                    this.preselecting = true;
                    if(this.entry.epsg === undefined || this.entry.epsg === null || this.entry.epsg <= 0) {
                        this.crsSelect = 0;
                        this.crsUserValue = 0;
                    } else if(this.entry.epsg === 3857) {
                        this.crsSelect = 3857;
                        this.crsUserValue = 3857;
                    } else {
                        this.crsSelect = -1;
                        this.crsUserValue = this.entry.epsg;
                    }
                    if(this.entry.bands !== undefined) {
                        this.selectedBands = this.entry.bands.slice();
                    } else {
                        this.selectedBands = [];
                    }
                } finally {
                    await this.$nextTick();
                    this.preselecting = false;
                }
            }
        }
    },
    mounted() {

    },
}

</script>

<style scoped>

.text-input {
  border-style: solid;
}

.text-input-invalid {
  color: red;
}

.disabled {
  color: grey;
}

</style>
