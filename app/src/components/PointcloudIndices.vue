<template>
  <q-dialog v-model="dialog" maximized>
    <q-card>
      <q-bar>
        Pointcloud indices processing of &nbsp;<b>{{ meta.name }}</b>
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
      <q-card-section v-if="functions !== undefined">
        <q-table
          :rows="functions"
          :columns="columns"
          :pagination="pagination"
          dense
          :loading="functionsMessage !== undefined"
          row-key="name"
          binary-state-sort
          :filter="filter"
          selection="multiple"
          v-model:selected="selectedFunctions"
          :selected-rows-label="getSelectedFunctionsText"
          table-header-class="table-header"
          class="indices-table"
          hide-pagination
          title="Pointcloud indices"
        >
          <template v-slot:top-right>
            <q-input
              debounce="300"
              v-model="filter"
              stack-label
              label="Search"
              filled
              clearable
              dense
            >
              <template v-slot:append>
                <q-icon name="search" />
              </template>
            </q-input> </template
        ></q-table>
      </q-card-section>

      <q-card-section>
        <q-table
          :rows="processingTable"
          :columns="processingTableColumns"
          :pagination="processingTablePagination"
          dense
          :loading="processingMessage !== undefined"
          row-key="name"
          binary-state-sort
          hide-pagination
          table-header-class="table-header"
          class="processing-table"
          no-data-label="No indices selected."
          title="Processing result"
        >
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="Process"
          color="primary"
          @click="onOk"
          :loading="processingMessage !== undefined"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "PointcloudIndices",
  props: ["interaction"],
  components: {},

  data() {
    return {
      dialog: false,
      timeSlice: undefined,
      functionsMessage: undefined,
      functions: undefined,
      selectedFunctions: [],
      filter: undefined,

      columns: [
        {
          name: "name",
          field: "name",
          label: "Index",
          headerStyle: "text-align: center; border-right: 1px solid #c6c6c6;",
          align: "left",
          sortable: true,
          classes: "q-table--col-auto-width name-column",
        },
        {
          name: "description",
          field: "description",
          label: "Description",
          headerStyle:
            "text-align: center; min-width: 150px; max-width: 150px;",
          align: "left",
          sortable: true,
          classes: "description-column",
        },
      ],
      pagination: {
        rowsPerPage: 0,
        sortBy: "name",
      },
      processing: undefined,
      processingMessage: undefined,
      processingTableColumns: [
        {
          name: "name",
          field: "name",
          label: "Index",
          headerStyle: "text-align: left; border-right: 1px solid #c6c6c6;",
          align: "left",
          sortable: true,
          classes: "q-table--col-auto-width name-column",
        },
        {
          name: "value",
          field: "value",
          label: "Value",
          headerStyle: "text-align: right; border-right: 1px solid #c6c6c6;",
          align: "left",
          sortable: true,
          classes: "value-column",
        },
      ],
      processingTablePagination: { rowsPerPage: 0, sortBy: "name" },
    };
  },

  computed: {
    extent() {
      if (
        this.interaction === undefined ||
        this.interaction.type !== "PointcloudIndices"
      ) {
        return undefined;
      } else {
        return this.interaction.extent;
      }
    },
    meta() {
      if (
        this.interaction === undefined ||
        this.interaction.type !== "PointcloudIndices"
      ) {
        return undefined;
      } else {
        return this.interaction.meta;
      }
    },
    processingTable() {
      if (this.processing === undefined) {
        if (!this.selectedFunctions) {
          return [];
        }
        return this.selectedFunctions.map((f, i) => {
          return {
            name: f.name,
            value: "To be processed.",
          };
        });
      }
      return this.processing.header.map((name, i) => {
        const p = this.processing.data["1"][i];
        const v = typeof p === "number" ? p.toFixed(3) : p;
        return { name: name, value: v };
      });
    },
  },

  methods: {
    open() {
      this.dialog = true;
    },
    getSelectedFunctionsText() {
      return this.selectedFunctions.length === 0
        ? `0 indices selected of ${this.functions.length}`
        : `${this.selectedFunctions.length} ${
            this.selectedFunctions.length > 1 ? "indices" : "index"
          } selected of ${this.functions.length}`;
    },
    async onOk() {
      this.processingMessage = "Processing indices ...";
      try {
        const urlPart = "pointclouds/" + this.meta.name + "/indices.json";
        let data = { omit_empty_areas: false };
        if (this.timeSlice) {
          data.time_slice_id = this.timeSlice.id;
        }
        let area = { bbox: this.extent };
        data.areas = [area];
        data.functions = this.selectedFunctions.map((f) => f.name);
        let response = await this.$api.post(urlPart, data);
        this.processing = response.data;
        this.processingMessage = undefined;
      } catch (e) {
        this.processingMessage = "Error processing indices.";
      }
    },
  },

  watch: {
    meta: {
      handler() {
        this.timeSlice =
          this.meta === undefined || this.meta.time_slices.length === 0
            ? undefined
            : this.meta.time_slices[0];
      },
      immediate: true,
    },
    selectedFunctions() {
      this.processing = undefined;
    },
  },

  async mounted() {
    this.functionsMessage = "Loading indices list ...";
    try {
      let response = await this.$api.get("pointdb/process_functions");
      this.functions = response.data.functions;
      this.functionsMessage = undefined;
    } catch (e) {
      this.functionsMessage = "Error loading indices list.";
    }
  },
});
</script>

<style>
.indices-table {
  max-height: 375px;
}

.indices-table thead tr:first-child th {
  background-color: #aaaaaa;
}

.indices-table thead tr th {
  position: sticky;
  z-index: 1;
}

.indices-table thead tr:first-child th {
  top: 0;
}

.table-header th {
  background-color: #0000004d;
  border-top: 1px solid #eaeaea;
}

td.name-column {
  border-right: 1px solid #c6c6c6;
}

td.description-column {
  border-right: 1px solid #c6c6c6;
  color: grey;
  min-width: 300px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-style: italic;
}

.processing-table {
  max-height: 375px;
  max-width: 375px;
}

.processing-table thead tr:first-child th {
  background-color: #aaaaaa;
}

.processing-table thead tr th {
  position: sticky;
  z-index: 1;
}

.processing-table thead tr:first-child th {
  top: 0;
}

td.value-column {
  border-right: 1px solid #c6c6c6;
  color: rgb(59, 59, 59);
  overflow: hidden;
  text-overflow: ellipsis;
  font-style: italic;
  text-align: right;
  font-family: monospace;
}
</style>
