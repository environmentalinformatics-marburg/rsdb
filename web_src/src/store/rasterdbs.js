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
    refresh(context) {
        var state = context.state;
        state.mode = 'load';
        axios.get('../../rasterdbs.json?vuex')
            .then(function(response) {
                state.data = response.data.rasterdbs;
                state.mode = 'ready';
            })
            .catch(function(error) {
                state.mode = 'error';
                context.commit('setMessage', error);
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