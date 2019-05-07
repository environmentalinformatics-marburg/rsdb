import Vue from 'vue'
import Vuex from 'vuex'

import rasterdbs from './rasterdbs'
import pointclouds from './pointclouds'
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
    vectordbs,
    pointdbs,
    identity,
    poi_groups,
    roi_groups,
    layer_tags,
    acl_roles,
}

const state = {
    isDev: isDev,
}

const getters = {
    apiUrl: (state) => (url) => state.isDev ? 'http://127.0.0.1:8081/' + url : '../../' + url,
}

const actions = {
    init({commit, dispatch}) {
        dispatch('identity/init');
        commit('setDev', isDev);
    },
}

const mutations = {
    setDev(isDev) {
        state.isDev = isDev;
    },
}

export default new Vuex.Store({
    modules,
    state,
    getters,
    actions,
    mutations,
});