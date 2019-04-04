import '@babel/polyfill'
import Vue from 'vue'
import './plugins/vuetify'
import App from './App.vue'
import router from './router'
import store from './store'

Vue.config.productionTip = false

function isDropTarget(element) {
  while(element !== undefined && element !== null) {
    if(element.classList.contains('uploader-drop')) {
      return true;
    }
    element = element.parentElement;
  }
  return false;
}

window.addEventListener("dragenter", function(e) {
  if(!isDropTarget(e.target)) {
    e.preventDefault();
    e.dataTransfer.effectAllowed = "none";
    e.dataTransfer.dropEffect = "none";
  }
}, false);

window.addEventListener("dragover", function(e) {
  if(!isDropTarget(e.target)) {
    e.preventDefault();
    e.dataTransfer.effectAllowed = "none";
    e.dataTransfer.dropEffect = "none";
  }
});

window.addEventListener("drop", function(e) {
  if(!isDropTarget(e.target)) {
    e.preventDefault();
    e.dataTransfer.effectAllowed = "none";
    e.dataTransfer.dropEffect = "none";
  }
});

window.addEventListener("dragstart", function(e) {
  e.preventDefault();
  e.dataTransfer.effectAllowed = "none";
  e.dataTransfer.dropEffect = "none";
});

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
