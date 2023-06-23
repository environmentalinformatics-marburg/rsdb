<template>

<table style="padding-right: 420px;">
    <tr v-if="meta.description.length !== 0">
        <td><b>description:</b></td>
        <td><span v-if="meta.description.length === 0" style="color: grey;">(none)</span><span v-else>{{meta.description}}</span></td>                        
    </tr>
    <tr v-if="meta.acquisition_date !== undefined">
        <td><b>date.created:</b></td>
        <td><span v-if="meta.acquisition_date === undefined" style="color: grey;">(none)</span><span v-else>{{meta.acquisition_date}}</span></td>                        
    </tr>
    <tr v-if="meta.corresponding_contact !== undefined">
        <td><b>publisher:</b></td>
        <td><span v-if="meta.corresponding_contact === undefined" style="color: grey;">(none)</span><span v-else>{{meta.corresponding_contact}}</span></td>                        
    </tr>
    <tr v-if="optArray(meta.tags).length > 0">
        <td><b>subject:</b></td>
        <td>                      
            <span v-for="tag in optArray(meta.tags)" :key="tag"><span class="meta-list">{{tag}}</span>&nbsp;&nbsp;&nbsp;</span>
            <span v-if="optArray(meta.tags).length === 0" style="color: grey;">(none)</span>
        </td>
    </tr>

    <template v-for="(contents, tag) in meta.properties">
        <tr v-for="(content, index) in contents" :key="tag + ':' + index">
            <td><b>{{tag}}:</b></td>
            <td><span>{{content}}</span></td>  
        </tr>
    </template>
</table> 

</template>

<script>

export default {
props: ['meta'],

components: {
},

data() {
    return {};
},

computed: {
},

methods: {
    optArray(a) {
        if(a === undefined) {
            return [];
        }
        if(Array.isArray(a)) {
            return a;
        }
        return [a];
    },        
},

watch: {
},

mounted() {
},
}

</script>

<style scoped>

.meta-list {
    background-color: rgb(232, 232, 232);
    padding: 1px;
    margin-right: 5px;
    border-color: rgba(0, 0, 0, 0.062);
    border-style: solid;
    border-width: 1px;
}

</style>