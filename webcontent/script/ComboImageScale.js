"use strict";

var ComboImageScale = Combo.extend({
	
	init: function(id) {
		this._super(id);	
	},
	
	refresh() {
		this.clear();
		this.addEntry("5cm",50);
		this.addEntry("10cm",100);
		this.addEntry("25cm",250);
		this.addEntry("50cm",500);
		this.addEntry("1m",1000);
		this.addEntry("1.5m",1500);
		this.addEntry("2m",2000);
		this.value(500);
		this.onChange();
	},
	
});