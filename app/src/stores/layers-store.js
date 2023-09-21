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
      this.lading = true;
      this.error = false;
      try {
        const response = await this.component.$api.get("api/layers");
        this.rsdb_layers = response.data.layers;
        this.rsdb_layers.sort((a, b) => {
          return (a.name > b.name) - (a.name < b.name);
        });
        this.lading = false;
      } catch (e) {
        this.lading = false;
        this.error = true;
        Console.log(e);
      }
    },
  },
});
