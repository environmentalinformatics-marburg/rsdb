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
        <q-btn @click="$refs.AddLayer.dialog = true">Add layer</q-btn>
        <AddLayer ref="AddLayer"></AddLayer>
        <q-item
          v-for="layer in layers"
          :key="layer.name + ':' + layer.type"
          clickable
          v-ripple
        >
          <q-item-section>
            <q-item-label overline>PostGIS layer</q-item-label>
            <q-item-label caption>{{ layer.name }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
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

    async setPostgisLayer() {
      try {
        const postgisLayerURLPart = "postgis/layers/" + this.postgisLayerID;

        const response = await this.$api.get(postgisLayerURLPart);
        const meta = response.data;
        //console.log(meta);

        const extent = [
          meta.extent.xmin,
          meta.extent.ymin,
          meta.extent.xmax,
          meta.extent.ymax,
        ];

        //console.log(this.$api.getUri());

        const wmsURL = this.$api.getUri() + postgisLayerURLPart + "/wms";
        //console.log(wmsURL);

        const layers = [
          new TileLayer({
            source: new OSM(),
          }),
          new ImageLayer({
            extent: extent,
            source: new ImageWMS({
              url: wmsURL,
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
          }),
        ];

        this.map.setLayers(layers);

        console.log(meta);

        const srs = "EPSG" + meta.epsg;

        proj4.defs(srs, meta.wkt_srs);
        register(proj4);

        const projection = new Projection({
          code: srs,
          units: "m",
        });

        this.view = new View({
          projection: projection,
        });

        this.map.setView(this.view);

        this.view.fit(extent);
      } catch (e) {
        Console.log(e);
      }
    },
  },

  mounted() {
    console.log(this.layersInit);
    this.layersInit(this);

    const query = this.route.query;
    console.log(query);
    if (query.postgis !== undefined) {
      this.postgisLayerID = query.postgis;
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

    this.map.on("loadstart", () => {
      this.map.getTargetElement().classList.add("spinner");
      console.log("loadstart");
    });
    this.map.on("loadend", () => {
      this.map.getTargetElement().classList.remove("spinner");
      console.log("loadend");
    });

    this.setPostgisLayer();
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
