"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);
Vue.config.productionTip = false;

var url_catalog = '../../api/catalog.json';

var temp;

function init() {

	var app = new Vue({

		el: '#app',

		data: {
			appMessage: "init app ...",
			catalog: [],
			hoverEntries: [],
			backgrounds: ["OpenStreetMap", "OpenTopoMap", "StamenTerrain"],
			background: "OpenStreetMap",

			map: undefined,
			catalogLayer: undefined,
			layerOpenTopoMap: undefined,
			layerOpenStreetMap: undefined,
			layerStamenTerrain: undefined,

			selectedLayersDialogShow: false,
			selectedEntries: [],

			sourceCatalog: undefined,

			tags: [],
			selectedTag: '-',
			selectedType: '-',

			associatedPointdbRasterdbMap: {},
			associatedPointcloudRasterdbMap: {},
		}, //end data

		mounted: function () {
			var self = this;
			this.appMessage = "init openlayers";
			this.loadCatalog();
			window.addEventListener('keyup', function (e) {
				if (e.keyCode == 27) {
					console.log(e);
					self.selectedLayersDialogShow = false;
				}
			});
		}, //end mounted

		methods: {
			loadCatalog: function () {
				var self = this;

				self.tags = [];
				axios.get('../../api/layer_tags')
                .then(function(response) {
                    self.tags = response.data.layer_tags;
                    console.log(self.tags);
                })
                .catch(function(error) {
                    console.log(error);
                });

				this.appMessage = "load catalog ...";
				axios.get(url_catalog)
					.then(function (response) {
						var catalog = response.data;
						catalog.sort(function (a, b) {
							var nameA = a.name.toLowerCase();
							var nameB = b.name.toLowerCase();
							if (nameA < nameB) {
								return -1;
							}
							if (nameA > nameB) {
								return 1;
							}
							return 0;
						});
						self.catalog = catalog;
						self.appMessage = undefined;
						var aPointdbMap = {};
						var aPointcloudMap = {};
						self.catalog.forEach(function (entry) {
							if (entry.type === 'RasterDB' && entry.associated !== undefined) {
								if (entry.associated.PointDB !== undefined) {
									var pointdb = entry.associated.PointDB;
									var rasterdb = entry.name;
									console.log(pointdb);
									aPointdbMap[pointdb] = rasterdb;
								}
								if (entry.associated.pointcloud !== undefined) {
									var pointcloud = entry.associated.pointcloud;
									var rasterdb = entry.name;
									console.log(pointcloud);
									aPointcloudMap[pointcloud] = rasterdb;
								}
							}						
						});
						self.associatedPointdbRasterdbMap = aPointdbMap;
						self.associatedPointcloudRasterdbMap = aPointcloudMap;
					})
					.catch(function (error) {
						self.appMessage = "ERROR " + error;
					});
			},
			refresh: function () {
				var self = this;
				self.appMessage = "create view ...";
				
				var fill = new ol.style.Fill({
					color: 'rgba(255,255,255,0.15)'
					//color: 'rgba(255,255,255,0.0)'
				});
				var stroke = new ol.style.Stroke({
					color: 'rgb(24, 56, 149)',
					width: 2,
					/*lineDash: [1, 5],*/
				});
				var styles = [
					new ol.style.Style({
						image: new ol.style.Circle({
							fill: fill,
							stroke: stroke,
							radius: 5
						}),
						fill: fill,
						stroke: stroke
					})
				];

				this.catalogLayer = new ol.layer.Vector({
					style: styles,
				});

				this.layerOpenTopoMap = new ol.layer.Tile({
					title: 'OpenTopoMap',
					type: 'base',
					visible: true,
					source: new ol.source.XYZ({
						url: 'https://{a-c}.tile.opentopomap.org/{z}/{x}/{y}.png',
						attributions: 'Kartendaten: © <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>-Mitwirkende, SRTM | Kartendarstellung: © <a href="http://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)',
						wrapX: false,
					}),
				});

				this.layerOpenStreetMap = new ol.layer.Tile({
					source: new ol.source.OSM({ wrapX: false })
				});

				this.layerStamenTerrain = new ol.layer.Tile({
					source: new ol.source.Stamen({
						layer: 'terrain',
						wrapX: false,
					})
				});

				this.map = new ol.Map({
					layers: [],
					target: 'map',
					view: new ol.View({
						//projection: 'EPSG:4326',
						projection: 'EPSG:3857', // Web Mercator default
						center: [0, 0],
						zoom: 2
					}),
					controls: ol.control.defaults({ attributionOptions: { collapsible: false } }),
				});

				var dragBox = new ol.interaction.DragBox({
					condition: ol.events.condition.platformModifierKeyOnly,
					style: styles,
				});
				dragBox.on('boxend', function (a) {
					console.log(a);
					var entries = [];
					var extent = dragBox.getGeometry().getExtent();
					self.sourceCatalog.forEachFeatureIntersectingExtent(extent, function (feature) {
						entries.push(feature.get("entry"));
					});
					entries.sort(function (a, b) {
						var nameA = a.name.toLowerCase();
						var nameB = b.name.toLowerCase();
						if (nameA < nameB) {
							return -1;
						}
						if (nameA > nameB) {
							return 1;
						}
						return 0;
					});
					self.selectedEntries = entries;
					self.selectedLayersDialogShow = true;
				});
				this.map.addInteraction(dragBox);

				this.map.on('click', function (e) {
					var entries = [];
					var features = self.sourceCatalog.getFeaturesAtCoordinate(e.coordinate);
					features.forEach(function (feature) {
						entries.push(feature.get("entry"));
					});
					entries.sort(function (a, b) {
						var nameA = a.name.toLowerCase();
						var nameB = b.name.toLowerCase();
						if (nameA < nameB) {
							return -1;
						}
						if (nameA > nameB) {
							return 1;
						}
						return 0;
					});
					self.selectedEntries = entries;
					self.selectedLayersDialogShow = true;
				});

				this.map.on('pointermove', function (e) {
					var entries = [];
					var features = self.sourceCatalog.getFeaturesAtCoordinate(e.coordinate);
					features.forEach(function (feature) {
						entries.push(feature.get("entry"));
					});
					entries.sort(function (a, b) {
						var nameA = a.name.toLowerCase();
						var nameB = b.name.toLowerCase();
						if (nameA < nameB) {
							return -1;
						}
						if (nameA > nameB) {
							return 1;
						}
						return 0;
					});
					self.hoverEntries = entries;
				});

				this.updateViewLayers();
				self.appMessage = undefined;

				this.updateCatalogLayer();
			},

			updateViewLayers: function () {
				var layers = this.map.getLayers();
				layers.clear();
				switch (this.background) {
					case "OpenTopoMap":
						layers.push(this.layerOpenTopoMap);
						break;
					case "OpenStreetMap":
						layers.push(this.layerOpenStreetMap);
						break;
					case "StamenTerrain":
						layers.push(this.layerStamenTerrain);
						break;
					default:
						layers.push(this.layerOpenStreetMap);
				}
				layers.push(this.catalogLayer);
			},

			onSelectAllLayers: function () {
				this.selectedEntries = this.filteredCatalog;
				this.selectedLayersDialogShow = true;
			},

			getLinkOfEntry: function (entry) {
				switch (entry.type) {
					case "RasterDB":
						return '../rasterdb_wms/rasterdb_wms.html#' + entry.name;
					case "PointDB":
						if (this.associatedPointdbRasterdbMap[entry.name] !== undefined) {
							return '../rasterdb_wms/rasterdb_wms.html#' + this.associatedPointdbRasterdbMap[entry.name];
						} else {
							return '../pointdb_map/pointdb_map.html#' + entry.name;
						}
					case "pointcloud":
						if (this.associatedPointcloudRasterdbMap[entry.name] !== undefined) {
							return '../rasterdb_wms/rasterdb_wms.html#' + this.associatedPointcloudRasterdbMap[entry.name];
						} else {
							return undefined;
						}
					default:
						return undefined;
				}
			},

			updateCatalogLayer: function() {
				if(this.catalogLayer == undefined) {
					return;
				}
				console.log("updateCatalogLayer");

				var src = ol.proj.get('EPSG:4326'); // WGS84
				var dst = ol.proj.get('EPSG:3857'); // Web Mercator
				console.log(dst);

				var features = [];
				var c = this.filteredCatalog === undefined ? this.catalog : this.filteredCatalog;
				c.forEach(function (entry) {
					var polygon = entry.polygon;
					if (polygon !== undefined) {
						var geometry = new ol.geom.Polygon([polygon]);
						geometry.transform(src, dst);
						features.push(new ol.Feature({
							geometry: geometry,
							entry: entry,
						}));
					}
				});

				this.sourceCatalog = new ol.source.Vector({
					features: features,
					wrapX: false,
				});

				this.catalogLayer.setSource(this.sourceCatalog);

			},

			createTagFilterFun: function()  {
				var tag = this.selectedTag;
				if(tag === '-' || tag === '' || tag === undefined) {
					return function (e) {
						return true;
					}
				}
				return function(e) {
					var tags = e.tags;
					if(tags === undefined) {
						return false;
					}
					if(!Array.isArray(tags)) {
						return tags === tag;
					}
					return tags.indexOf(tag) >= 0;
				}
			},

			createTypeFilterFun: function()  {
				var type = this.selectedType;
				if(type === '-' || type === '' || type === undefined) {
					return function (e) {
						return true;
					}
				}
				return function(e) {
					var t = e.type;
					if(t === undefined) {
						return false;
					}
					return t === type;
				}
			},

			createAND: function(fun1, fun2) {
				return function(e) {
					return fun1(e) && fun2(e);
				}
			},

		}, //end methods

		computed: {

			filteredCatalog: function() {
				console.log("filteredCatalog");
				return this.catalog.filter(this.createAND(this.createTagFilterFun(), this.createTypeFilterFun()));
			}

		},

		watch: {

			catalog: function () {
				this.refresh();
			},

			background: function () {
				this.updateViewLayers();
			},

			filteredCatalog: function() {
				console.log("watch filteredCatalog");
				this.updateCatalogLayer();
			}

		}, //end watch

	}); //end app

} //end init