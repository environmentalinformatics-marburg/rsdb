import axios from 'axios'

const isDev = process.env.NODE_ENV !== 'production'

const state = {
    data: undefined,
    mode: 'init',
    message: '',
    messageActive: false,
    transactionCount: 0,
    transactionRunCount: 0,
    urlPrefix: isDev ? 'http://127.0.0.1:8081/' : '',
}

const getters = {
    isAdmin(state) {
        if(state.data === undefined) {
            return false;
        }
        if(state.data.roles === undefined) {
            return false;
        }
        return state.data.roles.includes('admin');
    }
}

const mutations = {    
    closeMessage(state) {
        state.messageActive = false;
    },
    incrementTransactionCount(state) {
        state.transactionCount++;
    },
    incrementTransactionRunCount(state) {
        state.transactionRunCount++;
    },
    decrementTransactionRunCount(state) {
        state.transactionRunCount--;
    },
    setUrlPrefix(state, payload) {
        state.urlPrefix = payload;
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
        /*if(window.webpackHotUpdate) {
            commit('setUrlPrefix', 'http://127.0.0.1:8081/');
        } else {
            commit('setUrlPrefix', '');
        }*/
    },
    refresh({state, commit, rootState}) {
        state.mode = 'load';
        axios.get(rootState.identity.urlPrefix + '../../api/identity')
            .then(function(response) {
                commit('setData', response.data);
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