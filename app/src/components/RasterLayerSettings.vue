<template>
  <div>
    <q-select
      model-value="layer.timeSlice"
      @update:model-value="onUpdateTimeSlice"
      :options="timeSlices"
      label="Time"
      outlined
      stack-label
      dense
      option-label="name"
      :display-value="layer.timeSlice.name"
      v-if="timeSlices.length > 1"
    >
      <template v-slot:before>
        <q-btn
          size="md"
          flat
          round
          color="primary"
          icon="navigate_before"
          @click="onUpdateTimeSlice(timeSlices[layer.timeSlice.index - 1])"
          padding="none"
          v-visible="layer.timeSlice.index > 0"
          title="Select previous time from list."
        />
      </template>

      <template v-slot:after>
        <q-btn
          size="md"
          flat
          round
          color="primary"
          icon="navigate_next"
          @click="onUpdateTimeSlice(timeSlices[layer.timeSlice.index + 1])"
          padding="none"
          v-visible="layer.timeSlice.index < timeSlices.length - 1"
          title="Select next time from list."
        />
      </template>
    </q-select>

    <q-select
      model-value="layer.style"
      @update:model-value="onUpdateModelValue"
      :options="styles"
      label="Visualisation"
      outlined
      stack-label
      dense
      option-label="name"
    >
      <template v-slot:option="scope">
        <q-item v-bind="scope.itemProps">
          <q-item-section>
            <q-item-label
              ><b>{{ scope.opt.name }}</b>
              <span v-if="scope.opt.name !== scope.opt.description">
                - {{ scope.opt.description }}</span
              ></q-item-label
            >
          </q-item-section>
        </q-item>
      </template>

      <template v-slot:selected>
        <span
          ><b>{{ layer.style.name }}</b> - {{ layer.style.description }}</span
        >
      </template>

      <template v-slot:before>
        <q-btn
          size="md"
          flat
          round
          color="primary"
          icon="navigate_before"
          @click="onUpdateModelValue(styles[layer.style.index - 1])"
          padding="none"
          v-visible="layer.style.index > 0"
          title="Select previous visualization from list."
        />
      </template>

      <template v-slot:after>
        <q-btn
          size="md"
          flat
          round
          color="primary"
          icon="navigate_next"
          @click="onUpdateModelValue(styles[layer.style.index + 1])"
          padding="none"
          v-visible="layer.style.index < styles.length - 1"
          title="Select next visualization from list."
        />
      </template>
    </q-select>
  </div>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "RasterLayerSettings",
  props: ["layer"],
  components: {},

  data() {
    return {};
  },

  computed: {
    meta() {
      return this.layer.meta;
    },
    timeSlices() {
      return this.meta.time_slices;
    },
    styles() {
      return this.meta.wms.styles;
    },
  },

  methods: {
    onUpdateModelValue(value) {
      this.$emit("changeStyle", value);
      console.log(value);
    },
    onUpdateTimeSlice(value) {
      this.$emit("changeTimeSlice", value);
      console.log(value);
    },
  },

  mounted() {},
});
</script>
