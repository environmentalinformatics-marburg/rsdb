<template>
    <span>
        <v-dialog v-model="dialog" lazy absolute width="800px">
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set Infos of <i>RasterDB</i>&nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text>
                    <ul>
                        <li>Scroll down to commit changes by <b>'save'-button.</b></li>
                        <li><b>Mouse hover</b> over edit-lines to show field definitions at tooltip.</li>
                        <li>Fields comply with <a href="https://www.dublincore.org" target="_blank">Dublin Core</a> metadata standard.</li>
                    </ul>
                    
                </v-card-text>
                <v-card-text>
                    <v-text-field label="Title" v-model="newTitle" title="The name of the layer. If left empty defaults to the layer Idetifier-field."></v-text-field>
                    <v-text-field label="Description" v-model="newDescription" title="Content description of the layer."></v-text-field>
                    <v-text-field label="Date" v-model="newAcquisition_date" title="Creation or processing date of the layer."></v-text-field>
                    <v-text-field label="Publisher" v-model="new_corresponding_contact" title="Person or group that provides the layer."></v-text-field>
                    <div title="Keywords describing the layer.">
                        <span style="color: grey;">Subject</span>
                        <multiselect v-model="selectedTags" :options="layer_tags" multiple :taggable="true" @tag="createTag" placeholder="select tags" tagPlaceholder="Press enter to create a tag"/>
                    </div>
                    <v-text-field label="Source" v-model="new_Source" title="Source describtion of the layer data."></v-text-field>
                    <v-text-field label="Relation" v-model="new_Relation" title="Layers that are related to this layer."></v-text-field>
                    <v-text-field label="Coverage" v-model="new_Coverage" title="Spatial and Temporal extent of this layer."></v-text-field>
                    <v-text-field label="Creator" v-model="new_Creator" title="Person or group that created the layer."></v-text-field>
                    <v-text-field label="Contributor" v-model="new_Contributor" title="Persons or groups that contributed to this layer."></v-text-field>
                    <v-text-field label="Rights" v-model="new_Rights" title="Notice about rights on this layer."></v-text-field>
                    <v-text-field label="Audience" v-model="new_Audience" title="Target groups expected for layer usage."></v-text-field>
                    <v-text-field label="Provenance" v-model="new_Provenance" title="Layer history and changes in ownership."></v-text-field>                                    
                
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="dialog = false">Cancel</v-btn>
                    <v-btn class="green--text darken-1" flat="flat" @click.native="set">Save</v-btn>
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
    name: 'admin-rasterdb-dialog-set-info',
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

            new_Source: "",
            new_Relation: "",
            new_Coverage: "",
            new_Creator: "",
            new_Contributor: "",
            new_Rights: "",
            new_Audience: "",
            new_Provenance: "",
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

            this.new_Source = this.meta.Source === undefined ? '' : this.meta.Source;
            this.new_Relation = this.meta.Relation === undefined ? '' : this.meta.Relation;
            this.new_Coverage = this.meta.Coverage === undefined ? '' : this.meta.Coverage;
            this.new_Creator = this.meta.Creator === undefined ? '' : this.meta.Creator;
            this.new_Contributor = this.meta.Contributor === undefined ? '' : this.meta.Contributor;
            this.new_Rights = this.meta.Rights === undefined ? '' : this.meta.Rights;
            this.new_Audience = this.meta.Audience === undefined ? '' : this.meta.Audience;
            this.new_Provenance = this.meta.Provenance === undefined ? '' : this.meta.Provenance;

            this.$store.dispatch('layer_tags/refresh');
        },
        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + '/set');
            axios.post(url, {
                meta: {
                    title: self.newTitle,
                    description: self.newDescription,
                    acquisition_date: self.newAcquisition_date,
                    corresponding_contact: self.new_corresponding_contact,
                    tags: self.selectedTags,

                    Source: self.new_Source,
                    Relation: self.new_Relation,
                    Coverage: self.new_Coverage,
                    Creator: self.new_Creator,
                    Contributor: self.new_Contributor,
                    Rights: self.new_Rights,
                    Audience: self.new_Audience,
                    Provenance: self.new_Provenance,
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
