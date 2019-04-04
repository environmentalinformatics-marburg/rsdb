"use strict";

var ComboScale = Combo.extend({
	
	init: function(id) {
		this._super(id);	
	},
	
	refresh(info) {
		this.clear();
		this.addEntry(info.tile_size+"m",info.tile_size);
		this.addEntry(info.tile_size*2+"m",info.tile_size*2);
		this.addEntry(info.tile_size*3+"m",info.tile_size*3);
		this.addEntry(info.tile_size*4+"m",info.tile_size*4);
		this.addEntry(info.tile_size*5+"m",info.tile_size*5);
		this.value(info.tile_size*2);
		this.onChange();
		/*select_scale.innerHTML = "";
		select_scale.options.add(new Option(info.tile_size+"m",info.tile_size));
		select_scale.options.add(new Option(info.tile_size*2+"m",info.tile_size*2));
		select_scale.options.add(new Option(info.tile_size*3+"m",info.tile_size*3));
		select_scale.options.add(new Option(info.tile_size*4+"m",info.tile_size*4));
		select_scale.options.add(new Option(info.tile_size*5+"m",info.tile_size*5));
		select_scale.value = info.tile_size*2;
		select_scale_onchange();*/		
	},	
	
});