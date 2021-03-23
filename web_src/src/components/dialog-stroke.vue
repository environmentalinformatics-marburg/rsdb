<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Stroke</v-card-title>
        <v-card-text>
          Color
        <chrome-picker v-model="user_stroke_colors"/>
        </v-card-text>
        <v-card-text>
          Width: <b>{{user_stroke_width}}</b> pixel
          <v-slider v-model="user_stroke_width" min="0" max="20" ticks step="1">
            <template v-slot:prepend>
              <v-icon @click="if(user_stroke_width >= 1) user_stroke_width -= 1">
                remove
              </v-icon>
            </template>
            <template v-slot:append>
              <v-icon @click="if(user_stroke_width <= 19) user_stroke_width += 1">
                add
              </v-icon>
            </template>          
          </v-slider>          
        </v-card-text>
        
        <v-card-text>           
        <v-select v-model="line_style" :items="line_styles" box label="Line style" />
        <div v-if="line_style === 'dashed' || line_style === 'dashed 2' || line_style === 'dashed 3'">
          <span v-if="line_style === 'dashed'">Dash length: </span>
          <span v-else>First dash length: </span>
          <b>{{user_stroke_dash0}}</b> pixel
          <v-slider v-model="user_stroke_dash0" min="0" max="100" ticks step="1" />
        </div>
        <div v-if="line_style === 'dashed 2' || line_style === 'dashed 3'">
          Second dash length:
          <b>{{user_stroke_dash1}}</b> pixel 
          <v-slider v-model="user_stroke_dash1" min="0" max="100" ticks step="1" />
        </div>
        <div v-if="line_style === 'dashed 3'">
          Third dash length:
          <b>{{user_stroke_dash2}}</b> pixel 
          <v-slider v-model="user_stroke_dash2" min="0" max="100" ticks step="1" />
        </div>
        </v-card-text>
        
        
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="show = false">Cancel</v-btn>
          <v-btn color="green darken-1" flat="flat" @click.native="commit">Apply</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import { Chrome } from 'vue-color'

export default {
    name: 'dialog-stoke',
    props: {
      stroke_color: {
        type: String,
        required: true,
      },
      stroke_width: {
        type: Number,
        required: true,
      },
      stroke_dash: {
        type: Array,
        required: true,
      },      
    },

    components: {
      'chrome-picker': Chrome,
    },

    data() {
        return {
            show: false,
            user_stroke_colors: '#FF0000FF',
            user_stroke_width: 2.0,
            line_styles: ['solid', 'dashed', 'dashed 2', 'dashed 3'], 
            line_style: 'solid',
            user_stroke_dash0: 1,
            user_stroke_dash1: 1,
            user_stroke_dash2: 1,
        }
    },
    computed: {
    },    
    methods: {
      commit() {
        let stroke_color = this.user_stroke_colors.hex8 === undefined ? this.user_stroke_colors : this.user_stroke_colors.hex8;
        let stroke = {stroke_color: stroke_color, stroke_width: this.user_stroke_width};
        if(this.line_style === 'solid') {
          stroke.stroke_dash = [];
        } else if(this.line_style === 'dashed') {
          stroke.stroke_dash = [this.user_stroke_dash0];
        } else if(this.line_style === 'dashed 2') {
          stroke.stroke_dash = [this.user_stroke_dash0, this.user_stroke_dash1];
        } else if(this.line_style === 'dashed 3') {
          stroke.stroke_dash = [this.user_stroke_dash0, this.user_stroke_dash1, this.user_stroke_dash2];
        }
        this.$emit('stroke', stroke);
        this.show = false;
      },
      onStrokeWidthDecrement() {

      },
      onStrokeWidthIncrement() {

      },      
    },
    watch: {
      stroke_color: {
        immediate: true,
        handler() {
          this.user_stroke_colors = this.stroke_color;
        },
      },
      stroke_width: {
        immediate: true,
        handler() {
          console.log(this.stroke_width);
          this.user_stroke_width = this.stroke_width;
        },
      },
      stroke_dash: {
        immediate: true,
        handler() {
          if(this.stroke_dash === undefined || this.stroke_dash.length === 0) {
            this.line_style = 'solid';
          } else if(this.stroke_dash.length === 1) {
            this.line_style = 'dashed';
            this.user_stroke_dash0 = this.stroke_dash[0];
          } else if(this.stroke_dash.length === 2) {
            this.line_style = 'dashed 2';
            this.user_stroke_dash0 = this.stroke_dash[0];
            this.user_stroke_dash1 = this.stroke_dash[1];
          } else if(this.stroke_dash.length >= 3) {
            this.line_style = 'dashed 3';
            this.user_stroke_dash0 = this.stroke_dash[0];
            this.user_stroke_dash1 = this.stroke_dash[1];
            this.user_stroke_dash2 = this.stroke_dash[2];
          }
        },
      } ,           
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>