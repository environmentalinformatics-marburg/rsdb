<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card>
        <v-card-title class="headline">Create a new account</v-card-title>
        <v-card-text>
          <v-textarea label="User" auto-grow rows="1" v-model="username" title="Username" />
          <v-textarea label="Password" auto-grow rows="1" v-model="password" title="Password" append-outer-icon="create" @click:append-outer="password = generate_nonce(12)" />
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
          <v-textarea label="Comment, e.g. email address" auto-grow rows="1" v-model="comment" title="A comment describing the account. E.g. the full user name, the user email address, etc." />          
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
            comment: '',            
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
            let action = {
              action: 'add_account', 
              user: this.username, 
              password: this.password,
              roles: this.selectedRoles,
            };
            if(this.comment !== undefined && this.comment !== null && this.comment.length > 0) {
              action.comment = this.comment;
            }
            let data = {actions: [action]};
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
      
      generate_nonce(len) {
        var rnd = new Uint32Array(len);
        window.crypto.getRandomValues(rnd);
        var nonce = "";
        var chars = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var charsLen = chars.length;
        for(var i = 0; i < len; i++) {
          nonce += chars[rnd[i] % charsLen];
        }
        return nonce;
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
          this.comment = '';
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