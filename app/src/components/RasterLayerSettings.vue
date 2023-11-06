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

    <q-expansion-item popup caption="Visualisation settings" dense>
      <q-item>
        <q-item-section>
          <q-item-label overline
            >Gamma
            <q-toggle
              :model-value="layer.fixedGamma"
              @update:model-value="onUpdateFixedGamma"
              :label="
                layer.fixedGamma
                  ? 'fixed'
                  : layer.fixedRange
                  ? 'linear'
                  : 'dynamic'
              "
          /></q-item-label>
          <q-slider
            :model-value="layer.gammaStep"
            @update:model-value="onUpdateGammaStep"
            :marker-labels="markerLabels"
            :min="1"
            :max="8"
            dense
            snap
            v-visible="layer.fixedGamma"
          />
        </q-item-section>
      </q-item>

      <q-item>
        <q-item-section>
          <q-item-label overline
            >Value range
            <q-toggle
              :model-value="layer.fixedRange"
              @update:model-value="onUpdateFixedRange"
              :label="layer.fixedRange ? 'fixed' : 'dynamic'"
          /></q-item-label>
          <q-item-label class="row no-wrap" v-visible="layer.fixedRange">
            <q-input
              :model-value="layer.rangeMin"
              @update:model-value="onUpdateRangeMin"
              label="min"
              dense
              stack-label
              style="max-width: 100px"
              type="number"
              outlined
            />
            to
            <q-input
              :model-value="layer.rangeMax"
              @update:model-value="onUpdateRangeMax"
              label="max"
              dense
              stack-label
              style="max-width: 100px"
              type="number"
              outlined
            />
          </q-item-label>
        </q-item-section>
      </q-item>
    </q-expansion-item>
  </div>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "RasterLayerSettings",
  props: ["layer"],
  components: {},

  data() {
    return {
      markerLabels: [
        { value: 1, label: "0.1" },
        { value: 2, label: "0.2" },
        { value: 3, label: "0.5" },
        { value: 4, label: "1.0" },
        { value: 5, label: "1.5" },
        { value: 6, label: "2.0" },
        { value: 7, label: "2.5" },
        { value: 8, label: "3.0" },
      ],
    };
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
    onUpdateFixedGamma(value) {
      this.$emit("changeFixedGamma", value);
      console.log(value);
    },
    onUpdateGammaStep(value) {
      this.$emit("changeGammaStep", value);
      console.log(value);
    },
    onUpdateFixedRange(value) {
      this.$emit("changeFixedRange", value);
      console.log(value);
    },
    onUpdateRangeMin(value) {
      this.$emit("changeRangeMin", value);
      console.log(value);
    },
    onUpdateRangeMax(value) {
      this.$emit("changeRangeMax", value);
      console.log(value);
    },
  },

  mounted() {},
});
</script>
