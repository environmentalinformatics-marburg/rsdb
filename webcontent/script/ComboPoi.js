"use strict";

var ComboPoi = Combo.extend({
	
	init: function(id, editX, editY) {
		this._super(id);
		this.editX = editX;
		this.editY = editY;
	},
	
	refresh: function(poiGroup) {
		this.clear();
        if(poiGroup!="") {
            Helper.getJSON('/api/poi_group'+Helper.toQuery({name:poiGroup}),this.onLoad.bind(this));
            this.enable();
        } else {
            this.disable();
        }
	},
	
	onLoad: function(pois) {
		for(var i in pois) {
			this.addEntry(pois[i].name, JSON.stringify(pois[i]));
		}		
		this.onChange();		
	},
	
	onChange: function(poi) {
		if(this.combo.value!=undefined) {
			var poi = JSON.parse(this.combo.value);
			if(this.editX!==undefined) this.editX.value(poi.x);
			if(this.editY!==undefined) this.editY.value(poi.y);
		}
		//this._super(poi);
		this.onchangeFunc(poi); //TODO check
	}
	
});