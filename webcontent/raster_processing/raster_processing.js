"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);
Vue.config.productionTip = false;

var url_rasterdb = '../../rasterdb';

function init() {

  var rasterProcessingView = Vue.component('raster-processing-view', {

    template: '#raster-processing-view-template',

    data: function () {
      return {
        metaMessage: "init ... ",
        meta: {},

        areaSelections: { pos: "position", ext: "extent" },
        areaSelection: "pos",

        rasterdb: undefined,
        xpos: 0,
        ypos: 0,

        user_xpos: 0,
        user_ypos: 0,
        user_width: 0,
        user_height: 0,

        user_xmin: 0,
        user_ymin: 0,
        user_xmax: 0,
        user_ymax: 0,

        width: 1,
        height: 1,
        xmin: 0,
        ymin: 0,
        xmax: 0,
        ymax: 0,
        diameter_geo_x: 0,
        diameter_geo_y: 0,

        timestampSelection: undefined,

        dataSelectionTypes: { all: "all bands", one: "one band", multi: "multiple bands", processing: "processing"},
        dataSelectionType: "all",
        oneBandSelection: undefined,
        multiBandSelections: [],
        processingFormula: "ndvi",

        fileFormats: { tiff: "GeoTIFF", png: "PNG", rdat: "rDAT" },
        fileFormat: "tiff",

        visualisation: false,
      };
    },

    mounted: function () {
      this.loadUrlParameters();
    },

    methods: {

      loadUrlParameters: function () {
        var query = this.$route.query;
        switch (query.modus) {
          case 'pos':
            this.areaSelection = 'pos';
            this.rasterdb = query.db;
            this.user_xpos = query.x;
            this.user_ypos = query.y;
            break;
          case 'ext':
            this.areaSelection = 'ext';
            this.rasterdb = query.db;
            this.user_xmin = query.xmin;
            this.user_ymin = query.ymin;
            this.user_xmax = query.xmax;
            this.user_ymax = query.ymax;
            break;
        }
        //this.$router.push({ path: '/' });
        this.loadMeta();
      },

      onClickDownload: function () {
        var ext = this.xmin + ' ' + this.ymin + ' ' + this.xmax + ' ' + this.ymax;
        var parameters = { ext: ext };
        if(this.timestampSelection !== undefined) {
          parameters.timestamp = this.timestampSelection;
        }
        switch(this.dataSelectionType) {
          case "all":
            //nothing
            break;
          case "one":
            parameters.band = this.oneBandSelection;
            break;
            case "multi":
            parameters.band = this.multiBandSelections.join(' ');
            break;
            case "processing":
            parameters.product = this.processingFormula;
            break;
          default:
          console.log("unknown selection type: " + this.dataSelectionType);
        }
        if(this.visualisation && this.fileFormat === 'tiff') {
          parameters.visualisation = true;
        }
        var method = "unknown";
        switch (this.fileFormat) {
          case 'tiff':
            method = 'raster.tiff';
            break;
            case 'rdat':
            method = 'raster.rdat';
            break;
            case 'png':
            method = 'raster.png';
            break;
            default:
            console.log("unknown file format: " + this.fileFormat);
        }
        var url = url_rasterdb + '/' + this.rasterdb + '/' + method + this.toQuery(parameters);
        window.open(url, '_blank');
      },

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

      loadMeta: function () {
        var self = this;
        self.metaMessage = "load meta ...";
        axios.get(url_rasterdb + '/' + this.rasterdb + '/meta.json')
          .then(function (response) {
            self.metaMessage = "update meta ...";
            if (response.data.ref.pixel_size === undefined) {
              self.metaMessage = "ERROR no pixel_size defined";
            } else {
              self.meta = response.data;
              if(self.meta.timestamps.length > 1) {
                self.timestampSelection = self.meta.timestamps[self.meta.timestamps.length-1].datetime;
              } else {
                self.timestampSelection = undefined;
              }
              self.oneBandSelection = self.meta.bands[0].index;
              switch (self.areaSelection) {
                case 'pos': {
                  self.width = 1000;
                  self.height = 1000;
                  break;
                }
                case 'ext': {
                  self.update_pos_by_extent();
                  break;
                }
              }
              self.metaMessage = undefined;
            }
          })
          .catch(function (error) {
            self.metaMessage = "ERROR " + error;
            if (error.response !== undefined) {
              self.metaMessage = "ERROR " + error.response.data;
            }
          });
      },

      update_extent_by_diameter_pixel: function () {
        if (this.meta.ref !== undefined) {
          var x = this.xpos;
          var y = this.ypos;
          var sx = this.meta.ref.pixel_size.x;
          var sy = this.meta.ref.pixel_size.y;
          var dx = this.width * sx;
          var dy = this.height * sy;
          this.xmin = x - dx / 2;
          this.ymin = y - dy / 2;
          this.xmax = this.xmin + dx - sx; // ??
          this.ymax = this.ymin + dy - sy; // ??
        }
      },

      update_pos_by_extent: function () {
        if (this.meta.ref !== undefined) {
          this.xpos = (this.xmin + this.xmax) / 2;
          this.ypos = (this.ymin + this.ymax) / 2;
          var xrange = this.xmax - this.xmin;
          var yrange = this.ymax - this.ymin;
          this.width = Math.floor(xrange / this.meta.ref.pixel_size.x);
          this.height = Math.floor(yrange / this.meta.ref.pixel_size.y);
        }
      },

    }, //end methods

    watch: {

      xpos: function () {
        if (this.areaSelection === 'pos') {
          this.update_extent_by_diameter_pixel();
        }
        this.user_xpos = this.xpos;
      },

      ypos: function () {
        if (this.areaSelection === 'pos') {
          this.update_extent_by_diameter_pixel();
        }
        this.user_ypos = this.ypos;
      },

      user_xpos: function () {
        if (this.areaSelection === 'pos') {
          this.xpos = parseFloat(this.user_xpos);
        }
      },

      user_ypos: function () {
        if (this.areaSelection === 'pos') {
          this.ypos = parseFloat(this.user_ypos);
        }
      },

      width: function () {
        this.diameter_geo_x = this.width * this.meta.ref.pixel_size.x;
        if (this.areaSelection === 'pos') {
          this.update_extent_by_diameter_pixel();
        }
        this.user_width = this.width;
      },

      height: function () {
        this.diameter_geo_y = this.height * this.meta.ref.pixel_size.y;
        if (this.areaSelection === 'pos') {
          this.update_extent_by_diameter_pixel();
        }
        this.user_height = this.height;
      },

      user_width: function () {
        if (this.areaSelection === 'pos') {
          this.width = parseFloat(this.user_width);
        }
      },

      user_height: function () {
        if (this.areaSelection === 'pos') {
          this.height = parseFloat(this.user_height);
        }
      },

      xmin: function () {
        if (this.areaSelection === 'ext') {
          this.update_pos_by_extent();
        }
        this.user_xmin = this.xmin;
      },

      ymin: function () {
        if (this.areaSelection === 'ext') {
          this.update_pos_by_extent();
        }
        this.user_ymin = this.ymin;
      },

      xmax: function () {
        if (this.areaSelection === 'ext') {
          this.update_pos_by_extent();
        }
        this.user_xmax = this.xmax;
      },

      ymax: function () {
        if (this.areaSelection === 'ext') {
          this.update_pos_by_extent();
        }
        this.user_ymax = this.ymax;
      },

      user_xmin: function () {
        if (this.areaSelection === 'ext') {
          this.xmin = parseFloat(this.user_xmin);
        }
      },

      user_ymin: function () {
        if (this.areaSelection === 'ext') {
          this.ymin = parseFloat(this.user_ymin);
        }
      },

      user_xmax: function () {
        if (this.areaSelection === 'ext') {
          this.xmax = parseFloat(this.user_xmax);
        }
      },

      user_ymax: function () {
        if (this.areaSelection === 'ext') {
          this.ymax = parseFloat(this.user_ymax);
        }
      },

    },  //end watch

  }); //end rasterProcessingView


  var routes = [
    { path: '/', component: rasterProcessingView },
  ]

  var router = new VueRouter({
    routes
  });

  var app = new Vue({
    router
  }).$mount('#app')

} //end init