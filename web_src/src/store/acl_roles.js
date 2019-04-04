import axios from 'axios'

const state = {
    data: undefined,
    mode: 'init',
    message: '',
    messageActive: false,
}

const getters = {
}

const mutations = {
    setMessage(state, message) {
        state.message = message;
        state.messageActive = true;
    },
    closeMessage(state) {
        state.messageActive = false;
    },
    setData(state, payload) {
        state.data = payload;
        state.mode = 'ready';
    },
    setError(state, payload) {
        state.message = payload;
        state.messageActive = true;
        state.mode = 'error';
    },
}

const actions = {
    init({state, dispatch}) {
        var mode = state.mode;
        if(mode === 'init' || mode === 'error') {
            dispatch('refresh');
        }
    },
    refresh({state, commit}) {
        state.mode = 'load';
        axios.get('../../api/roles')
            .then(function(response) {
                commit('setData', response.data.roles);
            })
            .catch(function(error) {
                commit('setError', interpretError(error));
            });
        },
}

function interpretError(error) {
    if (error.response) {
        return error.response.data ? error.response.data : error.response;       
      } else if (error.request) {
        return "network error";
      } else {
        return error.message ? error.message : error;
      }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
}