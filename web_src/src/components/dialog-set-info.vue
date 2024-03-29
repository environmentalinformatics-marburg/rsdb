<template>
    <span>
        <v-dialog v-model="dialog" lazy width="800px" persistent>
            <v-btn icon class="indigo--text" slot="activator" title="edit">
                <v-icon>create</v-icon>
            </v-btn>
            <v-card>
                <v-card-title>
                    <div class="headline">Set Infos of layer &nbsp;&nbsp;&nbsp;<b>{{meta.name}}</b></div>
                </v-card-title>
                <v-card-text>
                    <span><ul>
                        <li>Scroll down to commit changes by <b>'save'-button.</b></li>
                        <li><b>Mouse hover</b> over edit-lines to show field definitions at tooltip.</li>
                    </ul></span>
                    <span style="color: #062744;"><br>{{identity?.meta_data_schema?.description}}</span>
                </v-card-text>
                <v-divider />
                <v-card-text>
                    <v-textarea label="title" auto-grow rows="1" v-model="newTitle" title="The name of the layer. If left empty defaults to the layer ID (Idetifier-field)." />
                    <div title="Keywords describing the layer." style="margin-bottom: 15px;">
                        <span style="color: grey;">subject</span>
                        <multiselect v-model="selectedTags" :options="layer_tags" multiple :taggable="true" @tag="createTag" placeholder="select or type subjects" tagPlaceholder="Press enter to create a tag"/>
                    </div>

                    <v-textarea v-model="newDescription" label="description" auto-grow rows="1" title="Layer description." />
                    <v-textarea v-model="newAcquisition_date" label="date.created" auto-grow rows="1" title="Creation or processing date of the layer." />
                    <v-textarea v-model="new_corresponding_contact" label="publisher" auto-grow rows="1" title="Person or group that provides the layer." />

                    <template v-for="(contents, tag) in newProperties">
                        <v-textarea v-for="(content, index) in contents" :key="tag + ':' + index" :label="tag" v-model="newProperties[tag][index]" :title="meta_keys_map[tag] && meta_keys_map[tag].description ? (meta_keys_map[tag].description) : (tag + ' field')" auto-grow rows="1" append-outer-icon="remove_circle" @click:append-outer="removeField(tag, index)" />
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
                                <!--<v-select v-model="selectedPropertyTag" :items="propertyTags" label="Field" solo>
                                    <template v-slot:append-outer>
                                        <v-btn class="indigo--text" :disabled="selectedPropertyTag === undefined" @click="appendField(selectedPropertyTag)" title="Append Field to list of properties">
                                            <v-icon>add</v-icon> Add Field
                                        </v-btn>
                                    </template>
                                </v-select>-->
                                <v-select v-model="selectedPropertyTag" :items="meta_keys" :item-text="x => x.name + (x.description ? ' - ' + x.description : '')" item-value="name" label="Field" solo>
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
    name: 'dialog-set-info',
    props: ['meta', 'url', 'data_name'],

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
                /*'date.created',*/
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
                /*'publisher',*/
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
            identity: state => state.identity.data,
        }),
        layer_tags() {
            return this.availableTags === undefined ? this.createdTags : this.availableTags.concat(this.createdTags);
        },
        meta_keys() {
            const identity = this.identity;
            if(identity === undefined || identity.meta_data_schema === undefined || identity.meta_data_schema.keys === undefined) {
                return [];
            }
            return identity.meta_data_schema.keys;
        },
        meta_keys_map() {
            return Object.fromEntries(this.meta_keys.map(e => [e.name, e]));
        }
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

            this.newProperties = JSON.parse(JSON.stringify(this.meta.properties));

            this.$store.dispatch('layer_tags/refresh');
        },
        async set() {
            try {
                let props = {
                    title: this.newTitle,
                    description: this.newDescription,
                    acquisition_date: this.newAcquisition_date,
                    corresponding_contact: this.new_corresponding_contact,
                    tags: this.selectedTags,
                    properties: this.newProperties,
                };
                let data = this.data_name === undefined ? props : { [this.data_name]: props };                
                let response = await axios.post(this.url, data);
                console.log(response);
                this.$emit('changed');
                this.dialog = false;
            } catch(error) {
                this.setError = true;
                this.setErrorMessage = "Error setting property";
                console.log(error);
                this.$emit('changed');
                this.dialog = false;
            }
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