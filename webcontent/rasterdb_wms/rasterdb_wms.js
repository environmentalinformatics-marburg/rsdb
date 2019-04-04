"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);
Vue.config.productionTip = false;

var url_wms = '../../rasterdb_wms?modus=openlayers';
var url_rasterdbs = '../../rasterdbs.json';
var url_rasterdb = '../../rasterdb';
var url_poi_groups = '../../api/poi_groups';
var url_poi_group = '../../api/poi_group';
var url_roi_groups = '../../api/roi_groups';
var url_roi_group = '../../api/roi_group';

var temp;

function init() {

	httpVueLoader.httpRequest = function(url) { // load as text (not as xml as default)   
		return axios({
			method: 'get',
			url: url,
			responseType: 'text',
		})
		.then(function(res) {	
			return res.data;
		})
		.catch(function(err) {			
			return Promise.reject(err.status);
		});
	}

	var app = new Vue({

		el: '#app',

		components: {
			'consumer-app': httpVueLoader('consumer-app.vue')
		},

		data: {
			appMessage: "init app ...",
			layerMessage: "init layer ...",

			rasterdbLayers: { rasterdbs: [] },
			session: undefined,
			tag: "-",
			layerName: undefined,
			layerMeta: { ref: { pixel_size: {} }, wms: { styles: [] }, associated: {} },

			timestamp: 0,

			styleName: undefined,

			rangeHover: false,
			rangeSelectFocus: false,

			gammas: ["auto", "0.1", "0.2", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0"],
			gamma: "auto",

			rangeTypes: ["auto", "static", "[0, 1]", "[-1, 1]", "[-1, 0]", "[0, 100]", "[0, 1000]", "[0, 10000]"],
			rangeTypeMap: { "[0, 1]": [0, 1], "[-1, 1]": [-1, 1], "[-1, 0]": [-1, 0], "[0, 100]": [0, 100], "[0, 1000]": [0, 1000], "[0, 10000]": [0, 10000] },
			rangeType: "auto",
			rangeStatic: [0, 1000],
			rangeStaticMinText: 0,
			rangeStaticMaxText: 1000,

			settingsHover: false,
			settingsSelectFocus: false,
			syncBands: false,

			sourceWMS: undefined,
			wmsReqestCount: 0,
			layerWMS: undefined,
			layerOSM: undefined,
			layerBingMaps: undefined,
			layerOpenTopoMap: undefined,
			layerStamenTerrain: undefined,
			layerPOIs: undefined,
			layerROIs: undefined,
			layerSelectedPosition: undefined,
			mapLayers: undefined,
			viewWMS: undefined,

			layerWMSopacities: [0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0],
			layerWMSopacity: 1.0,

			viewResolutionDisplay: 1,

			loadingSession: 0,
			loadingCount: 0,
			loadingErrorMessage: undefined,

			backgroundLayers: ["none", "OpenStreetMap", "BingMaps", "OpenTopoMap", "StamenTerrain"],
			backgroundLayer: "none",

			poiGroups: [],
			poiGroup: "none",
			pois: [],
			poiName: "none",
			sourcePOIs: undefined,
			movetopoiHover: false,
			movetopoiPanelHover: false,
			poiSelectFocus: false,

			roiGroups: [],
			roiGroup: "none",
			rois: [],
			roiName: "none",
			sourceROIs: undefined,
			movetoroiHover: false,
			movetoroiPanelHover: false,
			roiSelectFocus: false,

			selectedPosition: [NaN, NaN],
			selectedExtent: undefined,
			sourceSelectedPosition: undefined,
			viewExtent: [0, 0, 0, 0],
			interactionExtent: undefined,

			showNavigationPanel: false,

		}, //end data

		mounted: function () {
			this.appMessage = "init openlayers";
			this.loadRasterdbLayers();
			this.loadPOIgroups();
			this.loadROIgroups();
		}, //end mounted

		methods: {
			loadRasterdbLayers: function () {
				var self = this;
				this.appMessage = "load layers ...";
				axios.get(url_rasterdbs)
					.then(function (response) {
						var rasterdbLayers = response.data;
						console.log(rasterdbLayers);
						self.session = rasterdbLayers.session; 
						if (rasterdbLayers.rasterdbs.length > 0) {
							self.initLayers(rasterdbLayers);
						} else {
							self.appMessage = "no raster layers in database";
						}
					})
					.catch(function (error) {
						self.appMessage = "ERROR " + error;
					});
			},
			loadPOIgroups: function () {
				var self = this;
				//this.appMessage = "load POI groups ...";
				axios.get(url_poi_groups)
					.then(function (response) {
						self.poiGroups = response.data;
					})
					.catch(function (error) {
						self.loadingErrorMessage = "ERROR: Could not load POI groups";
					});
			},
			loadROIgroups: function () {
				var self = this;
				//this.appMessage = "load ROI groups ...";
				axios.get(url_roi_groups)
					.then(function (response) {
						self.roiGroups = response.data;
					})
					.catch(function (error) {
						self.loadingErrorMessage = "ERROR: Could not load ROI groups";
					});
			},
			initLayers: function (rasterdbLayers) {
				this.appMessage = "initLayers";
				this.rasterdbLayers = rasterdbLayers;
				if (window.location.hash.length) {
					var rel = window.location.hash.substr(1);
					this.layerName = rel;
				} else {
					this.layerName = this.rasterdbLayers.rasterdbs[0].name;
				}
				this.appMessage = undefined;
			},
			updateMapLayers() {
				this.mapLayers.clear();
				if (this.backgroundLayer === 'OpenStreetMap') {
					this.mapLayers.push(this.layerOSM);
				}
				if (this.backgroundLayer === 'BingMaps') {
					this.mapLayers.push(this.layerBingMaps);
				}
				if (this.backgroundLayer === 'OpenTopoMap') {
					this.mapLayers.push(this.layerOpenTopoMap);
				}
				if (this.backgroundLayer === 'StamenTerrain') {
					this.mapLayers.push(this.layerStamenTerrain);
				}
				this.mapLayers.push(this.layerWMS);
				this.mapLayers.push(this.layerSelectedPosition);
				this.mapLayers.push(this.layerPOIs);
				this.mapLayers.push(this.layerROIs);
			},
			updateLayer() {
				var self = this;
				this.layerMessage = "load layer ...";
				axios.get(url_rasterdb + '/' + this.layerName + '/meta.json')
					.then(function (response) {
						self.layerMessage = "update layer meta ...";
						console.log("  update   ");
						if (response.data.ref.pixel_size === undefined) {
							self.layerMessage = "ERROR no pixel_size defined for this layer";
						} else {
							self.updateLayerWithMeta(response.data);
						}
					})
					.catch(function (error) {
						console.log("+error+");
						console.log(error);
						console.log("-error-");
						self.layerMessage = "ERROR " + error;
						if (error.response !== undefined) {
							self.layerMessage = "ERROR " + error.response.data;
						}
					});


			},

			updateLayerWithMeta(meta) {
				var self = this;

				console.log(meta);
				this.layerMeta = meta;
				if (0 < this.layerMeta.wms.styles.length) {
					this.styleName = this.layerMeta.wms.styles[0].name;
				} else {
					this.styleName = undefined;
				}

				if (meta.timestamps.length > 0) {
					this.timestamp = meta.timestamps[meta.timestamps.length - 1].timestamp;
				} else {
					this.timestamp = 0;
				}

				//var crs = layerCapability.CRS[0];
				var crs = meta.ref.code;
				if (crs === undefined) {
					crs = "USER:" + meta.name;
				}
				//var boundingBox = layerCapability.BoundingBox[0];		
				//var extent = boundingBox.extent;
				var extent = meta.ref.extent;
				var proj = ol.proj.get(crs);
				console.log(crs);
				console.log(meta.ref.proj4);
				console.log(proj);
				console.log("extent");
				console.log(extent);

				if (proj == null && meta.ref.proj4 !== undefined) {
					proj4.defs(crs, meta.ref.proj4);
					proj = ol.proj.get(crs);
				}

				if (proj == null) {
					proj = new ol.proj.Projection({
						code: crs,
						units: 'm'
					});
				}

				if (proj != null) {
					/*if(proj.axisOrientation_ === "neu") {
						console.log("transpose");
						extent = [extent[1], extent[0], extent[3], extent[2]];
					}*/
					//proj.setExtent(extent);
					console.log(proj);
				}

				/*var proj= new ol.proj.Projection({
					code: 'xkcd-image',
					units: 'pixels',
					extent: extent
				});*/

				var wmsImageLoadFunction = function(image, src) {
					console.log(image);
					console.log(src);
					var cnt = self.wmsReqestCount++;
					image.wmsReqestCount = cnt;
					image.getImage().src = src + "&cnt=" + cnt;
				};

				this.sourceWMS = new ol.source.ImageWMS({
					url: url_wms,
					params: {},
					attributions: "Remote Sensing Database",
					projection: proj,
					imageLoadFunction: wmsImageLoadFunction,
					ratio: 1, // ratio of viewport
				});

				this.loadingSession++;
				this.loadingCount = 0;
				var eventLoadingSession = this.loadingSession;
				this.sourceWMS.on("imageloadstart", function () { self.onWMSimageloadstart(eventLoadingSession); });
				this.sourceWMS.on("imageloadend", function () { console.log("end"); self.onWMSimageloadend(eventLoadingSession); });
				this.sourceWMS.on("imageloaderror", function (e) { self.onWMSimageloaderror(eventLoadingSession, e); });

				this.updateSourceWMS_Params();

				this.layerWMS = new ol.layer.Image({
					extent: extent,
					source: this.sourceWMS,
					opacity: this.layerWMSopacity,
				});

				/*var layerWMSoverview = new ol.layer.Image({
					extent: extent,
					source: this.sourceWMS
				});*/

				var minRes = meta.ref.pixel_size.x / 16;
				var curRes = meta.ref.pixel_size.x;
				var maxResX = ((extent[2] - extent[0]) / 256);
				var maxResY = ((extent[3] - extent[1]) / 256);
				var maxRes = maxResX < maxResY ? maxResY : maxResX;
				if (maxRes < meta.ref.pixel_size.x * 4) {
					maxRes = meta.ref.pixel_size.x * 4;
				}
				console.log("minRes " + minRes + "  maxRes " + maxRes + "     pixel_size " + meta.ref.pixel_size.x + "    xRange " + extent[0] + " - " + extent[2] + " -> " + (extent[2] - extent[0]) + "   m per pixel " + ((extent[2] - extent[0]) / 1000) + "   m per pixel " + ((extent[2] - extent[0]) / 1000));

				var viewWMS = new ol.View({
					/*center: center,
					resolution: 40,
					minZoom: 10,
					maxZoom: 20,*/
					projection: proj,
					center: center,
					resolution: curRes,
					minResolution: minRes,
					//maxResolution: meta.ref.pixel_size * 10,
					maxResolution: maxRes,
				});
				this.viewResolutionDisplay = curRes;
				viewWMS.on("change:resolution", function (e) { self.viewResolutionDisplay = e.target.getResolution(); });

				var center = [(extent[0] + extent[2]) / 2, (extent[1] + extent[3]) / 2];
				console.log("center " + center);

				/*var viewOverview = new ol.View({
					  projection: proj,
					  center: center,
					  resolution: meta.ref.pixel_size,
					  minResolution: meta.ref.pixel_size * 2,
					  maxResolution: meta.ref.pixel_size / 2,
				});*/

				temp = viewWMS;

				console.log(viewWMS);

				viewWMS.fit(extent);

				/*this.layerOSM = new ol.layer.Tile({
					source: new ol.source.OSM({url:"https://{a-c}.tile.opentopomap.org/{z}/{x}/{y}.png"})
				});*/

				/*this.layerOSM = new ol.layer.Tile({
					source: new ol.source.OSM({url:"https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png"})
				});*/

				console.log(new ol.source.OSM());

				this.layerOSM = new ol.layer.Tile({
					source: new ol.source.OSM({ url: 'https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png' }),
					wrapX: false,
				});

				this.layerBingMaps = new ol.layer.Tile({
					source: new ol.source.BingMaps({
						key: 'AuXmtQOmKjVjD6Kse7m2WqpI8jD0eZCRdmtqX2MEQR-UoTqj2TM4C4HwuXcQ_dyp',
						imagerySet: 'AerialWithLabels',
						wrapX: false,
					})
				});

				this.layerOpenTopoMap = new ol.layer.Tile({
					title: 'OpenTopoMap',
					type: 'base',
					visible: true,
					source: new ol.source.XYZ({
						url: 'https://{a-c}.tile.opentopomap.org/{z}/{x}/{y}.png',
						attributions: 'Kartendaten: © <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>-Mitwirkende, SRTM | Kartendarstellung: © <a href="http://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)',
						wrapX: false,
					})
				});

				this.layerStamenTerrain = new ol.layer.Tile({
					source: new ol.source.Stamen({
						layer: 'terrain',
						wrapX: false,
					})
				});

				/*var overviewMap = new ol.control.OverviewMap({
					view: viewOverview,
				});*/

				this.sourcePOIs = new ol.source.Vector({
					features: [],
					wrapX: false,
				});

				var clusterPOIs = new ol.source.Cluster({
					source: this.sourcePOIs,
					wrapX: false,
				});

				var clusterPOIStyleFunction = function (clusterFeature, resolution) {
					var size = clusterFeature.get('features').length;
					if (size > 1) { // POI cluster
						var fill = new ol.style.Fill({
							color: 'rgba(255,255,255,0.4)'
						});
						var stroke = new ol.style.Stroke({
							color: '#3399CC',
							width: 1.25
						});
						var style = new ol.style.Style({
							image: new ol.style.Circle({
								fill: fill,
								stroke: stroke,
								radius: 10
							}),
							fill: fill,
							stroke: stroke,
							text: new ol.style.Text({
								text: "" + size,
								font: '15px sans-serif',
								fill: new ol.style.Fill({ color: '#ffffff' }),
							}),
						});
						return style;
					} else { // one POI
						var feature = clusterFeature.get('features')[0];
						var geometry = feature.getGeometry();
						var fill = new ol.style.Fill({
							color: 'rgba(255,255,255,0.4)'
						});
						var stroke = new ol.style.Stroke({
							color: '#AA2233',
							width: 1.25
						});
						var style = new ol.style.Style({
							image: new ol.style.Circle({
								fill: fill,
								stroke: stroke,
								radius: 5
							}),
							fill: fill,
							stroke: stroke,
							text: new ol.style.Text({
								text: feature.get('name'),
								font: '15px sans-serif',
								fill: new ol.style.Fill({ color: '#ffffff' }),
							}),
						});
						return style;
					}
				}

				this.layerPOIs = new ol.layer.Vector({
					source: clusterPOIs,
					style: clusterPOIStyleFunction
				});

				this.sourceROIs = new ol.source.Vector({
					features: [],
					wrapX: false,
				});

				var clusterROIs = new ol.source.Cluster({
					source: this.sourceROIs,
					geometryFunction: function (feature) { return feature.getGeometry().getInteriorPoint(); },
					wrapX: false,
				});

				var clusterROIStyleFunction = function (clusterFeature, resolution) {
					var size = clusterFeature.get('features').length;
					if (size > 1) { // ROI cluster
						var fill = new ol.style.Fill({
							color: 'rgba(255,255,255,0.4)'
						});
						var stroke = new ol.style.Stroke({
							color: '#3399CC',
							width: 1.25
						});
						var style = new ol.style.Style({
							image: new ol.style.Circle({
								fill: fill,
								stroke: stroke,
								radius: 10
							}),
							fill: fill,
							stroke: stroke,
							text: new ol.style.Text({
								text: "" + size,
								font: '15px sans-serif',
								fill: new ol.style.Fill({ color: '#ffffff' }),
							}),
						});
						return style;
					} else { // one ROI
						var feature = clusterFeature.get('features')[0];
						var geometry = feature.getGeometry();
						console.log("!!style Polygon");
						var fill = new ol.style.Fill({
							color: 'rgba(255,255,255,0.4)'
						});
						var stroke = new ol.style.Stroke({
							color: '#AA2233',
							width: 1.25
						});
						var style = new ol.style.Style({
							image: new ol.style.Circle({
								fill: fill,
								stroke: stroke,
								radius: 5
							}),
							fill: fill,
							stroke: stroke,
							text: new ol.style.Text({
								text: feature.get('name'),
								font: '15px sans-serif',
								fill: new ol.style.Fill({ color: '#ffffff' }),
							}),
							geometry: function (feature) {
								return geometry;
							}
						});
						return style;
					}
				}

				this.layerROIs = new ol.layer.Vector({
					source: clusterROIs,
					style: clusterROIStyleFunction
				});

				this.sourceSelectedPosition = new ol.source.Vector({
					features: [],
					wrapX: false,
				});

				var selectedPositionFill = new ol.style.Fill({
					color: 'rgba(0,255,0,0.8)'
				});
				var selectedPositionStroke = new ol.style.Stroke({
					color: 'rgba(180,0,0,1.0)',
					width: 3
				});

				var selectedPositionStyle = new ol.style.Style({
					image: new ol.style.Circle({
						fill: selectedPositionFill,
						stroke: selectedPositionStroke,
						radius: 6
					}),
					fill: selectedPositionFill,
					stroke: selectedPositionStroke,
				});

				this.layerSelectedPosition = new ol.layer.Vector({
					source: this.sourceSelectedPosition,
					style: selectedPositionStyle,
				});

				var interactionExtentBoxStyle = [
					new ol.style.Style({
						fill: new ol.style.Fill({
							color: [255, 255, 255, 0.5]
						})
					}),
					new ol.style.Style({
						stroke: new ol.style.Stroke({
							color: [50, 50, 50],
							width: 1
						})
					}),
				];
				self.interactionExtent = new ol.interaction.Extent({
					boxStyle: interactionExtentBoxStyle,
				})
				self.interactionExtent.on('extentchanged', function (e) {
					console.log(e);
					if (e.extent !== null) {
						self.selectedPosition = [NaN, NaN];
						self.selectedExtent = e.extent;
					}
				});
				self.interactionExtent.setActive(false);
				document.addEventListener('keydown', function (e) {
					if (e.keyCode == 17) { // ctrl-key
						self.interactionExtent.setActive(true);
					}
				});
				document.addEventListener('keyup', function (e) {
					if (e.keyCode == 17) { // ctrl-key
						self.interactionExtent.setActive(false);
						self.interactionExtent.createOrUpdatePointerFeature_([]);
					}
				});

				document.getElementById("map").innerHTML = "";
				var map = new ol.Map({
					layers: [],
					target: 'map',
					view: viewWMS,
					controls: ol.control.defaults({ attributionOptions: { collapsible: false } }).extend([
						new ol.control.ScaleLine(),
						/*new ol.control.FullScreen(),*/
						new ol.control.MousePosition({ coordinateFormat: function (pos) { return pos[0].toFixed(0) + " " + pos[1].toFixed(0); }, className: "ol-mouse-position ol-control" }),
						new ol.control.ZoomSlider(),
						new ol.control.Rotate(),
						//overviewMap,
					]),
					interactions: ol.interaction.defaults().extend([
						new ol.interaction.DragRotateAndZoom(),
						self.interactionExtent,
					]),
					logo: false,
					//renderer: webgl_detect()?"webgl":undefined
				});
				map.on('click', function (e) {
					if (!e.originalEvent.ctrlKey) {
						self.selectedPosition = e.coordinate;
						self.interactionExtentClear();
					}
				});
				viewWMS.on('change', function (e) {
					self.viewExtent = viewWMS.calculateExtent(map.getSize());
				});
				this.mapLayers = map.getLayers();
				this.viewWMS = viewWMS;
				this.updateMapLayers();
				this.updatePOIs();
				this.updateROIs();
				this.layerMessage = undefined;
			},

			updateSourceWMS_Params() {
				var styleParamter = this.styleName;
				if (this.rangeType !== 'auto') {
					var range = undefined;
					if (this.rangeType === 'static') {
						range = this.rangeStatic;
					} else {
						range = this.rangeTypeMap[this.rangeType];
					}
					if (range !== undefined) {
						styleParamter += " min" + range[0] + " max" + range[1];
					}
				}
				if (this.gamma !== 'auto') {
					styleParamter += " gamma" + this.gamma;
				}
				if (this.syncBands) {
					styleParamter += " sync_bands";
				}
				var params = { LAYERS: this.layerName, TIME: this.timestamp, STYLES: styleParamter };
				if(this.session !== undefined) {
					params.session = this.session;
				}
				this.sourceWMS.updateParams(params);
			},

			onWMSimageloadstart(eventLoadingSession) {
				if (this.loadingSession === eventLoadingSession) {
					this.loadingCount++;
					this.loadingErrorMessage = undefined;
				}
			},

			onWMSimageloadend(eventLoadingSession) {
				if (this.loadingSession === eventLoadingSession) {
					this.loadingCount--;
					this.loadingErrorMessage = undefined;
				}
			},

			onWMSimageloaderror(eventLoadingSession, e) {
				if (this.loadingSession === eventLoadingSession) {
					this.loadingCount--;
					if(e.image.wmsReqestCount + 1 == this.wmsReqestCount) { // current image
						this.loadingErrorMessage = "ERROR: Could not load image.";
						console.log("ERROR: Could not load image.");
					}
				}
			},

			updatePOIgroup() {
				var self = this;
				if (this.poiGroup == 'none') {
					this.pois = [];
					return;
				}
				//this.appMessage = "load POIs ...";
				axios.get(url_poi_group, { params: { name: this.poiGroup } })
					.then(function (response) {
						self.pois = response.data;
					})
					.catch(function (error) {
						self.loadingErrorMessage = "ERROR: Could not load POI group";
					});
			},

			updatePOIs() {
				console.log(this.pois);
				var features = [];
				this.pois.forEach(function (poi) {
					features.push(new ol.Feature({
						geometry: new ol.geom.Point([poi.x, poi.y]),
						name: poi.name,
					}));
				});
				this.sourcePOIs.clear();
				this.sourcePOIs.addFeatures(features);
			},

			moveToPoi() {
				var self = this;
				var poi = this.pois.find(function (p) { return p.name === self.poiName; });
				if (poi !== undefined) {
					console.log("POI " + poi.name + "    " + poi.x + "  " + poi.y);
					var pos = [poi.x, poi.y];
					this.viewWMS.setResolution(1);
					this.viewWMS.setCenter(pos);
					this.selectedPosition = pos;
					this.poiName = 'none';
				}
			},

			moveToRoi() {
				var self = this;
				var roi = this.rois.find(function (r) { return r.name === self.roiName; });
				if (roi !== undefined) {
					console.log("ROI " + roi.name + "    " + roi.x + "  " + roi.y);
					var pos = roi.center;
					this.viewWMS.setResolution(1);
					this.viewWMS.setCenter([pos[0], pos[1]]);
					this.selectedPosition = pos;
					this.roiName = 'none';
				}
			},

			updateROIgroup() {
				var self = this;
				if (this.roiGroup == 'none') {
					this.rois = [];
					return;
				}
				//this.appMessage = "load ROIs ...";
				axios.get(url_roi_group, { params: { name: this.roiGroup } })
					.then(function (response) {
						self.rois = response.data;
					})
					.catch(function (error) {
						self.loadingErrorMessage = "ERROR: Could not load ROI group";
					});
			},

			updateROIs() {
				console.log(this.rois);
				var features = [];
				this.rois.forEach(function (roi) {
					features.push(new ol.Feature({
						geometry: new ol.geom.Polygon([roi.polygon]),
						name: roi.name,
					}));
				});
				this.sourceROIs.clear();
				this.sourceROIs.addFeatures(features);
			},

			onRangeStaticSet() {
				var range = [parseFloat(this.rangeStaticMinText), parseFloat(this.rangeStaticMaxText)];
				if (range[1] < range[0]) {
					var temp = range[0];
					range[0] = range[1];
					range[1] = temp;
				}
				this.rangeStaticMinText = range[0];
				this.rangeStaticMaxText = range[1];
				this.rangeStatic = range;
			},

			updateSelectedPosition() {
				var feature = new ol.Feature({
					geometry: new ol.geom.Point(this.selectedPosition),
				});
				var features = [feature];
				this.sourceSelectedPosition.clear();
				this.sourceSelectedPosition.addFeatures(features);
			},

			/*onClickButtonPointCloud() {
				var pos = this.selectedExtent === undefined ? this.selectedPosition : this.centerOfExtent(this.selectedExtent);
				var parameters = { db: this.layerMeta.associated.PointDB, x: pos[0], y: pos[1] };
				var url = "../pointcloud_view/pointcloud_view.html#/" + this.toQuery(parameters);
				window.open(url, '_blank');
			},

			onClickButtonSurface() {
				var pos = this.selectedExtent === undefined ? this.selectedPosition : this.centerOfExtent(this.selectedExtent);
				var parameters = { db: this.layerMeta.associated.PointDB, x: pos[0], y: pos[1] };
				var url = "../height_map_view/height_map_view.html" + this.toQuery(parameters);
				window.open(url, '_blank');
			},

			onClickButtonRasterProcessing() {
				if (this.validSelectedPosition) {
					var parameters = { db: this.layerName, modus: "pos", x: this.selectedPosition[0], y: this.selectedPosition[1] };
					var url = "../raster_processing/raster_processing.html#/" + this.toQuery(parameters);
					window.open(url, '_blank');
				} else if (this.validSelectedExtent) {
					var parameters = { db: this.layerName, modus: "ext", xmin: this.selectedExtent[0], ymin: this.selectedExtent[1], xmax: this.selectedExtent[2], ymax: this.selectedExtent[3] };
					var url = "../raster_processing/raster_processing.html#/" + this.toQuery(parameters);
					window.open(url, '_blank');
				}
			},

			onClickButtonPointProcessing() {
				if (this.validSelectedPosition) {
					var parameters = { db: this.layerMeta.associated.PointDB, modus: "pos", x: this.selectedPosition[0], y: this.selectedPosition[1] };
					var url = "../point_processing/point_processing.html#/" + this.toQuery(parameters);
					window.open(url, '_blank');
				} else if (this.validSelectedExtent) {
					var parameters = { db: this.layerMeta.associated.PointDB, modus: "ext", xmin: this.selectedExtent[0], ymin: this.selectedExtent[1], xmax: this.selectedExtent[2], ymax: this.selectedExtent[3] };
					var url = "../point_processing/point_processing.html#/" + this.toQuery(parameters);
					window.open(url, '_blank');
				}
			},*/

			toQuery: function (parameters) {
				var query = "";
				for (var p in parameters) {
					if (query.length == 0) {
						query = "?";
					} else {
						query += "&";
					}
					query += p + "=" + parameters[p];
				}
				return query;
			},

			isInExtent: function (extent, pos) {
				if (extent !== undefined) {
					return extent[0] <= pos[0] && extent[1] <= pos[1] && pos[0] <= extent[2] && pos[1] <= extent[3];
				} else {
					return false;
				}
			},

			overlapExtent: function (extentA, extentB) { // derived from https://stackoverflow.com/questions/23302698/java-check-if-two-rectangles-overlap-at-any-point
				if (extentA !== undefined && extentB !== undefined) {
					return !(extentB[0] > extentA[2] || extentB[1] > extentA[3] || extentA[0] > extentB[2] || extentA[1] > extentB[3]);
				} else {
					return false;
				}
			},

			centerOfExtent: function (extent) {
				return [(extent[0] + extent[2]) / 2, (extent[1] + extent[3]) / 2];
			},

			interactionExtentClear: function () {
				this.selectedExtent = undefined;
				this.interactionExtent.setExtent();
				this.interactionExtent.createOrUpdatePointerFeature_([]);
			},

		}, // end methods

		computed: {

			viewScaleDisplay: function () {
				if (this.layerMeta.ref.pixel_size === undefined) {
					return "? : ?";
				}
				if (this.viewResolutionDisplay >= this.layerMeta.ref.pixel_size.x) {
					return "1 : " + (this.viewResolutionDisplay / this.layerMeta.ref.pixel_size.x).toFixed(1);
				} else {
					return (this.layerMeta.ref.pixel_size.x / this.viewResolutionDisplay).toFixed(1) + " : 1";
				}
			},

			validSelectedPosition: function () {
				return this.isInExtent(this.layerMeta.ref.extent, this.selectedPosition);
			},

			validSelectedExtent: function () {
				return this.overlapExtent(this.layerMeta.ref.extent, this.selectedExtent);
			},

			selectedPositionIsInView: function () {
				return this.isInExtent(this.viewExtent, this.selectedPosition);
			},

			selectedExtentIsInView: function () {
				return this.overlapExtent(this.viewExtent, this.selectedExtent);
			},

			noSelectedPosition: function () {
				return isNaN(this.selectedPosition[0]) || isNaN(this.selectedPosition[1]);
			},

			noSelectedExtent: function () {
				return this.selectedExtent === undefined;
			},

			selectedType: function() {
				if(this.validSelectedExtent) {
					return "extent";
				}
				if(this.validSelectedPosition) {
					return "position";
				}
				return "none";
			},

			filteredLayers: function() {
				var f = this.rasterdbLayers.rasterdbs;
				var tag = this.tag;
				if(tag !== '-') {
					f = f.filter(function(fun) {
						var tags = fun.tags;
						if(tags === undefined) {
							return false;
						}
						if(!Array.isArray(tags)) {
							return tags === tag;
						}
						return tags.indexOf(tag) >= 0;
					});
				}
				return f;
			},

		}, // end computed

		watch: {
			filteredLayers:function() {
				var self = this;
				var f = this.filteredLayers;
				if(f.length === 0) {
					this.layerName = undefined;
					return;
				}
				if(this.layerName === undefined) {
					this.layerName = f[0].name;
					return;
				}
				var layer = f.find(function(layer) {return layer.name === self.layerName;});
				if(layer === undefined) {
					this.layerName = f[0].name;
					return;
				}
			},

			layerName: function () {
				history.replaceState('', '', '#' + this.layerName);
				this.updateLayer();
			},

			timestamp: function () {
				this.updateSourceWMS_Params();
			},

			styleName: function () {
				this.updateSourceWMS_Params();
			},

			rangeType: function () {
				this.updateSourceWMS_Params();
			},

			rangeStatic: function () {
				this.updateSourceWMS_Params();
			},

			gamma: function () {
				this.updateSourceWMS_Params();
			},

			syncBands: function() {
				this.updateSourceWMS_Params();
			},

			backgroundLayer: function () {
				this.updateMapLayers();
			},

			layerWMSopacity: function () {
				this.layerWMS.setOpacity(this.layerWMSopacity);
			},

			poiGroup: function () {
				this.updatePOIgroup();
			},

			pois: function () {
				this.updatePOIs();
			},

			poiName: function () {
				this.moveToPoi();
			},

			roiGroup: function () {
				this.updateROIgroup();
			},

			rois: function () {
				this.updateROIs();
			},

			roiName: function () {
				this.moveToRoi();
			},

			selectedPosition: function () {
				this.updateSelectedPosition();
			}

		}, //end watch

	}); //end app

} //end init