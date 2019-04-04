"use strict";

var ComboPointdbFeature = Combo.extend({
	
	init: function(id) {
		this._super(id);
	},
	
	refresh: function() {
		this.clear();
		Helper.getJSON('/pointdb/features',this.onLoad.bind(this));		
	},
	
	onLoad: function(json) {
		var features = json.features;
		for(var i in features) {
			var name = features[i].name;
			this.addEntry(name);
		}		
		this.onChange();		
	},
	
});