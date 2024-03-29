import { defineStore } from "pinia";

export const useLayersStore = defineStore("layers", {
  state: () => ({
    rsdb_layers: [],
    lading: false,
    error: false,
    component: undefined,
    layers: [{ name: "atkis.merged_layers_alb", type: "postgis" }],
  }),
  getters: {},
  actions: {
    async init(component) {
      this.component = component;
      this.loading = true;
      this.error = false;
      try {
        const response = await this.component.$api.get("api/layers");
        this.rsdb_layers = response.data.layers;
        const collator = new Intl.Collator();
        this.rsdb_layers.sort((a, b) => {
          return collator.compare(a.name, b.name);
        });
        this.loading = false;
      } catch (e) {
        this.loading = false;
        this.error = true;
        console.log(e);
      }
    },
  },
});
