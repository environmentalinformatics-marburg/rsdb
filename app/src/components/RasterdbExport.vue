<template>
  <q-dialog v-model="dialog" maximized>
    <q-card>
      <q-bar>
        RasterDB data export of &nbsp;<b>{{ meta.name }}</b>
        <q-space />
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-white text-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <q-card-section v-if="meta !== undefined">
        <table>
          <tr>
            <td>Extent coordinates</td>
            <td>{{ extent }}</td>
          </tr>
          <tr>
            <td>Geo units extent</td>
            <td>
              {{ xlenLoc * ref.pixel_size.x }}
              x
              {{ ylenLoc * ref.pixel_size.y }}
            </td>
          </tr>
          <tr>
            <td>Pixel extent</td>
            <td>
              {{ xlenLoc }}
              x
              {{ ylenLoc }}
            </td>
          </tr>
        </table>
      </q-card-section>

      <q-card-section>
        <q-select
          v-model="filePackaging"
          :options="filePackagings"
          label="File packaging"
          outlined
          stack-label
          dense
        />
      </q-card-section>

      <q-card-section v-if="oneTimes.length > 1">
        <q-select
          v-model="timeMethod"
          :options="timeMethods"
          label="Time relation"
          outlined
          stack-label
          dense
        />
      </q-card-section>

      <q-card-section v-if="timeMethod.name === 'one'">
        <q-select
          :options="oneTimes"
          option-label="name"
          v-model="oneTime"
          :readonly="oneTimes.length == 1"
          :dropdown-icon="oneTimes.length == 1 ? 'none' : 'arrow_drop_down'"
          label="One time slice"
          stack-label
          outlined
          dense
          v-if="
            oneTimes.length > 0 &&
            (oneTimes.length !== 1 || oneTimes[0].name !== '---')
          "
        />
      </q-card-section>

      <q-card-section
        v-if="
          timeMethod.name === 'multi_time_band' ||
          timeMethod.name === 'multi_band_time'
        "
      >
        <q-select
          :options="oneTimes"
          option-label="name"
          v-model="multiTime"
          multiple
          clearable
          label="Select multiple time slices, leave empty for all time slices."
          stack-label
          outlined
          options-dense
          dense
          ><template v-slot:selected
            ><span
              v-if="
                multiTime === undefined ||
                multiTime === null ||
                multiTime.length === 0
              "
              >(all time slices)</span
            ><span v-else>{{
              multiTime.map((t) => t.name).join(", ")
            }}</span></template
          ></q-select
        >
      </q-card-section>

      <q-card-section>
        <q-select
          :options="oneBands"
          v-model="selectedBands"
          multiple
          clearable
          label="Select one or multiple bands, leave empty for all bands."
          stack-label
          outlined
          options-dense
          dense
        >
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps" :title="scope.opt.title">
              <q-item-section>
                <q-item-label
                  ><b><span class="band">band</span> {{ scope.opt.index }}</b>
                  <span style="padding-left: 10px"> {{ scope.opt.title }}</span
                  ><span
                    class="wavelength"
                    v-if="scope.opt.wavelength !== undefined"
                  >
                    {{ scope.opt.wavelength }}
                    nm</span
                  ></q-item-label
                >
              </q-item-section>
            </q-item>
          </template>
          <template v-slot:selected
            ><span
              v-if="
                selectedBands === undefined ||
                selectedBands === null ||
                selectedBands.length === 0
              "
              >(all bands)</span
            ><span v-else>{{
              selectedBands.map((band) => band.index).join(", ")
            }}</span></template
          ></q-select
        >
      </q-card-section>

      <q-card-section>
        {{ pixelLoc }} pixel per band ({{ xlenLoc }} width x
        {{ ylenLoc }} height)
      </q-card-section>

      <q-card-section v-if="pixelMax < pixelLoc" style="color: red">
        Count of selected pixels per band is higher than maximum allowed pixel
        count of
        {{ pixelMax }}. Try selecting a smaller area.
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="Download"
          color="primary"
          v-close-popup
          @click="onOk"
          :href="rasterUrl"
          target="_blank"
          :disable="pixelMax < pixelLoc"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "RasterdbExport",
  props: ["interaction"],
  components: {},

  data() {
    return {
      dialog: false,
      timeMethod: undefined,
      timeMethods: [
        { name: "one", label: "One time slice" },
        {
          name: "multi_time_band",
          label:
            "Multiple time slices (order --- time-band ---) (time1_bandA, time2_bandA, ..., time1_bandB, time2_bandB, ...)",
        },
        {
          name: "multi_band_time",
          label:
            "Multiple time slices (order --- band-time ---) (time1_bandA, time1_bandB, ..., time2_bandA, time2_bandB, ...)",
        },
      ],
      oneTime: undefined,
      filePackaging: undefined,
      filePackagings: [{ name: "plain", label: "Raster file" }],
      selectedBands: undefined,
      multiTime: [],
      pixelMax: 8192 * 8192,
    };
  },

  computed: {
    extent() {
      if (
        this.interaction === undefined ||
        this.interaction.type !== "RasterdbExport"
      ) {
        return undefined;
      } else {
        return this.interaction.extent;
      }
    },
    meta() {
      if (
        this.interaction === undefined ||
        this.interaction.type !== "RasterdbExport"
      ) {
        return undefined;
      } else {
        return this.interaction.meta;
      }
    },
    oneTimes() {
      if (this.meta === undefined) {
        return [];
      } else {
        return this.meta.time_slices;
      }
    },
    oneBands() {
      if (this.meta === undefined) {
        return [];
      } else {
        return this.meta.bands;
      }
    },
    ref() {
      return this.meta ? this.meta.ref : undefined;
    },
    xminLoc() {
      return this.ref
        ? Math.floor(
            (this.extent[0] - this.ref.internal_rasterdb_offset.x) /
              this.ref.pixel_size.x
          )
        : undefined;
    },
    xmaxLoc() {
      return this.ref
        ? Math.floor(
            (this.extent[2] - this.ref.internal_rasterdb_offset.x) /
              this.ref.pixel_size.x
          )
        : undefined;
    },
    yminLoc() {
      return this.ref
        ? Math.floor(
            (this.extent[1] - this.ref.internal_rasterdb_offset.y) /
              this.ref.pixel_size.y
          )
        : undefined;
    },
    ymaxLoc() {
      return this.ref
        ? Math.floor(
            (this.extent[3] - this.ref.internal_rasterdb_offset.y) /
              this.ref.pixel_size.y
          )
        : undefined;
    },
    xlenLoc() {
      return this.xmaxLoc - this.xminLoc + 1;
    },
    ylenLoc() {
      return this.ymaxLoc - this.yminLoc + 1;
    },
    pixelLoc() {
      return this.xlenLoc * this.ylenLoc;
    },
    rasterUrl() {
      let s =
        this.$api.getUri() + "rasterdb/" + this.meta.name + "/raster.tiff";
      s += "?ext=" + this.extent.join(",");

      if (this.timeMethod.name === "one") {
        if (this.oneTime === undefined || this.oneTime === null) {
          //return "";
        } else {
          s += "&time_slice_id=" + this.oneTime.id;
        }
      } else if (
        this.timeMethod.name === "multi_time_band" ||
        this.timeMethod.name === "multi_band_time"
      ) {
        if (
          this.multiTime === undefined ||
          this.multiTime === null ||
          this.multiTime.length === 0
        ) {
          s += "&time_slice_id=" + "all";
        } else {
          s +=
            "&time_slice_id=" +
            this.multiTime.map((timeSlice) => timeSlice.id).join(",");
        }
        if (this.timeMethod.name === "multi_time_band") {
          s += "&band_order=" + "time_band";
        } else if (this.timeMethod.name === "multi_band_time") {
          s += "&band_order=" + "band_time";
        } else {
          //return "";
        }
      }

      if (
        this.selectedBands !== undefined &&
        this.selectedBands !== null &&
        this.selectedBands.length !== 0
      ) {
        s += "&band=" + this.selectedBands.map((band) => band.index).join(",");
      }

      return s;
    },
  },

  methods: {
    open() {
      this.dialog = true;
    },
    onOk() {},
  },

  watch: {
    meta: {
      handler() {
        if (this.oneTimes.length === 0) {
          this.oneTime = undefined;
        } else {
          this.oneTime = this.oneTimes[0];
        }
      },
      immediate: true,
    },
    oneTimes: {
      handler() {
        if (this.oneTimes.length <= 1) {
          this.timeMethod = this.timeMethods[0];
        }

        if (this.oneTimes.length === 0) {
          this.oneTime = undefined;
        } else {
          this.oneTime = this.oneTimes[0];
        }
      },
      immediate: true,
    },
  },

  mounted() {
    this.timeMethod = this.timeMethods[0];
    this.filePackaging = this.filePackagings[0];
  },
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
