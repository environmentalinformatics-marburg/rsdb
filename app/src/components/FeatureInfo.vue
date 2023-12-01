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

      <template v-if="infos !== undefined">
        <template v-for="(info, i) in infos" :key="i">
          <q-card-section v-if="info.data.features !== undefined">
            Layer: {{ info.name }}
            <div v-for="(feature, i) in info.data.features" :key="i">
              <table>
                <tbody>
                  <template
                    v-for="(value, key) in feature.properties"
                    :key="key"
                  >
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
              &nbsp;
            </div>
          </q-card-section>
          <q-card-section v-else style="white-space: pre">
            {{ info.data }}
          </q-card-section>
          <hr />
        </template>
      </template>

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
      infos: undefined,
    };
  },

  computed: {},

  methods: {
    show(infos) {
      this.infos = infos;
      this.dialog = true;
    },

    onOk() {
      this.$emit("close");
    },
  },

  mounted() {},
});
</script>
