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
        <q-item clickable v-ripple>
          <q-item-section>
            <q-item-label overline>PostGIS layer</q-item-label>
            <q-item-label caption>{{ postgisLayerID }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from "vue";
import { useRouter, useRoute } from "vue-router";

import { Map, View } from "ol";
import { Image as ImageLayer, Tile as TileLayer } from "ol/layer";
import { OSM } from "ol/source";
import ImageWMS from "ol/source/ImageWMS";
import Projection from "ol/proj/Projection";
import Attribution from "ol/control/Attribution";

export default defineComponent({
  name: "MainLayout",

  components: {},

  data() {
    return {
      map: undefined,
      view: undefined,
      router: useRouter(),
      route: useRoute(),
      postgisLayerID: "atkis.merged_layers_alb",
    };
  },

  methods: {
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
          /*new TileLayer({
          source: new OSM(),
        }),*/
          new ImageLayer({
            extent: extent,
            source: new ImageWMS({
              url: wmsURL,
              ratio: 1,
              interpolate: false,
            }),
          }),
        ];

        this.map.setLayers(layers);

        this.view.fit(extent);
      } catch (e) {
        Console.log(e);
      }
    },
  },

  mounted() {
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
</style>
