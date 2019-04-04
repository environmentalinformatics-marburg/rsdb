"use strict";
window.onload = init;
var query;
function init() {
	Helper.injectHtml();
	query = new Query();
}

var Query = Class.extend({
	
	init: function() {
		
		document.getElementById("button_pointdb_feature").onclick = this.onClickPointdbFeature.bind(this);
		
		this.div_pointdb_feature_result = document.getElementById("div_pointdb_feature_result");
		
		this.comboPointdbFeature = new ComboPointdbFeature("select_pointdb_feature");
		
		this.div_pointdb_feature = document.getElementById("div_pointdb_feature");
		this.div_image_dimensions = document.getElementById("div_image_dimensions");
		
		this.comboBand = new ComboBand("select_hyperspectral_band");
		this.comboBand.refresh();
		this.comboHyperspectralImageType = new Combo("select_hyperspectral_image_type");
		this.comboHyperspectralImageType.clear();
		this.comboHyperspectralImageType.addEntry("one band");
		this.comboHyperspectralImageType.addEntry("color");
		this.comboHyperspectralImageType.addEntry("pseudocolor");
		this.comboHyperspectralImageType.addEntry("ndvi");
		this.comboHyperspectralImageType.value("color");
		
		this.comboQueryType = new ComboQueryType("select_query_type");
		this.comboQueryType.setOnChange(this.onComboQueryTypeChange.bind(this));
		
		this.editImageWidth = new Edit("imageWidth");
		this.editImageHeight = new Edit("imageHeight");
		this.editEasting = new Edit("easting");
		this.editNorthing = new Edit("northing");
		
		this.comboPoi = new ComboPoi("select_poi", this.editEasting, this.editNorthing);
		this.comboPoiGroup = new ComboPoiGroup("select_poi_group");
		this.comboPoiGroup.setOnChange(this.comboPoi.refresh.bind(this.comboPoi));
		
		
		this.eastingRange = document.getElementById("easting_range");
		this.northingRange = document.getElementById("northing_range");
		
		
		this.checkbox_image_fill = new Checkbox("checkbox_image_fill");
		this.comboImageScale = new ComboImageScale("select_image_scale");
		this.comboImageScale.refresh();
		this.comboImageType = new ComboImageType("select_image_type");
		this.comboImageType.refresh();
		document.getElementById("button_create_hyperspectral_image").onclick = this.onClickCreateHyperspectralImage.bind(this);
		document.getElementById("button_create_lidar_image").onclick = this.onClickCreateLidarImage.bind(this);
		this.div_create_lidar_image = document.getElementById("div_create_lidar_image");
		this.div_create_hyperspectral_image = document.getElementById("div_create_hyperspectral_image");
		this.comboLayer = new ComboLayer("select_layer");
		this.comboLayer.setOnChange(this.onComboLayerChange.bind(this));
		this.comboDbType = new ComboDbType("select_dbType");
		this.comboDbType.setOnChange(this.onComboDbTypeChange.bind(this));
		this.comboDbType.refresh();
	},
	
	onComboDbTypeChange: function(dbType) {
		this.comboLayer.refresh(dbType);
		this.comboQueryType.refresh(dbType);				
	},
	
	onComboQueryTypeChange: function(queryType) {
		this.div_image_dimensions.style.display = "none";
		this.div_create_lidar_image.style.display = "none";
		this.div_create_hyperspectral_image.style.display = "none";
		this.div_pointdb_feature.style.display = "none";
		var dbType = this.comboDbType.value();
		switch(dbType) {
			case "PointDB":
				switch(queryType) {
						case "feature":
						this.div_pointdb_feature.style.display = "inline";
						this.comboPointdbFeature.refresh();
						break;
					case "image":
						this.div_image_dimensions.style.display = "inline";
						this.div_create_lidar_image.style.display = "inline";
						break;
					default:
						console.log("error "+queryType);
				}
				break;
			case "SpectralDB":
			this.div_image_dimensions.style.display = "inline";
				this.div_create_hyperspectral_image.style.display = "inline";
				break;
			default:
				console.log("error "+type);
		}
	},
	
	onComboLayerChange: function(layerName) {
		this.eastingRange.innerHTML = "-";
		this.northingRange.innerHTML = "-";
		console.log(this.comboDbType.value());
		switch(this.comboDbType.value()) {
			case "PointDB":
				this.onComboPointdbLayerChange(layerName);
			break;
			case "SpectralDB":
				this.onComboSpectraldbLayerChange(layerName);
			break;
			default:
				console.log("error "+type);
		}
	},
	
	onComboPointdbLayerChange: function(layerName) {
		Helper.getJSON('/pointdb/info'+Helper.toQuery({db:layerName}),this.onLoadPointDbInfo.bind(this));
	},
	
	onComboSpectraldbLayerChange: function(layerName) {
		Helper.getJSON('/spectraldb/info'+Helper.toQuery({db:layerName}),this.onLoadSpectralDbInfo.bind(this));
	},

	onLoadPointDbInfo: function(info) {
		console.log(info);
		var eMin = info.tile_x_min;
		var eMax = info.tile_x_max + info.tile_size;
		this.eastingRange.innerHTML = eMin+" - "+eMax;
		var nMin = info.tile_y_min;
		var nMax = info.tile_y_max + info.tile_size;
		this.northingRange.innerHTML = nMin+" - "+nMax;
		this.comboPoiGroup.refresh({pointdb:info.db});		
	},
	
	onLoadSpectralDbInfo: function(info) {
		console.log(info);
		var eMin = info.tile_x_min;
		var eMax = info.tile_x_max + info.tile_size;
		this.eastingRange.innerHTML = eMin+" - "+eMax;
		var nMin = info.tile_y_min;
		var nMax = info.tile_y_max + info.tile_size;
		this.northingRange.innerHTML = nMin+" - "+nMax;
		this.comboPoiGroup.refresh({spectraldb:info.db});		
	},

	onClickCreateLidarImage: function() {
		
		var parameters = {	db:this.comboLayer.value(),
							x:this.editEasting.value(), 
							y:this.editNorthing.value(),
							width:this.editImageWidth.value(),
							height:this.editImageHeight.value(),
							scale:this.comboImageScale.value(),
							fill:this.checkbox_image_fill.checked(),
							type:this.comboImageType.value()
						};
							
		window.location = "/pointdb/image"+Helper.toQuery(parameters);		
	},
	
	onClickCreateHyperspectralImage: function() {
		var parameters = {	db:this.comboLayer.value(),
							x:this.editEasting.value(), 
							y:this.editNorthing.value(),
							width:this.editImageWidth.value(),
							height:this.editImageHeight.value(), 
						};
		window.location = "/spectraldb/image"+Helper.toQuery(parameters);
	},
	
	onClickPointdbFeature: function() {
		this.div_pointdb_feature_result.innerHTML = "query...";
		var dbName = this.comboLayer.value();
		var x = Number(this.editEasting.value());
		var y = Number(this.editNorthing.value());		
		var parameters = {db:dbName, name:this.comboPointdbFeature.value(), x1:x-5, y1:y-5, x2:x+5, y2:y+5};
		Helper.getJSON('/pointdb/feature'+Helper.toQuery(parameters),this.onLoadPointdbFeature.bind(this), this.onErrorPointdbFeature.bind(this));
	},
	
	onLoadPointdbFeature: function(json) {
		this.div_pointdb_feature_result.innerHTML = JSON.stringify(json, null, 2);	
	},
	
	onErrorPointdbFeature: function(text) {
		this.div_pointdb_feature_result.innerHTML = "ERROR "+text;	
	},
});