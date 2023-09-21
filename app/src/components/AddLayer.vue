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
          v-model="layerFilter.postgis"
          label="PostGIS"
          checked-icon="task_alt"
          unchecked-icon="highlight_off"
        />

        <q-checkbox
          v-model="layerFilter.rasterdb"
          label="RasterDB"
          checked-icon="task_alt"
          unchecked-icon="highlight_off"
        />

        <q-select
          v-model="selectedLayer"
          :options="filteredOptionsLayers"
          label="Layer"
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

      <q-card-actions align="right">
        <q-btn flat label="Add" color="primary" v-close-popup @click="onOk" />
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
        postgis: true,
        rasterdb: true,
      },
      filteredOptionsLayers: [],
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
          (this.layerFilter.postgis && e.type === "postgis") ||
          (this.layerFilter.rasterdb && e.type === "rasterdb")
      );
    },
  },

  methods: {
    ...mapActions(useLayersStore, { layersInit: "init" }),

    onOk() {
      this.layers.push({
        name: this.selectedLayer.name,
        type: this.selectedLayer.type,
      });
      this.$emit("close");
    },
    rsdbLayerTypeToTitle(type) {
      if (type === "postgis") {
        return "PostGIS";
      } else if (type === "rasterdb") {
        return "RasterDB";
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

  mounted() {
    this.layersInit(this);
  },
});
</script>
