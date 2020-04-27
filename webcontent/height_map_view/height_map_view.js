"use strict";
document.addEventListener('DOMContentLoaded', function() {init();}, false);

function init() {
	
var app = new Vue({
	
el: '#app',

data: {
	useAnimation: false,
	illuminated: false,
	//fog_default_density: 0.007378698,  //(1/1.25)^22
	fog_default_density: 0.001,
	fogDensity: 0,
	needsRedraw: false, // only used if useAnimation == false
	camera: undefined,
	renderer: undefined,
	scene: undefined,
	controls: undefined,
	plane: undefined,
	viewTypes: [{name: 'DTM', title: '', data_type: 'basic_raster'}],
	viewType: undefined,
	loadingMessage: "init",
},

mounted: function () {
	var self = this;
	this.refresh_raster_processing_types();
	
	window.addEventListener('keydown', this.onKeyDown, false);
	window.addEventListener('resize', this.onWindowResize, false);
	window.addEventListener('scroll', function(){window.scrollTo(0,0);}, false); //disable window move for up/down arrows
	
	this.camera = new THREE.PerspectiveCamera( 27, window.innerWidth / window.innerHeight, 0.1, 3500 );
	this.camera.position.z = 200;
	//this.camera.projectionMatrix.scale(new THREE.Vector3(-1, 1, 1)); // mirror		
	this.scene = new THREE.Scene();
	this.fogDensity = this.fog_default_density;
	this.scene.fog = new THREE.FogExp2(0x000000, 0);
	if(this.illuminated) {
		this.scene.fog.density = this.fogDensity;
	}
	
	var light = new THREE.PointLight(0xffffff, 1, 1000, 2);
	light.position.set(50, 50, 0);
	this.scene.add( light );
	
	var lightH = new THREE.HemisphereLight( 0x0000ff, 0xff0000, 0.3 );
	this.scene.add(lightH);
	
	var lightA = new THREE.AmbientLight( 0x404040, 0.05 );
	this.scene.add(lightA);
			
	this.renderer = new THREE.WebGLRenderer({antialias: true});
	document.getElementById("webgl_container").appendChild(this.renderer.domElement);
	this.renderer.setClearColor( this.scene.fog.color );
	this.renderer.setPixelRatio( window.devicePixelRatio );
	this.renderer.setSize( window.innerWidth, window.innerHeight );
	
	this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
	this.controls.addEventListener('change', this.requestRedraw);
	this.controls.enableZoom = true;
	this.controls.dampingFactor = 0.25;
	console.log(this.controls);
	this.controls.position0.set(-600, 600, 100);
	this.controls.reset();
		
	
	if(this.useAnimation) {
		this.controls.enableDamping = true;
		this.animate();
	} else {
		this.controls.enableDamping = false;		
	}
},

methods: {

	refresh_raster_processing_types: function() {
		var self = this;
		var urlParameters = extractParameters();
		var url = "";
		var queryParameters = {};
		if(urlParameters.db !== undefined) {
			this.loadingMessage = "loading raster types ...";
			url = "../../pointdb/info.json";
			queryParameters.db = urlParameters.db;
			queryParameters.statistics = false;
			axios.get(url, {params: queryParameters})
			.then(function(response) {
				self.loadingMessage = undefined;
				var json = response.data;
				self.viewTypes = json.raster_processing_types;
			})
			.catch(function(error) {
				self.loadingMessage = "ERROR loading " +error;
				console.log("could not load raster_processing_types " + error);
			});
		} else if(urlParameters.pointcloud !== undefined) {
			this.loadingMessage = "loading raster types ...";
			url = "../../pointclouds/" + urlParameters.pointcloud;
			axios.get(url, {params: queryParameters})
			.then(function(response) {
				self.loadingMessage = undefined;
				var json = response.data;
				self.viewTypes = json.pointcloud.raster_types;
			})
			.catch(function(error) {
				self.loadingMessage = "ERROR loading " +error;
				console.log("could not load raster_types " + error);
			});
		} else {
			throw "invalid parameters";
		}					
	},

	load_heightMap: function() {
		var self = this;
		this.loadingMessage = "loading " + this.viewType + " ...";
		var urlParameters = extractParameters();
		var url = "";
		var queryParameters = {};
		if(urlParameters.db !== undefined) {
			url = "../../pointdb/query_raster";
			var radius = 200;
			var qx = parseFloat(urlParameters.x);
			var qy = parseFloat(urlParameters.y);
			var ext = ""+(qx-radius)+","+(qx+radius)+","+(qy-radius)+","+(qy+radius);
			queryParameters.db = urlParameters.db;
			queryParameters.ext = ext;
			queryParameters.type = this.viewType;
			queryParameters.format = "js";
		} else if(urlParameters.pointcloud !== undefined) {
			url = "../../pointclouds/" + urlParameters.pointcloud  + "/raster.js";
			var radius = 200;
			var qx = parseFloat(urlParameters.x);
			var qy = parseFloat(urlParameters.y);
			var ext = ""+(qx-radius)+" "+(qy-radius)+" "+(qx+radius)+" "+(qy+radius);
			queryParameters.db = urlParameters.db;
			queryParameters.ext = ext;
			queryParameters.type = this.viewType;
			queryParameters.fill = 10;
		} else {
			throw "invalid parameters";
		}
		axios.get(url, {params: queryParameters, headers: {'Accept': 'application/octet-stream'}, responseType: 'arraybuffer'})
		.then(function(response) {
			self.loadingMessage = undefined;
			var arrayBuffer = response.data;
			var dataView = new DataView(arrayBuffer);
			var currentPos = 0;
			var xLen = dataView.getUint32(currentPos, true);
			currentPos += 4;
			var yLen = dataView.getUint32(currentPos, true);
			currentPos += 4;
			var array = new Float32Array(arrayBuffer, currentPos, xLen*yLen);			
			self.update_view(xLen, yLen, array);
		})
		.catch(function(error) {
			self.loadingMessage = "ERROR loading " +error;
		});	
	},
	
	update_view: function(xLen, yLen, array) {
		if(this.plane != undefined) {
			this.scene.remove(this.plane);
		}
		var factor = 1;
		var sum = 0;
		var cnt = 0;
		for ( var i = 0; i<array.length; i++ ) {
			var v = array[i];
			if(!isNaN(v)) {
				sum += v * factor;
				cnt++;
			}
		}
		var zoff = -(sum/cnt);
	
		/*var geometry = new THREE.PlaneGeometry(xLen, yLen, xLen-1, yLen-1);		
		var vertices = geometry.vertices;
		for ( var i = 0; i<array.length; i++ ) {
			vertices[i].z = zoff + array[i];
		}*/		
		
		var geometry = new THREE.PlaneBufferGeometry(xLen, yLen, xLen-1, yLen-1);
		var verticesBuffer = geometry.getAttribute('position').array;
		for ( var i = 0; i<array.length; i++ ) {
			verticesBuffer[i*3+2] = zoff + array[i] * factor;
		}
		

		geometry.rotateX( - (3.14159 / 2));
		geometry.computeVertexNormals();

		//var material = new THREE.MeshBasicMaterial( { color: 0xffffff, wireframe: true, side: THREE.DoubleSide } );
		//var material = new THREE.MeshBasicMaterial( { color: 0xffffff, side: THREE.DoubleSide} );
		//var material = new THREE.MeshNormalMaterial({side: THREE.DoubleSide});
		//var material = new THREE.MeshLambertMaterial({side: THREE.DoubleSide});
		var material = new THREE.MeshStandardMaterial({side: THREE.DoubleSide});
		this.plane = new THREE.Mesh( geometry, material );		
		this.scene.add(this.plane);		
		this.animate();
	},
	
	animate: function() {
		if(this.useAnimation) {
			requestAnimationFrame(this.animate);
		} else {
			this.needsRedraw = false;
		}
		this.controls.update();
		this.render();		 
	},

	requestRedraw: function() {
		if( (!this.useAnimation) && (!this.needsRedraw) ) {
			this.needsRedraw = true;
			requestAnimationFrame(this.animate);
		}
	},

	render: function() {
		this.renderer.render(this.scene, this.camera);
	},
	
	onWindowResize: function() {
		this.camera.aspect = window.innerWidth / window.innerHeight;
		this.camera.updateProjectionMatrix();
		this.camera.projectionMatrix.scale(new THREE.Vector3(-1, 1, 1)); // mirror
		this.renderer.setSize( window.innerWidth, window.innerHeight );
		this.requestRedraw();
	},
	
	onKeyDown: function(event) {
		switch ( event.keyCode ) {
		case 33: // PAGE_UP
			if(this.illuminated) {
				this.fogDensity /= 1.25;			
				this.scene.fog.density = this.fogDensity;
				this.requestRedraw();
			}
			break;
		case 34: // PAGE_DOWN
			if(this.illuminated) {
				this.fogDensity *= 1.25;			
				this.scene.fog.density = this.fogDensity;
				this.requestRedraw();
			}
			break;
		}
	},
}, //end methods

watch: {
	useAnimation: function() {		
		if(this.useAnimation) {
			this.controls.enableDamping = true;
			this.animate();
		} else {
			this.controls.enableDamping = false;		
		}		
	},
	viewType: function() {
		this.load_heightMap();
	},
	viewColor: function() {
		this.load_points();
	},
	viewZ: function() {
		this.load_points();
	},
	filterExtremes: function() {
		this.load_points();
	},
	illuminated: function() {
		if(this.illuminated) {
			this.scene.fog.density = this.fogDensity;
		} else {
			this.scene.fog.density = 0;
		}
		this.requestRedraw();
	},	
},  //end watch

}); //end app	
} //end init

