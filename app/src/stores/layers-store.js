import { defineStore } from "pinia";

export const useLayersStore = defineStore("layers", {
  state: () => ({
    postgis: [],
    lading: false,
    error: false,
    component: undefined,
    layers: [{ name: "atkis.merged_layers_alb", type: "postgis" }],
  }),
  getters: {},
  actions: {
    async init(component) {
      this.component = component;
      this.lading = true;
      this.error = false;
      try {
        const response = await this.component.$api.get("postgis/layers");
        this.postgis = response.data.postgis_layers;
        this.lading = false;
      } catch (e) {
        this.lading = false;
        this.error = true;
        Console.log(e);
      }
    },
  },
});
