"use strict";

var ComboImageType = Combo.extend({
	
	init: function(id) {
		this._super(id);	
	},
	
	refresh() {
		this.clear();
		this.addEntry("intensity","intensity");
		this.addEntry("elevation","z");
		this.addEntry("intensity+elevation","intensity_z");
		this.value("intensity_z");
	},
	
});