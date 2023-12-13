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
          options-dense
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
          options-dense
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
          options-dense
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
              style="color: grey"
              >(all bands)</span
            ><span v-else>{{
              selectedBands.map((band) => band.index).join(", ")
            }}</span></template
          ></q-select
        >
      </q-card-section>

      <q-card-section
        v-if="
          filePackaging.name === 'tiff' || filePackaging.name === 'tiled_tiff'
        "
      >
        <q-select
          v-model="dataType"
          :options="dataTypes"
          label="Data type of pixel values, leave empty for best fitting data type"
          outlined
          stack-label
          dense
          options-dense
          clearable
          ><template v-slot:selected
            ><span
              v-if="dataType === undefined || dataType === null"
              style="color: grey"
              >(best fitting data type)</span
            ><span v-else>
              {{ dataType.label }}
            </span>
          </template>
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section>
                <q-item-label>
                  <span class="dataType-space">{{ scope.opt.space }}</span>
                  <span class="dataType-label">{{ scope.opt.label }}</span>
                  <span class="dataType-description">{{
                    scope.opt.description
                  }}</span
                  ><span class="dataType-range">{{ scope.opt.range }}</span>
                </q-item-label>
              </q-item-section>
            </q-item>
          </template>
        </q-select>
      </q-card-section>

      <q-card-section>
        {{ pixelLoc }} pixel per band ({{ xlenLoc }} width x
        {{ ylenLoc }} height)
      </q-card-section>

      <q-card-section
        v-if="filePackaging.name === 'tiff' && pixelMax < pixelLoc"
        style="color: red"
      >
        Count of selected pixels per band is higher than maximum allowed pixel
        count of
        {{ pixelMax }}. <br /><br />Possible solutions:
        <ul>
          <li>
            Switch to file packaging method "<b>Cloud Optimized GeoTIFF file</b
            >", which does not have a size limit.
          </li>
          <li>Try selecting a smaller area.</li>
        </ul>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="Download"
          color="primary"
          v-close-popup
          @click="onOk"
          :href="downloadUrl"
          target="_blank"
          :disable="filePackaging.name === 'tiff' && pixelMax < pixelLoc"
        />
      </q-card-actions>
      <hr />
      <q-card-section v-if="filePackaging.name === 'tiff'">
        <div class="text-h6">GeoTIFF file format information</div>

        Plain GeoTIFF file. This export method has a limit of maximum possible
        pixel count.
      </q-card-section>

      <q-card-section v-if="filePackaging.name === 'tiled_tiff'">
        <div class="text-h6">
          Cloud Optimized GeoTIFF file format information
        </div>

        GeoTIFF file with internally tiled data. This export method has no limit
        of maximum possible pixel count. Because of the tiled structure the
        resulting file may have a bit larger image extent than selected on the
        map.
      </q-card-section>
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
      filePackagings: [
        { name: "tiff", label: "GeoTIFF file" },
        { name: "tiled_tiff", label: "Cloud Optimized GeoTIFF file" },
      ],
      selectedBands: undefined,
      multiTime: [],
      pixelMax: 8192 * 8192,
      dataType: undefined,
      dataTypes: [
        {
          name: "uint8",
          space: "1 byte per pixel",
          label: "uint8",
          description: "unsigned 8 bit integer",
          range: "[0 .. 255]",
        },
        {
          name: "int16",
          space: "2 bytes per pixel",
          label: "int16",
          description: "signed 16 bit integer",
          range: "[−32768 .. +32767]",
        },
        {
          name: "uint16",
          space: "2 bytes per pixel",
          label: "uint16",
          description: "unsigned 16 bit integer",
          range: "[0 .. 65535]",
        },
        {
          name: "int32",
          space: "4 bytes per pixel",
          label: "int32",
          description: "signed 32 bit integer",
          range: "[-2147483648 .. +2147483647]",
        },
        {
          name: "float32",
          space: "4 bytes per pixel ",
          label: "float32",
          description: "32 bit floating point number",
          range: "(precision of about 7 decimal digits)",
        },
        {
          name: "float64",
          space: "8 bytes per pixel",
          label: "float64",
          description: "64 bit floating point number",
          range: "(precision of about 16 decimal digits)",
        },
      ],
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
    downloadUrl() {
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

      if (this.filePackaging.name === "tiff") {
        s += "&format=" + "tiff:banded";
      } else if (this.filePackaging.name === "tiled_tiff") {
        s += "&format=" + "tiff:banded:tiled";
      } else {
        return "";
      }

      if (
        (this.filePackaging.name === "tiff" ||
          this.filePackaging.name === "tiled_tiff") &&
        this.dataType
      ) {
        s += "&data_type=" + this.dataType.name;
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
  font-weight: lighter;
}

.dataType-space {
  font-size: 0.75em;
}

.dataType-label {
  padding-left: 20px;
  font-size: 1em;
  font-weight: bold;
}

.dataType-description {
  padding-left: 20px;
  font-size: 1em;
  font-style: italic;
}

.dataType-range {
  padding-left: 20px;
  font-size: 0.75em;
  color: grey;
}
</style>
