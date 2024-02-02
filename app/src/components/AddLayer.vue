<template>
  <q-dialog v-model="dialog" maximized>
    <q-card>
      <q-bar>
        Add layer
        <q-space />
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-white text-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <div v-if="layersLoading">Loading RSDB layers meta data ...</div>
      <div v-if="layersError">ERROR loading RSDB layers meta data.</div>

      <q-card-section>
        Layer types:

        <q-checkbox
          v-model="layerFilter.rasterdb"
          label="RasterDB"
          checked-icon="task_alt"
          unchecked-icon="highlight_off"
        />

        <q-checkbox
          v-model="layerFilter.pointcloud"
          label="Pointcloud"
          checked-icon="task_alt"
          unchecked-icon="highlight_off"
        />

        <q-checkbox
          v-model="layerFilter.vectordb"
          label="VectorDB"
          checked-icon="task_alt"
          unchecked-icon="highlight_off"
        />

        <q-checkbox
          v-model="layerFilter.postgis"
          label="PostGIS"
          checked-icon="task_alt"
          unchecked-icon="highlight_off"
        />

        <q-select
          v-model="selectedLayer"
          :options="filteredOptionsLayers"
          :label="
            'Layer' +
            (filteredOptionsLayers.length > 0
              ? ' (' + filteredOptionsLayers.length + ')'
              : '')
          "
          option-label="name"
          clearable
          use-input
          hide-selected
          fill-input
          input-debounce="0"
          @filter="filterFnAutoselect"
        >
          <template v-slot:selected-item="scope">
            <q-item v-bind="scope.itemProps" dense>
              <q-item-section>
                <q-item-label>{{ scope.opt.name }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-item-label caption>
                  {{ rsdbLayerTypeToTitle(scope.opt.type) }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </template>

          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" dense>
              <q-item-section>
                <q-item-label>{{ scope.opt.name }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-item-label caption>
                  {{ rsdbLayerTypeToTitle(scope.opt.type) }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </template>

          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey"> (No layers) </q-item-section>
            </q-item>
          </template>
        </q-select>
      </q-card-section>

      <q-card-section v-if="selectedLayerMeta">
        Type of selected layer:
        <b>{{ rsdbLayerTypeToTitle(selectedLayer.type) }}</b>
        <div v-if="selectedLayer.type === 'pointcloud'">
          <div v-if="selectedLayerVisLayer">
            Visualisation RasterDB layer:
            <b>{{ selectedLayerVisLayer.name }}</b>
          </div>
          <div v-else>
            <span style="color: red"
              >No associated visualization RasterDB layer for this Pointcloud
              layer.</span
            >
            Pointcloud can not be viewed on map. You may first create a
            visualization RasterDB layer for this Pointcloud layer.
          </div>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="Add"
          color="primary"
          v-close-popup
          @click="onOk"
          :disable="!selectedLayerVisLayer"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import { defineComponent } from "vue";
import { mapState } from "pinia";
import { mapActions } from "pinia";
import { useLayersStore } from "../stores/layers-store";

export default defineComponent({
  name: "AddLayer",
  props: {},
  components: {},

  data() {
    return {
      dialog: false,
      selectedLayer: undefined,
      layerFilter: {
        rasterdb: true,
        pointcloud: true,
        vectordb: true,
        postgis: true,
      },
      filteredOptionsLayers: [],
      selectedLayerMeta: undefined,
      selectedLayerVisLayer: undefined,
    };
  },

  computed: {
    ...mapState(useLayersStore, {
      rsdbLayers: "rsdb_layers",
      layers: "layers",
      layersLoading: "loading",
      layersError: "error",
    }),

    optionsLayers() {
      return this.rsdbLayers.filter(
        (e) =>
          (this.layerFilter.rasterdb && e.type === "rasterdb") ||
          (this.layerFilter.pointcloud && e.type === "pointcloud") ||
          (this.layerFilter.vectordb && e.type === "vectordb") ||
          (this.layerFilter.postgis && e.type === "postgis")
      );
    },
  },

  methods: {
    ...mapActions(useLayersStore, { layersInit: "init" }),

    onOk() {
      this.layers.push(this.selectedLayerVisLayer);
      this.$emit("close");
    },
    rsdbLayerTypeToTitle(type) {
      if (type === "rasterdb") {
        return "RasterDB";
      } else if (type === "pointcloud") {
        return "Pointcloud";
      } else if (type === "vectordb") {
        return "VectorDB";
      } else if (type === "postgis") {
        return "PostGIS";
      } else {
        return type;
      }
    },
    filterFnAutoselect(val, update, abort) {
      //setTimeout(() => {
      update(
        () => {
          if (val === "") {
            this.filteredOptionsLayers = this.optionsLayers;
          } else {
            const needle = val.toLowerCase();
            this.filteredOptionsLayers = this.optionsLayers.filter(
              (v) => v.name.toLowerCase().indexOf(needle) > -1
            );
          }
        },

        // "ref" is the Vue reference to the QSelect
        (ref) => {
          if (
            val !== "" &&
            ref.options.length > 0 &&
            ref.getOptionIndex() === -1
          ) {
            ref.moveOptionSelection(1, true); // focus the first selectable option and do not update the input-value
            ref.toggleOption(ref.options[ref.optionIndex], true); // toggle the focused option
          }
        }
      );
      //}, 300);
    },
  },

  watch: {
    async selectedLayer() {
      try {
        this.selectedLayerMeta = undefined;
        this.selectedLayerVisLayer = undefined;
        if (this.selectedLayer) {
          if (this.selectedLayer.type === "rasterdb") {
            const layerURLPart = "rasterdb/" + this.selectedLayer.name;
            const response = await this.$api.get(layerURLPart + "/meta.json");
            this.selectedLayerMeta = response.data;
            this.selectedLayerVisLayer = {
              type: this.selectedLayer.type,
              name: this.selectedLayer.name,
            };
          } else if (this.selectedLayer.type === "pointcloud") {
            const layerURLPart = "pointclouds/" + this.selectedLayer.name;
            const response = await this.$api.get(layerURLPart);
            this.selectedLayerMeta = response.data.pointcloud;
            if (this.selectedLayerMeta.associated.rasterdb) {
              this.selectedLayerVisLayer = {
                type: "rasterdb",
                name: this.selectedLayerMeta.associated.rasterdb,
              };
            }
          } else if (this.selectedLayer.type === "vectordb") {
            const layerURLPart = "vectordbs/" + this.selectedLayer.name;
            const metaURL = layerURLPart + "?extent";
            const response = await this.$api.get(metaURL);
            this.selectedLayerMeta = response.data.vectordb;
            this.selectedLayerVisLayer = {
              type: this.selectedLayer.type,
              name: this.selectedLayer.name,
            };
          } else if (this.selectedLayer.type === "postgis") {
            const layerURLPart = "postgis/layers/" + this.selectedLayer.name;
            const response = await this.$api.get(layerURLPart);
            this.selectedLayerMeta = response.data;
            this.selectedLayerVisLayer = {
              type: this.selectedLayer.type,
              name: this.selectedLayer.name,
            };
          } else {
            // unknown layer
            this.selectedLayerMeta = undefined;
          }
        }
      } catch (e) {
        console.log(e.message ? e.message : e);
        this.$q.notify({
          type: "negative",
          message: "Layer load meta data error: " + (e.message ? e.message : e),
        });
      }
    },
  },

  mounted() {
    this.layersInit(this);
  },
});
</script>
