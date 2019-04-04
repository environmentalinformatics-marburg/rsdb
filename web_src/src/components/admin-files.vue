<template>
<div class="innergrid-container">
    <iframe class="innergrid-item-main" :src="urlPrefix + '../../files/'" frameborder="0" />
</div>



</template>

<script>

export default {
  name: 'admin-files',
  data() {
    return {
    }
  },
  methods: {
    refresh() {
    },
  },
  computed: {
    urlPrefix() {
        return this.$store.state.identity.urlPrefix;
    },
  },
  mounted() {
    this.refresh();
  },
}



</script>

<style scoped>
.innergrid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
}

.innergrid-item-main {
  width: 100%;
  height: 100%;
}
</style>
