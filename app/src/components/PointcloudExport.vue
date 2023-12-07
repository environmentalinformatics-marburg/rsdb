<template>
  <q-dialog v-model="dialog" maximized>
    <q-card>
      <q-bar>
        Pointcloud data export of &nbsp;<b>{{ meta.name }}</b>
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
        </table>
      </q-card-section>

      <q-card-section v-if="meta !== undefined">
        <q-select
          v-model="timeSlice"
          :options="meta.time_slices"
          label="Time slice"
          outlined
          stack-label
          dense
          option-label="name"
          :display-value="timeSlice.name"
          v-if="
            meta.time_slices.length > 0 &&
            (meta.time_slices.length !== 1 ||
              meta.time_slices[0].name !== '---')
          "
          :readonly="meta.time_slices.length == 1"
          :dropdown-icon="
            meta.time_slices.length == 1 ? 'none' : 'arrow_drop_down'
          "
        />
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

      <q-card-section v-if="filePackaging.name === 'csv'">
        <q-select
          v-model="selectedAttributes"
          :options="meta.attributes"
          multiple
          clearable
          label="Selected columns"
          outlined
          stack-label
          dense
          ><template v-slot:selected
            ><span
              v-if="
                selectedAttributes === undefined ||
                selectedAttributes === null ||
                selectedAttributes.length === 0
              "
              >(all columns)</span
            ><span v-else>{{
              selectedAttributes.map((name) => name).join(", ")
            }}</span></template
          ></q-select
        >
      </q-card-section>

      <q-card-section v-if="filePackaging.name === 'csv'">
        <q-select
          v-model="columnSeparator"
          :options="columnSeparators"
          label="Column separator"
          outlined
          stack-label
          dense
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="View file content"
          color="primary"
          @click="onView"
          v-if="filePackaging.name === 'xyz' || filePackaging.name === 'csv'"
        />
        <q-btn
          flat
          label="Download"
          color="primary"
          v-close-popup
          @click="onOk"
          :href="downloadUrl"
          target="_blank"
        />
      </q-card-actions>

      <q-card-section vif="viewData !== undefined">
        <hr />
        <b>View of first 1000 lines of file content to be downloaded:</b>
        <div style="white-space: pre">{{ viewData }}</div>
      </q-card-section>

      <hr />
      <q-card-section v-if="filePackaging.name === 'las'">
        <div class="text-h6">LAS file format information</div>

        LAS file. This export method has a limit of maximum possible file size.
      </q-card-section>

      <q-card-section v-if="filePackaging.name === 'xyz'">
        <div class="text-h6">XYZ file format information</div>

        XYZ files are human readable tabular text files with space separated
        columns without column names header row. This export method has no limit
        of maximum possible file size.
      </q-card-section>

      <q-card-section v-if="filePackaging.name === 'csv'">
        <div class="text-h6">CSV file format information</div>

        CSV files are human readable tabular text files with comma (or other
        character) separated columns with header, as first line, of column
        names. This export method has no limit of maximum possible file size.
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "PointcloudExport",
  props: ["interaction"],
  components: {},

  data() {
    return {
      dialog: false,
      filePackaging: undefined,
      filePackagings: [
        { name: "las", label: "LAS file" },
        { name: "xyz", label: "XYZ file" },
        { name: "csv", label: "CSV file" },
      ],
      viewData: undefined,
      selectedAttributes: undefined,
      columnSeparator: undefined,
      columnSeparators: [
        { name: ",", label: "',' comma" },
        { name: ";", label: "';' semicolon" },
        { name: " ", label: "' ' space" },
        { name: "tab", label: "'   ' tabulator" },
      ],
      timeSlice: undefined,
    };
  },

  computed: {
    extent() {
      if (
        this.interaction === undefined ||
        this.interaction.type !== "PointcloudExport"
      ) {
        return undefined;
      } else {
        return this.interaction.extent;
      }
    },
    meta() {
      if (
        this.interaction === undefined ||
        this.interaction.type !== "PointcloudExport"
      ) {
        return undefined;
      } else {
        return this.interaction.meta;
      }
    },
    downloadUrl() {
      let s = this.$api.getUri() + "pointclouds/" + this.meta.name;
      if (this.filePackaging.name === "las") {
        s += "/points.las";
      } else if (this.filePackaging.name === "xyz") {
        s += "/points.xyz";
      } else if (this.filePackaging.name === "csv") {
        s += "/points.csv";
      } else {
        s += "/unknown";
      }
      s += "?ext=" + this.extent.join(",");
      if (this.timeSlice !== undefined) {
        s += "&time_slice_id=" + this.timeSlice.id;
      }
      if (
        this.filePackaging.name === "csv" &&
        this.selectedAttributes !== undefined &&
        this.selectedAttributes !== null &&
        this.selectedAttributes.length !== 0
      ) {
        s += "&columns=" + this.selectedAttributes.join(",");
      }
      if (this.filePackaging.name === "csv") {
        s += "&separator=" + this.columnSeparator.name;
      }
      s += "&raw_points";

      return s;
    },
    viewUrl() {
      let s = this.downloadUrl;
      s += "&limit=" + 1000;
      return s;
    },
  },

  methods: {
    open() {
      this.dialog = true;
    },
    onOk() {},
    async onView() {
      this.viewData = "Loading view data ...";
      try {
        let response = await this.$axios.get(this.viewUrl);
        this.viewData = response.data;
      } catch (e) {
        this.viewData = "Error loading view data.";
      }
    },
  },

  watch: {
    meta: {
      handler() {
        this.selectedAttributes = undefined;
        this.timeSlice =
          this.meta === undefined || this.meta.time_slices.length === 0
            ? undefined
            : this.meta.time_slices[0];
      },
      immediate: true,
    },
  },

  mounted() {
    this.filePackaging = this.filePackagings[0];
    this.columnSeparator = this.columnSeparators[0];
  },
});
</script>

<style scoped></style>
