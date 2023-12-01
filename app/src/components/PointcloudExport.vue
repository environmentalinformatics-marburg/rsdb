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

      <q-card-actions align="right">
        <q-btn
          flat
          label="View file content"
          color="primary"
          @click="onView"
          v-if="filePackaging.name === 'xyz'"
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
      ],
      viewData: undefined,
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
      } else {
        s += "/unknown";
      }
      s += "?ext=" + this.extent.join(",");
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
      handler() {},
      immediate: true,
    },
  },

  mounted() {
    this.filePackaging = this.filePackagings[0];
  },
});
</script>

<style scoped></style>
