<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Add account</v-card-title>
        <v-card-text>
          <v-textarea label="User" auto-grow rows="1" v-model="username" title="Username" />
          <v-textarea label="Password" auto-grow rows="1" v-model="password" title="Password" />
          Roles
          <multiselect 
            v-model="selectedRoles" 
            :options="acl_roles" 
            multiple 
            :taggable="true" 
            @tag="createAclRole" 
            placeholder="select roles" 
            tagPlaceholder="Press enter to create a role"
          />          
        </v-card-text>
        <v-card-text>
          {{postMessage}}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" flat="flat" @click.native="show = false">Cancel</v-btn>
          <v-btn color="green darken-1" flat="flat" @click.native="commit" :disabled="!valid">Add</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios';
import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
    name: 'dialog-add-account',
    props: {
    },

    components: {
      Multiselect,
    },

    data() {
        return {
            show: false,
            username: '',
            password: '',
            postMessage: undefined,
            selectedRoles: [],
            availableRoles: [],
            createdAclRoles: [],            
        }
    },
    computed: {
      valid() {
        if(this.username === undefined || this.username === null || !(this.username.length > 0)) {
          return false;
        }
        return true;
      },
      acl_roles() {
        return this.availableRoles.concat(this.createdAclRoles);
      },
    },    
    methods: {
      async commit() {
        if(this.valid) {
          try {
            this.postMessage = "Sending add account...";  
            let data = {actions: [{
              action: 'add_account', 
              user: this.username, 
              password: this.password,
              roles: this.selectedRoles,
            }]};
            var url = this.$store.getters.apiUrl('api/accounts');             
            await axios.post(url, data);
            this.postMessage = undefined;
            this.$emit('changed');
            this.show = false;
          } catch(error) {
            this.postMessage = "Error add account.";
            console.log(error);
            this.$emit('changed');
          }
        }
      },

      createAclRole(newAclRole) {
        this.createdAclRoles.push(newAclRole);
        this.selectedRoles.push(newAclRole);
      },

      async refresh() {
        try {          
          var response = await axios.get(this.$store.getters.apiUrl('api/roles'));
          this.availableRoles = response.data.roles;
        } catch(error) {
          console.log(error);
        }
      },      
    },
    watch: {
      show: {
        immediate: true,
        handler() {
          this.username = '';
          this.password = '';
          this.postMessage = undefined;
          this.selectedRoles = [];
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