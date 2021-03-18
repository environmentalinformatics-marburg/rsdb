<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Color</v-card-title>
        <v-card-text>
        <chrome-picker v-model="colors"/>
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
    name: 'dialog-color',
    props: {
      value: {
          type: String,
          required: true
      },
    },

    components: {
      'chrome-picker': Chrome,
    },

    data() {
        return {
            show: false,
            colors: '#FF0000FF',
        }
    },
    computed: {
    },    
    methods: {
      commit() {
        if(this.colors.hex8 !== undefined) {
          console.log(this.colors);
          console.log(this.colors.hex8);
          this.$emit("input", this.colors.hex8);
        }
        this.show = false;
      }
    },
    watch: {
      value: {
        immediate: true,
        handler() {
          this.colors = this.value
        },
      },
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>