"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);
Vue.config.productionTip = false;

function init() {

    var app = new Vue({

        el: '#app',

        data: {
            loadingMessage: "init...",
            identity: {},
        },

        mounted: function () {
            var self = this;
            self.loadingMessage = "loading...";
            axios.get("../../api/identity")
                .then(function (response) {
                    self.loadingMessage = undefined;
                    self.identity = response.data;
                })
                .catch(function (error) {
                    self.loadingMessage = "ERROR " + error;
                });
        },

        computed: {
            plain_url: function () {
                return this.loadingMessage !== undefined ? this.loadingMessage : this.identity.plain_wms_url;
            },
            secure_url: function () {
                return this.loadingMessage !== undefined ? this.loadingMessage : this.identity.secure_wms_url;
            },
        }
    });

}