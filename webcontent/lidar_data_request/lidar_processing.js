"use strict";
document.addEventListener('DOMContentLoaded', function() {init();}, false);

var url_api_base = "../../";
var url_api_pointdb = url_api_base + "pointdb/";

var url_dbs_json = url_api_pointdb + "dbs.json";
var url_info_json = url_api_pointdb + "info.json";
var url_process = url_api_pointdb + "process";
var url_process_functions = url_api_pointdb + "process_functions";
var url_poigroups_json = url_api_base + "api/" + "poi_groups";
var url_poigroup_json = url_api_base + "api/" + "poi_group";
var url_roigroups_json = url_api_base + "api/" + "roi_groups";
var url_roigroup_json = url_api_base + "api/" + "roi_group";


function init() {
	
var app = new Vue({
	
el: '#app',

data: {
	appMessage: "init ...",
	topHover: false,
	topHoverStay: false,
	layers: [],
	layer: undefined,
	metadataMessage: "init ...",
	meta: {},
	internals: true,
	subsetMethodHover: false,
	subsetMethod: "POI",
	subsetMethodText: {POI:"Point-Group (POI)", ROI:"Region-Group (ROI)", TXT:"free text"},
	poigroupHover: false,
	poigroupHoverStay: false,
	poigroups: [],
	poigroup: undefined,
	poigroupMap: {},
	pois: [],
	pois_selected: [],
	poiEvalHover: false,
	poi_edge: 10,
	roigroupHover: false,
	roigroupHoverStay: false,
	roigroups: [],
	roigroup: undefined,
	roigroupMap: {},
	rois: [],
	rois_selected: [],
	subsetFreeText: "",
	declarationMethodHover: false,
	declarationMethod: "SELECT",
	declarationMethodText: {SELECT:"function selection", TEXT:"free text"},
	functions: [],
	functionsMap: {},
	functions_selected: [],
	declarationFreeText: "",
	data: {},
	subset_script: "",
	process_script: "",
	tableDownloadURL: "",
}, //end data

computed: {
	bound: function() {
		return {xmin: this.meta.tile_x_min, ymin: this.meta.tile_y_min, xmax: this.meta.tile_x_max + this.meta.tile_size - 1, ymax: this.meta.tile_y_max + this.meta.tile_size - 1};
	},
	rangeText: function() {
		var meta = this.meta;
		return (meta.x_range/1000).toFixed(1) + " x " + (meta.y_range/1000).toFixed(1) + " km";
	},
	poi_area: function() {
		var e = parseInt(this.poi_edge*1000);
		return (e*e)/1000000;
	},		
}, //end computed

methods: {
	updatePoigroups: function() {
		var self = this;
		var params = {pointdb: self.layer};
		axios.get(url_poigroups_json, {params: params})
		.then(function(response) {		
			self.poigroups = response.data;
			self.poigroup = self.poigroups[0].name;
			var m = {};
			for(var i in self.poigroups) {
				var p = self.poigroups[i];
				m[p.name] = p;
			}
			self.poigroupMap = m;
		})
		.catch(function(error) {
		});		
	},
	updateRoigroups: function() {
		var self = this;
		var params = {pointdb: self.layer};
		axios.get(url_roigroups_json, {params: params})
		.then(function(response) {		
			self.roigroups = response.data;
			self.roigroup = self.roigroups[0].name;
			var m = {};
			for(var i in self.roigroups) {
				var r = self.roigroups[i];
				m[r.name] = r;
			}
			self.roigroupMap = m;
		})
		.catch(function(error) {
		});		
	},	
	updatePois: function() {
		var self = this;
		var params = {name: self.poigroup};
		axios.get(url_poigroup_json, {params: params})
		.then(function(response) {		
			self.pois = response.data;
		})
		.catch(function(error) {
		});		
	},
	updateRois: function() {
		var self = this;
		var params = {name: self.roigroup};
		axios.get(url_roigroup_json, {params: params})
		.then(function(response) {		
			self.rois = response.data;
		})
		.catch(function(error) {
		});		
	},	
	updateFunctions: function() {
		var self = this;
		axios.get(url_process_functions)
		.then(function(response) {
			var f = response.data.functions;
			f.sort(function (a, b) {return a.name.localeCompare(b.name)});
			self.functions = f;
			self.functionsMap = f.reduce(function(r,v) {r[v.name]=v; return r;}, {});
		})
		.catch(function(error) {
		});
	},
	getSubsetText: function() {
		if(this.subsetMethod=="POI") {
			var subset = this.pois_selected.map(function(p){return p}).join(";");
			subset = "group="+this.poigroup+","+subset;
			subset = "square(poi(" + subset + ")," + this.poi_edge + ")";
			return subset;
		}
		if(this.subsetMethod=="ROI") {
			var subset = this.rois_selected.map(function(p){return p}).join(";");
			subset = "group="+this.roigroup+","+subset;
			subset = "roi(" + subset + ")";
			return subset;
		}
		if(this.subsetMethod=="TXT") {
			var subset = this.subsetFreeText;
			return subset;
		} 		
		return "unknown";
	},
	getScript: function() {
		if(this.declarationMethod=="SELECT") {
			return this.functions_selected.join(";");
		}
		if(this.declarationMethod=="TEXT") {
			return this.declarationFreeText;
		}
	},
	createtTableDownloadURL() {
		try {
		this.tableDownloadURL = "start";
		console.log("create blob");
		var csv_result = "name" + ',' + this.data.header + '\r\n';
		var data_rows = this.data.data;
		for (var key in data_rows) {
			if (data_rows.hasOwnProperty(key)) {
				csv_result += key + ',' + data_rows[key] + '\r\n';
			}
		}
		var blob = new Blob([csv_result], {type: 'text/csv'});
		console.log("created blob");
		console.log(blob);
		this.tableDownloadURL = URL.createObjectURL(blob);
		} catch(e) {console.log(e);}
	},
	onProcess: function() {
		var self = this;
		self.data = {};
		self.tableDownloadURL = "no_data";
		var subset = this.getSubsetText();		
		var script = this.getScript();
		this.subset_script = subset;
		this.process_script = script;
		var params = {db: self.layer, subset:subset, script: script, format: "json"};
		axios.get(url_process, {params: params})
		.then(function(response) {		
			var data = response.data;
			console.log(data);
			self.data = data;
			self.createtTableDownloadURL();
		})
		.catch(function(error) {
		});
	},
}, //end methods

mounted: function () {
	var self = this;
	self.appMessage = "query LiDAR layers...";
	axios.get(url_dbs_json)
	.then(function(response) {		
		self.layers = response.data;
		self.layer = self.layers[0];
		self.appMessage = undefined;		
	})
	.catch(function(error) {
		self.appMessage = "ERROR: "+error;
	});
	this.updateFunctions();
},

watch: {
	layer: function() {
		var self = this;
		self.updatePoigroups();
		self.updateRoigroups();
		self.metadataMessage = "query meta data of layer ...";
		var params = {db: self.layer};
		axios.get(url_info_json, {params: params})
		.then(function(response) {		
			self.meta = response.data;
			self.metadataMessage = undefined;		
		})
		.catch(function(error) {
			self.metadataMessage = "ERROR: "+error;
		});
	},
	poigroup: function() {
		this.updatePois();
	},
	roigroup: function() {
		this.updateRois();
	},
},

}); //end app
	
} //end init