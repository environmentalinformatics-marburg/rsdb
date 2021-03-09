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
            <b>Gamma</b> (non-linear value transformation)
            <div style="font-size: 0.8em; padding-left: 50px;">
                auto --> Equalized value distribution
                <br>&gamma; &lt; 1 --> Emphasis on low values
                <br>&gamma; = 1 --> Linear values
                <br>&gamma; &gt; 1 --> Emphasis on high values
            </div>
            <v-select v-model="entry.gamma" :items="gammas" :show-labels="false" :allowEmpty="false" solo />
        </v-card-text>

        <v-card-text>
            <b>Single band mapping</b> (value to color pixel mapping of one band)
            <v-select v-model="entry.palette" :items="oneBandMappings" :show-labels="false" :allowEmpty="false" solo />
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
            gammas: ["auto", "0.1", "0.2", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0"],
            oneBandMappings: ["grey", "inferno", "viridis", "jet", "cividis"],
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

</style>
