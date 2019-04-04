"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);
Vue.config.productionTip = false;

function init() {
	var url_api_base = "../../";
	var url_api_pointdb = url_api_base + "pointdb/";
	var url_process_functions = url_api_pointdb + "process_functions";


	var app = new Vue({

		el: '#app',

		data: {

			appMessage: "init...",
			functions: [],
			descriptions: {},

		},

		methods: {

			refreshFunctions: function () {
				var self = this;
				self.appMessage = "query functions...";
				axios.get(url_process_functions)
					.then(function (response) {
						var f = response.data.functions;
						f.sort(function (a, b) { return a.name.localeCompare(b.name) });
						self.functions = f;
						self.appMessage = undefined;
						self.refreshDetails();
					})
					.catch(function (error) {
						self.appMessage = "ERROR could not query functions: " + error;
					});
			},

			refreshDetails: function () {
				var self = this;
				self.appMessage = "query details...";
				axios.get('details.yaml', { responseType: 'text' })
					.then(function (response) {
						var text = response.data;
						var yaml = jsyaml.safeLoad(text);
						self.descriptions = yaml;
						if (self.descriptions.valid) {
							self.appMessage = undefined;
						} else {
							self.appMessage = "ERROR could not load details";
						}
					})
					.catch(function (error) {
						self.appMessage = "ERROR could not query details: " + error;
					});
			},
		},

		computed: {

		},

		mounted: function () {
			this.refreshFunctions();
		},

		watch: {

		},

	});

}