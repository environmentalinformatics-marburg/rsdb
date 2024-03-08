<template>
  <div>
    <div>
      <q-select
        v-if="
          meta &&
          meta.details &&
          meta.details.attributes &&
          meta.details.attributes.length > 0
        "
        :options="meta.details.attributes"
        :model-value="layer.labelField"
        @update:model-value="onUpdateLabelField"
        label="Label field"
        outlined
        stack-label
        dense
        options-dense
        clearable
        :option-label="(v) => v"
      >
        <template v-slot:selected>
          <span v-if="layer.labelField">{{ layer.labelField }}</span>
          <span v-else class="text-italic text-grey">(none)</span>
        </template>
        <template v-slot:option="scope">
          <q-item v-bind="scope.itemProps">
            <q-item-section>
              <q-item-label v-if="scope.opt === layer.labelFieldDefault"
                >{{ scope.opt }}
                <span class="text-italic text-grey"
                  >(default)</span
                ></q-item-label
              >
              <q-item-label v-else>{{ scope.opt }}</q-item-label>
            </q-item-section>
          </q-item>
        </template>
      </q-select>
    </div>
    <div
      style="
        text-align: center;
        border: 1px solid rgba(0, 0, 0, 0.151);
        margin-top: 15px;
      "
    >
      <div style="font-size: 0.75em; color: grey">Legend</div>
      <img
        :src="layer.wmsURL + '?REQUEST=GetLegendGraphic'"
        style="margin-top: -10px"
      />
    </div>
  </div>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "VectorLayerSettings",
  props: ["layer"],
  components: {},

  data() {
    return { labelField: null };
  },

  computed: {
    meta() {
      return this.layer.meta;
    },
  },

  methods: {
    onUpdateLabelField(value) {
      this.$emit("changeLabelField", value);
      console.log(value);
    },
  },

  mounted() {},
});
</script>

<style scoped></style>
