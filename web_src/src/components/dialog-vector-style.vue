<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Styles</v-card-title>
        <hr>
        <v-card-title class="headline">Point feature</v-card-title>
        <v-card-text v-if="point_style != undefined">
            <span :style="{color: point_style.color}">_______</span>
            <dialog-color v-model="point_style.color" ref="point_style_color"/>
            <v-btn @click="$refs.point_style_color.show = true;">color</v-btn>
            {{point_style.color}}
        </v-card-text>
        <hr>
        <v-card-title class="headline">Line feature</v-card-title>
         <v-card-text v-if="line_style != undefined">
            <div>
            <span :style="{color: line_style.color}">_______</span>
            <dialog-color v-model="line_style.color" ref="line_style_color"/>            
            <v-btn @click="$refs.line_style_color.show = true;">color</v-btn>
            </div>
            {{line_style.color}}
        </v-card-text>
        <hr>
        <v-card-title class="headline">Polygon feature</v-card-title>
         <v-card-text v-if="polygon_style != undefined">
            <div>
            <span :style="{color: polygon_style.color}">_______</span>
            <dialog-color v-model="polygon_style.color" ref="polygon_style_color"/>            
            <v-btn @click="$refs.polygon_style_color.show = true;">color</v-btn>
            </div>
            {{polygon_style.color}}
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
            line_style: undefined,
            polygon_style: undefined,
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
                if(this.line_style !== undefined) {
                   vector_style.line_style = this.line_style;
                }
                if(this.polygon_style !== undefined) {
                   vector_style.polygon_style = this.polygon_style;
                }
                let data = {vector_style: vector_style};
                var url = this.$store.getters.apiUrl('vectordbs/' + this.meta.name);             
                let response = await axios.post(url, data);
                this.postMessage = undefined;
                //console.log(response);
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

                let line_style = vector_style.line_style;
                if(line_style === undefined) {
                    line_style = {
                        line_type: 'box',
                        color: '#772B2BFF',
                    }; 
                }
                this.line_style = line_style;

                let polygon_style = vector_style.polygon_style;
                if(polygon_style === undefined) {
                    polygon_style = {
                        polygon_type: 'box',
                        color: '#772B2BFF',
                        outline_color: '#772B2BFF',
                    }; 
                }
                this.polygon_style = polygon_style;
            }
        },
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>