//derived from http://stackoverflow.com/questions/2353211/hsl-to-rgb-color-conversion
function hue2rgb(p, q, t){
	if(t < 0) t += 1;
	if(t > 1) t -= 1;
	if(t < 1/6) return p + (q - p) * 6 * t;
	if(t < 1/2) return q;
	if(t < 2/3) return p + (q - p) * (2/3 - t) * 6;
	return p;
}

/**
 * Converts an HSL color value to RGB. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 * Assumes h, s, and l are contained in the set [0, 1] and
 * returns r, g, and b in the set [0, 255].
 *
 * @param   {number}  h       The hue
 * @param   {number}  s       The saturation
 * @param   {number}  l       The lightness
 * @return  {Array}           The RGB representation
 */
function hslToRgb(h, s, l){
    var r, g, b;

    if(s == 0){
        r = g = b = l; // achromatic
    }else{
        var q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        var p = 2 * l - q;
        r = hue2rgb(p, q, h + 1/3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1/3);
    }

    return [r, g, b];
}

//Extract parameters from URL
//derived from http://www.xul.fr/javascript/parameters.php
function extractParameters() {
	var parameters = {};
	var parameterTexts = location.search.substring(1).split("&");
	for(var i in parameterTexts) {
		var p = parameterTexts[i].split("=");
		parameters[p[0]] = p[1];				
	}
	return parameters; 
}