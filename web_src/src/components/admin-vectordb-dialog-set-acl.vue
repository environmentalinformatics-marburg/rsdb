<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set access control of <i>VectorDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-divider></v-divider>
                <v-card-text>
                    Specified roles can <b>read</b> layer data.
                    <multiselect v-model="selectedRoles" :options="acl_roles" multiple :taggable="true" @tag="createAclRole" placeholder="select roles" tagPlaceholder="Press enter to create a role"/>
                </v-card-text>
                <v-card-text>
                    Specified roles can <b>modify</b> layer data.
                    <multiselect v-model="selectedRolesMod" :options="acl_roles" multiple :taggable="true" @tag="createAclModRole" placeholder="select roles" tagPlaceholder="Press enter to create a role"/>
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="set">Set</v-btn>
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

import { mapState } from 'vuex'
import axios from 'axios'

import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.min.css'

export default {
    name: 'admin-vectordb-dialog-set-acl',
    props: ['meta'],

    components: {
        Multiselect,
    },

    data() {
        return {
            dialog: false,
            setError: false,
            setErrorMessage: undefined,
            selectedRoles: [],
            selectedRolesMod: [],
            availableRoles: [],
            createdAclRoles: [],
        }
    },

    computed: {
        ...mapState({
            urlPrefix: state => state.identity.urlPrefix,
        }),
        acl_roles() {
            return this.availableRoles.concat(this.createdAclRoles);
        },
    },

    methods: {

        createAclRole(newAclRole) {
            this.createdAclRoles.push(newAclRole);
            this.selectedRoles.push(newAclRole);
        },

        createAclModRole(newAclModRole) {
            this.createdAclRoles.push(newAclModRole);
            this.selectedRolesMod.push(newAclModRole);
        },
        
        refresh() {
            var self = this;
            this.selectedRoles = this.meta.acl;
            this.selectedRolesMod = this.meta.acl_mod;

            axios.get(this.urlPrefix + '../../api/roles')
                .then(function(response) {
                    self.availableRoles = response.data.roles;
                    //console.log(self.availableRoles);
                })
                .catch(function(error) {
                    console.log(error);
                });
        },

        set() {
            var self = this;
            var url = this.urlPrefix + '../../vectordbs/' + self.meta.name;
            axios.post(url, {
                acl: self.selectedRoles,
                acl_mod: self.selectedRolesMod,
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
                self.dialog = false;
            }).catch(function(error) {
                console.log(error);
                console.log(self.errorToText(error));
                self.setError = true;
                self.setErrorMessage = "Error setting property: " + self.errorToText(error);
                console.log(error);
                self.$emit('changed');
                self.dialog = false;
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
            return error.message + " - " + error.response.data;
        }

    },
    watch: {
        meta() {
            this.refresh();
        },
        dialog() {
            if(this.dialog) {
                this.refresh();
            }
        }
    },
    mounted() {
        this.refresh();
    },
}

</script>

<style>

</style>
