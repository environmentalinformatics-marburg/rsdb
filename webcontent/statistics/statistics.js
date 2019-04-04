"use strict";
window.onload = init;
var statistics;
function init() {
	statistics = new Statistics();
}

var Statistics = Class.extend({
	
	init: function() {
		this.div_statistics_2 = document.getElementById("div_statistics_2");
		this.div_statistics = document.getElementById("div_statistics");
		this.div_statistics_header = document.getElementById("div_statistics_header");
		document.getElementById("button_get_statistics").onclick = this.onClickGetStatistics.bind(this);
		this.comboLayer = new ComboLayer("select_layer");
		this.comboDbType = new ComboDbType("select_dbType");
		this.comboDbType.setOnChange(this.comboLayer.refresh.bind(this.comboLayer));
		this.comboDbType.refresh([{name:"PoiGroups", title:"Point of Interest Groups"}]);
	},	
	
	onClickGetStatistics: function() {
		this.div_statistics.innerHTML = "getting data...";
		this.div_statistics_2.innerHTML = "";
		
		switch(this.comboDbType.combo.value) {
			case "PointDB":
				this.div_statistics_header.innerHTML = "<b>PointDB</b> "+this.comboLayer.combo.value;
				Helper.getJSON('/pointdb/info'+Helper.toQuery({db:this.comboLayer.combo.value}),this.onLoadPointDbInfo.bind(this), this.onErrorDbInfo.bind(this));
				break;
			case "SpectralDB":
				this.div_statistics_header.innerHTML = "<b>SpectralDB</b> "+this.comboLayer.combo.value;
				Helper.getJSON('/spectraldb/info'+Helper.toQuery({db:this.comboLayer.combo.value}),this.onLoadSpectralDbInfo.bind(this), this.onErrorDbInfo.bind(this));
				Helper.getJSON('/spectraldb/band_info.json'+Helper.toQuery({db:this.comboLayer.combo.value}),this.onLoadSpectralDbBandInfo.bind(this), this.onErrorDbBandInfo.bind(this));
				break;
			case "PoiGroups":
				this.div_statistics_header.innerHTML = "<b>Point of Interest Group</b> "+this.comboLayer.combo.value;
				Helper.getJSON('/api/poi_group'+Helper.toQuery({name:this.comboLayer.combo.value}),this.onLoadPoiGroupInfo.bind(this), this.onErrorDbInfo.bind(this));
				break;			
			default:
				div_statistics_header.innerHTML = "(error)";
				div_statistics.innerHTML = "";
				break;
		}	
	},
	
	onLoadPoiGroupInfo: function(info) {
		var table = Helper.jsonArrayToTable(info);
		this.div_statistics.innerHTML = table;
	},

	onLoadPointDbInfo: function(info) {
		var table = Helper.jsonToTable(info);
		this.div_statistics.innerHTML = table;
	},

	onLoadSpectralDbInfo: function(info) {
		var table = Helper.jsonToTable(info);
		this.div_statistics.innerHTML = table;
	},
	
	onErrorDbInfo: function(text) {
		this.div_statistics.innerHTML = "ERROR "+text;
	},

	onLoadSpectralDbBandInfo: function(info) {
		var table = Helper.jsonArrayToTable(info);
		this.div_statistics_2.innerHTML = table;
	},
	
	onErrorDbBandInfo: function(text) {
		this.div_statistics_2.innerHTML = "ERROR "+text;
	},
	
});