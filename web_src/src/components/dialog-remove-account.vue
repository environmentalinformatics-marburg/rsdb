<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card v-if="account !== undefined">
        <v-card-title class="headline">Remove account</v-card-title>
        <v-card-text>
          Should this account be removed?
          <br>
          <br>
          <b>{{account.name}}</b>
        </v-card-text>
        <v-card-text>
          {{postMessage}}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="show = false">Cancel</v-btn>
          <v-btn color="green darken-1" flat="flat" @click.native="commit" :disabled="!valid">Remove</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios';

export default {
    name: 'dialog-remove-account',
    props: [
      'account',
    ],

    components: {
    },

    data() {
        return {
            show: false,
            postMessage: undefined,            
        }
    },
    computed: {
      valid() {
        return true;
      },
    },    
    methods: {
      async commit() {
        if(this.valid) {
          try {
            this.postMessage = "Sending remove account...";  
            let data = {actions: [{
              action: 'remove_account', 
              user: this.account.name,
            }]};
            var url = this.$store.getters.apiUrl('api/accounts');             
            await axios.post(url, data);
            this.postMessage = undefined;
            this.$emit('changed');
            this.show = false;
          } catch(error) {
            this.postMessage = "Error remove account.";
            console.log(error);
            this.$emit('changed');
          }
        }
      },

      async refresh() {
      },      
    },
    watch: {
      show: {
        immediate: true,
        handler() {
          this.postMessage = undefined;
          if(this.show) {
            this.refresh();
          }
        },
      },
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>