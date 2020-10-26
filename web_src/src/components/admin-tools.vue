<template>
<div class="tools-grid-container">
<v-tabs color="grey lighten-1" slider-color="yellow" show-arrows class="sub-tab-bar">
  <v-tab to="/tools/info"><v-icon>view_compact</v-icon> Info</v-tab>
  <v-tab to="/tools/status"><v-icon>pending_actions</v-icon> Task Status</v-tab>
  <v-tab to="/tools/task"><v-icon>card_travel</v-icon> Task Creation</v-tab>  
  <v-tab to="/tools/accounts" v-if="isAdmin"><v-icon>contact_mail</v-icon> Accounts</v-tab>
  <!--<v-tab to="/tools/vectorviewer"><v-icon>map</v-icon> vectorviewer</v-tab>-->
</v-tabs>
<router-view class="tools-grid-item-content" /> 
</div>
</template>

<script>

import { mapGetters } from 'vuex'

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
    ...mapGetters({
      isAdmin: 'identity/isAdmin',
    }),
  },
  mounted() {
  },
}

</script>

<style scoped>

.sub-tab-bar {
  border-bottom-color: #5e666b;
  border-bottom-width: 1px;
  border-bottom-style: solid;
}

.tools-grid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: max-content minmax(100px, 1fr);
}

</style>
