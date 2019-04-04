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
          </h3>
          <v-divider></v-divider>
          <v-data-table :headers="headers" :items="accounts" v-if="accounts != undefined" hide-actions>
              <template slot="items" slot-scope="props">
                <td :class="{admin: accountIsAdmin(props.item), empty: props.item.roles.length == 0}">{{props.item.name}}</td>
                <td>
                  <span v-for="role in props.item.roles" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span>
                  <span v-if="props.item.roles.length === 0" style="color: grey;">(none)</span>
                </td>
              </template>
          </v-data-table>
          <v-divider></v-divider>
          <v-divider></v-divider>
          <br>
          <h3 class="headline mb-0">Roles</h3>
          <v-divider></v-divider>
          <v-data-table :headers="roles_headers" :items="roles" v-if="acl_roles != undefined" hide-actions>
              <template slot="items" slot-scope="props">
                <td><span class="meta-list">{{props.item.role}}</span></td>
                <td>
                  <span v-for="user in props.item.users" :key="user"><span>{{user}}</span>&nbsp;&nbsp;&nbsp;</span>
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

import axios from 'axios'

export default {
  name: 'admin-overview',
  data() {
    return {
      accounts: undefined,
      accountsError: false,
      accountsErrorMessage: undefined,

      headers: [
        {text: "user", value: "name"},
        {text: "roles", value: "roles"},        
      ],
      
      roles_headers: [
        {text: "role", value: "role"},    
        {text: "users", value: "users"}, 
      ],
    }
  },
  methods: {
    refresh() {
      var self = this;
      axios.get('../../api/accounts')
        .then(function(response) {
          self.accounts = response.data.accounts;
        })
        .catch(function(error) {
          console.log(error);
          self.accountsError = true;
          self.accountsErrorMessage = "ERROR getting accounts: " + error;
        });
    },
    accountIsAdmin(account) {
      return account.roles.indexOf('admin') >= 0;
    },
  },
  computed: {
    acl_roles() {
      return ['admin'].concat(this.$store.state.acl_roles.data);
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

 .meta-list {
    background-color: rgb(243, 243, 243);
    padding: 1px;
    border-color: rgb(227, 227, 227);
    border-style: solid;
    border-width: 1px;
}
</style>
