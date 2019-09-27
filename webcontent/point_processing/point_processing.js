"use strict";
document.addEventListener('DOMContentLoaded', function () { init(); }, false);
Vue.config.productionTip = false;

var url_pointdb = '../../pointdb';
var url_pointdb_info = url_pointdb + '/info.json';
var url_pointdb_points_rdat = url_pointdb + '/points.rdat';
var url_pointdb_points_xyz = url_pointdb + '/points.xyz';
var url_pointdb_points_las = url_pointdb + '/points.las';

function init() {

  var rasterProcessingView = Vue.component('raster-processing-view', {

    template: '#raster-processing-view-template',

    data: function () {
      return {
        metaMessage: "init ... ",
        meta: {},

        areaSelections: { pos: "position", ext: "extent" },
        areaSelection: "pos",

        pointdb: undefined,
        xpos: 0,
        ypos: 0,
        width: 1,
        height: 1,

        user_xpos: 0,
        user_ypos: 0,
        user_width: 0,
        user_height: 0,

        user_xmin: 0,
        user_ymin: 0,
        user_xmax: 0,
        user_ymax: 0,

        xmin: 0,
        ymin: 0,
        xmax: 0,
        ymax: 0,


        fileFormats: { las: "LAS", xyz: "xyz", rdat: "rDAT", zip: "las file tiles in ZIP file" },
        fileFormat: "las",
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
            this.pointdb = query.db;
            this.user_xpos = query.x;
            this.user_ypos = query.y;
            this.user_width = 100;
            this.user_height = 100;
            break;
          case 'ext':
            this.areaSelection = 'ext';
            this.pointdb = query.db;
            this.user_xmin = query.xmin;
            this.user_ymin = query.ymin;
            this.user_xmax = query.xmax;
            this.user_ymax = query.ymax;
            break;
        }
        //this.$router.push({ path: '/' });
        this.loadMeta();
      },

      loadMeta: function () {
        var self = this;
        self.metaMessage = "load meta ...";
        axios.get(url_pointdb_info + '?db=' + this.pointdb + '&statistics=false')
          .then(function (response) {
            self.metaMessage = "update meta ...";
            self.meta = response.data;
            self.metaMessage = undefined;
          })
          .catch(function (error) {
            self.metaMessage = "ERROR " + error;
            if (error.response !== undefined) {
              self.metaMessage = "ERROR " + error.response.data;
            }
          });
      },

      onClickDownload: function () {
        var ext = this.xmin + ',' + this.xmax + ',' + this.ymin + ',' + this.ymax;
        var method = "unknown";
        var url = "unknown";
        switch (this.fileFormat) {
          case 'rdat':
            method = 'rdat';
            url = url_pointdb_points_rdat;
            break;
          case 'xyz':
            method = 'xyz';
            url = url_pointdb_points_xyz;
            break;
          case 'las':
            method = 'las';
            url = url_pointdb_points_las;
            break;
          default:
            console.log("unknown file format");
        }
        var parameters = { db: this.pointdb, ext: ext, format: method };
        var full_url = url + this.toQuery(parameters);
        window.open(full_url, '_blank');
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

      update_extent_by_diameter_pixel: function () {
          var xmin = this.xpos - this.width / 2;
          var ymin = this.ypos - this.height / 2;
          this.user_xmin = xmin;
          this.user_ymin = ymin;
          this.user_xmax = xmin + this.width;
          this.user_ymax = ymin + this.height;
      },

      update_pos_by_extent: function () {
          var xrange = this.xmax - this.xmin;
          var yrange = this.ymax - this.ymin;
          this.user_xpos = (this.xmin + this.xmax) / 2;
          this.user_ypos = (this.ymin + this.ymax) / 2;
          this.user_width = xrange;
          this.user_height = yrange;
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
          this.xpos = parseFloat(this.user_xpos);
      },

      user_ypos: function () {
          this.ypos = parseFloat(this.user_ypos);
      },

      width: function () {
        if (this.areaSelection === 'pos') {
          this.update_extent_by_diameter_pixel();
        }
        this.user_width = this.width;
      },

      height: function () {
        if (this.areaSelection === 'pos') {
          this.update_extent_by_diameter_pixel();
        }
        this.user_height = this.height;
      },

      user_width: function () {
          this.width = parseFloat(this.user_width);
      },

      user_height: function () {
          this.height = parseFloat(this.user_height);
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
          this.xmin = parseFloat(this.user_xmin);
      },

      user_ymin: function () {
          this.ymin = parseFloat(this.user_ymin);
      },

      user_xmax: function () {
          this.xmax = parseFloat(this.user_xmax);
      },

      user_ymax: function () {
          this.ymax = parseFloat(this.user_ymax);
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