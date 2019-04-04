<template>
<div class="consumerPanelContainer" v-if="visible" @click.self="visible = false;">
    <div class="consumerPanel">
        <button style="position: absolute; right: 0px; top: 0px;" @click="hide()"><b>X</b></button>
        <h2>Tools</h2>
        <hr/>
        <span v-if="selectedType === 'position'">
            selected position
            <b>{{selectedPosition[0].toFixed(3)}}</b>,
            <b>{{selectedPosition[1].toFixed(3)}}</b>
        </span>
        <span v-if="selectedType === 'extent'">
            selected extent
            <b>{{selectedExtent[0].toFixed(3)}}</b>,
            <b>{{selectedExtent[1].toFixed(3)}}</b>,
            <b>{{selectedExtent[2].toFixed(3)}}</b>,
            <b>{{selectedExtent[3].toFixed(3)}}</b>
        </span>
        <div v-if="layerMeta.associated.PointDB !== undefined">
            <hr> associated PointDB:
            <b>{{layerMeta.associated.PointDB}}</b>
            <table>
            <tr>
            <td><button @click="onClickButtonPointCloud" title="view at position interactive point cloud" style="width:100%">view points</button></td>
            <td>3D view of point cloud points</td>
            </tr>
            <tr>
            <td><button @click="onClickButtonSurface" title="view at position DTM (Digital Terrain Model), DSM, CHM, etc" style="width:100%">view surface</button></td>
            <td>3D view of rastered point cloud as <i>Digital Terrain Model</i> and others</td>
            </tr>
            <tr>
            <td><button @click="onClickButtonPointProcessing" title="start point processing" style="width:100%">process points</button></td>
            <td>process and download point data</td>
            </tr>
            <tr>
            <td><button @click="onClickButtonRasterPointProcessing" title="start raster processing of point data" style="width:100%">process to raster</button></td>
            <td>process and download points as raster data</td>
            </tr>
            </table>
        </div>
        <div v-if="layerMeta.associated.pointcloud !== undefined">
            <hr> associated pointcloud:
            <b>{{layerMeta.associated.pointcloud}}</b>
            <table>
            <tr>
            <td><button @click="onClickButtonPointCloud" title="view at position interactive point cloud" style="width:100%">view points</button></td>
            <td>3D view of point cloud points</td>
            </tr>
            </table>
        </div>
        <hr>
        Raster Layer: <b>{{layerMeta.name}}</b>
        <div v-if="layerMeta.associated.PointDB !== undefined" style="font-size: .8rem; color: rgb(209, 5, 5); margin-left: 50px; margin-bottom: 5px;">
            This raster layer contains an associated PointDB.
            <br>Following button just accesses pre generated visualisation raster.
        </div>
        <div v-if="layerMeta.associated.pointcloud !== undefined" style="font-size: .8rem; color: rgb(209, 5, 5); margin-left: 50px; margin-bottom: 5px;">
            This raster layer contains an associated pointcloud.
            <br>Following button just accesses pre generated visualisation raster.
        </div>
        <div>        
		<button @click="onClickButtonRasterProcessing" title="start raster processing">process raster</button> process and download raster data
        </div>
    </div>
</div>
</template>

