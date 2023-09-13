<template>
  <div id="map"></div>

  <div id="overlay">
    <div
      class="q-pa-md"
      style="
        max-width: 350px;
        pointer-events: auto;
        background-color: rgba(255, 255, 255, 0.932);
      "
      dense
    >
      <q-list bordered separator>
        <q-item
          v-for="(layer, index) in layers"
          :key="layer.name + ':' + layer.type"
          clickable
          v-ripple
        >
          <template v-if="layer.type === 'postgis'">
            <q-item-section side top
              ><q-btn
                size="sm"
                flat
                round
                color="grey"
                icon="delete_forever"
                @click.stop="
                  layers.splice(index, 1);
                  refreshLayers();
                  console.log('done!');
                "
            /></q-item-section>
            <q-item-section>
              <q-item-label caption>PostGIS</q-item-label>
              <q-item-label>{{ layer.name }}</q-item-label>
            </q-item-section>
            <q-item-section side top
              ><q-checkbox
                size="xs"
                v-model="layer.visible"
                val="xs"
                color="grey"
                @update:model-value="refreshLayerVisibility"
            /></q-item-section>
          </template>
          <template v-else-if="layer.type === 'background'">
            <q-item-section>
              <q-item-label caption>Background</q-item-label>
              <q-item-label>{{ layer.name }}</q-item-label>
            </q-item-section>
            <q-item-section side top
              ><q-checkbox
                size="xs"
                v-model="layer.visible"
                val="xs"
                color="grey"
                @update:model-value="refreshLayerVisibility"
            /></q-item-section>
          </template>
          <template v-else>
            <q-item-section>
              <q-item-label caption>{{ layer.type }} layer</q-item-label>
              <q-item-label>{{ layer.name }}</q-item-label>
            </q-item-section>
            <q-item-section side top
              ><q-checkbox
                size="xs"
                v-model="layer.visible"
                val="xs"
                color="grey"
                @update:model-value="refreshLayerVisibility"
            /></q-item-section>
          </template>
          <q-menu>
            <q-list style="min-width: 300px">
              <q-item>
                <q-item-section>
                  <q-item-label caption>Opacity</q-item-label>
                  <q-item-label>
                    <q-slider
                      v-model="layer.opacity"
                      :min="0"
                      :max="100"
                      @update:model-value="
                        layer.layer.setOpacity(layer.opacity / 100)
                      "
                    />
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-item>
      </q-list>
      <q-btn @click="$refs.AddLayer.dialog = true"
        >Add layer <AddLayer ref="AddLayer" @close="refreshLayers()"></AddLayer
      ></q-btn>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { mapState } from "pinia";
import { mapActions } from "pinia";
import { useLayersStore } from "../stores/layers-store";

import { Map, View } from "ol";
import { Image as ImageLayer, Tile as TileLayer } from "ol/layer";
import { OSM } from "ol/source";
import ImageWMS from "ol/source/ImageWMS";
import Projection from "ol/proj/Projection";
import Attribution from "ol/control/Attribution";
import proj4 from "proj4";
import { register } from "ol/proj/proj4.js";

import AddLayer from "../components/AddLayer.vue";

export default defineComponent({
  name: "MainLayout",

  components: { AddLayer },

  data() {
    return {
      map: undefined,
      view: undefined,
      router: useRouter(),
      route: useRoute(),
      postgisLayerID: "atkis.merged_layers_alb",
      session: Math.floor(Math.random() * 1000000000),
      sessionCnt: 0,
    };
  },

  computed: {
    ...mapState(useLayersStore, { layers: "layers" }),
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

    async refreshLayers() {
      try {
        if (this.layers.length > 0) {
          for (const layerEntry of this.layers) {
            this.completeLayer(layerEntry);
            if (layerEntry.type === "background") {
              // nothing
            } else if (layerEntry.type === "postgis") {
              await this.completePostgisLayer(layerEntry);
            } else {
              console.log("unknown layer type");
            }
          }
          const mainLayer =
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

          this.view = new View({
            projection: mainLayer.projection,
          });

          this.map.setView(this.view);

          if (mainLayer.extent !== undefined) {
            this.view.fit(mainLayer.extent);
          }
        } else {
          this.map.setLayers([]);
        }
      } catch (e) {
        console.log(e);
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
  },

  async mounted() {
    this.layersInit(this);

    this.layers.length = 0;
    this.layers.push({
      name: "OpenStreetMap",
      type: "background",
      layer: new TileLayer({
        source: new OSM(),
      }),
      visible: false,
      extent: [-20037508.34, -20048966.1, 20037508.34, 20048966.1],
    });

    const query = this.route.query;
    //console.log(query);
    if (query.postgis !== undefined) {
      this.postgisLayerID = query.postgis;

      this.layers.push({ name: query.postgis, type: "postgis" });
    }

    const projection = new Projection({
      code: "custom",
      units: "m",
    });

    this.view = new View({
      projection: projection,
    });

    this.map = new Map({
      target: "map",
      view: this.view,
      controls: [new Attribution()],
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
</style>
