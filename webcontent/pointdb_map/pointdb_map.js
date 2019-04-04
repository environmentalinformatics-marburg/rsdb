"use strict";

window.onload = init;

var info = {};

var comboImageScale;
var comboImageType;
var comboPointdb;
var comboScale;
var comboTone;
var checkboxImageFill;
var checkboxShowHide;
var map;
var imageViewer;

var comboPoi;
var comboPoiGroup;

function init() {
	document.getElementById("button_goto").onclick = onClickGoto;
	comboPoi = new ComboPoi("select_poi");
	comboPoiGroup = new ComboPoiGroup("select_poi_group");
	comboPoiGroup.setOnChange(comboPoi.refresh.bind(comboPoi));
	
	comboImageScale = new ComboImageScale("select_image_scale");
	comboImageScale.setOnChange(select_image_scale_onchange);
	
	comboImageType = new ComboImageType("select_image_type");
	comboImageType.setOnChange(select_image_type_onchange);
	
	var defaultDbName = undefined;
	if(window.location.hash.length) {
		var rel = window.location.hash.substr(1);
		defaultDbName = rel;
	}
	comboPointdb = new ComboPointdb("select_pointdb", defaultDbName);
	comboPointdb.setOnChange(select_pointdb_onchange);
	
	comboScale = new ComboScale("select_scale");
	comboScale.setOnChange(select_scale_onchange);
	
	comboTone = new Combo("select_tone");
	comboTone.addEntry("linear", 1);
	comboTone.addEntry("gamma 1.5", 1.5);
	comboTone.addEntry("gamma 2", 2);
	comboTone.addEntry("gamma 2.5", 2.5);
	comboTone.addEntry("gamma 3", 3);
	comboTone.value(1.5);
	comboTone.setOnChange(select_tone_onchange);
	
	checkboxImageFill = new Checkbox("checkbox_image_fill");
	checkboxImageFill.setOnchange(checkbox_image_fill_onchange);
	
	checkboxShowHide = new CheckboxShowHide("checkbox_settings", true, "tools");
	
	map = new Map("map");
	
	imageViewer = new ImageViewer();
	
	document.getElementById("buttonView3D").onclick = imageViewer.buttonView3D_onclick.bind(imageViewer);
	document.getElementById("buttonViewElevationMap").onclick = imageViewer.buttonViewElevationMap_onclick.bind(imageViewer);
	
	checkboxImageFill.onChange();
	comboImageScale.refresh();
	comboImageType.refresh();
	comboPointdb.refresh();		
}

function select_image_type_onchange(value) {
	imageViewer.loadImage({imageType:value});
}

function checkbox_image_fill_onchange(checked) {
	imageViewer.loadImage({imageFill:checked});
}

function select_image_scale_onchange(value) {
	console.log("v" +value);
	imageViewer.loadImage({scale:value});
}

function select_pointdb_onchange() {
	comboScale.clear();
	var div_map = document.getElementById("map");
	div_map.innerHTML = "<b>query meta data of "+comboPointdb.value()+" ...</b>";
	var div_image = document.getElementById("image");
	div_image.innerHTML = "";	
	
	var param = {db:comboPointdb.value()};
	Helper.getJSON('/pointdb/info'+Helper.toQuery(param),onLoadInfo, onErrorLoadInfo);
	
	comboPoiGroup.refresh({pointdb:comboPointdb.value()});
	history.replaceState('', '', '#' + comboPointdb.value());	
}

function onLoadInfo(info_new) {
	info = info_new;
	comboScale.refresh(info_new);
	imageViewer.loadImage({info:info_new});	
}

function onErrorLoadInfo(text) {
	map.onError(text);
	imageViewer.loadImage({center_utmX:0, center_utmY:0});
}

function select_scale_onchange() {
	var div_map = document.getElementById("map");
	div_map.innerHTML = "";	
	map.loadMap(comboPointdb.value(), comboScale.value(), comboTone.value());
}

function select_tone_onchange() {
	select_scale_onchange();
}

function onClickGoto() {
	if(comboPoi.value() != "") {
		var poi = JSON.parse(comboPoi.value());
		var parameters = {center_utmX:poi.x, center_utmY:poi.y};
		imageViewer.loadImage(parameters);
	}
}