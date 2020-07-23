import Vue from 'vue'
import Vuex from 'vuex'

import rasterdbs from './rasterdbs'
import pointclouds from './pointclouds'
import voxeldbs from './voxeldbs'
import vectordbs from './vectordbs'
import pointdbs from './pointdbs'
import identity from './identity'
import poi_groups from './poi_groups'
import roi_groups from './roi_groups'
import layer_tags from './layer_tags'
import acl_roles from './acl_roles'


Vue.use(Vuex)

const isDev = process.env.NODE_ENV !== 'production'

const modules = {
    rasterdbs,
    pointclouds,
    voxeldbs,
    vectordbs,
    pointdbs,
    identity,
    poi_groups,
    roi_groups,
    layer_tags,
    acl_roles,
}

const state = {
}

const getters = {
    apiUrl: () => (url) => isDev ? 'http://127.0.0.1:8081/' + url : '../../' + url,
}

const actions = {
    init({dispatch}) {
        dispatch('identity/init');
    },
}

const mutations = {
}

export default new Vuex.Store({
    modules,
    state,
    getters,
    actions,
    mutations,
});