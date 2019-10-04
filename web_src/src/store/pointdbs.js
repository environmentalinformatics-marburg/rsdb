import axios from 'axios'

const state = {
    data: undefined,
    mode: 'init',
    message: '',
    messageActive: false,
}

const getters = {
    names(state) {
        return state.data === undefined ? [] : state.data.map(item => item.name);
    }
}

const actions = {
    init({state, dispatch}) {
        var mode = state.mode;
        if(mode === 'init' || mode === 'error') {
            dispatch('refresh');
        }
    },
    refresh({state, commit, rootState}) {
        state.mode = 'load';
        axios.get(rootState.identity.urlPrefix + '../../pointdbs?vuex')
            .then(function(response) {
                state.data = response.data.pointdbs;
                state.mode = 'ready';
            })
            .catch(function(error) {
                state.mode = 'error';
                commit('setMessage', error);
            });
    },
}

const mutations = {
    setMessage(state, message) {
        state.message = message;
        state.messageActive = true;
    },
    closeMessage(state) {
        state.messageActive = false;
    },
}

export default {
  namespaced: true,
  state,
  getters,
  actions,
  mutations
}