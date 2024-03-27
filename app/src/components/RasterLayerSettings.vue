<template>
  <div>
    <q-btn
      flat
      round
      icon="file_download"
      title="Download (parts of) raster
    layer data"
      @click="
        $emit('interaction', {
          type: 'RasterdbExport',
          rasterdb: layer.name,
          meta: layer.meta,
        })
      "
    />
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
      v-if="
        timeSlices.length > 0 &&
        (timeSlices.length !== 1 || timeSlices[0].name !== '---')
      "
      :readonly="timeSlices.length == 1"
      :dropdown-icon="timeSlices.length == 1 ? 'none' : 'arrow_drop_down'"
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
        <q-item v-bind="scope.itemProps" :title="scope.opt.description">
          <q-item-section>
            <q-item-label
              ><b
                v-if="
                  scope.opt.band_index !== undefined &&
                  scope.opt.name === 'band' + scope.opt.band_index
                "
                ><span class="band">band</span> {{ scope.opt.band_index }}</b
              >
              <b v-else>{{ scope.opt.name }}</b>
              <span
                v-if="scope.opt.name !== scope.opt.title"
                style="padding-left: 10px"
              >
                {{ scope.opt.title }}</span
              ><span
                class="wavelength"
                v-if="
                  scope.opt.band_index !== undefined &&
                  layer.bandMap[scope.opt.band_index].wavelength !== undefined
                "
              >
                {{ layer.bandMap[scope.opt.band_index].wavelength }} nm</span
              ></q-item-label
            >
          </q-item-section>
        </q-item>
      </template>

      <template v-slot:selected>
        <span :title="layer.style.description"
          ><b
            v-if="
              layer.style.band_index !== undefined &&
              layer.style.name === 'band' + layer.style.band_index
            "
            ><span class="band">band</span> {{ layer.style.band_index }}</b
          >
          <b v-else>{{ layer.style.name }}</b
          ><span
            v-if="layer.style.name !== layer.style.title"
            style="padding-left: 10px"
          >
            {{ layer.style.title }}</span
          ><span
            class="wavelength"
            v-if="
              layer.style.band_index !== undefined &&
              layer.bandMap[layer.style.band_index].wavelength !== undefined
            "
          >
            {{ layer.bandMap[layer.style.band_index].wavelength }} nm</span
          ></span
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
            />
            <q-checkbox
              :model-value="layer.syncBands"
              @update:model-value="onUpdateSyncBands"
              label="Sync"
              size="xs"
              title="Same dynamic value range and gamma for all bands."
              v-visible="!layer.fixedRange"
            />
          </q-item-label>
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

      <q-item>
        <q-item-section>
          <q-select
            :model-value="layer.palette"
            @update:model-value="onUpdatePalette"
            :options="layer.palettes"
            label="Colors (for one band only)"
            outlined
            stack-label
            dense
            title="testing"
          >
          </q-select>
        </q-item-section>
      </q-item>
    </q-expansion-item>

    <!--<q-expansion-item
      popup
      caption="Associated pointcloud"
      dense
      v-if="meta.associated.pointcloud !== undefined"
    >
      {{ meta.associated.pointcloud }}
      <q-btn
        @click="
          $emit('interaction', {
            type: 'Pointcloud3dPointView',
            pointcloud: meta.associated.pointcloud,
          })
        "
        >3D-view</q-btn
      >
    </q-expansion-item>-->
    <template v-for="entry in layer.associated" :key="entry.name">
      <q-expansion-item
        popup
        caption="Associated pointcloud"
        dense
        v-if="true || entry.type === 'pointcloud'"
      >
        {{ entry.name }}
        <q-btn
          flat
          round
          icon="file_download"
          title="Download (parts of) pointcloud
    layer data"
          @click="
            $emit('interaction', {
              type: 'PointcloudExport',
              pointcloud: entry.name,
              meta: entry.meta,
            })
          "
        />
        <q-btn
          flat
          round
          icon="calculate"
          title="Process parts of pointcloud
    layer data"
          @click="
            $emit('interaction', {
              type: 'PointcloudIndices',
              pointcloud: entry.name,
              meta: entry.meta,
            })
          "
        />
        <q-btn
          flat
          round
          icon="view_in_ar"
          @click="
            $emit('interaction', {
              type: 'Pointcloud3dPointView',
              pointcloud: entry.name,
              color: entry.color,
            })
          "
          title="Open 3d-view of box selected points."
        />
        <q-btn
          flat
          round
          icon="public"
          @click="
            $emit('interaction', {
              type: 'Pointcloud3dSurfaceView',
              pointcloud: entry.name,
              color: entry.color,
            })
          "
          title="Open 3d-view of box selected points."
        />
      </q-expansion-item>
    </template>
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
    onUpdateSyncBands(value) {
      this.$emit("changeSyncBands", value);
      console.log(value);
    },
    onUpdatePalette(value) {
      this.$emit("changePalette", value);
      console.log(value);
    },
  },

  mounted() {},
});
</script>

<style scoped>
.wavelength {
  padding-left: 10px;
  color: grey;
  font-size: 0.75em;
}

.band {
  font-size: 0.75em;
}
</style>
