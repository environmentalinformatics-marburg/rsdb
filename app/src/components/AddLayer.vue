<template>
  <q-dialog v-model="dialog" persistent maximized>
    <q-card>
      <q-bar>
        Add layer
        <q-space />
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-white text-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <q-card-section>
        <q-select
          v-model="postgis"
          :options="layersPostgis"
          label="PostGIS layer"
          option-label="name"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="OK" color="primary" v-close-popup @click="onOk" />
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
      dialog: true,
      postgis: undefined,
    };
  },

  computed: {
    ...mapState(useLayersStore, { layersPostgis: "postgis", layers: "layers" }),
  },

  methods: {
    ...mapActions(useLayersStore, { layersInit: "init" }),

    onOk() {
      this.layers.push({ name: this.postgis.name, type: "postgis" });
    },
  },

  mounted() {
    this.layersInit(this);
  },
});
</script>
