<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn title="create new VectorDB layer" slot="activator">
                <v-icon>add</v-icon> create layer
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">create new VectorDB layer</div>
                </v-card-title>

                <v-card-text>
                    <v-text-field v-model="name" label="name" />
                    <i>Space, not latin chars and other special chars are not allowed. You may replace space chars by underscore or hyphen.
                    <br>Allowed chars: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-</i>                    
                </v-card-text>

                <v-card-text>
                    <span v-if="identity !== undefined">Your roles: <span v-for="role in identity.roles" :key="role"><span class="meta-list">{{role}}</span>&nbsp;&nbsp;&nbsp;</span></span>
                    <div v-if="isAdmin">Your are <b>admin</b>, so you are able read/modify layers independent of specified roles!</div>
                    <br>
                    <br>Specified roles can <b>read</b> layer data.
                    <multiselect v-model="selectedRoles" :options="acl_roles" multiple :taggable="true" @tag="createAclRole" placeholder="select roles" tagPlaceholder="Press enter to create a role"/>

                    <br>Specified roles can <b>modify</b> layer data.
                    <multiselect v-model="selectedRolesMod" :options="acl_roles" multiple :taggable="true" @tag="createAclModRole" placeholder="select roles" tagPlaceholder="Press enter to create a role"/>

                    <br><i>Note: Only users that have one of the the read/modify roles are able to view/change created vector layer!</i>
                    <br><span style="color: red;" v-if="!name">ERROR: You need to specify a name for the new layer.</span>
                    <br><span style="color: red;" v-if="selectedRoles.length == 0 || selectedRolesMod.length == 0">ERROR: You need to specify at least one role for read and for modify.</span>                </v-card-text>


                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="grey--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="execute()" :disabled="!name || selectedRoles.length == 0 || selectedRolesMod.length == 0">Execute</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-snackbar v-model="setError" :top="true">
            {{setErrorMessage}}
            <v-btn flat class="pink--text" @click.native="setError = false">Close</v-btn>
        </v-snackbar>
    </span>
</template>

<script>

import { mapGetters } from 'vuex'
import axios from 'axios'
import Multiselect from 'vue-multiselect'

export default {
    name: 'admin-vectordb-create',
    props: [],        
    components: {
        Multiselect,
    },
    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            name: "",
            selectedRoles: [],
            selectedRolesMod: [],
            createdAclRoles: [],            
        }
    },
    methods: {

        refresh() {
            this.$store.dispatch('acl_roles/refresh');
        },
        
        execute() {
            var self = this;
            var url = this.$store.getters.apiUrl('vectordbs');
            axios.post(url, {
                create_vectordb: {
                    name: self.name,
                    acl: this.selectedRoles,
                    acl_mod: this.selectedRolesMod,
                }
            }).then(function(response) {
                console.log(response);
                self.dialog = false;
                self.$store.dispatch('vectordbs/refresh');
                self.$emit('created_vectordb', self.name);
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error: " + self.errorToText(error);
                console.log(error);                
                self.dialog = false;
                self.$store.dispatch('vectordbs/refresh');
            });                        
        },

        errorToText(error) {
            if(error === undefined) {
                return "unknown error";
            }
            if(error.message === undefined) {
                return error;
            }
            if(error.response === undefined || error.response.data === undefined) {
                return error.message;
            }
            return error.message + " - " + JSON.stringify(error.response.data);
        },

        createAclRole(newAclRole) {
            this.createdAclRoles.push(newAclRole);
            this.selectedRoles.push(newAclRole);
        },

        createAclModRole(newAclModRole) {
            this.createdAclRoles.push(newAclModRole);
            this.selectedRolesMod.push(newAclModRole);
        },

    },
    computed: {
        ...mapGetters({
            isAdmin: 'identity/isAdmin',
        }),         
        identity() {
            return this.$store.state.identity.data;
        },
        acl_roles() {
            return this.availableRoles === undefined ? this.createdAclRoles : this.availableRoles.concat(this.createdAclRoles);
        },
        availableRoles() {
            var db_roles = this.$store.state.acl_roles === undefined ? [] : this.$store.state.acl_roles.data;
            if(this.identity === undefined) {
                return db_roles;
            }
            return this.identity.roles.concat(db_roles);
        }
    },
    watch: {
        dialog() {
            if(this.dialog) {
                this.selectedRoles = this.identity === undefined ? [] : (this.identity.roles.length == 0 ? [] : this.identity.roles[0]);
                this.selectedRolesMod = this.identity === undefined ? [] : (this.identity.roles.length == 0 ? [] : this.identity.roles[0]);
                this.refresh();                
            }
        },
        identity() {
            if(this.selectedRoles.length == 0) {
               this.selectedRoles = this.identity === undefined ? [] : (this.identity.roles.length == 0 ? [] : this.identity.roles[0]); 
            }
            if(this.selectedRolesMod.length == 0) {
               this.selectedRolesMod = this.identity === undefined ? [] : (this.identity.roles.length == 0 ? [] : this.identity.roles[0]);
            }
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style scoped>

.meta-list {
    background-color: rgb(243, 243, 243);
    padding: 1px;
    border-color: rgb(227, 227, 227);
    border-style: solid;
    border-width: 1px;
}

</style>
