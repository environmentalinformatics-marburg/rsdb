<template>
    <v-dialog :value="show" lazy persistent max-width="500px">
      <v-card v-if="account !== undefined">
        <v-card-title class="headline">Manage account</v-card-title>
        <v-card-text>
          User: <b>{{account.name}}</b>
        </v-card-text>
        <v-card-text>
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
          Password, leave empty to keep current password.
         <v-textarea auto-grow rows="1" v-model="password" title="Password. Leave empty if you do not want to change the password." append-outer-icon="create" @click:append-outer="password = generate_nonce(12)" />
        </v-card-text>
        <v-card-text v-if="account.date_created !== undefined">
          <b>date_created:</b> {{account.date_created}}
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
          <v-btn color="green darken-1" flat="flat" @click.native="commit" :disabled="!valid">Apply</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<script>

import axios from 'axios';
import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
    name: 'dialog-manage-account',
    props: [
      'account',
    ],

    components: {
      Multiselect,      
    },

    data() {
        return {
            show: false,
            postMessage: undefined,
            
            selectedRoles: [],
            availableRoles: [],
            createdAclRoles: [],

            password: '',
            comment: '',
        }
    },
    computed: {
      valid() {
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
            this.postMessage = "Sending set account...";
            let action = {
              action: 'set_account', 
              user: this.account.name,
              roles: this.selectedRoles,
            };
            if(this.password !== undefined && this.password !== null && this.password.length > 0) {
              action.password = this.password;
            }
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
            this.postMessage = "Error set account.";
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

        this.selectedRoles = [];
        this.password = '';
        this.comment = '';
        if(this.account !== undefined) {
          this.selectedRoles = this.account.roles;
        }
        if(this.account !== undefined && this.account.comment !== undefined) {
          this.comment = this.account.comment;
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
          this.postMessage = undefined;
          if(this.show) {
            this.refresh();
          }
        },
      },
      account: {
        immediate: true,
        handler() {
          this.refresh();
        },
      },
    },
    mounted() {
    },
}

</script>

<style scoped>

</style>