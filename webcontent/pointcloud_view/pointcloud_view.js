"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);

function init() {

	var pointCloudView = Vue.component('point-cloud-view', {

		template: '#point-cloud-view-template',

		data: function () {
			return {
				urlParameters: { db: "name", x: "0", y: "0" },
				pointCount: 0,
				useAnimation: false,
				filterExtremes: false,
				illuminated: true,
				fog_default_density: 0.007378698,  //(1/1.25)^22
				fogDensity: 0,
				pointSize: 2.5,
				needsRedraw: false, // only used if useAnimation == false
				camera: undefined,
				renderer: undefined,
				scene: undefined,
				controls: undefined,
				particleSystem: undefined,
				viewTypes: ["all", "0. Not Classified", "1. Unclassified", "2. Ground", "3. Low Vegetation", "4. Medium Vegetation", "5. High Vegetation", "6. Building", "7. Low Point Noise", "8. Model Key-point", "9. Water", "12. Overlap Points", "13. (unofficial) Vegetation"],
				viewTypeMap: { "all": -1, "0. Not Classified": 0, "1. Unclassified": 1, "2. Ground": 2, "3. Low Vegetation": 3, "4. Medium Vegetation": 4, "5. High Vegetation": 5, "6. Building": 6, "7. Low Point Noise": 7, "8. Model Key-point": 8, "9. Water": 9, "12. Overlap Points": 12, "13. (unofficial) Vegetation": 13 },
				viewType: "all",
				viewColors: ['classified', 'grey', 'color'],
				viewColor: 'classified',
				viewZs: ['elevation', 'height'],
				viewZ: 'elevation',
				loadingMessage: "init",
			};
		},

		mounted: function () {
			var self = this;

			//this.urlParameters = extractParameters();
			this.loadUrlParameters();

			window.addEventListener('keydown', this.onKeyDown, false);
			window.addEventListener('resize', this.onWindowResize, false);
			window.addEventListener('scroll', function () { window.scrollTo(0, 0); }, false); //disable window move for up/down arrows

			this.camera = new THREE.PerspectiveCamera(27, window.innerWidth / window.innerHeight, 50, 3500);
			this.camera.position.z = 200;
			this.camera.projectionMatrix.scale(new THREE.Vector3(-1, 1, 1)); // mirror		
			this.scene = new THREE.Scene();
			this.fogDensity = this.fog_default_density;
			this.scene.fog = new THREE.FogExp2(0x000000, this.fogDensity);

			this.renderer = new THREE.WebGLRenderer({ antialias: true });
			document.getElementById("webgl_container").appendChild(this.renderer.domElement);
			this.renderer.setClearColor(this.scene.fog.color);
			this.renderer.setPixelRatio(window.devicePixelRatio);
			this.renderer.setSize(window.innerWidth, window.innerHeight);

			this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
			this.controls.addEventListener('change', this.requestRedraw);
			this.controls.enableZoom = true;
			this.controls.dampingFactor = 0.25;

			if (this.useAnimation) {
				this.controls.enableDamping = true;
				this.animate();
			} else {
				this.controls.enableDamping = false;
			}
		},

		methods: {
			load_points: function () {
				var self = this;
				this.loadingMessage = "loading points";
				//var radius = 10;
				var radius = 100;
				var qx = parseFloat(this.urlParameters.x);
				var qy = parseFloat(this.urlParameters.y);				

				if(this.urlParameters.pointcloud === undefined) {
					var ext = "" + (qx - radius) + "," + (qx + radius) + "," + (qy - radius) + "," + (qy + radius);

					var queryParameters = { db: this.urlParameters.db, ext: ext, format: "js"/*, sort: "z"*/ };
					var i = this.viewTypeMap[this.viewType];
					if (0 <= i) {
						queryParameters.filter = 'classification=' + i;
					}
					if (this.viewZ == "height") {
						queryParameters.normalise = "ground";
						if (this.filterExtremes) {
							queryParameters.normalise += ",extremes";
						}
					} else {
						if (this.filterExtremes) {
							queryParameters.normalise = "extremes";
						}
					}
					axios.get("../../pointdb/query", { params: queryParameters, headers: { 'Accept': 'application/octet-stream' }, responseType: 'arraybuffer' })
						.then(function (response) {
							self.loadingMessage = undefined;
							var arrayBuffer = response.data;
							var dataView = new DataView(arrayBuffer);
							var currentPos = 0;
							var points = dataView.getUint32(currentPos, true);
							self.pointCount = points;
							currentPos += 4;
							var pos_array = new Float32Array(arrayBuffer, currentPos, points * 3);
							currentPos += points * 3 * 4;
							console.log("points " + points + "  bytes " + arrayBuffer.byteLength);
							var classification_array = new Uint8Array(arrayBuffer, currentPos, points);
							currentPos += points;
							self.update_view(pos_array, classification_array);
						})
						.catch(function (error) {
							self.loadingMessage = "ERROR loading points";
						});

				} else {
					var queryParameters = {};
					queryParameters.ext = "" + (qx - radius) + " " + (qy - radius) + " " + (qx + radius) + " " + (qy + radius);
					switch(this.viewColor) {
						case 'grey':
							queryParameters.columns = "x y z";
							queryParameters.format = "xzy";
							break;
						case 'classified':
							queryParameters.columns = "x y z classification";
							queryParameters.format = "xzy_classification";
							break;
						case 'color':
							queryParameters.columns = "x y z red green blue";
							queryParameters.format = "xzy_rgb";
							break;							
						default:
							console.log("unknown viewColor: " + this.viewColor);
					}
					var i = this.viewTypeMap[this.viewType];
					if (0 <= i) {
						queryParameters.filter = 'classification=' + i;
					}
					if (this.viewZ == "height") {
						queryParameters.normalise = "ground";
						if (this.filterExtremes) {
							queryParameters.normalise += ",extremes";
						}
					} else {
						if (this.filterExtremes) {
							queryParameters.normalise = "extremes";
						}
					}
					console.time("get points");
					var url = "../../pointclouds/" + this.urlParameters.pointcloud + "/points.js";
					axios.get(url, { params: queryParameters, headers: { 'Accept': 'application/octet-stream' }, responseType: 'arraybuffer' })
						.then(function (response) {
							console.timeEnd("get points");
							self.loadingMessage = undefined;
							var arrayBuffer = response.data;
							var dataView = new DataView(arrayBuffer);
							var currentPos = 0;
							var points = dataView.getUint32(currentPos, true);
							self.pointCount = points;
							currentPos += 4;
							var pos_array = new Float32Array(arrayBuffer, currentPos, points * 3);
							currentPos += points * 3 * 4;
							var classification_array;
							var rgb_array;
							switch(self.viewColor) {
								case 'grey':
									// nothing
									break;
								case 'classified':
									classification_array = new Uint8Array(arrayBuffer, currentPos, points);
									currentPos += points;
									break;
								case 'color':
									rgb_array = new Uint16Array(arrayBuffer, currentPos, points * 3);
									currentPos += points * 3 * 2;
									break;							
								default:
									console.log("unknown viewColor: " + self.viewColor);
							}						
							self.update_view(pos_array, classification_array, rgb_array);
						})
						.catch(function (error) {
							self.loadingMessage = "ERROR loading points " + error;
						});

				}
			},

			update_view: function (array, classification_array, rgb_array) {
				if (this.particleSystem != undefined) {
					this.scene.remove(this.particleSystem);
				}

				var geometry = new THREE.BufferGeometry();

				var points = array.length / 3;
				var positions = array;
				var colors = new Float32Array(points * 3);

				var xmin = 10000000;
				var ymin = 10000000;
				var zmin = 10000000;
				var xmax = 0;
				var ymax = 0;
				var zmax = 0;

				for (var i = 0; i < positions.length; i += 3) {

					if (positions[i] < xmin) {
						xmin = positions[i];
					}
					if (positions[i + 1] < ymin) {
						ymin = positions[i + 1];
					}
					if (positions[i + 2] < zmin) {
						zmin = positions[i + 2];
					}

					if (positions[i] > xmax) {
						xmax = positions[i];
					}
					if (positions[i + 1] > ymax) {
						ymax = positions[i + 1];
					}
					if (positions[i + 2] > zmax) {
						zmax = positions[i + 2];
					}
				}

				var yrange = ymax - ymin;
				var zrange = zmax - zmin;

				var color = new THREE.Color();

				var viewType = this.viewColor;
				if(viewType === 'classified' && classification_array === undefined) {
					viewType = 'grey';
				}
				if(viewType === 'color' && rgb_array === undefined) {
					viewType = 'grey';
				}

				switch (viewType) {
					case 'grey':
						for (var i = 0; i < colors.length; i++) {
							colors[i] = 1;							
						}
						break;
					case 'classified':
						for (var i = 0; i < points; i++) {
							var pos = i * 3;
							switch (classification_array[i]) {
								case 0: //	Created, never classified
								case 1: // Unclassified
									colors[pos] = 0;
									colors[pos + 1] = 1;
									colors[pos + 2] = 0;
									break;
								case 2: // Ground
								case 8: // Model Key-point (mass point)
								case 9: // Water
									colors[pos] = 1;
									colors[pos + 1] = 0;
									colors[pos + 2] = 0;
									break;
								case 3: // Low Vegetation
								case 4: // Medium Vegetation
								case 5: // High Vegetation
								case 13: // unofficial classification in layer "hessen" Vegetation
									colors[pos] = 1;
									colors[pos + 1] = 1;
									colors[pos + 2] = 1;
									break;
								case 6: // Building
									colors[pos] = 1;
									colors[pos + 1] = 1;
									colors[pos + 2] = 0;
									break;
								default:
									colors[pos] = 0;
									colors[pos + 1] = 0;
									colors[pos + 2] = 1;
								//console.log(classification_array[i]);			
							}
						}
						break;
					case 'color':
						for (var i = 0; i < colors.length; i++) {
							colors[i] = rgb_array[i] / 65535;							
						}
						break;						
					default:
						console.log("unknown viewType: " + viewType);
				}

				var xoff = -xmin - (xmax - xmin) / 2;
				//var yoff = -ymin-(ymax-ymin)/2;
				var zoff = -zmin - (zmax - zmin) / 2;
				var half = positions.length / 2;
				var yoff = - positions[(half - (half % 3)) + 1];
				console.log("yoff " + yoff + "   middle " + -ymin - (ymax - ymin) / 2);
				for (var i = 0; i < positions.length; i += 3) {
					positions[i] += xoff;
					positions[i + 1] += yoff;
					positions[i + 2] += zoff;
				}

				geometry.addAttribute('position', new THREE.BufferAttribute(positions, 3));
				geometry.addAttribute('color', new THREE.BufferAttribute(colors, 3));
				geometry.computeBoundingSphere();

				var material = new THREE.PointsMaterial({ size: this.pointSize, vertexColors: THREE.VertexColors, sizeAttenuation: false });
				this.particleSystem = new THREE.Points(geometry, material);

				this.scene.add(this.particleSystem);
				this.animate();
			},

			animate: function () {
				if (this.useAnimation) {
					requestAnimationFrame(this.animate);
				} else {
					this.needsRedraw = false;
				}
				this.controls.update();
				this.render();
			},

			requestRedraw: function () {
				if ((!this.useAnimation) && (!this.needsRedraw)) {
					this.needsRedraw = true;
					requestAnimationFrame(this.animate);
				}
			},

			render: function () {
				this.renderer.render(this.scene, this.camera);
			},

			onWindowResize: function () {
				this.camera.aspect = window.innerWidth / window.innerHeight;
				this.camera.updateProjectionMatrix();
				this.camera.projectionMatrix.scale(new THREE.Vector3(-1, 1, 1)); // mirror
				this.renderer.setSize(window.innerWidth, window.innerHeight);
				this.requestRedraw();
			},

			onKeyDown: function (event) {
				switch (event.keyCode) {
					case 33: // PAGE_UP
						if (this.illuminated) {
							this.illuminatedPlus();
						}
						break;
					case 34: // PAGE_DOWN
						if (this.illuminated) {
							this.illuminatedMinus();
						}
						break;
				}
			},

			illuminatedPlus: function () {
				this.fogDensity /= 1.25;
				this.scene.fog.density = this.fogDensity;
				this.requestRedraw();
			},

			illuminatedMinus: function () {
				this.fogDensity *= 1.25;
				this.scene.fog.density = this.fogDensity;
				this.requestRedraw();
			},

			pointSizeMinus: function () {
				if (this.pointSize > 1) {
					this.pointSize -= 0.5;
				}
			},

			pointSizePlus: function () {
				this.pointSize += 0.5;
			},

			updateURL: function () {
				var query = {};
				if(this.urlParameters.pointcloud === undefined)  {
					query.db = this.urlParameters.db;
				} else {
					query.pointcloud = this.urlParameters.pointcloud;
				}
				query.x = this.urlParameters.x;
				query.y = this.urlParameters.y;				
				this.$router.push({ path: '/', query: query });
			},

			loadUrlParameters: function () {
				var parameters = {};
				if(this.$route.query.pointcloud === undefined) {
					parameters.db = this.$route.query.db;
				} else {
					parameters.pointcloud = this.$route.query.pointcloud;
				}
				parameters.x = this.$route.query.x;
				parameters.y = this.$route.query.y;
				if(this.urlParameters.db !== parameters.db || this.urlParameters.pointcloud !== parameters.pointcloud || this.urlParameters.x !== parameters.x || this.urlParameters.y !== parameters.y) {
					this.urlParameters = parameters;
				}
			},

			moveStep: function (stepX, stepY) {
				var stepSize = 20;
				var x = parseFloat(this.urlParameters.x) + stepX * stepSize;
				var y = parseFloat(this.urlParameters.y) + stepY * stepSize;
				var parameters = {};
				if(this.urlParameters.pointcloud === undefined) {
					parameters.db = this.urlParameters.db;
				} else {
					parameters.pointcloud = this.urlParameters.pointcloud;
				}
				parameters.x = x;
				parameters.y = y;
				this.urlParameters = parameters;
			},
		}, //end methods

		watch: {
			useAnimation: function () {
				if (this.useAnimation) {
					this.controls.enableDamping = true;
					this.animate();
				} else {
					this.controls.enableDamping = false;
				}
			},
			viewType: function () {
				this.load_points();
			},
			viewColor: function () {
				this.load_points();
			},
			viewZ: function () {
				this.load_points();
			},
			filterExtremes: function () {
				this.load_points();
			},
			pointSize: function () {
				this.load_points();
			},
			illuminated: function () {
				if (this.illuminated) {
					this.scene.fog.density = this.fogDensity;
				} else {
					this.scene.fog.density = 0;
				}
				this.requestRedraw();
			},
			'$route': function () {
				this.loadUrlParameters();
			},
			urlParameters: function () {
				this.updateURL();
				this.load_points();
			}
		},  //end watch

	}); //end pointCloudView


	const routes = [
		{ path: '/', component: pointCloudView },
	]

	const router = new VueRouter({
		routes
	})

	const app = new Vue({
		router
	}).$mount('#app')

} //end init

//derived from http://stackoverflow.com/questions/2353211/hsl-to-rgb-color-conversion
function hue2rgb(p, q, t) {
	if (t < 0) t += 1;
	if (t > 1) t -= 1;
	if (t < 1 / 6) return p + (q - p) * 6 * t;
	if (t < 1 / 2) return q;
	if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
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
function hslToRgb(h, s, l) {
	var r, g, b;

	if (s == 0) {
		r = g = b = l; // achromatic
	} else {
		var q = l < 0.5 ? l * (1 + s) : l + s - l * s;
		var p = 2 * l - q;
		r = hue2rgb(p, q, h + 1 / 3);
		g = hue2rgb(p, q, h);
		b = hue2rgb(p, q, h - 1 / 3);
	}

	return [r, g, b];
}

//Extract parameters from URL
//derived from http://www.xul.fr/javascript/parameters.php
function extractParameters() {
	var parameters = {};
	var parameterTexts = location.search.substring(1).split("&");
	for (var i in parameterTexts) {
		var p = parameterTexts[i].split("=");
		parameters[p[0]] = p[1];
	}
	return parameters;
}