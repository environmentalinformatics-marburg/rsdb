import axios from 'axios'

const state = {
    data: undefined,
    mode: 'init',
    message: '',
    messageActive: false,
}

const getters = {
}

const actions = {
    init({state, dispatch}) {
        var mode = state.mode;
        if(mode === 'init' || mode === 'error') {
            dispatch('refresh');
        }
    },
    refresh({state, commit, rootState}, refresh) {
        state.mode = 'load';
        const url = refresh ? '../../postgis/layers?refresh' : '../../postgis/layers';
        axios.get(rootState.identity.urlPrefix + url)
            .then(function(response) {
                state.data = response.data.postgis_layers;
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