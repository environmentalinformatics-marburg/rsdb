"use strict";

var ComboQueryType = Combo.extend({
	
	init: function(id) {
		this._super(id);
	},
	
	refresh: function(dbType) {
		this.clear();
		switch(dbType) {
			case "PointDB":
				this.addEntry("Feature", "feature");
				this.addEntry("Image", "image");
			break;
			case "SpectralDB":
				//this.addEntry("Feature", "feature");
				this.addEntry("Image", "image");
			break;
			default:
				console.log("error "+type);
		}
		this.onChange();
	},
	
});