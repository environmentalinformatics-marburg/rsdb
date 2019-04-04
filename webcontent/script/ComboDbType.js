"use strict";

var ComboDbType = Combo.extend({
	
	init: function(id) {
		this._super(id);
	},
	
	refresh: function(array) {
		this.clear();
		this.addEntry("LiDAR point cloud","PointDB");
		this.addEntry("(hyperspectral) surface","SpectralDB");
		if(array !== undefined) {
			for(var i in array) {
				this.addEntry(array[i].title,array[i].name);
			}
		}
		this.onChange();
	},
	
});