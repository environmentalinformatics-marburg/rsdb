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
                <v-divider />
                <v-card-text>
                    <v-textarea label="title" auto-grow rows="1" v-model="newTitle" title="The name of the layer. If left empty defaults to the layer Idetifier-field." />
                    <div title="Keywords describing the layer." style="margin-bottom: 15px;">
                        <span style="color: grey;">subject</span>
                        <multiselect v-model="selectedTags" :options="layer_tags" multiple :taggable="true" @tag="createTag" placeholder="select or type subjects" tagPlaceholder="Press enter to create a tag"/>
                    </div>

                    <v-textarea v-model="newDescription" label="description.abstract" auto-grow rows="1" />


                    <!--<v-text-field label="Description" v-model="newDescription" title="Content description of the layer."></v-text-field>
                    <v-text-field label="Date" v-model="newAcquisition_date" title="Creation or processing date of the layer."></v-text-field>
                    <v-text-field label="Publisher" v-model="new_corresponding_contact" title="Person or group that provides the layer."></v-text-field>-->



                    <!--<v-text-field label="Source" v-model="new_Source" title="Source describtion of the layer data."></v-text-field>
                    <v-text-field label="Relation" v-model="new_Relation" title="Layers that are related to this layer."></v-text-field>
                    <v-text-field label="Coverage" v-model="new_Coverage" title="Spatial and Temporal extent of this layer."></v-text-field>
                    <v-text-field label="Creator" v-model="new_Creator" title="Person or group that created the layer."></v-text-field>
                    <v-text-field label="Contributor" v-model="new_Contributor" title="Persons or groups that contributed to this layer."></v-text-field>
                    <v-text-field label="Rights" v-model="new_Rights" title="Notice about rights on this layer."></v-text-field>
                    <v-text-field label="Audience" v-model="new_Audience" title="Target groups expected for layer usage."></v-text-field>
                    <v-text-field label="Provenance" v-model="new_Provenance" title="Layer history and changes in ownership."></v-text-field>-->

                    <template v-for="(contents, tag) in newProperties">
                        <v-textarea v-for="(content, index) in contents" :key="tag + ':' + index" :label="tag" v-model="newProperties[tag][index]" :title="tag" auto-grow rows="1" append-outer-icon="remove_circle" @click:append-outer="removeField(tag, index)" />
                    </template>
                                    
                </v-card-text>
                <v-divider />
                <v-card-actions>
                    <v-menu
                        v-model="addFieldMenuVisible"
                        bottom
                        origin="center center"
                        transition="scale-transition"
                        :close-on-content-click="false"
                    >
                        <template v-slot:activator="{ on }">
                            <v-btn v-on="on" class="indigo--text" slot="activator" title="edit">
                                <v-icon>add</v-icon> Add Field
                            </v-btn>
                        </template>

                        <v-card>
                            <v-card-title>
                                <v-select v-model="selectedPropertyTag" :items="propertyTags" label="Field" solo>
                                    <template v-slot:append-outer>
                                        <v-btn class="indigo--text" :disabled="selectedPropertyTag === undefined" @click="appendField(selectedPropertyTag)" title="Append Field to list of properties">
                                            <v-icon>add</v-icon> Add Field
                                        </v-btn>
                                    </template>
                                </v-select>
                            </v-card-title>
                        </v-card>
                    </v-menu>                    
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
            /*newAcquisition_date: "",*/
            /*new_corresponding_contact: "",*/
            setError: false,
            setErrorMessage: undefined,
            selectedTags: [],
            createdTags: [],

            /*new_Source: "",
            new_Relation: "",
            new_Coverage: "",
            new_Creator: "",
            new_Contributor: "",
            new_Rights: "",
            new_Audience: "",
            new_Provenance: "",*/

            newProperties: {},

            propertyTags: [
                'accrualMethod',
                'accrualPeriodicity',
                'accrualPolicy',
                'audience',
                'audience.educationLevel',
                'audience.mediator',
                'contributor',
                'contributor.creator',
                'coverage',
                'coverage.spatial',
                'coverage.temporal',
                'creator',
                'date',
                'date.available',
                'date.created',
                'date.dateAccepted',
                'date.dateCopyrighted',
                'date.dateSubmitted',
                'date.issued',
                'date.modified',
                'date.valid',
                'description',
                /*'description.abstract',*/
                'description.tableOfContents',
                'format',
                'format.extent',
                'format.medium',
                'identifier',
                'identifier.bibliographicCitation',
                'instructionalMethod',
                'language',
                'provenance',
                'publisher',
                'relation',
                'relation.conformsTo',
                'relation.hasFormat',
                'relation.hasPart',
                'relation.hasVersion',
                'relation.isFormatOf',
                'relation.isPartOf',
                'relation.isReferencedBy',
                'relation.isReplacedBy',
                'relation.isRequiredBy',
                'relation.isVersionOf',
                'relation.references',
                'relation.replaces',
                'relation.requires',
                'relation.source',
                'rights',
                'rights.accessRights',
                'rights.license',
                'rightsHolder',
                'source',
                /*'subject',*/
                /*'title',*/
                'title.alternative',
                'type',                
            ],

            addFieldMenuVisible: false,
            selectedPropertyTag: undefined,
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
            //this.newAcquisition_date = this.meta.acquisition_date === undefined ? '' : this.meta.acquisition_date;
            //this.new_corresponding_contact = this.meta.corresponding_contact === undefined ? '' : this.meta.corresponding_contact;
            this.selectedTags = optArray(this.meta.tags);

            /*this.new_Source = this.meta.Source === undefined ? '' : this.meta.Source;
            this.new_Relation = this.meta.Relation === undefined ? '' : this.meta.Relation;
            this.new_Coverage = this.meta.Coverage === undefined ? '' : this.meta.Coverage;
            this.new_Creator = this.meta.Creator === undefined ? '' : this.meta.Creator;
            this.new_Contributor = this.meta.Contributor === undefined ? '' : this.meta.Contributor;
            this.new_Rights = this.meta.Rights === undefined ? '' : this.meta.Rights;
            this.new_Audience = this.meta.Audience === undefined ? '' : this.meta.Audience;
            this.new_Provenance = this.meta.Provenance === undefined ? '' : this.meta.Provenance;*/

            this.newProperties = JSON.parse(JSON.stringify(this.meta.properties));

            this.$store.dispatch('layer_tags/refresh');
        },
        set() {
            var self = this;
            var url = this.$store.getters.apiUrl('rasterdb/' + self.meta.name + '/set');
            axios.post(url, {
                meta: {
                    title: self.newTitle,
                    description: self.newDescription,
                    //acquisition_date: self.newAcquisition_date,
                    //corresponding_contact: self.new_corresponding_contact,
                    tags: self.selectedTags,

                    /*Source: self.new_Source,
                    Relation: self.new_Relation,
                    Coverage: self.new_Coverage,
                    Creator: self.new_Creator,
                    Contributor: self.new_Contributor,
                    Rights: self.new_Rights,
                    Audience: self.new_Audience,
                    Provenance: self.new_Provenance,*/

                    properties: self.newProperties,
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
        appendField(selectedPropertyTag) {
            if(selectedPropertyTag === undefined || selectedPropertyTag === null) {
                return;
            }
            if(this.newProperties[selectedPropertyTag] === undefined) {
                //this.newProperties[selectedPropertyTag] = [''];
                this.$set(this.newProperties, selectedPropertyTag, ['']);  
            } else {
                this.newProperties[selectedPropertyTag].push('');
            }
            this.addFieldMenuVisible = false; 
        },
        removeField(tag, index) {
            if(this.newProperties[tag] !== undefined) {
               if(this.newProperties[tag].length === 1) {
                   this.$set(this.newProperties, tag, undefined);  
               } else {
                    this.newProperties[tag].splice(index, 1);
               }
            }
        },
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
