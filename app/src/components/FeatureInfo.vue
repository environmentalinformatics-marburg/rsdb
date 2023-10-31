<template>
  <q-dialog v-model="dialog" maximized>
    <q-card>
      <q-bar>
        Feature info
        <q-space />
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-white text-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <q-card-section v-if="data !== undefined && data.features !== undefined">
        <div v-for="(feature, i) in data.features" :key="i">
          <table>
            <tbody>
              <template v-for="(value, key) in feature.properties" :key="key">
                <tr v-if="value !== null">
                  <td
                    style="
                      font-weight: bold;
                      background-color: rgb(233, 233, 233);
                    "
                  >
                    {{ key }}
                  </td>
                  <td>{{ value }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="Close" color="primary" v-close-popup @click="onOk" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "FeatureInfo",
  props: {},
  components: {},

  data() {
    return {
      dialog: false,
      data: undefined,
    };
  },

  computed: {},

  methods: {
    show(data) {
      this.data = data;
      this.dialog = true;
    },

    onOk() {
      this.$emit("close");
    },
  },

  mounted() {},
});
</script>
