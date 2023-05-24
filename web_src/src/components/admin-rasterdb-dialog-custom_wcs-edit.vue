<template>
<v-menu
    v-model="visible"
    transition="scale-transition"
    :close-on-content-click="false"
>
    <template v-slot:activator="{ on }">
        <v-btn v-on="on" slot="activator" flat icon title="Change settings of custom WCS entry."><v-icon>edit</v-icon></v-btn>
    </template>
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
    </v-card>
</v-menu>
</template>

<script>


export default {
    name: 'admin-rasterdb-dialog-custom_wcs-edit',
    props: ['entry', 'id'],        
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
        }
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