<script>
module.exports = {

    props: {
        layerMeta: Object,
        selectedType: String,
        selectedPosition: Array,
        selectedExtent: Array,
    }, //end props

    data: function() {
        return {
           visible: false,
        }
    },

    mounted: function() {
        var self = this;
        window.addEventListener('keyup', function (e) {
            if (e.keyCode == 27) {
                self.visible = false;
            }
        });
    },
        
    methods: {

        show() {
            this.visible = true;
        },

        hide() {
            this.visible = false;
        },

        centerOfExtent: function (extent) {
				return [(extent[0] + extent[2]) / 2, (extent[1] + extent[3]) / 2];
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

        onClickButtonPointCloud() {
            var pos = this.selectedType === 'position' ? this.selectedPosition : this.centerOfExtent(this.selectedExtent);
            var parameters = {};
            if(this.layerMeta.associated.pointcloud === undefined) {
                parameters.db = this.layerMeta.associated.PointDB;
            } else {
                parameters.pointcloud = this.layerMeta.associated.pointcloud;
            }
            parameters.x = pos[0];
            parameters.y = pos[1];                
            var url = "../pointcloud_view/pointcloud_view.html#/" + this.toQuery(parameters);           
            window.open(url, '_blank');
        },

        onClickButtonSurface() {
            var pos = this.selectedType === 'position' ? this.selectedPosition : this.centerOfExtent(this.selectedExtent);
            var parameters = { db: this.layerMeta.associated.PointDB, x: pos[0], y: pos[1] };
            var url = "../height_map_view/height_map_view.html" + this.toQuery(parameters);
            window.open(url, '_blank');
        },

        onClickButtonRasterProcessing() {
            if (this.selectedType === 'position') {
                var parameters = { db: this.layerMeta.name, modus: "pos", x: this.selectedPosition[0], y: this.selectedPosition[1] };
                var url = "../raster_processing/raster_processing.html#/" + this.toQuery(parameters);
                window.open(url, '_blank');
            } else if (this.selectedType === 'extent') {
                var parameters = { db: this.layerMeta.name, modus: "ext", xmin: this.selectedExtent[0], ymin: this.selectedExtent[1], xmax: this.selectedExtent[2], ymax: this.selectedExtent[3] };
                var url = "../raster_processing/raster_processing.html#/" + this.toQuery(parameters);
                window.open(url, '_blank');
            }
        },

        onClickButtonPointProcessing() {
            if (this.selectedType === 'position') {
                var parameters = { db: this.layerMeta.associated.PointDB, modus: "pos", x: this.selectedPosition[0], y: this.selectedPosition[1] };
                var url = "../point_processing/point_processing.html#/" + this.toQuery(parameters);
                window.open(url, '_blank');
            } else if (this.selectedType === 'extent') {
                var parameters = { db: this.layerMeta.associated.PointDB, modus: "ext", xmin: this.selectedExtent[0], ymin: this.selectedExtent[1], xmax: this.selectedExtent[2], ymax: this.selectedExtent[3] };
                var url = "../point_processing/point_processing.html#/" + this.toQuery(parameters);
                window.open(url, '_blank');
            }
        },

        onClickButtonRasterPointProcessing() {
            if (this.selectedType === 'position') {
                var parameters = { db: this.layerMeta.associated.PointDB, modus: "pos", x: this.selectedPosition[0], y: this.selectedPosition[1] };
                var url = "../raster_point_processing/raster_point_processing.html#/" + this.toQuery(parameters);
                window.open(url, '_blank');
            } else if (this.selectedType === 'extent') {
                var parameters = { db: this.layerMeta.associated.PointDB, modus: "ext", xmin: this.selectedExtent[0], ymin: this.selectedExtent[1], xmax: this.selectedExtent[2], ymax: this.selectedExtent[3] };
                var url = "../raster_point_processing/raster_point_processing.html#/" + this.toQuery(parameters);
                window.open(url, '_blank');
            }
        },

    },

}
</script>

<style>
.consumerPanelContainer {
	display: flex;
	position: absolute;
	top: 0px;
	left: 0px;
	height: 100%;
    width: 100%;
    pointer-events: auto;
	background-color: rgba(0, 0, 0, 0.75);
}

.consumerPanel {
    margin: auto;
    position: relative;
	background-color: rgba(230, 226, 218, 0.85);
	padding: 5px;
	font-size: 1rem;
	border-radius: 4px;
    font-family: sans-serif;
    border-style: solid;
    border-width: 3px;
    border-color: rgb(105, 105, 105);
}

.consumerPanel h2 {
    margin-top: 0px;
    margin-bottom: 0px;
    margin-left: 32px;
    margin-right: 32px;
}
</style>