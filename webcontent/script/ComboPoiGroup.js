"use strict";

var ComboPoiGroup = Combo.extend({
	
	/*
	withNone: option boolean if one entry should be added
	*/
	init: function(id, withNone) {
		this._super(id);
		this.withNone = withNone?true:false; //conversion to boolean 
	},
	
	refresh: function(parameters) {
		this.clear();
		Helper.getJSON('/api/poi_groups'+Helper.toQuery(parameters),this.onLoad.bind(this));		
	},
	
	onLoad: function(groups) {
		if(this.withNone) {
			this.addEntry("(none)", "none");
		}
        if(groups.length>0) {
            for(var i in groups) {
                var group = groups[i];
                this.addEntry(group.title, group.name);
            }
			this.value(groups[0].name);
            this.enable();
        } else {
            this.disable();
        }
		this.onChange();		
	},
	
});