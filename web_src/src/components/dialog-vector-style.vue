<template>
    <v-dialog :value="show" lazy persistent max-width="600px">
      <v-card>
        <v-card-title class="headline">Style</v-card-title>
        <v-card-text>
            Change <b>stroke color</b> (<v-icon>brush</v-icon>), change <b>fill color</b> (<v-icon>format_color_fill</v-icon>).
        </v-card-text>        
        <hr>
        <hr>
        <v-card-text v-if="style != undefined">             
            <table>
                <tr>
                    <td>
                        <dialog-stroke 
                            :stroke_color="style.stroke_color"
                            :stroke_width="style.stroke_width"
                            :stroke_dash="style.stroke_dash"
                            @stroke="onStroke"  
                            ref="stroke"
                        />
                        <v-btn @click="$refs.stroke.show = true;" icon><v-icon>brush</v-icon></v-btn>
                    </td>
                    <td><b>Stroke</b></td>                    
                    <td :style="{'background-color': 'white', 'border-top-style': 'solid', 'border-top-color': style.stroke_color, 'border-top-width': style.stroke_width + 'px',}" style="margin-top: 10px; padding-left: 50px; padding-top: 10px; padding-bottom: 10px;"></td>                    
                    <td style="padding-left: 10px; padding-right: 10px;">+</td>
                    <td><dialog-color v-model="style.fill_color" ref="fill_color"/><v-btn @click="$refs.fill_color.show = true;" icon><v-icon>format_color_fill</v-icon></v-btn></td>
                    <td><b>Fill</b></td>                    
                    <td :style="{'background-color': style.fill_color}" style="padding-left: 80px;"></td>
                    <td style="padding-left: 10px; padding-right: 10px;"> --> </td>
                    <td style="background: linear-gradient(90deg, rgba(0,0,0,1) 0%, rgba(255,255,255,1) 100%);">
                        <span :style="{'background-color': style.fill_color, 'border-style': 'solid', 'border-color': style.stroke_color, 'border-width': style.stroke_width + 'px',}" style="margin: 5px; padding-left: 50px; padding-top: 10px; padding-bottom: 10px;"></span>   
                    </td>
                    <td style="background: linear-gradient(90deg, red, yellow, lime, aqua, blue, magenta);">
                        <span :style="{'background-color': style.fill_color, 'border-style': 'solid', 'border-radius': '20px', 'border-color': style.stroke_color, 'border-width': style.stroke_width + 'px',}" style="margin: 1px; padding-left: 60px; padding-top: 10px; padding-bottom: 10px;"></span>   
                    </td>
                </tr>
            </table>
        </v-card-text>
        <hr>
        <v-card-text>
            {{postMessage}}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="show = false">Discard</v-btn>
          <v-btn color="green darken-1" flat="flat" @click.native="apply">Apply</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios';

import dialogStroke from './dialog-stroke.vue'
import dialogColor from './dialog-color.vue'

export default {
    name: 'dialog-vector-style',
    props: ['meta'],

    components: {
      'dialog-stroke': dialogStroke,
      'dialog-color': dialogColor,
    },

    data() {
        return {
            show: false,
            postMessage: undefined,
            style: undefined,
        }
    },
    computed: {
    },    
    methods: {
        async apply() {
            try {
                this.postMessage = "Sending style...";  
                let data = {style: this.style};
                var url = this.$store.getters.apiUrl('vectordbs/' + this.meta.name);             
                await axios.post(url, data);
                this.postMessage = undefined;
                //console.log(response);
                this.$emit('changed');
                this.show = false;
            } catch(error) {
                this.postMessage = "Error sending style.";
                console.log(error);
                this.$emit('changed');
                //this.show = false;
            }
        },
        onStroke(event) {
            this.style.stroke_color = event.stroke_color;
            this.style.stroke_width = event.stroke_width;
            this.style.stroke_dash = event.stroke_dash;
        }
    },
    watch: {
        meta: {
            immediate: true,
            handler() {
                let style = this.meta === undefined ? undefined : this.meta.style;
                if(style === undefined) {
                    style = {
                        type: 'basic',
                    };  
                }
                if(style.stroke_color === undefined) {
                    style.stroke_color = '#772B2BFF';
                }
                if(style.stroke_width === undefined) {
                    style.stroke_width = 2.0;
                }
                if(style.stroke_dash === undefined) {
                    style.stroke_dash = [];
                }
                if(style.fill_color === undefined) {
                    style.fill_color = '#772B2BFF';
                }
                this.style = style;
            }
        },
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>