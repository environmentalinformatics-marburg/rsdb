"use strict";

var ImageViewer = Class.extend({
	
	init: function() {
		this.info = {};
		this.center_utmX = 0;
		this.center_utmY = 0;
		this.image_width = 800;
		this.image_height = 400;
		this.scale = 0;
		this.imageFill = false;
		this.imageType = "";

		this.image_UTM_offsetX = 0;
		this.image_UTM_offsetY = 0;
		
		this.div_image = document.getElementById("image");
		this.div_image.style.width = this.image_width+"px";
		this.div_image.style.height = this.image_height+"px";
		//this.div_image.style.backgroundColor = "grey";
		
		this.showloadingText = false;
	},
	
	loadImage: function(config) {
		if(config.center_utmX!==undefined) {
			this.center_utmX = config.center_utmX;
		}
		if(config.center_utmY!==undefined) {
			this.center_utmY = config.center_utmY;
		}
		if(config.scale!==undefined) {
			this.scale = config.scale;
		}
		if(config.imageFill!==undefined) {
			this.imageFill = config.imageFill;
		}
		if(config.imageType!==undefined) {
			this.imageType = config.imageType;
		}
		if(config.info!==undefined) {
			this.info = config.info;
		}
		if(this.center_utmX === 0 && this.center_utmX === 0) {
			document.getElementById("text_easting").value = 0;
			document.getElementById("text_northing").value = 0;
			var div_image = document.getElementById("image");
			div_image.innerHTML = "no data selected<br>(View loading may take some seconds.)<h3>Click at point on below map to view details image here.</h3>";
			return;
		}
		var parameters = {	db:this.info.db,
							x:this.center_utmX, 
							y:this.center_utmY,
							width:this.image_width,
							height:this.image_height,
							scale:this.scale,
							fill:this.imageFill,
							type:this.imageType};
		
		this.image_UTM_offsetX = parameters.x - ((parameters.width/2)*this.scale/1000);
		this.image_UTM_offsetY = parameters.y + ((parameters.height/2)*this.scale/1000);
		
		document.getElementById("text_easting").value = parameters.x.toFixed(0);
		document.getElementById("text_northing").value = parameters.y.toFixed(0);
		
		var image = new Image();
		image.onerror = function(e,f) {
			document.getElementById("image").innerHTML = "server error"; 
			Helper.getText(image.src, Helper.nothingFunc, function(value) {document.getElementById("image").innerHTML = "server error: "+value;})
		};
		if(!this.showloadingText) {
			image.onload = function() {				
				var div_image = document.getElementById("image");
			div_image.innerHTML = "";
			div_image.appendChild(image);
			};
		}
		image.src = "/pointdb/image"+Helper.toQuery(parameters);
		image.alt = "loading image of "+this.info.db+" at UTM "+parameters.x+" "+parameters.y+" ...";
		image.title = "click\t\t==> move to position \nctrl + click\t==> view in full window \nshift + click\t==> show coordinates";
		image.onclick = this.onImageClick.bind(this);
		image.onmousemove = this.onImageMouseMove.bind(this);
		image.onmouseout = this.onImageMouseout.bind(this);

		if(this.showloadingText) {
			var div_image = document.getElementById("image");
			div_image.innerHTML = "";
			div_image.appendChild(image);
		}

		var image_text = document.getElementById("image_text");
		image_text.innerHTML = "image UTM "+parameters.x+" "+parameters.y;	
	},
	
	eventToUTM: function(event) {
		var rect = event.target.getBoundingClientRect();
		var offsetX = event.clientX - rect.left;
		var offsetY = event.clientY - rect.top;
		var utmX = this.image_UTM_offsetX + (offsetX*this.scale)/1000;
		//var utmY = info.tile_y_min + offsetY*tile_size;
		var utmY = this.image_UTM_offsetY - (offsetY*this.scale)/1000;
		return {x:utmX, y:utmY};
	},

	onImageMouseMove: function(event) {
		var utm = this.eventToUTM(event);
		var div_image_text = document.getElementById("image_text");
		div_image_text.innerHTML = "image UTM "+utm.x.toFixed(1)+" "+utm.y.toFixed(1);
	},

	onImageMouseout: function(event) {
		var div_image_text = document.getElementById("image_text");
		div_image_text.innerHTML = "---";
	},
	
	onImageClick: function(event) {
		var utm = this.eventToUTM(event);
		if(event.shiftKey) {
			Helper.messagebox("Position", utm.x+", "+utm.y);
		} else if(event.ctrlKey){			
			var parameters = {	db:this.info.db,
								x:utm.x, 
								y:utm.y,
								width:document.body.clientWidth,
								height:document.body.clientHeight,
								scale:this.scale,
								fill:this.imageFill,
								type:this.imageType};
			window.location = "/pointdb/image"+Helper.toQuery(parameters);
		} else {
			this.loadImage({center_utmX:utm.x, center_utmY:utm.y});
		}
	},
	
	buttonView3D_onclick: function() {
		var parameters = {db:this.info.db, x:this.center_utmX, y:this.center_utmY};
		var url = "../pointcloud_view/pointcloud_view.html#/"+Helper.toQuery(parameters);
		window.open(url, '_blank');
	},
	
	buttonViewElevationMap_onclick: function() {
		var parameters = {db:this.info.db, x:this.center_utmX, y:this.center_utmY};
		var url = "../height_map_view/height_map_view.html"+Helper.toQuery(parameters);
		window.open(url, '_blank');
	},
	
});