"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);

function init() {
	var url_api_base = "../../";
	var url_api_pointdb = url_api_base + "pointdb/";
	var url_process_functions = url_api_pointdb + "process_functions";
	var url_dbs_json = url_api_pointdb + "dbs.json?structured";
	var url_poigroups_json = url_api_base + "api/" + "poi_groups";
	var url_roigroups_json = url_api_base + "api/" + "roi_groups";
	var url_poigroup_json = url_api_base + "api/" + "poi_group";
	var url_roigroup_json = url_api_base + "api/" + "roi_group";
	var url_process = url_api_pointdb + "process";
	var url_pointclouds = url_api_base + "pointclouds";

	var app = new Vue({

		el: '#app',

		data: {
			functionsMessage: "init functions...",
			functions: [],
			functionsMap: {},
			tags: [],
			tag: "-",
			functionsFilterText: undefined,

			pointdbsMessage: undefined,
			pointdbs: [],
			pointclouds: [],

			layerIndex: undefined,

			pointcloudMeta: undefined,

			subsetMethods: ["POI", "ROI"],
			subsetMethodTexts: ["Point Surroundings", "Polygon"],
			subsetMethod: "POI",
			showAllGroups: false,

			poiGroupsMessage: "init POI groups...",
			poiGroups: [],
			poiGroup: undefined,
			poiGroupMap: {},

			roiGroupsMessage: "init ROI groups...",
			roiGroups: [],
			roiGroup: undefined,
			roiGroupMap: {},

			poisMessage: "init POIs...",
			pois: [],
			poisFilterText: undefined,
			poiDiameter: 10,

			roisMessage: "init ROIs...",
			rois: [],
			roisFilterText: undefined,

			processMessage: "",
			processBusy: false,
			processCancelToken: undefined,
			processData: {},
			processDownload: undefined,

		},

		methods: {
			updateFunctions: function () {
				var self = this;
				self.functionsMessage = "query functions...";
				axios.get(url_process_functions)
					.then(function (response) {
						var f = response.data.functions;
						var t = response.data.tags;
						f.sort(function (a, b) { return a.name.localeCompare(b.name) });
						self.functionsMap = f.reduce(function (r, v) { v.selected = false; r[v.name] = v; return r; }, {});
						self.functions = f;
						self.tags = t;
						self.functionsMessage = undefined;
					})
					.catch(function (error) {
						self.functionsMessage = "ERROR could not query functions: " + error;
					});
			},
			updatePointdbs: function () {
				var self = this;
				self.pointdbsMessage = "query PointDB layers...";
				axios.get(url_dbs_json)
					.then(function (response) {
						self.pointdbs = response.data.pointdbs;
						self.pointdbsMessage = undefined;
					})
					.catch(function (error) {
						self.pointdbsMessage = "ERROR could not query PointDB layers: " + error;
					});
			},
			updatePointclouds: function() {
				var self = this;
				self.pointcloudsMessage = "query pointcloud layers...";
				axios.get(url_pointclouds)
					.then(function (response) {
						self.pointclouds = response.data.pointclouds;
						self.pointcloudsMessage = undefined;
					})
					.catch(function (error) {
						self.pointcloudsMessage = "ERROR could not query pointcloud layers: " + error;
					});				
			},
			log: function (e) {
				console.log(e);
			},
			onFunctionClick: function (f) {
				f.selected = !f.selected;
			},
			onFunctionsAllClick: function () {
				this.functionsFiltered.forEach(function (f) { f.selected = true; });
			},
			onFunctionsClearClick: function () {
				this.functions.forEach(function (f) { f.selected = false; });
			},
			updatePoiGroupsPointdb: function () {
				var self = this;
				self.poiGroupsMessage = "query POI groups...";
				axios.get(url_poigroups_json, { params: { pointdb: self.layer.name } })
					.then(function (response) {
						self.poiGroups = response.data;						
					})
					.catch(function (error) {
						self.poiGroupsMessage = "ERROR could not query POI groups: " + error;
					});
			},
			updatePoiGroupsAll: function () {
				var self = this;
				self.poiGroupsMessage = "query POI groups...";
				axios.get(url_poigroups_json)
					.then(function (response) {
						self.poiGroups = response.data;						
					})
					.catch(function (error) {
						self.poiGroupsMessage = "ERROR could not query POI groups: " + error;
					});
			},			
			updateRoiGroupsPointdb: function () {
				var self = this;
				self.roiGroupsMessage = "query ROI groups...";
				axios.get(url_roigroups_json, { params: { pointdb: self.layer.name } })
					.then(function (response) {
						self.roiGroups = response.data;						
					})
					.catch(function (error) {
						self.roiGroupsMessage = "ERROR could not query ROI groups: " + error;
					});
			},
			updateRoiGroupsAll: function () {
				var self = this;
				self.roiGroupsMessage = "query ROI groups...";
				axios.get(url_roigroups_json)
					.then(function (response) {
						self.roiGroups = response.data;						
					})
					.catch(function (error) {
						self.roiGroupsMessage = "ERROR could not query ROI groups: " + error;
					});
			},
			updatePointcloudMeta: function() {
				var self = this;
				self.pointcloudMeta = undefined;
				self.poiGroupsMessage = "query POI groups...";
				self.roiGroupsMessage = "query ROI groups...";
				axios.get(url_pointclouds + '/' + self.layer.name + "?poi_groups&roi_groups")
					.then(function (response) {
						console.log(response.data);
						self.pointcloudMeta = response.data.pointcloud;
						self.poiGroups = self.pointcloudMeta.poi_groups;
						self.roiGroups = self.pointcloudMeta.roi_groups;
					})
					.catch(function (error) {
						self.poiGroupsMessage = "ERROR could not query POI groups: " + error;
						self.roiGroupsMessage = "ERROR could not query ROI groups: " + error;
					});	
			},
			updatePOIs: function () {
				var self = this;
				self.poisMessage = "query POIs...";
				axios.get(url_poigroup_json, { params: { name: self.poiGroup } })
					.then(function (response) {
						var pois = response.data;
						pois.forEach(function (p) { p.selected = false; });
						self.pois = pois;
						if (self.pois.length == 0) {
							self.poisMessage = "note: no POIs in POI group";
						} else {
							self.poisMessage = undefined;
						}
					})
					.catch(function (error) {
						self.poisMessage = "ERROR could not query POIs: " + error;
					});
			},
			onPoiClick: function (f) {
				f.selected = !f.selected;
			},
			onPoisAllClick: function () {
				this.poisFiltered.forEach(function (f) { f.selected = true; });
			},
			onPoisClearClick: function () {
				this.pois.forEach(function (f) { f.selected = false; });
			},
			updateROIs: function () {
				var self = this;
				self.roisMessage = "query ROIs...";
				axios.get(url_roigroup_json, { params: { name: self.roiGroup } })
					.then(function (response) {
						var rois = response.data;
						rois.forEach(function (p) { p.selected = false; });
						self.rois = rois;
						if (self.rois.length == 0) {
							self.roisMessage = "note: no ROIs in ROI group";
						} else {
							self.roisMessage = undefined;
						}
					})
					.catch(function (error) {
						self.roisMessage = "ERROR could not query ROIs: " + error;
					});
			},
			onRoiClick: function (f) {
				f.selected = !f.selected;
			},
			onRoisAllClick: function () {
				this.roisFiltered.forEach(function (f) { f.selected = true; });
			},
			onRoisClearClick: function () {
				this.rois.forEach(function (f) { f.selected = false; });
			},
			isProcessReady: function () {
				return this.functionsMessage == undefined && this.functionsSelectedCount > 0
					&& this.layersMessage == undefined && this.layer !== undefined
					&& ((this.subsetMethod == "POI" && this.isValidDiameter(this.poiDiameter) && this.poiGroupsMessage == undefined && this.poisMessage == undefined && this.poisSelectedCount > 0)
						|| (this.subsetMethod == "ROI" && this.roiGroupsMessage == undefined && this.roisMessage == undefined && this.roisSelectedCount > 0));
			},
			getSubset: function () {
				if (this.subsetMethod == "POI") {
					var poisText = this.pois.filter(function (f) { return f.selected; }).map(function (f) { return f.name }).join(';');
					return 'square(poi(group=' + this.poiGroup + ',' + poisText + '),' + this.poiDiameter + ')';
				}
				if (this.subsetMethod == "ROI") {
					var roisText = this.rois.filter(function (f) { return f.selected; }).map(function (f) { return f.name }).join(';');
					return 'roi(group=' + this.roiGroup + ',' + roisText + ')';
				}
				return undefined;
			},
			onProcess: function () {
				var self = this;
				self.processMessage = "processing...";
				self.processBusy = true;
				self.processDownload = undefined;
				self.onProcessCancel();
				self.processCancelToken = axios.CancelToken.source();
				var subset = this.getSubset();
				var functions = this.functions.filter(function (f) { return f.selected; }).map(function (f) { return f.name });
				var body = {areas: [{script: subset}], functions: functions};
				var params = undefined;
				var url = undefined;								
				switch(self.layer.type) {
					case "PointDB":
					var url = url_process;
					var params = { db: self.layer.name, format: "json" };
					break;
					case "pointcloud":
					var url = url_pointclouds + '/' + self.layer.name + '/indices.json';
					body.omit_empty_areas = true;
					var params = { };
					break;
					default:
					self.processMessage = "ERROR processing: unknown type " + self.layer.type;
					return;
				}
				axios.post(url, body, { params: params, cancelToken: self.processCancelToken.token })
					.then(function (response) {
						self.processData = response.data;
						console.log(self.processData);
						self.processBusy = false;
						self.processMessage = undefined;
						self.createProcessDownload();
					})
					.catch(function (error) {
						console.log(error);
						self.processBusy = false;
						self.processMessage = "ERROR processing: " + error;
						if(error.response !== undefined && error.response.data !== undefined) {
							self.processMessage += " --  " + error.response.data;
						}
					});
			},
			/*onProcess: function () {
				var self = this;
				self.processMessage = "processing...";
				self.processBusy = true;
				self.processDownload = undefined;
				var subset = this.getSubset();
				var script = this.getScript();
				console.log(script);
				var params = { db: self.layer, subset: subset, script: script, format: "json" };
				self.onProcessCancel();
				self.processCancelToken = axios.CancelToken.source();
				axios.get(url_process, { params: params, cancelToken: self.processCancelToken.token })
					.then(function (response) {
						self.processData = response.data;
						console.log(self.processData);
						self.processBusy = false;
						self.processMessage = undefined;
						self.createProcessDownload();
					})
					.catch(function (error) {
						self.processBusy = false;
						self.processMessage = "ERROR processing: " + error;
					});
			},*/
			onProcessCancel: function () {
				if (this.processCancelToken != undefined) {
					this.processCancelToken.cancel();
				}
			},
			createProcessDownload: function () {
				try {
					console.log("start blob");
					this.processDownload = undefined;
					var csv_result = "name" + ',' + this.processData.header + '\r\n';
					var data_rows = this.processData.data;
					for (var key in data_rows) {
						if (data_rows.hasOwnProperty(key)) {
							csv_result += key + ',' + data_rows[key] + '\r\n';
						}
					}
					var blob = new Blob([csv_result], { type: 'text/csv' });
					console.log("created blob");
					console.log(blob);
					this.processDownload = URL.createObjectURL(blob);
				} catch (e) {
					console.log('ERROR');
					console.log(e);
				}
			},
			onProcessCopy: function () {
				var s = 'name\t';
				s += this.processData.header.join('\t');
				var data = this.processData.data;
				for (var d in data) {
					s += '\n' + d + '\t' + data[d].join('\t');
				}

				console.log(this.processData.header);
				console.log(this.processData.data);
				console.log(s);
				copyTextToClipboard(s);
			},
			toFilterFun: function (text) {
				var filter = text;
				if (filter == undefined) {
					return function (f) { return true; };
				}
				filter = filter.trim().toLowerCase();
				if (filter == '') {
					return function (f) { return true; };
				}
				var filters = filter.split(/\s+/);
				var len = filters.length;
				console.log(filters);
				if (len == 1) {
					return function (f) { return f.name.toLowerCase().indexOf(filter) >= 0; };
				}
				return function (f) {
					for (var i = 0; i < len; i++) {
						if (f.name.toLowerCase().indexOf(filters[i]) >= 0) {
							return true;
						}
					}
					return false;
				};
			},
			//derived from http://stackoverflow.com/questions/1303646/check-whether-variable-is-number-or-string-in-javascript
			isNumber: function (o) {
				return !isNaN(o - 0) && o !== null && o !== "" && o !== false;
			},
			isValidDiameter: function (x) {
				return this.isNumber(x) && 0 < x && x <= 1000;
			},
			updateGroups: function() {
				var self = this;
				if(this.showAllGroups) {
					this.updatePoiGroupsAll();
					this.updateRoiGroupsAll();
				} else {
					switch(self.layer.type) {
						case "PointDB":
						this.updatePoiGroupsPointdb();
						this.updateRoiGroupsPointdb();
						break;
						case "pointcloud":
						console.log("pointcloud!!!!!");
						this.updatePointcloudMeta();
						break;
						default:
						console.log("error");
					}
				}				
			},
		},

		computed: {
			functionsSelectedCount: function () {
				var cnt = 0;
				this.functions.forEach(function (f) { if (f.selected) { cnt++; } });
				return cnt;
			},
			poisSelectedCount: function () {
				var cnt = 0;
				this.pois.forEach(function (f) { if (f.selected) { cnt++; } });
				return cnt;
			},
			roisSelectedCount: function () {
				var cnt = 0;
				this.rois.forEach(function (f) { if (f.selected) { cnt++; } });
				return cnt;
			},
			functionsFiltered: function () {				
				var f = this.functions;
				var tag = this.tag;
				if(tag !== '-') {
					f = f.filter(function(fun) {
						var tags = fun.tags;
						if(tags === undefined) {
							return false;
						}
						if(!Array.isArray(tags)) {
							return tags === tag;
						}
						return tags.indexOf(tag) >= 0;
					});
				}
				return f.filter(this.toFilterFun(this.functionsFilterText));
			},
			poisFiltered: function () {
				return this.pois.filter(this.toFilterFun(this.poisFilterText));
			},
			roisFiltered: function () {
				return this.rois.filter(this.toFilterFun(this.roisFilterText));
			},
			layers: function() {
				var col = [];
				var a = this.pointdbs.map(function(p) {return {name: p.name, type: "PointDB", description: p.description};});
				var b = this.pointclouds.map(function(p) {return {name: p.name, type: "pointcloud", description: p.title};});
				col = col.concat(a);
				col = col.concat(b);
				return col;
			},
			layer: function() {
				return this.layerIndex === undefined ? undefined : this.layers[this.layerIndex];
			},
			layersMessage: function() {
				return this.pointdbsMessage;
			},
		},

		mounted: function () {
			this.updateFunctions();
			this.updatePointdbs();
			this.updatePointclouds();
		},

		watch: {
			layer: function () {
				this.updateGroups();
			},
			showAllGroups: function() {
				this.updateGroups();				
			},
			poiGroup: function (q) {
				this.updatePOIs();
			},
			roiGroup: function (q) {
				this.updateROIs();
			},
			layers: function() {
				this.layerIndex = this.layers.length > 0 ? 0 : undefined;
			},
			poiGroups: function() {
				var self = this;
				if (self.poiGroups.length == 0) {
					self.poiGroupsMessage = "note: no POI groups in layer";
					self.poiGroup = undefined;
					self.poiGroupMap = {};
				} else {
					self.poiGroup = self.poiGroups[0].name;
					var m = {};
					for (var i in self.poiGroups) {
						var p = self.poiGroups[i];
						m[p.name] = p;
					}
					self.poiGroupMap = m;
					self.poiGroupsMessage = undefined;
				}
			},
			roiGroups: function() {
				var self = this;
				if (self.roiGroups.length == 0) {
					self.roiGroupsMessage = "note: no ROI groups in layer";
					self.roiGroup = undefined;
					self.roiGroupMap = {};
				} else {
					self.roiGroup = self.roiGroups[0].name;
					var m = {};
					for (var i in self.roiGroups) {
						var p = self.roiGroups[i];
						m[p.name] = p;
					}
					self.roiGroupMap = m;
					self.roiGroupsMessage = undefined;
				}
			},
		},

	});

}