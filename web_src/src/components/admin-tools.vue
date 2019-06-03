<template>
<div class="tools-grid-container">
<v-tabs color="grey" slider-color="yellow" show-arrows>
  <v-tab to="/tools/info">Info</v-tab>
  <v-tab to="/tools/status">Status</v-tab>
  <v-tab to="/tools/task">Task</v-tab>  
</v-tabs>
<router-view class="tools-grid-item-content" /> 
</div>
</template>

<script>

export default {
  name: 'admin-tools',
  components: {
  },
  data() {
    return {
    }
  },
  methods: {
  },
  computed: {
  },
  mounted() {
  },
}

</script>

<style scoped>

.tools-grid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: max-content minmax(100px, 1fr);
}

</style>
