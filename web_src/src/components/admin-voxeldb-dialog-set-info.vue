<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set Infos of <i>VoxelDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                 <v-card-text>
                    <v-text-field label="title" v-model="newTitle"></v-text-field>
                </v-card-text>
                <v-card-text>
                    <v-text-field label="description" v-model="newDescription"></v-text-field>
                </v-card-text>
                <v-card-text>
                    <v-text-field label="acquisition date" v-model="newAcquisition_date"></v-text-field>
                </v-card-text>
                <v-card-text>
                    <v-text-field label="corresponding contact" v-model="new_corresponding_contact"></v-text-field>
                </v-card-text>
                <v-card-text>                    
                    Tags
                   <multiselect v-model="selectedTags" :options="layer_tags" multiple :taggable="true" @tag="createTag" placeholder="select tags" tagPlaceholder="Press enter to create a tag"/>
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

function optArray(a) {
    if(a === undefined) {
        return [];
    }
    if(Array.isArray(a)) {
        return a;
    }
    return [a];
}

export default {
    name: 'admin-voxeldb-dialog-set-description',
    props: ['meta'],

    components: {
        Multiselect,
    },

    data() {
        return {
            dialog: false,
            newTitle: "",
            newDescription: "",
            newAcquisition_date: "",
            new_corresponding_contact: "",
            setError: false,
            setErrorMessage: undefined,
            selectedTags: [],
            createdTags: [],            
        }
    },
    computed: {
        ...mapState({
            availableTags: state => state.layer_tags.data,
        }),
        layer_tags() {
            return this.availableTags === undefined ? this.createdTags : this.availableTags.concat(this.createdTags);
        },
    }, 
    methods: {
        createTag(newTag) {
            this.createdTags.push(newTag);
            this.selectedTags.push(newTag);
        }, 
        
        refresh() {
            this.newTitle = this.meta.title;
            this.newDescription = this.meta.description;
            this.newAcquisition_date = this.meta.acquisition_date === undefined ? '' : this.meta.acquisition_date;
            this.new_corresponding_contact = this.meta.corresponding_contact === undefined ? '' : this.meta.corresponding_contact;
            this.selectedTags = optArray(this.meta.tags);
            this.$store.dispatch('layer_tags/refresh');
        },

        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('voxeldbs/' + self.meta.name);
            axios.post(url, {
                voxeldb: {
                    title: self.newTitle,
                    description: self.newDescription,
                    acquisition_date: self.newAcquisition_date,
                    corresponding_contact: self.new_corresponding_contact,
                    tags: self.selectedTags,
                }
            }).then(function(response) {
                console.log(response);
                self.$emit('changed');
                self.dialog = false;
            }).catch(function(error) {
                 self.setError = true;
                self.setErrorMessage = "Error setting property";
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
