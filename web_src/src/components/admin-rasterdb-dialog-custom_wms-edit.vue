<template>
<v-menu
    v-model="visible"
    transition="scale-transition"
    :close-on-content-click="false"
>
    <template v-slot:activator="{ on }">
        <v-btn v-on="on" slot="activator" flat icon title="Change settings of custom WMS entry."><v-icon>edit</v-icon></v-btn>
    </template>
    <v-card>
        <v-card-title>
            Custom WMS entry: <b> {{id}}</b>
        </v-card-title>
        <hr />
        <v-card-text>
            <b>Value range</b> (mapped value interval)
            <v-select v-model="entry.value_range" :items="value_range" :show-labels="false" :allowEmpty="false" solo hide-details />
            <span :class="{disabled: (entry.value_range !== 'static')}">
                Static range 
                min: <input type="text" id="name" name="name" maxlength="8" size="10" class="text-input" :disabled="(entry.value_range !== 'static')" placeholder="minimum" v-model="entry.value_range_static_min" :class="{'text-input-invalid': (entry.value_range === 'static') && isNaN(entry.value_range_static_min)}"/>
                max: <input type="text" id="name" name="name" maxlength="8" size="10" class="text-input" :disabled="(entry.value_range !== 'static')" placeholder="maximum" v-model="entry.value_range_static_max" :class="{'text-input-invalid': (entry.value_range === 'static') && isNaN(entry.value_range_static_max)}"/>
            </span>            
        </v-card-text>

        <v-card-text>
            <b>Gamma</b> (non-linear value transformation)
            <div style="font-size: 0.8em; padding-left: 50px;">
                auto --> Equalized value distribution
                <br>&gamma; &lt; 1 --> Emphasis on low values
                <br>&gamma; = 1 --> Linear values
                <br>&gamma; &gt; 1 --> Emphasis on high values
            </div>
            <v-select v-model="entry.gamma" :items="gammas" :show-labels="false" :allowEmpty="false" solo hide-details />
            <v-checkbox v-model="entry.gamma_auto_sync" label="Same gamma for multiple bands" hide-details :disabled="entry.gamma !== 'auto'" />
        </v-card-text>

        <v-card-text>
            <b>Single band mapping</b> (value to color pixel mapping of one band)
            <v-select v-model="entry.palette" :items="oneBandMappings" :show-labels="false" :allowEmpty="false" solo hide-details />
        </v-card-text>

         <v-card-text>
            <b>Format</b> (image type)
            <v-select v-model="entry.format" :items="formats" :show-labels="false" :allowEmpty="false" solo hide-details />
        </v-card-text>      
    </v-card>
</v-menu>
</template>

<script>


export default {
    name: 'admin-rasterdb-dialog-custom_wms-edit',
    props: ['entry', 'id'],        
    components: {
   
    },
    data() {
        return {
            visible: false,
            value_range: ["auto", "static"],
            gammas: ["auto", "0.1", "0.2", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0"],
            oneBandMappings: ["grey", "inferno", "viridis", "jet", "cividis"],
            formats: ["png:compressed", "png:uncompressed", "jpg", "jpg:small"],
        }
    },
    methods: {
        


    },
    watch: {

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
