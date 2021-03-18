<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Styles</v-card-title>
        <hr>
        <v-card-title class="headline">Point feature</v-card-title>
        <v-card-text v-if="point_style != undefined">
            <span :style="{color: point_style.color}">_______</span>
            <v-btn @click="$refs.color.show = true;">color</v-btn>
            <dialog-color v-model="point_style.color" ref="color"/>
            {{point_style.color}}
        </v-card-text>
        <hr>
        <v-card-title class="headline">Line feature</v-card-title>
        <v-card-text>
        </v-card-text>
        <hr>
        <v-card-title class="headline">Polygon feature</v-card-title>
        <v-card-text>
        </v-card-text>
        <hr>
        <v-card-text>
            {{postMessage}}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="show = false">Cancel</v-btn>
          <v-btn color="green darken-1" flat="flat" @click.native="apply">Apply</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios';

import dialogColor from './dialog-color.vue'

export default {
    name: 'dialog-vector-style',
    props: ['meta'],

    components: {
      'dialog-color': dialogColor,
    },

    data() {
        return {
            show: false,
            postMessage: undefined,
            point_style: undefined,
        }
    },
    computed: {
    },    
    methods: {
        async apply() {
            try {
                this.postMessage = "Sending style...";  
                let vector_style = {};
                if(this.point_style !== undefined) {
                   vector_style.point_style = this.point_style;
                }
                let data = {vector_style: vector_style};
                var url = this.$store.getters.apiUrl('vectordbs/' + this.meta.name);             
                let response = await axios.post(url, data);
                this.postMessage = undefined;
                console.log(response);
                this.$emit('changed');
                this.show = false;
            } catch(error) {
                this.postMessage = "Error sending syle.";
                console.log(error);
                this.$emit('changed');
                //this.show = false;
            }
        },
    },
    watch: {
        meta: {
            immediate: true,
            handler() {
                let vector_style = this.meta === undefined ? undefined : this.meta.vector_style;
                if(vector_style === undefined) {
                vector_style = {};  
                }
                let point_style = vector_style.point_style;
                if(point_style === undefined) {
                point_style = {
                    point_type: 'box',
                    color: '#772B2BFF',
                }; 
                }
                this.point_style = point_style;
            }
        },
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>