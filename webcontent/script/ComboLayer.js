"use strict";

var ComboLayer = Combo.extend({
	
	init: function(id) {
		this._super(id);
	},
	
	refresh: function(type) {
		this.clear();
		switch(type) {
			case "PointDB":
				Helper.getJSON('/pointdb/dbs.json',this.onLoad.bind(this));
				break;
			case "SpectralDB":
				Helper.getJSON('/spectraldb/dbs.json',this.onLoad.bind(this));
				break;
			case "PoiGroups":
				Helper.getJSON('/api/poi_groups',this.onLoadPointGroups.bind(this));
				break;	
			default:
				this.addEntry("[error]");
				break;
		}		
	},
	
	onLoad: function(db_names) {
		for(var i in db_names) {
			this.addEntry(db_names[i]);
		}		
		this.onChange();		
	},
	
	onLoadPointGroups: function(db_names) {
		for(var i in db_names) {
			this.addEntry(db_names[i].title, db_names[i].name);
		}		
		this.onChange();		
	},
	
});