"use strict";

var Map = Class.extend({
	
	init: function(id) {
		this.div_map = document.getElementById("map");
		this.tile_size = 0;
	},
	
	onError: function (text) {
		this.div_map.innerHTML = "ERROR "+text;
	},
	
	loadMap: function(dbName, tile_size, gamma) {
		this.tile_size = tile_size;
		
		var parameters = {	db:dbName,
							tile_size:tile_size, 
							x:info.tile_x_min/tile_size, 
							y:info.tile_y_min/tile_size,
							width:(info.tile_x_max-info.tile_x_min)/tile_size+1,
							height:(info.tile_y_max-info.tile_y_min)/tile_size+1,
							gamma:gamma};
		
		var image = new Image();
		image.alt = "loading map of "+dbName+" ...";
		image.onclick = this.onMapClick.bind(this);
		image.onmousemove = this.onMapMove.bind(this);
		image.onmouseout = this.onMapMouseout.bind(this);
		image.src = "/pointdb/map"+Helper.toQuery(parameters);
		image.style.backgroundColor = "black";

		var div_map = document.getElementById("map");
		//div_map.style.backgroundColor = "gray";
		div_map.innerHTML = "";
		div_map.appendChild(image);	
	},
	
	eventToUTM: function(event) {
		var rect = event.target.getBoundingClientRect();
		var offsetX = event.clientX - rect.left;
		var offsetY = event.clientY - rect.top;
		var center_utmX = info.tile_x_min + offsetX*this.tile_size;
		var center_utmY = info.tile_y_max - offsetY*this.tile_size;
		return {x:center_utmX, y:center_utmY};
	},

	onMapClick: function(event) {
		var utm = this.eventToUTM(event);
		if(event.shiftKey) {
			Helper.messagebox("Position", utm.x+", "+utm.y);
		} else {
			imageViewer.loadImage({center_utmX: utm.x, center_utmY: utm.y});
		}		
	},
	
	onMapMove: function(event) {
		var utm = this.eventToUTM(event);
		var div_map_text = document.getElementById("map_text");
		div_map_text.innerHTML = "map UTM "+utm.x.toFixed(0)+" "+utm.y.toFixed(0);	
	},

	onMapMouseout: function(event) {
		var div_map_text = document.getElementById("map_text");
		div_map_text.innerHTML = "---";
	},	
	
});