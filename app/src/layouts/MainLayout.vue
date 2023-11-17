<template>
  <div id="map" :style="{ cursor: mapCursor }"></div>

  <div id="overlay">
    <FeatureInfo ref="FeatureInfo" />
    <q-icon
      :name="overlayExpand ? 'expand_less' : 'expand_more'"
      @click="overlayExpand = !overlayExpand"
      style="position: absolute; pointer-events: auto; cursor: pointer"
      color="black"
    />
    <div
      style="
        position: absolute;
        right: 300px;
        pointer-events: auto;
        background-color: rgba(255, 255, 255, 0.932);
        border: 1px solid rgba(0, 0, 0, 0.24);
        padding: 5px;
      "
      v-if="interaction !== undefined"
    >
      <div v-if="interaction.type === 'Pointcloud3dPointView'">
        <div
          style="
            font-size: 0.75em;
            text-align: center;
            background-color: #dbdbdb;
          "
        >
          <b>Pointcloud 3d-view selection</b> of
          <i>{{ interaction.pointcloud }}</i>
        </div>
        <b>Click</b> on a position on the map to 3D-view pointcloud points.
        <i>--- OR ---</i>
        <br />
        Hold <b>shift-key</b>, hold <b>left mouse button</b> and move mouse to
        draw a box on the map, then
        <q-btn
          title="Open 3d-view of box selected points."
          :disable="selectedExtent === undefined"
          @click="onPointcloud3dPointViewBboxClick"
          >click here</q-btn
        >
        to 3D-view.
      </div>
      <span v-else>{{ interaction.type }}</span>
      <q-btn
        dense
        flat
        icon="close"
        @click="interaction = undefined"
        style="
          position: absolute;
          top: 0px;
          right: 0px;
          background-color: #eaeaea;
          pointer-events: auto;
        "
      >
        <q-tooltip class="bg-white text-primary">Cancel</q-tooltip>
      </q-btn>
    </div>
    <div
      class="q-pa-md"
      style="
        max-width: 350px;
        pointer-events: auto;
        background-color: rgba(255, 255, 255, 0.932);
        border: 1px solid rgba(0, 0, 0, 0.24);
      "
      v-show="overlayExpand"
    >
      <q-list bordered separator>
        <draggable
          v-model="layers"
          item-key="name"
          @update="refreshLayers(true)"
        >
          <template #item="{ element, index }">
            <div>
              <q-item clickable v-ripple>
                <template v-if="element.type === 'postgis'">
                  <q-item-section side top
                    ><q-btn
                      size="sm"
                      flat
                      round
                      color="grey"
                      icon="delete_forever"
                      @click.stop="
                        layers.splice(index, 1);
                        refreshLayers(true);
                        //console.log('done!');
                      "
                  /></q-item-section>
                  <q-item-section>
                    <q-item-label caption>PostGIS</q-item-label>
                    <q-item-label class="ellipsis">{{
                      element.name
                    }}</q-item-label>
                  </q-item-section>
                  <q-item-section side top
                    ><q-checkbox
                      size="xs"
                      v-model="element.visible"
                      val="xs"
                      color="grey"
                      @update:model-value="refreshLayerVisibility"
                  /></q-item-section>
                </template>
                <template v-else-if="element.type === 'rasterdb'">
                  <q-item-section side top
                    ><q-btn
                      size="sm"
                      flat
                      round
                      color="grey"
                      icon="delete_forever"
                      @click.stop="
                        layers.splice(index, 1);
                        refreshLayers(true);
                        //console.log('done!');
                      "
                  /></q-item-section>
                  <q-item-section>
                    <q-item-label caption>RasterDB</q-item-label>
                    <q-item-label class="ellipsis">{{
                      element.name
                    }}</q-item-label>
                  </q-item-section>
                  <q-item-section side top
                    ><q-checkbox
                      size="xs"
                      v-model="element.visible"
                      val="xs"
                      color="grey"
                      @update:model-value="refreshLayerVisibility"
                  /></q-item-section>
                </template>
                <template v-else-if="element.type === 'background'">
                  <q-item-section>
                    <q-item-label caption>Background</q-item-label>
                    <q-item-label class="ellipsis">{{
                      element.name
                    }}</q-item-label>
                  </q-item-section>
                  <q-item-section side top
                    ><q-checkbox
                      size="xs"
                      v-model="element.visible"
                      val="xs"
                      color="grey"
                      @update:model-value="refreshLayerVisibility"
                  /></q-item-section>
                </template>
                <template v-else>
                  <q-item-section>
                    <q-item-label caption
                      >{{ element.type }} layer</q-item-label
                    >
                    <q-item-label class="ellipsis">{{
                      element.name
                    }}</q-item-label>
                  </q-item-section>
                  <q-item-section side top
                    ><q-checkbox
                      size="xs"
                      v-model="element.visible"
                      val="xs"
                      color="grey"
                      @update:model-value="refreshLayerVisibility"
                  /></q-item-section>
                </template>
                <q-menu>
                  <q-list style="min-width: 300px">
                    <q-item>
                      <q-item-section>
                        <q-item-label overline>
                          Opacity
                          <span v-show="!element.visible" style="color: red">
                            (not visible)
                          </span>
                        </q-item-label>
                        <q-item-label>
                          <q-slider
                            v-model="element.opacity"
                            :min="0"
                            :max="100"
                            @update:model-value="
                              element.layer.setOpacity(element.opacity / 100)
                            "
                          />
                          <q-btn
                            flat
                            round
                            icon="crop_free"
                            title="Zoom to layer"
                            @click="onZoomToLayer(element)"
                          />
                        </q-item-label>
                        <div v-if="element.type === 'background'">
                          <q-select
                            v-model="backgroundMap"
                            :options="backgroundMaps"
                            label="Background map"
                            outlined
                            stack-label
                            dense
                            option-label="name"
                          />
                        </div>
                        <div v-if="element.type === 'postgis'">
                          <img
                            :src="element.wmsURL + '?REQUEST=GetLegendGraphic'"
                            style="border: 1px solid rgba(0, 0, 0, 0.151)"
                          />
                        </div>
                        <RasterLayerSettings
                          v-if="element.type === 'rasterdb'"
                          :layer="element"
                          @changeTimeSlice="
                            (timeSlice) => {
                              element.timeSlice = timeSlice;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeStyle="
                            (style) => {
                              element.style = style;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeFixedGamma="
                            (fixedGamma) => {
                              element.fixedGamma = fixedGamma;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeGammaStep="
                            (gammaStep) => {
                              element.gammaStep = gammaStep;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeFixedRange="
                            (fixedRange) => {
                              element.fixedRange = fixedRange;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeRangeMin="
                            (rangeMin) => {
                              element.rangeMin = rangeMin;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeRangeMax="
                            (rangeMax) => {
                              element.rangeMax = rangeMax;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @changeSyncBands="
                            (syncBands) => {
                              element.syncBands = syncBands;
                              refreshRasterdbLayerParameters(element);
                            }
                          "
                          @interaction="
                            interaction = $event;
                            interaction.layer = element;
                          "
                        />
                      </q-item-section>
                    </q-item>
                  </q-list>
                </q-menu>
              </q-item>
              <q-separator />
            </div>
          </template>
        </draggable>
      </q-list>

      <q-btn
        @click="$refs.AddLayer.dialog = true"
        icon="add_to_photos"
        title="Add a layer to the list of viewer layers."
      >
        <AddLayer ref="AddLayer" @close="refreshLayers()"></AddLayer>
      </q-btn>
    </div>
    <div
      v-if="initializingLayers"
      style="
        position: absolute;
        top: 50px;
        left: 700px;
        background-color: #eaeaea;
        padding: 10px;
      "
    >
      <q-spinner-tail color="grey" size="2em" />
      Initializing layers ...
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { mapWritableState } from "pinia";
import { mapActions } from "pinia";
import { useLayersStore } from "../stores/layers-store";

import { Map, View } from "ol";
import { Image as ImageLayer, Tile as TileLayer, Layer } from "ol/layer";
import { OSM } from "ol/source";
import ImageWMS from "ol/source/ImageWMS";
import Projection from "ol/proj/Projection";
import Attribution from "ol/control/Attribution";
import ScaleLine from "ol/control/ScaleLine";
import { createStringXY } from "ol/coordinate";
import MousePosition from "ol/control/MousePosition";
import { defaults as interactionDefaults } from "ol/interaction/defaults";
import ExtentInteraction from "ol/interaction/Extent";
import { shiftKeyOnly } from "ol/events/condition";
import { transformExtent } from "ol/proj";
import proj4 from "proj4";
import { register } from "ol/proj/proj4";
import draggable from "vuedraggable";

import AddLayer from "../components/AddLayer.vue";
import FeatureInfo from "../components/FeatureInfo.vue";
import RasterLayerSettings from "../components/RasterLayerSettings.vue";

export default defineComponent({
  name: "MainLayout",

  components: { draggable, AddLayer, FeatureInfo, RasterLayerSettings },

  data() {
    return {
      overlayExpand: true,
      map: undefined,
      view: undefined,
      router: useRouter(),
      route: useRoute(),
      session: Math.floor(Math.random() * 1000000000),
      sessionCnt: 0,
      drag: false,
      initializingLayers: false,
      mainLayer: undefined,
      backgroundMap: undefined,
      backgroundMaps: undefined,
      mapCursor: "auto",
      mapCursorStandard: "auto",
      interaction: undefined,
      selectedExtent: undefined,
    };
  },

  computed: {
    ...mapWritableState(useLayersStore, { layers: "layers" }),
  },

  methods: {
    ...mapActions(useLayersStore, { layersInit: "init" }),

    completeLayer(layerEntry) {
      if (layerEntry.visible === undefined) {
        layerEntry.visible = true;
      }
      if (layerEntry.opacity === undefined) {
        layerEntry.opacity = 100;
      }
    },

    async completePostgisLayer(layerEntry) {
      try {
        if (layerEntry.postgisLayerURLPart === undefined) {
          layerEntry.postgisLayerURLPart = "postgis/layers/" + layerEntry.name;
        }

        if (layerEntry.meta === undefined) {
          const response = await this.$api.get(layerEntry.postgisLayerURLPart);
          layerEntry.meta = response.data;
        }

        if (layerEntry.extent === undefined) {
          layerEntry.extent = [
            layerEntry.meta.extent.xmin,
            layerEntry.meta.extent.ymin,
            layerEntry.meta.extent.xmax,
            layerEntry.meta.extent.ymax,
          ];
        }

        if (layerEntry.wmsURL === undefined) {
          layerEntry.wmsURL =
            this.$api.getUri() + layerEntry.postgisLayerURLPart + "/wms";
        }

        if (layerEntry.layer === undefined) {
          layerEntry.layer = new ImageLayer({
            extent: layerEntry.extent,
            source: new ImageWMS({
              url: layerEntry.wmsURL,
              ratio: 1,
              interpolate: false,
              imageLoadFunction: (image, src) => {
                const srcUrl =
                  src +
                  "&session=" +
                  this.session +
                  "&cnt=" +
                  this.sessionCnt++;
                image.getImage().src = srcUrl;
              },
              params: {},
            }),
          });
        }

        if (layerEntry.projection === undefined) {
          const srs = "EPSG" + layerEntry.meta.epsg;

          proj4.defs(srs, layerEntry.meta.wkt_srs);
          register(proj4);

          layerEntry.projection = new Projection({
            code: srs,
            units: "m",
          });
        }
      } catch (e) {
        console.log(e);
      }
    },

    async completeRasterdbLayer(layerEntry) {
      try {
        if (layerEntry.rasterdbLayerURLPart === undefined) {
          layerEntry.rasterdbLayerURLPart = "rasterdb/" + layerEntry.name;
        }

        if (layerEntry.meta === undefined) {
          const response = await this.$api.get(
            layerEntry.rasterdbLayerURLPart + "/meta.json"
          );
          layerEntry.meta = response.data;

          layerEntry.meta.time_slices.forEach((element, i) => {
            element.index = i;
          });

          layerEntry.meta.wms.styles.forEach((element, i) => {
            element.index = i;
          });

          layerEntry.fixedGamma = false;
          layerEntry.gammaStep = 4;

          layerEntry.fixedRange = false;
          layerEntry.rangeMin = 0;
          layerEntry.rangeMax = 255;

          layerEntry.syncBands = false;

          let bandMap = {};
          layerEntry.meta.bands.forEach((band) => (bandMap[band.index] = band));
          layerEntry.bandMap = bandMap;
        }

        if (layerEntry.extent === undefined) {
          layerEntry.extent = [
            layerEntry.meta.ref.extent[0],
            layerEntry.meta.ref.extent[1],
            layerEntry.meta.ref.extent[2],
            layerEntry.meta.ref.extent[3],
          ];
        }

        /*const resX = layerEntry.meta.ref.pixel_size.x;
        const resY = layerEntry.meta.ref.pixel_size.y;
        const res = resX <= resY ? resX : resY;*/

        if (layerEntry.wmsURL === undefined) {
          layerEntry.wmsURL =
            this.$api.getUri() + layerEntry.rasterdbLayerURLPart + "/wms";
        }

        if (layerEntry.style === undefined) {
          layerEntry.style = layerEntry.meta.wms.styles[0];
        }

        if (layerEntry.timeSlice === undefined) {
          layerEntry.timeSlice = layerEntry.meta.time_slices[0];
        }

        if (layerEntry.layer === undefined) {
          layerEntry.layer = new ImageLayer({
            extent: layerEntry.extent,
            //minResolution: res,
            source: new ImageWMS({
              url: layerEntry.wmsURL,
              ratio: 1,
              interpolate: false,
              /*resolutions: [
                res,
                res * 2,
                res * 4,
                res * 8,
                res * 16,
                res * 32,
                res * 64,
                res * 128,
                res * 256,
              ],*/
              imageLoadFunction: (image, src) => {
                const srcUrl =
                  src +
                  "&session=" +
                  this.session +
                  "&cnt=" +
                  this.sessionCnt++;
                image.getImage().src = srcUrl;
              },
              params: {},
            }),
          });
          this.refreshRasterdbLayerParameters(layerEntry);
        }

        if (layerEntry.projection === undefined) {
          //const srs = layerEntry.meta.ref.code;
          const srs = "SRS:" + layerEntry.name;

          proj4.defs(srs, layerEntry.meta.ref.proj4);
          register(proj4);

          layerEntry.projection = new Projection({
            code: srs,
            units: "m",
          });
        }
      } catch (e) {
        console.log(e);
      }
    },

    completeBackgroundLayer(layerEntry) {
      try {
        if (layerEntry.projection === undefined) {
          if (layerEntry.layer !== undefined) {
            layerEntry.projection = layerEntry.layer
              .getSource()
              .getProjection();
          }
        }
        if (layerEntry.projection === undefined) {
          layerEntry.projection = new Projection({
            code: "EPSG:3857",
            units: "m",
          });
        }
      } catch (e) {
        console.log(e);
      }
    },

    async refreshLayers(keepView) {
      try {
        this.initializingLayers = true;
        if (this.layers.length > 0) {
          for (const layerEntry of this.layers) {
            this.completeLayer(layerEntry);
            if (layerEntry.type === "background") {
              this.completeBackgroundLayer(layerEntry);
            } else if (layerEntry.type === "postgis") {
              await this.completePostgisLayer(layerEntry);
            } else if (layerEntry.type === "rasterdb") {
              await this.completeRasterdbLayer(layerEntry);
            } else {
              console.log("unknown layer type");
            }
          }
          this.mainLayer =
            this.layers.length > 1 ? this.layers[1] : this.layers[0];
          const layers = [];
          /*layers.push(
            new TileLayer({
              source: new OSM(),
            })
          );*/
          for (const layerEntry of this.layers) {
            if (layerEntry.layer !== undefined) {
              layerEntry.layer.setVisible(layerEntry.visible);
              layers.push(layerEntry.layer);
            }
          }
          this.map.setLayers(layers);

          if (keepView === undefined || !keepView) {
            this.view = new View({
              projection: this.mainLayer.projection,
            });
            this.map.setView(this.view);
            if (this.mainLayer.extent !== undefined) {
              this.view.fit(this.mainLayer.extent);
            }
          }
        } else {
          this.map.setLayers([]);
        }
        this.initializingLayers = false;
      } catch (e) {
        console.log(e);
        this.initializingLayers = false;
      }
    },

    refreshLayerVisibility() {
      //console.log("refreshLayerVisibility");
      for (const layerEntry of this.layers) {
        const layer = layerEntry.layer;
        if (layer !== undefined) {
          layer.setVisible(layerEntry.visible);
        }
      }
    },

    onZoomToLayer(layer) {
      try {
        console.log(layer);
        console.log(layer.extent);
        if (this.mainLayer !== undefined) {
          const targetExtent = transformExtent(
            layer.extent,
            layer.projection,
            this.mainLayer.projection
          );
          if (targetExtent !== undefined) {
            console.log(targetExtent);
            this.view.fit(targetExtent);
          }
        }
      } catch (e) {
        console.log(e);
      }
    },

    async getFeatureInfo(coordinate) {
      //console.log(coordinate);

      for (const layerEntry of this.layers) {
        if (layerEntry.type === "postgis") {
          try {
            console.log(layerEntry);
            const imageWMS = layerEntry.layer.getSource();
            console.log(imageWMS);
            const resolution = this.view.getResolution();
            const projection = this.view.getProjection();
            //const params = { INFO_FORMAT: "text/html" };
            const params = { INFO_FORMAT: "application/geo+json" };
            const url = imageWMS.getFeatureInfoUrl(
              coordinate,
              resolution,
              projection,
              params
            );
            console.log(url);
            const response = await this.$api.get(url);
            //console.log(response.data);
            this.$refs.FeatureInfo.show(response.data);
          } catch (e) {
            console.log(e);
          }
        }
      }
    },

    refreshRasterdbLayerParameters(layerEntry) {
      console.log("refreshRasterdbLayerParameters " + layerEntry.name);
      const paramLayers = layerEntry.style.name + "/" + layerEntry.timeSlice.id;
      let params = { LAYERS: paramLayers };
      if (layerEntry.fixedGamma) {
        let markerLabels = [
          { value: 1, label: "0.1" },
          { value: 2, label: "0.2" },
          { value: 3, label: "0.5" },
          { value: 4, label: "1.0" },
          { value: 5, label: "1.5" },
          { value: 6, label: "2.0" },
          { value: 7, label: "2.5" },
          { value: 8, label: "3.0" },
        ];
        const gamma = markerLabels[layerEntry.gammaStep - 1].label;
        params.gamma = gamma;
      } else {
        params.gamma = undefined;
      }
      if (layerEntry.fixedRange) {
        params.range = layerEntry.rangeMin + "," + layerEntry.rangeMax;
      } else {
        params.range = undefined;
      }
      if (layerEntry.syncBands) {
        params.sync_bands = true;
      } else {
        params.sync_bands = false;
      }
      layerEntry.layer.getSource().updateParams(params);
    },

    onPointcloud3dPointViewCoordinateClick(coordinate) {
      const radius = 10;
      const extent = [
        coordinate[0] - radius,
        coordinate[1] - radius,
        coordinate[0] + radius,
        coordinate[1] + radius,
      ];
      this.openPointcloud3dPointView(
        this.interaction.pointcloud,
        extent,
        this.interaction.layer.timeSlice.id
      );
    },

    onPointcloud3dPointViewBboxClick() {
      this.openPointcloud3dPointView(
        this.interaction.pointcloud,
        this.selectedExtent,
        this.interaction.layer.timeSlice.id
      );
    },
    openPointcloud3dPointView(pointcloud, extent, timeSlice_id) {
      let url = this.$api.getUri();
      url += "web/pointcloud_view/pointcloud_view.html#/?";
      url += "&pointcloud=" + pointcloud;
      url += "&bbox=" + extent.map((v) => v.toFixed(4)).join(",");
      url += "&time_slice_id=" + timeSlice_id;
      console.log(url);
      window.open(url, "_blank", "noreferrer");
    },
  },

  watch: {
    backgroundMap() {
      const srcIndex = this.layers.findIndex((e) => e.type === "background");
      if (srcIndex >= 0) {
        const src = this.layers[srcIndex];
        this.backgroundMap.visible = src.visible;
        this.backgroundMap.opacity = src.opacity;
        this.backgroundMap.layer.setOpacity(this.backgroundMap.opacity / 100);
        this.layers[srcIndex] = this.backgroundMap;
        this.refreshLayers(true);
        this.refreshLayerVisibility();
      }
    },
    interaction() {
      console.log("watch interaction");
      console.log(this.interaction);
      if (this.interaction !== undefined) {
        if (this.interaction.type === "Pointcloud3dPointView") {
          console.log("Pointcloud3dPointView");
          this.mapCursorStandard = "crosshair";
          this.mapCursor = this.mapCursorStandard;
        } else {
          this.mapCursorStandard = "cell";
          this.mapCursor = this.mapCursorStandard;
        }
      } else {
        this.mapCursorStandard = "auto";
        this.mapCursor = this.mapCursorStandard;
      }
      console.log("watched interaction");
    },
  },

  async mounted() {
    this.layersInit(this);

    this.backgroundMaps = [
      {
        name: "OpenStreetMap",
        type: "background",
        layer: new TileLayer({
          source: new OSM(),
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "OpenTopoMap",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions:
              'Kartendaten: © <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>-Mitwirkende, SRTM | Kartendarstellung: © <a href="http://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)',
            url: "https://{a-c}.tile.opentopomap.org/{z}/{x}/{y}.png",
          }),
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Black",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(0, 0, 0, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "White",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(255, 255, 255, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Grey",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(127, 127, 127, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Red",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(255, 0, 0, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Green",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(0, 255, 0, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Blue",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(0, 0, 255, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Yellow",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(255, 255, 0, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Magenta",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(255, 0, 255, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
      {
        name: "Cyan",
        type: "background",
        layer: new TileLayer({
          source: new OSM({
            attributions: "",
            url: null,
          }),
          background: "rgba(0, 255, 255, 1)",
        }),
        visible: false,
        extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
      },
    ];
    this.backgroundMap = this.backgroundMaps[0];

    this.layers.length = 0;
    this.layers.push(this.backgroundMap);

    const query = this.route.query;
    //console.log(query);
    if (query.rasterdb !== undefined) {
      this.layers.push({ name: query.rasterdb, type: "rasterdb" });
    }
    if (query.postgis !== undefined) {
      this.layers.push({ name: query.postgis, type: "postgis" });
    }

    const projection = new Projection({
      code: "custom",
      units: "m",
    });

    this.view = new View({
      projection: projection,
    });

    const scaleControl = new ScaleLine({
      units: "metric",
      bar: true,
      steps: 4,
      text: false,
      minWidth: 140,
    });

    const mousePositionControl = new MousePosition({
      coordinateFormat: createStringXY(4),
    });

    const extentInteraction = new ExtentInteraction({
      condition: shiftKeyOnly,
    });

    extentInteraction.on("extentchanged", (e) => {
      console.log(e);
      this.selectedExtent = e.extent ? e.extent : undefined;
    });

    this.map = new Map({
      target: "map",
      view: this.view,
      controls: [new Attribution(), scaleControl, mousePositionControl],
      interactions: interactionDefaults().extend([extentInteraction]),
    });

    let cntStart = 0;
    let cntEnd = 0;

    this.map.on("loadstart", (e) => {
      this.map.getTargetElement().classList.add("spinner");
      //console.log("loadstart");
    });
    this.map.on("loadend", (e) => {
      this.map.getTargetElement().classList.remove("spinner");
      //console.log("loadend");
    });
    this.map.on("singleclick", (e) => {
      if (this.interaction !== undefined) {
        if (
          this.interaction.type === "Pointcloud3dPointView" &&
          !e.originalEvent.shiftKey
        ) {
          this.onPointcloud3dPointViewCoordinateClick(e.coordinate);
        } else {
          console.log("unknown interaction");
        }
      } else {
        this.getFeatureInfo(e.coordinate);
      }
    });
    this.map.on("movestart", (e) => {
      //console.log("movestart");
      this.mapCursor = "move";
    });
    this.map.on("moveend", (e) => {
      //console.log("moveend");
      this.mapCursor = this.mapCursorStandard;
    });

    await this.refreshLayers();
  },
});
</script>

<style>
@import url("../../node_modules/ol/ol.css");

html,
body,
#q-app,
#map {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
}

#map {
  position: fixed;
}

#overlay {
  pointer-events: none;
  margin: 0;
  padding: 0;
  position: relative;
}

@keyframes spinner {
  to {
    transform: rotate(360deg);
  }
}

.spinner:after {
  content: "";
  box-sizing: border-box;
  position: absolute;
  top: 50%;
  left: 50%;
  width: 40px;
  height: 40px;
  margin-top: -20px;
  margin-left: -20px;
  border-radius: 50%;
  border: 5px solid rgba(180, 180, 180, 0.6);
  border-top-color: rgba(0, 0, 0, 0.6);
  animation: spinner 0.6s linear infinite;
}

.ol-mouse-position {
  top: unset;
  right: unset;
  bottom: 0px;
  left: 50%;
  transform: translateX(-50%);
  position: absolute;
  font-size: 0.75em;
  /*background-color: #ffffff9e;*/
  padding-left: 1px;
  padding-right: 1px;
  text-shadow: 0px 0px 3px white, 1px 1px 2px white, -1px -1px 2px white,
    -1px 1px 2px white, 1px -1px 2px white;
}

.ol-scale-bar {
  bottom: 4px;
}
</style>
