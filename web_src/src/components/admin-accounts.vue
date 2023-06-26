<template>
<div class="innergrid-container">
    <div class="innergrid-item-main">
    <v-layout>
      <v-flex xs12 sm6 offset-sm3>
        <v-card>
          <h3 class="headline mb-0">Accounts
          <v-btn flat icon color="green" @click="refresh()" title="reload accounts from source and refresh view">
            <v-icon>refresh</v-icon>
          </v-btn>
          <v-btn style="margin-left: 100px;" @click="$refs.add_account.show = true;" title="Insert a new account."><v-icon>add</v-icon> create account</v-btn>
          {{refreshMessage}}
          <dialog-add-account ref="add_account" @changed="refresh"/>
          <v-text-field
            label="Search"
            placeholder="Username or role"
            v-model="search"
          />
          </h3>
          <v-divider></v-divider>
          <v-data-table :headers="headers" :items="accounts" v-if="accounts != undefined" hide-actions :search="search">
              <template slot="items" slot-scope="props">
                <td :class="{admin: accountIsAdmin(props.item), empty: props.item.roles.length == 0}">
                  <v-icon v-if="accountIsAdmin(props.item)" title="Account is admin.">admin_panel_settings</v-icon><v-icon v-else title="User account, no admin.">account_box</v-icon>
                  <span class="user-list">{{props.item.name}}</span>
                  <v-icon v-if="!props.item.managed" title="Account can not be changed or removed.">lock</v-icon>                  
                </td>
                <td>
                  <v-btn v-if="props.item.managed" @click="selectedAccount = props.item; $refs.manage_account.show = true;" icon color="grey lighten-3">
                    <v-icon title="Manage account.">folder_open</v-icon>
                  </v-btn>
                  <v-btn v-if="props.item.managed" @click="selectedAccount = props.item; $refs.remove_account.show = true;" icon color="grey lighten-3">
                    <v-icon title="Delete account.">delete_forever</v-icon>
                  </v-btn>
                </td>                
                <td>
                  <span v-for="role in props.item.roles" :key="role" class="role-list">{{role}}</span>
                  <span v-if="props.item.roles.length === 0" style="color: grey;">(none)</span>
                </td>
              </template>
          </v-data-table>
          <v-divider></v-divider>
          <dialog-manage-account ref="manage_account" :account="selectedAccount" @changed="refresh"/>
          <dialog-remove-account ref="remove_account" :account="selectedAccount" @changed="refresh"/>
          <br>
          <br>         

          <br>
          <h3 class="headline mb-0">Roles</h3>
          <v-divider></v-divider>
          <v-data-table :headers="roles_headers" :items="roles" v-if="acl_roles != undefined" hide-actions>
              <template slot="items" slot-scope="props">
                <td><!--<v-icon>vpn_key</v-icon>--><span class="role-list">{{props.item.role}}</span></td>
                <td>
                  <span v-for="user in props.item.users" :key="user" class="user-list">{{user}}</span>
                  <!--<span v-if="props.item.users.length === 0" style="color: grey;">(none)</span>-->
                </td>                
              </template>
          </v-data-table>
          <v-divider></v-divider>
        </v-card>
      </v-flex>
    </v-layout>
    <v-snackbar v-model="accountsError" :top="true">
      {{accountsErrorMessage}}
      <v-btn flat class="pink--text" @click.native="accountsError = false">Close</v-btn>
    </v-snackbar>
  </div>
  </div>
</template>

<script>

import { mapState } from 'vuex'
import axios from 'axios'

import dialogAddAccount from './dialog-add-account.vue'
import dialogManageAccount from './dialog-manage-account.vue'
import dialogRemoveAccount from './dialog-remove-account.vue'

export default {
  name: 'admin-overview',

  components: {
    'dialog-add-account': dialogAddAccount,
    'dialog-manage-account': dialogManageAccount,
    'dialog-remove-account': dialogRemoveAccount,
  },

  data() {
    return {
      accounts: undefined,
      accountsError: false,
      accountsErrorMessage: undefined,
      refreshMessage: 'init...',

      headers: [
        {text: "User", value: "name"},  
        {text: "Actions", value: "managed"}, 
        {text: "Roles", value: "roles"},         
      ],
      
      roles_headers: [
        {text: "Role", value: "role"},    
        {text: "Users", value: "users"}, 
      ],

      selectedAccount: undefined,
      search: undefined,
    }
  },
  methods: {
    refresh() {
      this.$store.dispatch('acl_roles/refresh');
      this.refreshMessage = "Reloading accounts list...";
      var self = this;
      axios.get(this.urlPrefix + '../../api/accounts')
        .then(function(response) {
          self.accounts = response.data.accounts;
          self.refreshMessage = "";
        })
        .catch(function(error) {
          console.log(error);
          self.accountsError = true;
          self.accountsErrorMessage = "ERROR getting accounts: " + error;
          self.refreshMessage = "Error loading accounts.";
        });
    },
    accountIsAdmin(account) {
      return account.roles.indexOf('admin') >= 0;
    },
  },
  computed: {
    ...mapState({
      urlPrefix: state => state.identity.urlPrefix,
    }),
    acl_roles() {
      //return ['admin'].concat(this.$store.state.acl_roles.data);
      return this.$store.state.acl_roles.data;
    },
    roles() {
      if(this.acl_roles === undefined) {
        return undefined;
      }
      var roles = this.acl_roles.map(acl_role => {
        var users = [];
        if(this.accounts !== undefined) {
          users = this.accounts.filter(account => {
            return account.roles.some(role => role === acl_role);
          }).map(account => account.name);
        }
        return {role: acl_role, users: users};
      })
      return roles;
    },
    filteredAccount() {
      return null;
    },
  },
  mounted() {
    this.$store.dispatch('acl_roles/init');
    this.refresh();
  },
}



</script>

<style scoped>
.innergrid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
}

.innergrid-item-main {
  padding: 0;
  overflow-y: auto;
}
</style>

<style scoped> 
 .admin {
   font-weight: bold;
 }

  .empty {
   color: grey;
 }

 .role-list {
  background-color: rgb(243, 243, 243);
  padding: 5px;
  /*border-color: rgb(227, 227, 227);
  border-style: solid;
  border-width: 1px;*/
  margin: 5px;
  border-radius: 25px;
  color: #0064ff;
}

 .user-list {
  padding: 5px;
  background-color: rgb(243, 243, 243);
  /*border-color: rgb(136, 136, 136);
  border-style: solid;
  border-width: 1px;*/
  margin: 5px;
  border-radius: 3px;
}
</style>
